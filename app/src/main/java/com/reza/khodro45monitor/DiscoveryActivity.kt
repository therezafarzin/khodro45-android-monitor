package com.reza.khodro45monitor

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.webkit.*
import android.widget.*
import androidx.webkit.WebViewCompat
import org.json.JSONObject

class DiscoveryActivity: Activity() {
 private val records=mutableListOf<String>(); private lateinit var status:TextView; private lateinit var candidates:LinearLayout
 @SuppressLint("SetJavaScriptEnabled") override fun onCreate(s:Bundle?){super.onCreate(s); val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; status=TextView(this).apply{setPadding(20,20,20,20)}; val export=Button(this).apply{text="خروجی گزارش کشف API";setOnClickListener{startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE,"khodro45-discovery.json"),77)}};val clear=Button(this).apply{text="پاک کردن نتایج قبلی";setOnClickListener{records.clear();render()}};candidates=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val scroll=ScrollView(this).apply{addView(candidates)}; val web=WebView(this); root.addView(status);root.addView(export);root.addView(clear);root.addView(scroll,LinearLayout.LayoutParams(-1,240));root.addView(web,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root);render()
  web.settings.javaScriptEnabled=true; web.settings.domStorageEnabled=true; CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web,true)
  web.addJavascriptInterface(Bridge(),"K45Discovery")
  val hook="""(()=>{if(window.__k45)return;window.__k45=1;const send=(x)=>{try{K45Discovery.capture(JSON.stringify(x))}catch(e){}};const ok=u=>{try{return new URL(u,location.href).hostname.endsWith('khodro45.com')}catch(e){return false}};const f=window.fetch;window.fetch=async function(a,b={}){let r=await f.apply(this,arguments);let u=typeof a==='string'?a:a.url;if(ok(u)){let c=r.clone();let t=(r.headers.get('content-type')||'');if(t.includes('json'))c.text().then(x=>send({url:u,method:b.method||'GET',status:r.status,type:t,body:x.slice(0,20000)}))}return r};const o=XMLHttpRequest.prototype.open,s=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.open=function(m,u){this.__m=m;this.__u=u;return o.apply(this,arguments)};XMLHttpRequest.prototype.send=function(){this.addEventListener('load',()=>{let t=this.getResponseHeader('content-type')||'';if(ok(this.__u)&&t.includes('json'))send({url:this.__u,method:this.__m,status:this.status,type:t,body:String(this.responseText||'').slice(0,20000)})});return s.apply(this,arguments)}})();"""
  WebViewCompat.addDocumentStartJavaScript(web,hook,setOf("https://dealer.khodro45.com"))
  web.webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(v:WebView?,r:WebResourceRequest?)=!trusted(r?.url?.host); override fun shouldInterceptRequest(v:WebView?,r:WebResourceRequest?):WebResourceResponse?{r?.url?.takeIf{trusted(it.host)}?.let{record(JSONObject().put("kind","resource").put("url",it.toString()))};return super.shouldInterceptRequest(v,r)}}
  web.loadUrl(FilterUrl.LISTING)
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==77&&resultCode==RESULT_OK){runCatching{contentResolver.openOutputStream(data?.data?:error("uri"))?.use{it.write(DiscoveryReportFactory.json(records).toByteArray(Charsets.UTF_8))}?:error("stream")}.onSuccess{Toast.makeText(this,"گزارش کشف API ذخیره شد",Toast.LENGTH_LONG).show()}.onFailure{Toast.makeText(this,"ذخیره گزارش ناموفق بود",Toast.LENGTH_LONG).show()}}}
 private fun trusted(h:String?)=h=="dealer.khodro45.com" || h?.endsWith(".khodro45.com")==true
 private fun record(o:JSONObject){records.add(o.toString());runOnUiThread{render()}}
 private fun render(){val list=records.mapNotNull{raw->runCatching{JSONObject(raw)}.getOrNull()}.mapNotNull{o->val payload=o.optString("payload");if(payload.isBlank())null else VehicleApiCandidateScorer.score(JSONObject(payload).optString("url"),JSONObject(payload).optString("body"))}.sortedByDescending{it.score};status.text="وضعیت ورود: صفحه خودرو45\nتعداد درخواست‌های ثبت‌شده: ${records.size}\nتعداد پاسخ‌های JSON: ${list.size}\nتعداد APIهای احتمالی: ${list.count{it.score>20}}";candidates.removeAllViews();list.filter{it.score>0}.forEach{c->val b=Button(this);b.text="${c.score}%  ${c.url}\nآیتم: ${c.summary.arraySize} | ${c.summary.sampleKeys.joinToString()}\n${c.reasons.joinToString()}";b.setOnClickListener{AlertDialog.Builder(this).setTitle("جزئیات API احتمالی").setMessage(b.text).setPositiveButton("استفاده از این API"){_,_->getSharedPreferences("discovery",0).edit().putString("candidate_url",c.url).putInt("candidate_score",c.score).apply();Toast.makeText(this,"گزارش انتخاب ذخیره شد؛ مسیر تولیدیِ مانیتور تغییر نمی‌کند",Toast.LENGTH_LONG).show()}.show()};candidates.addView(b)}}
 inner class Bridge{@JavascriptInterface fun capture(payload:String){if(payload.length<200_000) record(JSONObject().put("kind","json").put("payload",payload.take(20_000)))}}
}
