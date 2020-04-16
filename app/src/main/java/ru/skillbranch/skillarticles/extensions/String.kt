package ru.skillbranch.skillarticles.extensions

public fun String?.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    var list = mutableListOf<Int>() // TODO: Чекнуть другие решения
    if (substr.isNullOrBlank()) return list
    var count = 0;
    this?.split(substr, ignoreCase = ignoreCase)?.forEach {
        count += it.length
        list.add(count)
        count += substr.length
    }
    list.remove(list.get(list.size-1))
    return list // TODO: Почему тут нельзя убрать return?
}