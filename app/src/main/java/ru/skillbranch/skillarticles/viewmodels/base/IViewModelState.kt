package ru.skillbranch.skillarticles.viewmodels.base

import android.os.Bundle

interface IViewModelState { // TODO: Зачем оно надо?
    fun save(outState: Bundle)
    fun restore(savedState: Bundle) : IViewModelState
}