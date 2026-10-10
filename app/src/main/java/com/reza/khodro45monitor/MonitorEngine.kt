package com.reza.khodro45monitor
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MonitorEngine(private val context:Context){
 private val db=Room.databaseBuilder(context,MonitorDatabase::class.java,"khodro45.db").build(); private val dao=db.dao(); private val lock=Mutex()
 suspend fun scan(client:Khodro45Client, notify:suspend(Vehicle,FilterProfile)->Unit) = lock.withLock {
  val profiles=dao.profiles().filter{it.enabled}; if(profiles.isEmpty()){dao.event(MonitorEvent(type="info",message="پروفایل فعالی وجود ندارد"));return}
  val initial=dao.vehicles().isEmpty(); var count=0
  profiles.forEach { p->
   var next:String?=null; val visited=mutableSetOf<String>(); var pageCount=0
   do {
    val result=client.list(p.query,next); result.items.forEach { vehicle-> if(matchesLocally(vehicle,p)){
     val old=dao.vehicle(vehicle.id); val current=vehicle.copy(firstSeen=old?.firstSeen?:System.currentTimeMillis(),lastSeen=System.currentTimeMillis()); dao.upsert(current); dao.match(Match(vehicle.id,p.id)); count++
     if(old==null && !initial && p.notifyNew) notify(current,p)
     if(old!=null && fingerprint(old)!=fingerprint(current)) { dao.snapshot(Snapshot(vehicleId=current.id,price=current.currentPrice,endTime=current.endTime,finished=current.finished,fingerprint=fingerprint(current))); if(p.notifyPrice && old.currentPrice!=current.currentPrice) notify(current,p) }
    } }
    next=result.nextUrl; pageCount++
   } while(next!=null && pageCount<100 && visited.add(next!!))
  }
  if(initial) Settings(context).setBaselined(); dao.event(MonitorEvent(type="success",message="بررسی موفق — $count خودرو"))
 }
 private fun matchesLocally(v:Vehicle,p:FilterProfile):Boolean { val q=android.net.Uri.parse("https://x/?${p.query}"); q.getQueryParameter("price__lte")?.let{Persian.parseNumeric(it)?.let { max->if((v.currentPrice?:Long.MAX_VALUE)>max)return false }}; q.getQueryParameter("price__gte")?.let{Persian.parseNumeric(it)?.let { min->if((v.currentPrice?:0)<min)return false }}; q.getQueryParameter("klm__lte")?.let{Persian.parseNumeric(it)?.let { max->if((v.mileage?:Long.MAX_VALUE)>max)return false }}; q.getQueryParameter("klm__gte")?.let{Persian.parseNumeric(it)?.let { min->if((v.mileage?:0)<min)return false }}; q.getQueryParameter("year__gte")?.let{Persian.parseNumeric(it)?.let { min->if((Persian.parseNumeric(v.year)?:0)<min)return false }}; q.getQueryParameter("year__lte")?.let{Persian.parseNumeric(it)?.let { max->if((Persian.parseNumeric(v.year)?:Long.MAX_VALUE)>max)return false }}; return true }
 private fun fingerprint(v:Vehicle)="${v.currentPrice}|${v.endTime}|${v.finished}|${v.brand}|${v.model}|${v.trim}|${v.year}"
}
