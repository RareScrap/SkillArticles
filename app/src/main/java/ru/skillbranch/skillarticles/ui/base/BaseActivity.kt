package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import ru.skillbranch.skillarticles.viewmodels.base.ViewModelDelegate

/*
Так уж устроено, что компилятор java не воспринимает BaseActivity<ArticleViewModel> как
подтип BaseViewModel<IViewModelState>. Даже не смотря на то что ArticleViewModel имплементит
IViewModelState.

Java в таком случае требует вилдкарт BaseViewModel<? extends IViewModelState>, что в
каком-то роде бессмысленно, потому что у переменной такого типа мы можем вызывать те же
методы, что и ранее. Стало быть, более сложный тип не добавляет смысла. Но компилятор
этого не понимает.

В Kotlin существует способ объяснить вещь такого рода компилятору. Он называется "вариантность
на уровне объявления": мы можем объявить типовой параметр T класса BaseActivity таким образом,
чтобы удостовериться, что он только возвращается (производится) членами BaseActivity<T>, и
никогда не потребляется. Чтобы сделать это, нам необходимо использовать модификатор out

Теперь мы можем сделать так:
val baseActivities: BaseActivity<BaseViewModel<IViewModelState>>
    = BaseActivity<ArticleViewModel>()

val baseActivity: BaseActivity<out BaseViewModel<out IViewModelState>>
           = RootActivity() // Всё в порядке, т.к. T — out-параметры

Общее правило таково: когда параметр T класса С объявлен как out, он может использоваться
только в out-местах в членах C. Но зато C<Base> может быть родителем C<Derived>, и это будет безопасно.

Модификатор out определяет вариантность, и так как он указывается на месте объявления типового
параметра, речь идёт о вариативности на месте объявления. Эта концепция противопоставлена
вариативности на месте использования из Java, где маски при использовании типа делают типы
ковариантными.

См доки котлина для подробной инфы: https://kotlinlang.ru/docs/reference/generics.html
 */
abstract class BaseActivity<T: BaseViewModel<out IViewModelState>> : AppCompatActivity() {
    protected abstract val binding:Binding
    protected abstract val viewModel : T
    protected abstract val layout:Int

    //set listeners, tuning views
    abstract fun setupViews()
    abstract fun renderNotification(notify: Notify)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        setupViews()
        binding.onFinishInflate()
        viewModel.observeState(this){binding.bind(it)} // TODO: Как это блять работает?
        viewModel.observeNotifications(this){renderNotification(it)}
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState(outState)
        binding.saveUi(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.restoreState(savedInstanceState)
        binding.restoreUi(savedInstanceState)
    }

    // TODO: Зачем оно мне надо, если я не юзаю это для инциализации viewModel? Просто шоб проходили тесты? втф?
    // TODO: Как работает reified?
    internal inline fun <reified T: ViewModel> provideViewModel(arg : Any?) : ViewModelDelegate<T> {
        return ViewModelDelegate(T::class.java, arg)
    }
}