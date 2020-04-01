package ru.skillbranch.skillarticles.ui.custom.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginRight
import ru.skillbranch.skillarticles.ui.custom.ArticleSubmenu
import ru.skillbranch.skillarticles.ui.custom.Bottombar

class SubmenuBehavior() : CoordinatorLayout.Behavior<ArticleSubmenu>() {
    constructor(context: Context?, attrs: AttributeSet?) : this()

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: ArticleSubmenu,
        dependency: View
    ): Boolean {
        return dependency is Bottombar
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: ArticleSubmenu,
        dependency: View
    ): Boolean {
        return if (child.isOpen && dependency is Bottombar && dependency.translationY >= 0) {
            animate(child, dependency)
            true
        } else false
    }

    private fun animate(
        child: ArticleSubmenu,
        dependency: Bottombar
    ) {
        val fraction = dependency.translationY / dependency.minimumHeight;
        child.translationX = (child.width + child.marginRight) * fraction

    }
}