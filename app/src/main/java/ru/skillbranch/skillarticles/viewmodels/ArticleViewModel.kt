package ru.skillbranch.skillarticles.viewmodels

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.ArticleData
import ru.skillbranch.skillarticles.data.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.extensions.data.toAppSettings
import ru.skillbranch.skillarticles.extensions.data.toArticlePersonalInfo
import ru.skillbranch.skillarticles.extensions.format
import ru.skillbranch.skillarticles.extensions.indexesOf
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify

class ArticleViewModel(private val articleId: String) : BaseViewModel<ArticleState>(ArticleState()), IArticleViewModel {

    private val repository = ArticleRepository

    init {
        subscribeOnDataSource(getArticleData()) { article, state ->
            article ?: return@subscribeOnDataSource null
            state.copy(
                shareLink = article.shareLink, // TODO: Она же null будет!
                title = article.title,
                category = article.category,
                author = article.author,
                categoryIcon = article.categoryIcon,
                date = article.date.format()
            )
        }

        subscribeOnDataSource(getArticleContent()) { content, state ->
            content ?: return@subscribeOnDataSource null // TODO: Почему content вообще может быть null?
            state.copy(
                isLoadingContent = false,
                content = content
            )
        }

        subscribeOnDataSource(getArticlePersonalInfo()) { info, state ->
            info ?: return@subscribeOnDataSource null
            state.copy(
                isBookmark = info.isBookmark,
                isLike = info.isLike
            )
        }

        subscribeOnDataSource(repository.getAppSettings()) { settings, state ->
            state.copy(
                isDarkMode = settings.isDarkMode,
                isBigText = settings.isBigText
            )

        }
    }

    // TODO: Зачем этим методы вообще нужны? Ведь у нас данные из репы не в rx-последовательностях приходят, а уже в livedata'е
    override fun getArticleContent(): LiveData<List<Any>?> {
        return repository.loadArticleContent(articleId)
    }

    override fun getArticleData(): LiveData<ArticleData?> {
        return repository.getArticle(articleId)
    }

    override fun getArticlePersonalInfo(): LiveData<ArticlePersonalInfo?> {
        return repository.loadArticlePersonalInfo(articleId)
    }

    override fun handleUpText() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = true))
    }

    override fun handleDownText() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = false))
    }

    // app settings
    override fun handleNightMode() { // TODO: Нахуя это вообще надо?
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isDarkMode = !settings.isDarkMode))
    }

    override fun handleToggleMenu() {
        updateState { it.copy(isShowMenu = !it.isShowMenu)}
    }

    override fun handleSearchMode(isSearch: Boolean) {
        updateState { it.copy(isSearch = isSearch, isShowMenu = false, searchPosition = 0) }
    }

    override fun handleSearch(query: String?) {
        query ?: return // TODO: Нужно?
        // TODO: Что такое "?"
        val result = (currentState.content.firstOrNull() as? String).indexesOf(query)
            .map{ it to it + query.length }
        updateState { it.copy(searchQuery = query, searchResults = result) }
    }

    override fun handleShare() {
        notify(Notify.ActionMessage("Share is not implemented", "OK", actionHandler = {}))
        val msg = Notify.ErrorMessage("Share is not implemented", "OK", null)
        notify(msg)
    }

    override fun handleBookmark() {
        repository.updateArticlePersonalInfo(currentState.toArticlePersonalInfo().copy(isBookmark = !currentState.isBookmark))
        val msg = Notify.TextMessage(if (currentState.isBookmark) "Add to bookmarks" else "Remove from bookmarks")
        notify(msg)
    }

    override fun handleLike() {
        val toggleLike = {
            val info = currentState.toArticlePersonalInfo()
            repository.updateArticlePersonalInfo(info.copy(isLike = !info.isLike))
        }

        toggleLike()

        val msg = if (currentState.isLike) Notify.TextMessage("Mark as liked")
        else Notify.ActionMessage("Don`t like it anymore", "No, still like it", toggleLike)

        notify(msg)
    }

    fun handleUpResult() {
        updateState { it.copy(searchPosition = it.searchPosition.dec()) }
    }

    fun handleDownResult() {
        updateState { it.copy(searchPosition = it.searchPosition.inc()) }
    }

}

data class ArticleState(
    val isAuth: Boolean = false, // Пользователь авторизован
    val isLoadingContent: Boolean = true, // контент загружается
    val isLoadingReviews: Boolean = true, // комментарии загружается
    val isLike: Boolean = false, // Лайк поставлен
    val isBookmark: Boolean = false, // добавлено в закладки
    val isShowMenu: Boolean = false, // меню отображается
    val isBigText: Boolean = false, // включен режим большого текста
    val isDarkMode: Boolean = false, // включена темная тема
    val isSearch: Boolean = false, // включен реим поиска
    val searchQuery: String? = null, // поисковый запрос
    val searchResults: List<Pair<Int, Int>> = emptyList(), // Результаты поиска (стартовая и конечная позиция)
    val searchPosition: Int = 0, // текущая позиция найденного результата
    val shareLink: String? = null, // share-ссылка
    val title: String? = null, // заголовок статьи
    val category: String? = null, // категория
    val categoryIcon: Any? = null, // иконка изображения
    val date: String? = null, // дата публикации
    val author: Any? = null, // автор статьи
    val poster: String? = null, // Обложка статьи
    val content: List<Any> = emptyList(), // контент // TODO: Почему Any?
    val reviews: List<Any> = emptyList() // комментарии
) : IViewModelState {
    override fun save(outState: Bundle) {
        outState.putAll(
            bundleOf(
                "isSearch" to isSearch,
                "searchQuery" to searchQuery,
                "searchPosition" to searchPosition,
                "searchResults" to searchResults
            )
        )
    }

    override fun restore(savedState: Bundle): IViewModelState {
        return copy(
            isSearch = savedState["isSearch"] as Boolean,
            searchQuery = savedState["searchQuery"] as? String,
            searchResults = savedState["searchResults"] as List<Pair<Int, Int>>,
            searchPosition = savedState["searchPosition"] as Int
        )
    }
}