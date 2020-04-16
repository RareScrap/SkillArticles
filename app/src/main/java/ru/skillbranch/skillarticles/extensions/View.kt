package ru.skillbranch.skillarticles.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.*

public fun View.setMarginOptionally(left:Int = marginLeft, top : Int = marginTop, right : Int = marginRight, bottom : Int = marginBottom) {
    updateLayoutParams<ViewGroup.MarginLayoutParams>{
        setMargins(left, top, right, bottom)
    }
}