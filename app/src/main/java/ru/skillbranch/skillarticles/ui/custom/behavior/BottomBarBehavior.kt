package ru.skillbranch.skillarticles.ui.custom.behavior

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import ru.skillbranch.skillarticles.ui.custom.Bottombar

class BottomBarBehavior() : CoordinatorLayout.Behavior<Bottombar>() {
    constructor(context: Context?, attrs: AttributeSet?) : this()

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: Bottombar,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    // Не вызовется если visible == gone
    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: Bottombar,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        // dy<0 - скроллим вниз
        // dy>0 - скроллим вверх
        val offset = MathUtils.clamp(child.translationY + dy, 0f, child.minHeight.toFloat())
        if (offset != child.translationY) child.translationY = offset
        Log.e("BottomBarBehavir", "dy : $dy translationY : ${child.translationY}")
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }
}