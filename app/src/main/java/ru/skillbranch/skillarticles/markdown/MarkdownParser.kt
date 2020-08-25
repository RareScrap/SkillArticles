package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

object MarkdownParser {

    // TODO: Почему в коде, приложенным к лекции не юзается getProperty()?
    private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

    //group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+$)"
    private const val HEADER_GROUP = "(^#{1,6} .+?$)" // TODO: Зачем нужен "?"
    private const val QUOTE_GROUP = "(^> .+?$)"
    // "*?" нужен чтобы брать только одну звездочку слева направа и пропускать следющие за ней звездочки (*text****)
    // TODO: почему бы не юзать (\*[^*].*?\*)?
    // TODO: Понять эту регулярку
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"
    // TODO: Почему моя регулярка тоже проходит тест? "(\\*{2}.*?\\*{2}|_{2}.*?_{2})"
    private const val BOLD_GROUP = "((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"
    // TODO: Почему мне не понадобились всякие ретроспективные проверки?
    private const val STRIKE_GROUP = "(~{2}.*?~{2})"
    private const val RULE_GROUP = "(^[-_*]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))"
    // TODO: зачем нам вторая часть регулярки если первая и так проходит тест?
    private const val LINK_GROUP = "(\\[[^\\[\\]]*?]\\(.+?\\)|^\\[*?]\\(.*?\\))" // мое решение - "(\\[.*\\]\\(.*\\))"
    private const val BLOCK_CODE_GROUP = "(^```[\\s\\S]*?```)" // TODO: В уроке 6 юзается "(^```[\\s\\S]+?```$)"
    private const val ORDER_LIST_GROUP = "(^\\d\\. .+$)" // TODO: В уроке 6 юзается "(^\\d[1,2]\\.\\s.+?$)"

    //result regex
    private const val MARKDOWN_GROUPS = "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP" +
            "|$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP|$INLINE_GROUP|$LINK_GROUP" +
            "|$BLOCK_CODE_GROUP|$ORDER_LIST_GROUP"

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    /**
     * parse markdown text to elements
     */
    fun parse(string: String): MarkdownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    /**
     * clear markdown text to string without markdown characters
     */
    fun clear(string: String?): String? {
        string ?: return null

        val sb = StringBuilder()
        findElements(string)
            .spread()
            .filter { it.elements.isEmpty() }
            .forEach { sb.append(it.text) }

        return sb.toString()
    }

    // TODO: Вынести в extension или другим образом устранить копирование из ExampleUnitTest
    private fun Element.spread():List<Element>{
        val elements = mutableListOf<Element>()
        elements.add(this)
        elements.addAll(this.elements.spread())
        return elements
    }

    private fun List<Element>.spread():List<Element>{
        val elements = mutableListOf<Element>()
        if(this.isNotEmpty()) elements.addAll(
            // TODO: Как это работает?
            this.fold(mutableListOf()){acc, el -> acc.also { it.addAll(el.spread()) }}
        )
        return elements
    }

    /**
     * find markdown elements in markdown text
     */
    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>() // Результат (TODO: странное имя для результирующей коллекции)
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            // Когда матчер что-то найдет, то весь текст который был до этого - просто текст без md.
            if (lastStartIndex < startIndex) { //
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }

            // Нашли текст!
            val text: CharSequence

            // Это индексы групп в регулярке, на которой основан наш матчер.
            // Данным шагом мы говорим искать элементы для первой, второй, и т.д. групп
            // (см. MARKDOWN_GROUPS) чтобы затем превратить их в элементы markdown'а.
            val groups = 1..11
            var group = -1 // индекс найденной в элементе группы
            for (gr in groups) {
                if (matcher.group(gr) != null) { // этот метод ищет группу только в диапазоне последнего match'а
                    group = gr
                    break
                }
            }

            when (group) { // TODO: как нам могут тут помочь именованные регекспы? (урок 5, 1:02:16)
                //NOT FOUND -> BREAK
                -1 -> break@loop

                //UNORDERED LIST
                1 -> {
                    text = string.subSequence(startIndex.plus(2), endIndex) //text without "*. "
                    val subs = findElements(text) // рекурсивно находим вложенные группы
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element) // добавляем к результирующей коллекции

                    // next find start from position "endIndex" (last regexp character)
                    lastStartIndex = endIndex
                }

                //HEADER
                2 -> {
                    // регексп для поиска решеток, которых может быть от 1 до 6
                    val reg = "^#{1,6}".toRegex().find(string.subSequence(startIndex, endIndex))
                    val level = reg!!.value.length

                    //text without "{#} "
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex) // inc() нужен чтобы исключить следующий за решетками пробел

                    val element = Element.Header(level, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //QUOTE
                3 -> {
                    text = string.subSequence(startIndex.plus(2), endIndex) //text without "> "
                    val subelements = findElements(text)
                    val element = Element.Quote(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //ITALIC
                4 -> {
                    text = string.subSequence(startIndex.inc(), endIndex.dec()) //text without "*{}*"
                    val subelements = findElements(text)
                    val element = Element.Italic(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //BOLD
                5 -> {
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2)) //text without "**{}**"
                    val subelements = findElements(text)
                    val element = Element.Bold(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //STRIKE
                6 -> {
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2)) //text without "~~{}~~"
                    val subelements = findElements(text)
                    val element = Element.Strike(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //RULE
                7 -> {
                    //text without "***" insert empty character
                    val element = Element.Rule()
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //INLINE
                8 -> {
                    text = string.subSequence(startIndex.inc(), endIndex.dec()) //text without "`{}`"
                    val element = Element.InlineCode(text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //LINK
                9 -> {
                    //full text for regex
                    text = string.subSequence(startIndex, endIndex)
                    // TODO: Почитать про деструктурирование
                    val (titile:String, link:String) = "\\[(.*)]\\((.*)\\)".toRegex().find(text)!!.destructured
                    val element = Element.Link(link, titile)
                    parents.add(element)
                    lastStartIndex = endIndex

                    // Моя реализация, оставленная для сравнения с подходом юзания деструктурирования
//                    val reg = "\\[.*\\]".toRegex().find(string.subSequence(startIndex, endIndex))
//                    text = reg?.value!!.subSequence(1, reg.value.length-1)
//                    val url = string.subSequence(startIndex.inc() + reg.value.length, endIndex.dec()) as String //text without "`{}`"
//                    val element = Element.Link(url, text)
//                    parents.add(element)
//                    lastStartIndex = endIndex
                }

                //10 -> BLOCK CODE - optionally
                10 -> {
                    text = string.subSequence(startIndex+3, endIndex-3).toString() // TODO: Зачем каст к String?

                    if (text.contains(LINE_SEPARATOR)) {
                        for ((index, line) in text.lines().withIndex()) {
                            when (index) {
                                text.lines().lastIndex -> parents.add( // TODO: Действительно нужно опять вызывать lines()? Нельзя юзать уже созданный список?
                                    Element.BlockCode(
                                        Element.BlockCode.Type.END,
                                        line
                                    )
                                )
                                0 -> parents.add(
                                    Element.BlockCode(
                                        Element.BlockCode.Type.START,
                                        line + LINE_SEPARATOR
                                    )
                                )
                                else -> parents.add(
                                    Element.BlockCode(
                                        Element.BlockCode.Type.MIDDLE,
                                        line + LINE_SEPARATOR
                                    )
                                )
                            }
                        }
                    } else parents.add(Element.BlockCode(Element.BlockCode.Type.SINGLE, text))

                    lastStartIndex = endIndex
                }

                //11 -> NUMERIC LIST
                11 -> {
                    val reg = "(^\\d{1,2}.)".toRegex().find(string.substring(startIndex, endIndex))
                    val order = reg!!.value
                    text = string.subSequence(startIndex + order.length.inc(), endIndex).toString() // text without "1. "
                    val subs = findElements(text)
                    val element = Element.OrderedListItem(order, text.toString(), subs) // TODO: Зачем каст к String?
                    parents.add(element)
                    lastStartIndex = endIndex
                }
            }
        }

        if (lastStartIndex < string.length) {
            val text = string.subSequence(lastStartIndex, string.length)
            parents.add(Element.Text(text))
        }

        return parents
    }

}

data class MarkdownText(val elements: List<Element>)

sealed class Element() {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text( // TODO: Мне кажется что этот элемент не должен создаваться для символа "\n"
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Rule( // TODO: если нам не нужно вставлять туда элементы и текст, то зачем под них есть конструктор?
        override val text: CharSequence = " ", //for insert span // TODO: Учесть в clear()
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence, //for insert span // TODO: Почему тут нет " ", а на видео лекции есть? (1:12:37)
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence, //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        val type: Type = Type.MIDDLE,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element() {
        enum class Type { START, END, MIDDLE, SINGLE }
    }
}