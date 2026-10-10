package com.reza.khodro45monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

data class CatalogModel(val id: Long, val title: String)
data class CatalogBrand(val id: Long, val title: String, val models: List<CatalogModel>)
data class CatalogCity(val id: Long, val title: String)
data class FilterCatalog(val brands: List<CatalogBrand>, val cities: List<CatalogCity>)

/** Dynamic filter metadata from the verified dealer endpoints; no lists are hard-coded. */
class FilterCatalogClient {
    private val http = OkHttpClient()
    suspend fun load(): FilterCatalog = withContext(Dispatchers.IO) {
        val token = Khodro45Session.token() ?: throw ApiException("ابتدا از بخش «ورود به خودرو45» وارد حساب شوید.", 401)
        fun array(path: String): JsonArray {
            val request = Request.Builder().url("https://khodro45.com/api$path")
                .header("Accept", "application/json").header("Authorization", "Token $token").build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException("دریافت فیلترها ناموفق بود: HTTP ${response.code}", response.code)
                return Json.parseToJsonElement(response.body?.string().orEmpty()).jsonArray
            }
        }
        val brands = array("/v1/brands/collective-brand-model/").mapNotNull { value ->
            val obj = value.jsonObject
            val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val models = (obj["prefetched_models"] as? JsonArray).orEmpty().mapNotNull { model ->
                val modelObj = model.jsonObject
                val modelId = modelObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                val modelTitle = modelObj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                CatalogModel(modelId, modelTitle)
            }
            CatalogBrand(id, title, models)
        }.sortedBy { it.title }
        val cities = array("/v1/city/").mapNotNull { value ->
            val obj = value.jsonObject
            val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            CatalogCity(id, title)
        }.sortedBy { it.title }
        FilterCatalog(brands, cities)
    }
}
