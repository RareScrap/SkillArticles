package ru.skillbranch.skillarticles.markdown.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting

class InlineCodeSpan( // TODO: НЕ РАБОТАЕТ!
    @ColorInt
    private val textColor: Int,
    @ColorInt
    private val bgColor: Int,
    @Px
    private val cornerRadius: Float,
    @Px
    private val padding: Float
) : ReplacementSpan() {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var rect: RectF = RectF()
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var measureWidth: Int = 0

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        // т.к. мы переопределили размер шрифта, то нужно перерасчитать сколько места будет занимать текст с нашим спаном
        paint.forText {
            val measureText = paint.measureText(text.toString(), start, end) // TODO: Почему toString()?
            measureWidth = (measureText + 2*padding).toInt() // TODO: Понять как это расчитывается
        }
        return measureWidth
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        paint.forBackground { // TODO: Понять как это расчитывается
            rect.set(x, top.toFloat(), x + measureWidth, bottom.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }

        paint.forText {
            canvas.drawText(text, start, end, x + padding, y.toFloat(), paint) // TODO: Понять как это расчитывается
        }
    }

    private inline fun Paint.forText(block: () -> Unit) {
        val oldSize = textSize
        val oldStyle = typeface?.style ?: 0
        val oldFont = typeface
        val oldColor = color

        color = textColor
        typeface = Typeface.create(Typeface.MONOSPACE, oldStyle) // TODO: почему нельзя просто юзать констанку MONOSPACE без оборачивания в create()?
        textSize = 0.95f // процент

        block()

        color = oldColor
        typeface = oldFont
        textSize = oldSize
    }

    private inline fun Paint.forBackground(block: () -> Unit) {
        val oldColor = color
        val oldStyle = style

        color = bgColor
        style = Paint.Style.FILL

        block()

        color = oldColor
        style = oldStyle
    }
}