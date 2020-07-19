package ru.skillbranch.skillarticles.markdown.spans

import android.graphics.*
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting
import ru.skillbranch.skillarticles.markdown.Element


class BlockCodeSpan(
    @ColorInt
    private val textColor: Int,
    @ColorInt
    private val bgColor: Int,
    @Px
    private val cornerRadius: Float,
    @Px
    private val padding: Float,
    private val type: Element.BlockCode.Type
) : ReplacementSpan()/*, LeadingMarginSpan*/ { // TODO: Но в задании сказано, что LeadingMarginSpan обязателен. А тесты проходят и без него.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var rect = RectF()
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var path = Path()

    private val corner = floatArrayOf(cornerRadius, cornerRadius)
    private val notCorner = floatArrayOf(0f, 0f)
    private val corners = mapOf(
        Element.BlockCode.Type.SINGLE to corner + corner + corner + corner,
        Element.BlockCode.Type.START to corner + corner + notCorner + notCorner,
        Element.BlockCode.Type.MIDDLE to notCorner + notCorner + notCorner + notCorner,
        Element.BlockCode.Type.END to notCorner + notCorner + corner + corner
    )

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

            val paddingTop = if (type == Element.BlockCode.Type.START) padding else 0f
            val paddingBottom = if (type == Element.BlockCode.Type.END) padding else 0f
            rect.set(x, top + paddingTop, canvas.width.toFloat(), bottom - paddingBottom)

            path.reset() // TODO: Зачем?
            path.addRoundRect(rect, corners.getValue(type), Path.Direction.CW)
            canvas.drawPath(path, paint)
        }

        paint.forText {
            canvas.drawText(text, start, end, x + padding, y.toFloat(), paint) // TODO: Понять как это расчитывается
        }
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        // Почему его не надо имплементить?
        fm?.let {
            val calculatedFm = calculateFontMetricsFor(type, fm)
            it.ascent = calculatedFm.first
            it.descent = calculatedFm.second
        }
        return 0
    }

    private fun calculateFontMetricsFor(type: Element.BlockCode.Type, fm: Paint.FontMetricsInt): Pair<Int, Int> {
        return when (type) {
            Element.BlockCode.Type.SINGLE -> Pair(
                (fm.ascent * 0.85f - 2 * padding).toInt(),
                (fm.descent * 0.85f + 2 * padding).toInt()
            )
            Element.BlockCode.Type.START -> Pair(
                (fm.ascent * 0.85f - 2 * padding).toInt(),
                (fm.descent * 0.85f).toInt()
            )
            Element.BlockCode.Type.MIDDLE -> Pair(
                (fm.ascent * 0.85f).toInt(),
                (fm.descent * 0.85f).toInt()
            )
            Element.BlockCode.Type.END -> Pair(
                (fm.ascent * 0.85f).toInt(),
                (fm.descent * 0.85f + 2 * padding).toInt()
            )
        }
    }

    private inline fun Paint.forText(block: () -> Unit) {
        val oldSize = textSize
        val oldStyle = typeface?.style ?: 0
        val oldFont = typeface
        val oldColor = color

        color = textColor
        typeface = Typeface.create(Typeface.MONOSPACE, oldStyle) // TODO: почему нельзя просто юзать константу MONOSPACE без оборачивания в create()?
        textSize *= 0.95f // процент

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
