package com.reza.khodro45monitor
import android.app.*
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class MonitorService:Service(){
 companion object { const val ACTION_PAUSE="com.reza.khodro45monitor.PAUSE"; const val ACTION_RESUME="com.reza.khodro45monitor.RESUME" }
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO); private var job:Job?=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(intent?.action==ACTION_PAUSE){ stopSelf(); return START_NOT_STICKY }
  createChannel(); startForeground(10,status("مانیتور خودرو45 فعال است"))
  if(job==null) job=scope.launch {
   val settings=Settings(this@MonitorService); val engine=MonitorEngine(this@MonitorService)
   while(isActive) {
    try { engine.scan(Khodro45Client()){v,p->notifyVehicle(v,p)}
    } catch(e:ApiException) { record("error",e.message?:"خطای اتصال خودرو45"); if(e.status==401) updateStatus("نیاز به ورود مجدد به خودرو45")
    } catch(e:Exception) { record("error","خطای شبکه؛ بررسی دوباره انجام می‌شود") }
    delay(settings.interval()*1000L)
   }
  }; return START_STICKY
 }
 override fun onBind(intent:Intent?):IBinder?=null; override fun onDestroy(){job?.cancel();scope.cancel();super.onDestroy()}
 private fun createChannel(){ val c=NotificationChannel("monitor","پایش خودرو45",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager::class.java).createNotificationChannel(c); getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("vehicles","خودروهای جدید",NotificationManager.IMPORTANCE_HIGH)) }
 private fun status(text:String)=NotificationCompat.Builder(this,"monitor").setSmallIcon(R.drawable.app_icon_foreground).setContentTitle("مانیتور خودرو45 فعال است").setContentText(text).setOngoing(true).addAction(android.R.drawable.ic_media_pause,"توقف موقت",PendingIntent.getService(this,1,Intent(this,MonitorService::class.java).setAction(ACTION_PAUSE),PendingIntent.FLAG_IMMUTABLE)).build()
 private fun updateStatus(text:String){ getSystemService(NotificationManager::class.java).notify(10,status(text)) }
 private suspend fun record(type:String,message:String){ androidx.room.Room.databaseBuilder(this,MonitorDatabase::class.java,"khodro45.db").build().dao().event(MonitorEvent(type=type,message=message)) }
 private fun notifyVehicle(v:Vehicle,p:FilterProfile){ val i=Intent(Intent.ACTION_VIEW,android.net.Uri.parse(v.url)); val pi=PendingIntent.getActivity(this,v.id.hashCode(),i,PendingIntent.FLAG_IMMUTABLE); getSystemService(NotificationManager::class.java).notify(v.id.hashCode(),NotificationCompat.Builder(this,"vehicles").setSmallIcon(R.drawable.app_icon_foreground).setContentTitle("خودروی جدید پیدا شد").setContentText("${listOfNotNull(v.brand,v.model,v.trim).joinToString(" ")} — ${Persian.toman(v.currentPrice)}").setStyle(NotificationCompat.BigTextStyle().bigText("پروفایل: ${p.name}\nقیمت: ${Persian.toman(v.currentPrice)}")).setContentIntent(pi).setAutoCancel(true).build()) }
}
class BootReceiver:BroadcastReceiver(){ override fun onReceive(context:Context,intent:Intent){ if(intent.action==Intent.ACTION_BOOT_COMPLETED) goAsync().also { pending-> CoroutineScope(Dispatchers.IO).launch { if(Settings(context).boot()) context.startForegroundService(Intent(context,MonitorService::class.java)); pending.finish() } } } }
