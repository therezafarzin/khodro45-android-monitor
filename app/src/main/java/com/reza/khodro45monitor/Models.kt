package com.reza.khodro45monitor

import androidx.room.*
import kotlinx.serialization.json.*
import java.text.NumberFormat
import java.util.Locale

@Entity(tableName="profiles") data class FilterProfile(
 @PrimaryKey(autoGenerate=true) val id:Long=0, val name:String, val enabled:Boolean=true,
 val query:String="", val ordering:String="", val intervalSeconds:Int?=null, val notifyNew:Boolean=true,
 val notifyPrice:Boolean=false, val notifyEnding:Boolean=false, val createdAt:Long=System.currentTimeMillis())
@Entity(tableName="vehicles") data class Vehicle(
 @PrimaryKey val id:String, val brand:String?=null, val model:String?=null, val trim:String?=null,
 val year:String?=null, val mileage:Long?=null, val startPrice:Long?=null, val currentPrice:Long?=null,
 val fairMarketPrice:Long?=null, val step:Long?=null, val endTime:String?=null, val finished:Boolean=false,
 val inspection:String?=null, val url:String, val imageUrl:String?=null, val rawJson:String,
 val firstSeen:Long=System.currentTimeMillis(), val lastSeen:Long=System.currentTimeMillis(), val ignored:Boolean=false, val favorite:Boolean=false)
@Entity(tableName="snapshots", indices=[Index("vehicleId")]) data class Snapshot(@PrimaryKey(autoGenerate=true) val id:Long=0, val vehicleId:String, val createdAt:Long=System.currentTimeMillis(), val price:Long?, val endTime:String?, val finished:Boolean, val fingerprint:String)
@Entity(tableName="matches", primaryKeys=["vehicleId","profileId"]) data class Match(val vehicleId:String,val profileId:Long,val seenAt:Long=System.currentTimeMillis())
@Entity(tableName="events") data class MonitorEvent(@PrimaryKey(autoGenerate=true) val id:Long=0,val at:Long=System.currentTimeMillis(),val type:String,val message:String)

@Dao interface MonitorDao {
 @Query("SELECT * FROM profiles ORDER BY id DESC") suspend fun profiles():List<FilterProfile>; @Insert suspend fun insertProfile(p:FilterProfile):Long; @Delete suspend fun deleteProfile(p:FilterProfile)
 @Query("SELECT * FROM vehicles ORDER BY lastSeen DESC") suspend fun vehicles():List<Vehicle>; @Query("SELECT * FROM vehicles WHERE id=:id") suspend fun vehicle(id:String):Vehicle?; @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(v:Vehicle); @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun match(m:Match)
 @Insert suspend fun snapshot(s:Snapshot); @Insert suspend fun event(e:MonitorEvent); @Query("SELECT * FROM events ORDER BY at DESC LIMIT 100") suspend fun events():List<MonitorEvent>
}
@Database(entities=[FilterProfile::class,Vehicle::class,Snapshot::class,Match::class,MonitorEvent::class],version=1,exportSchema=false) abstract class MonitorDatabase:RoomDatabase(){abstract fun dao():MonitorDao}

object Persian {
 fun normalize(value:String?)=value.orEmpty().replace('ي','ی').replace('ك','ک').replace("ـ","").trim().replace(Regex("\\s+")," ")
 fun toman(value:Long?):String = value?.let { NumberFormat.getNumberInstance(Locale("fa","IR")).format(it)+" تومان" } ?: "—"
 fun parseNumeric(value:String?):Long?=value?.replace("\"","")?.filter { it.isDigit() }?.toLongOrNull()
}
data class ImportedFilter(val name:String,val query:String,val ordering:String)
object FilterUrl {
 const val LISTING="https://dealer.khodro45.com/cars"
 fun import(url:String):ImportedFilter? { val u=runCatching { java.net.URI(url) }.getOrNull() ?: return null; if(u.scheme != "https" || u.host != "dealer.khodro45.com" || !u.path.orEmpty().startsWith("/cars")) return null; val raw=u.rawQuery.orEmpty(); val ordering=raw.split("&").firstOrNull { it.substringBefore("=") == "ordering" }?.substringAfter("=", "")?.let { java.net.URLDecoder.decode(it,"UTF-8") }.orEmpty(); val query=raw.split("&").filter { it.isNotBlank() && it.substringBefore("=") != "ordering" }.joinToString("&"); return ImportedFilter("فیلتر واردشده",query,ordering) }
 fun export(p:FilterProfile)=LISTING+(listOf(p.query, p.ordering.takeIf{it.isNotBlank()}?.let{"ordering=${java.net.URLEncoder.encode(it,"UTF-8")}"}).filterNotNull().filter{it.isNotBlank()}.joinToString("&").takeIf{it.isNotEmpty()}?.let{"?$it"}.orEmpty())
}
