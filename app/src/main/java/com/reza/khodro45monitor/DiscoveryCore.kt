package com.reza.khodro45monitor

import org.json.JSONArray
import org.json.JSONObject

data class DiscoverySummary(val topKeys:List<String>,val sampleKeys:List<String>,val arraySize:Int,val pagination:List<String>)
data class DiscoveryCandidate(val url:String,val score:Int,val reasons:List<String>,val summary:DiscoverySummary)
object DiscoveryRedactor {
 private val secret=Regex("password|passwd|cookie|authorization|token|secret|session|csrf|jwt|bearer|phone|mobile|email|national|credential",RegexOption.IGNORE_CASE)
 fun redact(value:Any?):Any?=when(value){is JSONObject->{val out=JSONObject();value.keys().forEach{k->out.put(k,if(secret.containsMatchIn(k))"[REDACTED]" else redact(value.opt(k)))};out};is JSONArray->JSONArray().apply{for(i in 0 until value.length())put(redact(value.opt(i)))};else->value}
}
object DiscoveryReportFactory {
 fun json(records: List<String>): String {
  val captures=JSONArray(); records.take(150).forEach { raw -> runCatching { captures.put(JSONObject(raw)) } }
  val report=JSONObject().put("version","1.1.0").put("listingUrl",FilterUrl.LISTING).put("captures",captures)
  return (DiscoveryRedactor.redact(report) as JSONObject).toString(2)
 }
}
object VehicleApiCandidateScorer {
 private fun names(o: JSONObject): List<String> { val result=mutableListOf<String>(); val iterator=o.keys(); while(iterator.hasNext()) result += iterator.next(); return result }
 fun score(url:String,body:String):DiscoveryCandidate { val root=runCatching{JSONObject(body)}.getOrElse{return DiscoveryCandidate(url,0,listOf("JSON object نیست"),DiscoverySummary(emptyList(),emptyList(),0,emptyList()))}; val keys=names(root); val listKey=listOf("results","items","data").firstOrNull{root.opt(it) is JSONArray}; val arr=listKey?.let{root.optJSONArray(it)}; val fields=arr?.optJSONObject(0)?.let{names(it)}?:keys; val signals=listOf("id","last_price","start_price","fair_market_price","end_time","finished","step","inspection","trimyear"); val found=signals.filter{it in fields}; val score=(found.size*10+(if((arr?.length()?:0)>1)20 else 0)+(if(keys.any{it in listOf("count","next","pagination","meta")})10 else 0)).coerceAtMost(100); return DiscoveryCandidate(url,score,found.map{"فیلد $it"},DiscoverySummary(keys,fields,arr?.length()?:0,keys.filter{it in listOf("count","next","previous","pagination","meta")})) }
}
