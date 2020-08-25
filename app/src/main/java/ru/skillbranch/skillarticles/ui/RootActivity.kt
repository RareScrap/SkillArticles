package ru.skillbranch.skillarticles.ui

import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.text.getSpans
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_botombar.*
import kotlinx.android.synthetic.main.layout_submenu.*
import kotlinx.android.synthetic.main.search_view_layout.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.setMarginOptionally
import ru.skillbranch.skillarticles.ui.custom.markdown.MarkdownBuilder
import ru.skillbranch.skillarticles.ui.base.BaseActivity
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.custom.spans.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchSpan
import ru.skillbranch.skillarticles.ui.delegates.AttrValue
import ru.skillbranch.skillarticles.ui.delegates.ObserveProp
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import ru.skillbranch.skillarticles.viewmodels.base.ViewModelFactory

class RootActivity : BaseActivity<ArticleViewModel>(), IArticleView {
    override val layout: Int = R.layout.activity_root // TODO: Как устроен оверрайд свойств?
    override val viewModel: ArticleViewModel by lazy {
        val vmFactory = ViewModelFactory("0")
        ViewModelProviders.of(this, vmFactory).get(ArticleViewModel::class.java)
    }
    override public val binding: ArticleBinding by lazy { ArticleBinding() }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val bgColor by AttrValue(R.attr.colorSecondary) // TODO: А зачем вообще делегат?
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val fgColor by AttrValue(R.attr.colorOnSecondary)

    // TODO: Почему нам больше не понаобится onCreate? ведь в родителе не вызывается фабрика
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val vmFactory =
//            ViewModelFactory("0")
//        viewModel = ViewModelProviders.of(this, vmFactory).get(ArticleViewModel::class.java)
//        viewModel.observeState(this) {
//            renderUi(it)
//        }
//        viewModel.observeNotifications(this) {
//            renderNotification(it)
//        }
//    }

    override fun setupViews() {
        setupActionBar()
        setupBottomBar()
        setupSubenu()
    }

    override fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        val content = tv_text_content.text as Spannable

        // clear entry search result
        clearSearchResult()

        searchResult.forEach { (start, end) ->
            content.setSpan(
                SearchSpan(bgColor, fgColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE // TODO: Что это?
            )
        }

        renderSearchPosition(0) // При обновлении текста перемещаемся на первый найденный текст
    }

    override fun renderSearchPosition(searchPosition: Int) {
        val content = tv_text_content.text as Spannable

        val spans = content.getSpans<SearchSpan>()
        // clear last search position
        content.getSpans<SearchFocusSpan>().forEach { content.removeSpan(it) }

        if (spans.isNotEmpty()) { // TODO: Что это блять вообще такое? (53.00)
            // find position span
            val result = spans[searchPosition]
            Selection.setSelection(content, content.getSpanStart(result))
            content.setSpan(
                SearchFocusSpan(bgColor, fgColor),
                content.getSpanStart(result),
                content.getSpanEnd(result),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun clearSearchResult() {
        val content = tv_text_content.text as Spannable
        content.getSpans<SearchSpan>()
            .forEach { content.removeSpan(it) }
    }

    override fun showSearchBar() {
        bottombar.setSearchState(true)
        scroll.setMarginOptionally(bottom = dpToIntPx(56))
    }

    override fun hideSearchBar() {
        bottombar.setSearchState(false)
        scroll.setMarginOptionally(bottom = dpToIntPx(0))
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

        btn_result_up.setOnClickListener {
            // TODO: Почему в видео нет проверки на null и все работает?
            search_view?.let {
                if (it.hasFocus()) it.clearFocus()

            }
            // Мы ревестируем фокус чтобы скрол-вью промотался до нужного там фрагмента поиска
            if (!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            viewModel.handleUpResult()
        }

        btn_result_down.setOnClickListener {
            search_view?.let {
                if (it.hasFocus()) it.clearFocus()
            }
            if (!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            viewModel.handleDownResult()
        }

        btn_search_close.setOnClickListener {
            viewModel.handleSearchMode(false)
            invalidateOptionsMenu()
        }
    }

    // TODO: Почему мы его удалили?
//    private fun renderUi(data: ArticleState) {
//
//        if (data.isSearch) showSearchBar() else hideSearchBar()
//
//        if (data.searchResults.isNotEmpty()) renderSearchResult(data.searchResults)
//        if (data.searchResults.isNotEmpty()) renderSearchPosition(data.searchPosition) // TODO: один if?
//
//        // bind submenu state
//        btn_settings.isChecked = data.isShowMenu
//        if (data.isShowMenu) submenu.open() else submenu.close()
//
//        // bind article person data
//        btn_like.isChecked = data.isLike
//        btn_bookmark.isChecked = data.isBookmark
//
//        // bind submenu views
//        switch_mode.isChecked = data.isDarkMode
//        delegate.localNightMode = if (data.isDarkMode) MODE_NIGHT_YES else MODE_NIGHT_NO
//        if (data.isBigText) {
//            tv_text_content.textSize = 18f
//            btn_text_up.isChecked = true
//            btn_text_down.isChecked = false
//        } else {
//            tv_text_content.textSize = 14f
//            btn_text_up.isChecked = false
//            btn_text_down.isChecked = true
//        }
//
//        // bind content
//        if (data.isLoadingContent) {
//            tv_text_content.text = "loading"
//        } else if (tv_text_content.text == "loading"){ // dont override content (Вставлять контент только в том случае, если отрисовка закончился и лоадинг исчез)
//            val content = data.content.first() as String
//            tv_text_content.setText(content, TextView.BufferType.SPANNABLE)
//            tv_text_content.movementMethod = ScrollingMovementMethod() // Чтобы мы могли скролироваться по нашему текствью (TODO: А что будет без него?)
//        }
//
//        // bind toolbar
//        toolbar.title = data.title ?: "loading"
//        toolbar.subtitle = data.category ?: "loading"
//        data.category?.let { toolbar.logo = getDrawable(data.categoryIcon as Int) }
//    }

    override fun renderNotification(notify: Notify) {
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

    // TODO: Код в видео протиоречит моему. См 1:23:19
    // TODO: У меня фокус клавы сохраняется, в отличии от видео. См 1:25:54
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.root_activity_menu, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchItem =  menu?.findItem(R.id.action_search)!!
        val searchView = menuItem?.actionView as? SearchView

        if (binding.isSearch) {
            menuItem?.expandActionView()
            searchView?.setQuery(binding.searchQuery, false)
            if(binding.isFocusedSearch) searchView?.requestFocus()
            else searchView?.clearFocus()
        }

        menuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(false)
                return true
            }
        })

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_search) viewModel.handleSearchMode(true)
        return true
    }

    inner class ArticleBinding : Binding() {
        var isFocusedSearch: Boolean = false
        var searchQuery: String? = null
        var isLoadingContent by ObserveProp(true)

        private var isLike: Boolean by RenderProp(false) {btn_like.isChecked = it}
        private var isBookmark: Boolean by RenderProp(false) {btn_bookmark.isChecked = it}
        private var isShowMenu: Boolean by RenderProp(false) {
            btn_settings.isChecked = it
            if (it) submenu.open() else submenu.close()
        }
        private var title: String by RenderProp("loading") { toolbar.title = it }
        private var category: String by RenderProp("loading") { toolbar.subtitle = it }
        private var categoryIcon: Int by RenderProp(R.drawable.logo_placeholder) {
            toolbar.logo = getDrawable(it)
        }
        private var isBigText: Boolean by RenderProp(false) {
            if (it) {
                tv_text_content.textSize = 18f
                btn_text_up.isChecked = true
                btn_text_down.isChecked = false
            } else {
                tv_text_content.textSize = 14f
                btn_text_up.isChecked = false
                btn_text_down.isChecked = true
            }
        }
        private var isDarkMode: Boolean by RenderProp(false ,false) { // TODO: Попробовать без второго арга
            switch_mode.isChecked = it
            delegate.localNightMode = if (it) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        }

        var isSearch: Boolean by ObserveProp(false) {
            if (it) showSearchBar() else hideSearchBar()
        }

        private var searchResults: List<Pair<Int, Int>> by ObserveProp(emptyList())
        private var searchPosition: Int by ObserveProp(0)

        private var content: String by ObserveProp("loading") {
            MarkdownBuilder(this@RootActivity)
                .markdownToSpan(it)
                .run {
                    tv_text_content.setText(this, TextView.BufferType.SPANNABLE)
                }
//            tv_text_content.movementMethod = ScrollingMovementMethod() // TODO: Зачем это было?
            tv_text_content.movementMethod = LinkMovementMethod.getInstance() // TODO: Зачем это теперь?
        }

        override fun onFinishInflate() {
            dependsOn<Boolean, Boolean, List<Pair<Int, Int>>, Int>(
                ::isLoadingContent,
                ::isSearch,
                ::searchResults,
                ::searchPosition
            ) { ilc, iss, sr, sp ->
                if (!ilc && iss) {
                    renderSearchResult(sr)
                    renderSearchPosition(sp)
                }
                if (!ilc && !iss) {
                    clearSearchResult()
                }

                bottombar.bindSearchInfo(sr.size, sp)

            }
        }

        override fun bind(data: IViewModelState) {
            data as ArticleState

            isLike = data.isLike
            isBookmark = data.isBookmark
            isShowMenu = data.isShowMenu
            isBigText = data.isBigText
            isDarkMode = data.isDarkMode

            // TODO: что за хуйня тут вообще происходит?
            if (data.title != null) title = data.title // TODO: Можно ли сделать проверку на нул силами котлина?
            if (data.category != null) category = data.category // TODO: Можно ли сделать проверку на нул силами котлина?
            if (data.categoryIcon != null) categoryIcon = data.categoryIcon as Int// TODO: Можно ли сделать проверку на нул силами котлина?
            if (data.content != null) content = data.content

            isLoadingContent = data.isLoadingContent
            isSearch = data.isSearch
            searchQuery = data.searchQuery
            searchPosition = data.searchPosition
            searchResults = data.searchResults
        }

        override fun saveUi(outState: Bundle) {
            outState.putBoolean(::isFocusedSearch.name, search_view?.hasFocus() ?: false)
        }

        override fun restoreUi(savedState: Bundle) {
            isFocusedSearch = savedState.getBoolean(::isFocusedSearch.name)
        }
    }
}
