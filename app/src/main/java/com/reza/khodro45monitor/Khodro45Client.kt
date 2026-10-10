package com.reza.khodro45monitor

import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit

/** Production client for the listing route verified in the dealer web application's public bundle. */
class Khodro45Client {
 private val http=OkHttpClient.Builder().callTimeout(25,TimeUnit.SECONDS).build()
 companion object { const val LISTING_ENDPOINT = "https://khodro45.com/api/v1/auctions/" }
 suspend fun list(query:String, nextUrl:String?=null):PageResult {
  val token=Khodro45Session.token() ?: throw ApiException("ابتدا از بخش «ورود به خودرو45» وارد حساب شوید.",401)
  val url=if(nextUrl == null) initialUrl(query) else verifiedNextUrl(nextUrl)
  val request=Request.Builder().url(url).header("Accept","application/json").header("Authorization","Token $token").build()
  http.newCall(request).execute().use { response->
   if(!response.isSuccessful) throw ApiException(if(response.code==401) "ورود خودرو45 منقضی شده است." else "HTTP ${response.code}",response.code)
   return parse(response.body?.string() ?: throw ApiException("بدون محتوای پاسخ"))
  }
 }
 private fun initialUrl(query:String): HttpUrl {
  val base=LISTING_ENDPOINT.toHttpUrl().newBuilder()
  if(query.isNotBlank()) android.net.Uri.parse("https://filters.invalid/?$query").queryParameterNames.forEach { key ->
   android.net.Uri.parse("https://filters.invalid/?$query").getQueryParameters(key).forEach { base.addQueryParameter(key,it) }
  }
  return base.build()
 }
 private fun verifiedNextUrl(value:String): HttpUrl {
  val url=value.toHttpUrl(); if(url.host!="khodro45.com" || !url.encodedPath.startsWith("/api/v1/auctions")) throw ApiException("صفحه‌بندی نامعتبر است")
  return url
 }
 private fun parse(raw:String):PageResult {
  val root=Json.parseToJsonElement(raw)
  val items=when(root) { is JsonArray -> root; is JsonObject -> (root["results"]?:root["data"]?:root["items"]?:JsonArray(emptyList())).jsonArray; else -> JsonArray(emptyList()) }
  val next=(root as? JsonObject)?.get("next")?.jsonPrimitive?.contentOrNull
  return PageResult(items.mapNotNull { element -> (element as? JsonObject)?.let { normalize(it) } },next)
 }
 private fun normalize(o: JsonObject): Vehicle? {
  fun string(vararg keys: String): String? = keys.firstNotNullOfOrNull { key -> o[key]?.jsonPrimitive?.contentOrNull }
  fun number(vararg keys: String): Long? = Persian.parseNumeric(string(*keys))
  val id = string("id", "uuid", "vehicle_id") ?: return null
  val ty = o["trimyear"]?.jsonObject
  val trim = ty?.get("trim")?.jsonObject
  val model = trim?.get("model")?.jsonObject
  val brand = model?.get("brand")?.jsonObject
  val sanitized = DiscoveryRedactor.redact(org.json.JSONObject(o.toString())) as org.json.JSONObject
  return Vehicle(id=id,brand=brand?.get("title")?.jsonPrimitive?.contentOrNull?:string("brand"),model=model?.get("title")?.jsonPrimitive?.contentOrNull?:string("model"),trim=trim?.get("title")?.jsonPrimitive?.contentOrNull?:string("trim"),year=ty?.get("year")?.jsonObject?.get("title")?.jsonPrimitive?.contentOrNull?:string("year"),mileage=number("mileage","kilometer"),startPrice=number("start_price"),currentPrice=number("last_price","current_price","price"),fairMarketPrice=number("fair_market_price"),step=number("step"),endTime=string("end_time"),finished=o["finished"]?.jsonPrimitive?.booleanOrNull?:false,inspection=string("inspection"),url="${FilterUrl.LISTING}/$id",imageUrl=string("image","image_url"),rawJson=sanitized.toString())
 }
}
data class PageResult(val items:List<Vehicle>,val nextUrl:String?); class ApiException(message:String,val status:Int?=null):Exception(message)
