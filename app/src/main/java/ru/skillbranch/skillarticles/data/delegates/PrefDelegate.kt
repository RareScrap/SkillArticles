package ru.skillbranch.skillarticles.data.delegates

import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefDelegate<T>(private val defaultValue: T) : ReadWriteProperty<PrefManager, T?> {

    // Решение Михаила в лекции (урок 5, 50:34) отличается от моего тем, что юзает storedValue в
    // качестве кешируемого значения, в кто время как мое просто работает в SharedPreferences
    // без какого-либо кеширования. Отказался от моего решения в пользу коду лекции за одним
    // исключением - делегат реализует provideDelegate() и при этом наследуется от ReadWriteProperty,
    // а не использует класс-обертку как в решении Михаила. Моё решения я нахожу более лаконичным.

    private var storedValue: T? = null

    operator fun provideDelegate(thisRef: PrefManager, property: KProperty<*>): PrefDelegate<T> {
        val key = property.name
        return PrefDelegate(defaultValue)
    }

    override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
        if (storedValue == null) {
            @Suppress("UNCHECKED_CAST")
            storedValue = when (defaultValue) {
                is Int -> thisRef.preferences.getInt(property.name, defaultValue as Int) as T
                is Boolean -> thisRef.preferences.getBoolean(
                    property.name,
                    defaultValue as Boolean
                ) as T
                is Float -> thisRef.preferences.getFloat(property.name, defaultValue as Float) as T
                is String -> thisRef.preferences.getString(
                    property.name,
                    defaultValue as String
                ) as T
                is Long -> thisRef.preferences.getLong(property.name, defaultValue as Long) as T
                else -> throw RuntimeException("This type can not be stored in Preferences")
            }
        }
        return storedValue
    }

    override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
        with(thisRef.preferences.edit()) {
            when (value) {
                is Int -> putInt(property.name, value as Int)
                is Boolean -> putBoolean(property.name, value as Boolean)
                is Float -> putFloat(property.name, value as Float)
                is String -> putString(property.name, value as String)
                is Long -> putLong(property.name, value as Long)
                else -> throw RuntimeException("This type can not be stored in Preferences")
            }
            apply()
        }
        storedValue = value
        return
    }
}