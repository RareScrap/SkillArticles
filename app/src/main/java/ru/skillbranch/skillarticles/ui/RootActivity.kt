package ru.skillbranch.skillarticles.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_botombar.*
import kotlinx.android.synthetic.main.layout_submenu.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.Notify
import ru.skillbranch.skillarticles.viewmodels.ViewModelFactory

class RootActivity : AppCompatActivity() {

    private lateinit var viewModel: ArticleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)
        setupActionBar()
        setupBottomBar()
        setupSubenu()

        val vmFactory = ViewModelFactory("0")
        viewModel = ViewModelProviders.of(this, vmFactory).get(ArticleViewModel::class.java)
        viewModel.observeState(this) {
            renderUi(it)
        }
        viewModel.observeNotifications(this) {
            renderNotifications(it)
        }
    }

    private fun setupSubenu() {
        btn_text_up.setOnClickListener { viewModel.handleUpText() }
        btn_text_down.setOnClickListener { viewModel.handleDownText() }
        switch_mode.setOnClickListener { viewModel.handleNightMode() }
    }

    private fun setupBottomBar() {
        btn_like.setOnClickListener { viewModel.handleLike() }
        btn_bookmark.setOnClickListener { viewModel.handleBookmark() }
        btn_share.setOnClickListener { viewModel.handleShare() }
        btn_settings.setOnClickListener { viewModel.handleToggleMenu() }
    }

    private fun renderUi(data: ArticleState) {
        // bind submenu state
        btn_settings.isChecked = data.isShowMenu
        if (data.isShowMenu) submenu.open() else submenu.close()

        // bind article person data
        btn_like.isChecked = data.isLike
        btn_bookmark.isChecked = data.isBookmark

        // bind submenu views
        switch_mode.isChecked = data.isDarkMode
        delegate.localNightMode = if (data.isDarkMode) MODE_NIGHT_YES else MODE_NIGHT_NO
        if (data.isBigText) {
            tv_text_content.textSize = 18f
            btn_text_up.isChecked = true
            btn_text_down.isChecked = false
        } else {
            tv_text_content.textSize = 14f
            btn_text_up.isChecked = false
            btn_text_down.isChecked = true
        }

        // bind content
        tv_text_content.text = if (data.isLoadingContent) "loading" else data.content.first() as String

        // bind toolbar
        toolbar.title = data.title ?: "loading"
        toolbar.subtitle = data.category ?: "loading"
        data.category?.let { toolbar.logo = getDrawable(data.categoryIcon as Int) }
    }

    private fun renderNotifications(notify: Notify) {
        val snackbar  = Snackbar.make(coordinator_container, notify.message, Snackbar.LENGTH_LONG)
            .setAnchorView(bottombar)
            .setActionTextColor(ContextCompat.getColor(this, R.color.color_accent_dark))

        when(notify) {
            is Notify.TextMessage -> {}
            is Notify.ActionMessage -> {
                snackbar.setAction(notify.actionLabel) { notify.actionHandler.invoke()}
            }
            is Notify.ErrorMessage -> {
                with(snackbar) {
                    setBackgroundTint(ContextCompat.getColor(this@RootActivity, R.color.design_default_color_error))
                    setTextColor(ContextCompat.getColor(this@RootActivity, android.R.color.white))
                    setActionTextColor(ContextCompat.getColor(this@RootActivity, android.R.color.white))
                    setAction(notify.errLabel) { notify.errHandler?.invoke() }
                }
            }
        }

        snackbar.show()
    }

    private fun setupSearchView(searchView: SearchView, data: ArticleState) {
        searchView.maxWidth = Integer.MAX_VALUE // Занимаем весь улбар
        searchView.queryHint = "Введите запрос"
        searchView.setQuery(data.searchQuery, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }
        })
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // TODO: Хуево настраивать вьюхи програмно, когда есть XML. Неужели нет способа обойтись без этой хуйни?
        val logo = if (toolbar.childCount > 2) toolbar.getChildAt(2) as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP
        val lp = logo?.layoutParams as? Toolbar.LayoutParams
        lp?.let {
            it.width = dpToIntPx(40)
            it.height = dpToIntPx(40)
            it.marginEnd = dpToIntPx(16)
            logo.layoutParams = it
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.root_activity_menu, menu)
        val searchItem =  menu?.findItem(R.id.action_search)!!
        val searchView = searchItem.actionView as SearchView
        viewModel.observeOnce(this, onChanged = {
            if (it.isSearch) searchItem.expandActionView() // Предварительный экспанд не позволит вьюхе поиска очистить текст при сетапе
            searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener { // TODO: Жаль что setOnCloseListener() не работает
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    viewModel.handleSearchMode(false)
                    return true
                }
            })

            setupSearchView(searchView, it)
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_search) viewModel.handleSearchMode(true)
        return true
    }
}
