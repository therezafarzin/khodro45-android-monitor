package com.reza.khodro45monitor
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
private val Context.dataStore by preferencesDataStore("monitor_settings")
class Settings(private val context:Context){
 private val interval=intPreferencesKey("interval"); private val boot=booleanPreferencesKey("boot"); private val baseline=booleanPreferencesKey("baseline_done")
 suspend fun interval()=context.dataStore.data.first()[interval]?:30; suspend fun setInterval(v:Int){context.dataStore.edit{it[interval]=v.coerceAtLeast(10)}}
 suspend fun boot()=context.dataStore.data.first()[boot]?:false; suspend fun setBoot(v:Boolean){context.dataStore.edit{it[boot]=v}}
 suspend fun baselined()=context.dataStore.data.first()[baseline]?:false; suspend fun setBaselined(){context.dataStore.edit{it[baseline]=true}}
}
