package ru.skillbranch.skillarticles.ui.custom.spans

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.Spanned
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import android.text.style.MetricAffectingSpan
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting


class HeaderSpan constructor(
    @IntRange(from = 1, to = 6)
    private val level: Int,
    @ColorInt
    private val textColor: Int,
    @ColorInt
    private val dividerColor: Int,
    @Px
    private val marginTop: Float,
    @Px
    private val marginBottom: Float
) :
    MetricAffectingSpan(), LineHeightSpan, LeadingMarginSpan { // TODO: Почему такое наследование и интерфейсы?

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val linePadding = 0.4f
    private var originAscent = 0 // TODO: Что это?
    /** Коэфиценты размеров хедеровов по уровням */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val sizes = mapOf(
        1 to 2f,
        2 to 1.5f,
        3 to 1.25f,
        4 to 1f,
        5 to 0.875f,
        6 to 0.85f
    )

    override fun chooseHeight( // тут будем работать с FontMetric'ами
        text: CharSequence?,
        start: Int,
        end: Int,
        spanstartv: Int,
        lineHeight: Int,
        fm: Paint.FontMetricsInt?
    ) {
         fm ?: return // TODO: Посмотреть что вернет декомпил

        text as Spanned
        val spanStart = text.getSpanStart(this)
        val spanEnd = text.getSpanEnd(this)

        if (spanStart == start) { // если эта строка - последняя
            originAscent = fm.ascent
            // т.к. ascent идет вверх, то это отрицательное число
            fm.ascent = (fm.ascent - marginTop).toInt() // TODO: Понять как это расчитывается
            // а descent - положительное
        } else {
            fm.ascent = originAscent // восстанавливаем ascent для всех последующих линий (т.к. мы изменили его для первой)
        }

        if (spanEnd == end.dec()) { // декрементом мы учитываем символ "\n" // TODO: Попробовать без декремента
            val originHeight = fm.descent - originAscent // TODO: Понять как это расчитывается
            fm.descent = (originHeight * linePadding + marginBottom).toInt() // TODO: Понять как это расчитывается

        }

        fm.top = fm.ascent // TODO: Зачем?
        fm.bottom = fm.descent // TODO: Зачем?
    }

    override fun updateMeasureState(paint: TextPaint) {
        with(paint) {
            textSize *= sizes.getOrElse(level) {1f}
            isFakeBoldText = true
        }
    }

    override fun updateDrawState(tp: TextPaint) {
        with(tp) { // TODO: Зачем мы это дублируем из updateMeasureState()?
            textSize *= sizes.getOrElse(level) {1f}
            isFakeBoldText = true
            color = textColor // потому что этот метод ОТРИСОВЫВАЕТ шрифт, а не измеряет как в updateMeasureState()
        }
    }

    // используется для отрисовки отступов на уровне параграфа
    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, currentMarginLocation: Int, paragraphDirection: Int,
        lineTop: Int, lineBaseline: Int, lineBottom: Int, text: CharSequence?, lineStart: Int,
        lineEnd: Int, isFirstLine: Boolean, layout: Layout?
    ) {

        // TODO: Что такое "infix"
        if ((level == 1 || level == 2) && (text as Spanned).getSpanEnd(this) == lineEnd) { // проверяем что этот спан - последний в строке
            paint.forLine {
                // высота шрифта
                val lh = (paint.descent() - paint.ascent() // вычисляем высоту шрифта
                        ) * sizes.getOrElse(level) {1f} // и умножаем на множитель уровня
                val lineOffset = lineBaseline + lh * linePadding // TODO: Понять как это расчитывается

                canvas.drawLine( // рисуем от левого края
                    0f,
                    lineOffset,
                    canvas.width.toFloat(),
                    lineOffset,
                    paint
                )
            }
        }

        // НИЖЕ - УЧЕБНЫЙ СТАФФ

//        val oldSize = paint.textSize
//        paint.textSize *= sizes[level]!! // а теперь знает!

        // Внимание! На момент вызова этой функции, paint который нам передался имеет textSize
        // равный размеру ОСНОВНОГО ШРИФТА. Поэтому изменив размер в updateDrawState() линии
        // не будут отрисовывать там где должны, потому что drawLeadingMargin() об этом ничего
        // не знает
//        canvas.drawFontLines(lineTop, lineBottom, lineBaseline, paint)

//        paint.textSize = oldSize

        // нужно помнить о том, что для разных жизненных колбэков спанов могут юзаться разные paint'ы
    }

    override fun getLeadingMargin(first: Boolean): Int {
        //TODO implement me
        return 0
    }

    private inline fun Paint.forLine(block: () -> Unit) {
        val oldColor = color
        val oldStyle = style
        val oldWidth = strokeWidth

        color = dividerColor
        style = Paint.Style.STROKE
        strokeWidth = 0f

        block()

        color = oldColor
        style = oldStyle
        strokeWidth = oldWidth
    }

    // для учебных целей (показывает линии при отрисовки шрифтов)
    // поставь пустые строки между хедерами (DataHolder#longText) для того, чтобы посмотреть под капот отрисовки текста
    private fun Canvas.drawFontLines(
        top: Int,
        bottom: Int,
        lineBaseLine: Int,
        paint: Paint
    ) {
        drawLine(0f, top+0f, width+0f, top+0f, Paint().apply { color = Color.BLUE })
        drawLine(0f, bottom+0f, width+0f, bottom+0f, Paint().apply { color = Color.GREEN })
        drawLine(0f, lineBaseLine+0f, width+0f, lineBaseLine+0f, Paint().apply { color = Color.RED })
        drawLine(0f, paint.ascent()+lineBaseLine, width+0f, paint.ascent()+lineBaseLine, Paint().apply { color = Color.BLACK })
        drawLine(0f, paint.descent()+lineBaseLine, width+0f, paint.descent()+lineBaseLine, Paint().apply { color = Color.MAGENTA })
    }
}