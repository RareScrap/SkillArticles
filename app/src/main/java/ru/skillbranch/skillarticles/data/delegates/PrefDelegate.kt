package ru.skillbranch.skillarticles.data.delegates

import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefDelegate<T>(private val defaultValue: T) : ReadWriteProperty<PrefManager, T?> {

    override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
        return when (defaultValue) {
            is Int -> thisRef.preferences.getInt(property.name, defaultValue as Int) as T
            is Boolean -> thisRef.preferences.getBoolean(property.name, defaultValue as Boolean) as T
            is Float -> thisRef.preferences.getFloat(property.name, defaultValue as Float) as T
            is String -> thisRef.preferences.getString(property.name, defaultValue as String) as T
            is Long -> thisRef.preferences.getLong(property.name, defaultValue as Long) as T
            else -> throw RuntimeException("Ебанный пиздец. Я попаду в ад за это")
        }
    }

    override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
        when (defaultValue) {
            is Int -> thisRef.preferences.edit().putInt(property.name, value as Int).apply()
            is Boolean -> thisRef.preferences.edit().putBoolean(property.name, value as Boolean).apply()
            is Float -> thisRef.preferences.edit().putFloat(property.name, value as Float).apply()
            is String -> thisRef.preferences.edit().putString(property.name, value as String).apply()
            is Long -> thisRef.preferences.edit().putLong(property.name, value as Long).apply()
            else -> throw RuntimeException("Ебанный пиздец. Я попаду в ад за это")
        }
    }
}