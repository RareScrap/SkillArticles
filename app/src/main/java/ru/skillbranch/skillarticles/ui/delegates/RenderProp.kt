package ru.skillbranch.skillarticles.ui.delegates

import ru.skillbranch.skillarticles.ui.base.Binding
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RenderProp<T>(
    var value: T,
    needInit: Boolean = true,
    private val onChange: ((T) -> Unit)? = null
) : ReadWriteProperty<Binding, T> {
    private val listeners: MutableList<()->Unit> = mutableListOf()

    init { // not nullable! Нужно всегда проинициализировать наш свойства
        if (needInit) onChange?.invoke(value)
    }

    override fun getValue(thisRef: Binding, property: KProperty<*>): T = value

    override fun setValue(thisRef: Binding, property: KProperty<*>, value: T) {
        if (value == this.value) return; // TODO: А разве это норм? Почему не equals?
        this.value = value
        onChange?.invoke(this.value)
        if (listeners.isNotEmpty()) listeners.forEach{ it.invoke() }
    }

    fun addListener(listener: ()->Unit) {
        listeners.add(listener)
    }
}

class ObserveProp<T: Any>(private var value: T, private val onChange: ((T) -> Unit)? = null) { // TODO: Почему onChange nullable?
    operator fun provideDelegate(
        thisRef: Binding,
        prop: KProperty<*> // TODO: Что это за вилдкарт?
    ) : ReadWriteProperty<Binding, T> {
        val delegate = RenderProp(value, true, onChange)
        registerDelegate(thisRef, prop.name, delegate)
        return delegate
    }

    private fun registerDelegate(thisRef: Binding, name: String, delegate: RenderProp<T>) {
        thisRef.delegates[name] = delegate
    }
}