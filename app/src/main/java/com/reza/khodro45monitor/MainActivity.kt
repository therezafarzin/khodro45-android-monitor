package com.reza.khodro45monitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF006C7A))) { Dashboard() } }
    }
    override fun onResume() { super.onResume(); refreshTick++ }

    @Composable
    private fun Dashboard() {
        refreshTick // refresh login state after returning from the single persistent login WebView.
        val context = this
        val scope = rememberCoroutineScope()
        val db = remember { Room.databaseBuilder(context, MonitorDatabase::class.java, "khodro45.db").build() }
        val settings = remember { Settings(context) }
        var profiles by remember { mutableStateOf(emptyList<FilterProfile>()) }
        var vehicles by remember { mutableStateOf(emptyList<Vehicle>()) }
        var lastEvent by remember { mutableStateOf<MonitorEvent?>(null) }
        var interval by remember { mutableIntStateOf(30) }
        var url by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("آمادهٔ راه‌اندازی") }
        var catalog by remember { mutableStateOf<FilterCatalog?>(null) }
        var catalogLoading by remember { mutableStateOf(false) }
        var selectedBrand by remember { mutableStateOf<CatalogBrand?>(null) }
        var selectedModel by remember { mutableStateOf<CatalogModel?>(null) }
        var selectedCity by remember { mutableStateOf<CatalogCity?>(null) }
        var brandPicker by remember { mutableStateOf(false) }
        var modelPicker by remember { mutableStateOf(false) }
        var cityPicker by remember { mutableStateOf(false) }
        fun reload() = scope.launch {
            profiles = db.dao().profiles(); vehicles = db.dao().vehicles(); lastEvent = db.dao().events().firstOrNull(); interval = settings.interval()
        }
        LaunchedEffect(Unit) { profiles=db.dao().profiles(); vehicles=db.dao().vehicles(); lastEvent=db.dao().events().firstOrNull(); interval=settings.interval() }
        val loggedIn = Khodro45Session.isLoggedIn()
        fun refreshCatalog() = scope.launch {
            if (!loggedIn) { message = "ابتدا وارد خودرو45 شوید"; return@launch }
            catalogLoading = true
            try { catalog = FilterCatalogClient().load(); message = "فیلترهای خودرو45 بروزرسانی شد" }
            catch (error: Exception) { message = error.message ?: "دریافت فیلترهای خودرو45 ناموفق بود" }
            finally { catalogLoading = false }
        }
        LaunchedEffect(loggedIn) { if (loggedIn && catalog == null) refreshCatalog() }
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("مانیتور خودرو45", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("پایش خودروهای واقعیِ خودرو45", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (loggedIn) "وضعیت ورود: متصل به خودرو45" else "وضعیت ورود: نیاز به ورود", style = MaterialTheme.typography.titleMedium)
                    Text("API: ${if (loggedIn) "آمادهٔ بررسی" else "پس از ورود فعال می‌شود"}")
                    Text("آخرین وضعیت: ${lastEvent?.message ?: "هنوز بررسی انجام نشده"}")
                    Text("بازهٔ بررسی: هر $interval ثانیه")
                } }
                Button(onClick = { startActivity(Intent(context, LoginActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text(if (loggedIn) "مدیریت ورود خودرو45" else "ورود به خودرو45") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        if (!loggedIn) { message="ابتدا وارد خودرو45 شوید"; startActivity(Intent(context, LoginActivity::class.java)); return@Button }
                        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        startForegroundService(Intent(context, MonitorService::class.java)); message="مانیتورینگ آغاز شد"
                    }, modifier = Modifier.weight(1f)) { Text("شروع مانیتور") }
                    OutlinedButton(onClick = { stopService(Intent(context, MonitorService::class.java)); message="مانیتور متوقف شد" }, modifier = Modifier.weight(1f)) { Text("توقف") }
                }
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("فیلترها", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { refreshCatalog() }, enabled = loggedIn && !catalogLoading, modifier = Modifier.fillMaxWidth()) { Text(if (catalogLoading) "در حال دریافت فیلترها…" else "بروزرسانی فیلترهای خودرو45") }
                    Text(catalog?.let { "${it.brands.size} برند و ${it.cities.size} شهر از خودرو45 دریافت شد" } ?: "برای دریافت خودکار برند، مدل و شهر وارد خودرو45 شوید.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { brandPicker = true }, enabled = !catalogLoading && catalog != null, modifier = Modifier.weight(1f)) { Text(selectedBrand?.title ?: "انتخاب برند") }
                        OutlinedButton(onClick = { modelPicker = true }, enabled = selectedBrand?.models?.isNotEmpty() == true, modifier = Modifier.weight(1f)) { Text(selectedModel?.title ?: "انتخاب مدل") }
                    }
                    OutlinedButton(onClick = { cityPicker = true }, enabled = !catalogLoading && catalog != null, modifier = Modifier.fillMaxWidth()) { Text(selectedCity?.title ?: "انتخاب شهر") }
                    Button(onClick = {
                        val query = listOfNotNull(selectedBrand?.let { "brand=${it.id}" }, selectedModel?.let { "model=${it.id}" }, selectedCity?.let { "city=${it.id}" }).joinToString("&")
                        if (query.isBlank()) message = "حداقل یک برند، مدل یا شهر انتخاب کنید" else scope.launch {
                            val title = listOfNotNull(selectedBrand?.title, selectedModel?.title, selectedCity?.title).joinToString(" — ")
                            db.dao().insertProfile(FilterProfile(name = title, query = query, ordering = "suggested")); message = "فیلتر انتخاب‌شده ذخیره شد"; reload()
                        }
                    }, enabled = selectedBrand != null || selectedModel != null || selectedCity != null, modifier = Modifier.fillMaxWidth()) { Text("ساخت فیلتر از انتخاب‌ها") }
                    OutlinedTextField(value=url, onValueChange={url=it}, label={Text("وارد کردن فیلتر از لینک خودرو45")}, modifier=Modifier.fillMaxWidth(), singleLine=true)
                    Button(onClick = {
                        val imported=FilterUrl.import(url)
                        if(imported==null) message="لینک خودرو45 معتبر نیست" else scope.launch { db.dao().insertProfile(FilterProfile(name=imported.name,query=imported.query,ordering=imported.ordering)); message="فیلتر ذخیره شد"; reload() }
                    }, modifier=Modifier.fillMaxWidth()) { Text("وارد کردن فیلتر") }
                    profiles.forEach { profile -> Text("• ${profile.name}: ${FilterUrl.export(profile)}", style=MaterialTheme.typography.bodySmall) }
                } }
                Card { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("تنظیمات پایش", style=MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf(10,15,30,45,60,120,300,600,900,1800).take(5).forEach { seconds -> FilterChip(selected=interval==seconds,onClick={scope.launch{settings.setInterval(seconds);interval=seconds}},label={Text("$seconds ث")}) } }
                    Text("برای اجرای مطمئن‌تر در Samsung: Battery → Unrestricted و اجازهٔ اعلان را فعال کنید. اجرای پس‌زمینه همچنان تابع محدودیت‌های Android است.", style=MaterialTheme.typography.bodySmall)
                } }
                Card { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("خودروهای پیدا شده: ${vehicles.size}", style=MaterialTheme.typography.titleLarge)
                    vehicles.take(10).forEach { v -> Text("${listOfNotNull(v.brand,v.model,v.trim,v.year).joinToString(" ")} — ${Persian.toman(v.currentPrice)}") }
                    if(vehicles.isEmpty()) Text("در اولین بررسی، خودروهای فعلی به‌عنوان مبنا ذخیره می‌شوند و اعلان نمی‌گیرند.", style=MaterialTheme.typography.bodySmall)
                } }
                OutlinedButton(onClick={startActivity(Intent(context,DiscoveryActivity::class.java))},modifier=Modifier.fillMaxWidth()) { Text("گزارش و تشخیص اتصال API") }
                Text(message, modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.Center)
            }
        }
        if (brandPicker && catalog != null) CatalogPicker("انتخاب برند", catalog!!.brands, { it.title }, { selectedBrand = it; selectedModel = null; brandPicker = false }) { brandPicker = false }
        if (modelPicker && selectedBrand != null) CatalogPicker("انتخاب مدل", selectedBrand!!.models, { it.title }, { selectedModel = it; modelPicker = false }) { modelPicker = false }
        if (cityPicker && catalog != null) CatalogPicker("انتخاب شهر", catalog!!.cities, { it.title }, { selectedCity = it; cityPicker = false }) { cityPicker = false }
    }
}

@Composable
private fun <T> CatalogPicker(title: String, items: List<T>, label: (T) -> String, select: (T) -> Unit, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            items.forEach { item -> TextButton(onClick = { select(item) }, modifier = Modifier.fillMaxWidth()) { Text(label(item), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) } }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("بستن") } })
}
