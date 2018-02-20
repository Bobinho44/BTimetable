package de.tobiasschuerg.weekview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import de.tobiasschuerg.weekview.data.WeekViewConfig
import de.tobiasschuerg.weekview.util.DayHelper
import de.tobiasschuerg.weekview.util.dipToPixeel
import de.tobiasschuerg.weekview.util.toLocalString
import org.threeten.bp.Duration
import org.threeten.bp.LocalTime
import org.threeten.bp.temporal.ChronoUnit
import java.text.DateFormatSymbols
import kotlin.math.roundToInt

internal class WeekBackgroundView(
        context: Context,
        private val config: WeekViewConfig,
        private val days: List<Int> = DayHelper.createListStartingOn()
) : View(context) {

    /** Default constructor just for android system. Not used. */
    constructor(context: Context) : this(context, WeekViewConfig()) {}

    private val TAG: String = javaClass.simpleName

    private val paintDivider: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = DIVIDER_WIDTH_PX
            color = DIVIDER_COLOR
        }
    }
    private val mPaintLabels: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            textSize = context.dipToPixeel(12f)
            textAlign = Paint.Align.CENTER
        }
    }
    
    private var isInScreenshotMode = false

    val topOffsetPx: Float
    private val leftOffset: Float

    private var drawCount = 0

    var startTime: LocalTime = LocalTime.of(10, 0)
        private set
    private var endTime: LocalTime = LocalTime.of(13, 0)

    init {
        topOffsetPx = context.dipToPixeel(32f)
        leftOffset = context.dipToPixeel(48f)
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(TAG, "Drawing background for the ${++drawCount}. time.")
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        drawHorizontalDividers(canvas)
        canvas.drawColumnsWithHeaders()

        Log.d(TAG, "Screenshot mode? $isInScreenshotMode")
        if (!isInScreenshotMode && !isInEditMode) {
            drawNowIndicator(canvas)
        }
        Log.d(TAG, "Drawing background completed.")
    }

    private fun drawNowIndicator(canvas: Canvas) {
        if (startTime.isBefore(LocalTime.now()) && endTime.isAfter(LocalTime.now())) {
            Log.v(TAG, "Drawing now indicator")
            paintDivider.color = config.accentColor
            val nowOffset = Duration.between(startTime, LocalTime.now())

            val minutes = nowOffset.toMinutes()
            val y = topOffsetPx + context.dipToPixeel(minutes * config.stretchingFactor)
            canvas.drawLine(0f, y.toFloat(), canvas.width.toFloat(), y.toFloat(), paintDivider)
            paintDivider.color = DIVIDER_COLOR
        }
    }

    private fun Canvas.drawColumnsWithHeaders() {
        Log.v(TAG, "Drawing vertical dividers on canvas")
        for ((column, dayId) in days.withIndex()) {
            // draw the divider
            val xValue: Float = getColumnStart(column, false)
            left
            drawLine(xValue, 0f, xValue, bottom.toFloat(), paintDivider)

            // draw name
            val shortName = DateFormatSymbols().shortWeekdays[dayId]
            val xLabel = (getColumnStart(column, false) + getColumnEnd(column, false)) / 2
            drawText(shortName, xLabel, topOffsetPx / 2 + mPaintLabels.descent(), mPaintLabels)

        }
    }

    private fun drawHorizontalDividers(canvas: Canvas) {
        Log.d(TAG, "Drawing horizontal dividers")
        var localTime = startTime
        var last = LocalTime.MIN
        while (localTime.isBefore(endTime) && !last.isAfter(localTime)) {
            val offset = Duration.between(startTime, localTime)
            Log.v(TAG, "Offset $offset")
            val y = topOffsetPx + context.dipToPixeel(offset.toMinutes() * config.stretchingFactor)
            canvas.drawLine(0f, y, canvas.width.toFloat(), y, paintDivider)

            // final String timeString = localTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
            val timeString = localTime.toLocalString(context)
            drawMultiLineText(canvas, timeString, context.dipToPixeel(25f), y + context.dipToPixeel(20f), mPaintLabels)

            last = localTime
            localTime = localTime.plusHours(1)
        }
        val offset = Duration.between(startTime, localTime)
        Log.v(TAG, "Offset + $offset")
        canvas.drawLine(0f, bottom.toFloat(), canvas.width.toFloat(), bottom.toFloat(), paintDivider)
    }

    private fun drawMultiLineText(canvas: Canvas, text: String, initialX: Float, initialY: Float, paint: Paint) {
        var currentY = initialY
        text.split(" ")
                .dropLastWhile(String::isEmpty)
                .forEach {
                    canvas.drawText(it, initialX, currentY, paint)
                    currentY += (-paint.ascent() + paint.descent()).toInt()
                }
    }

    /**
     * Returns the offset (px!) from left for a given column.
     * First column is the first weekday.
     *
     * @param column starting to count at 0
     * @return offset in px
     */
    internal fun getColumnStart(column: Int, considerDivider: Boolean): Float {
        val contentWidth: Float = width - leftOffset
        var offset: Float = leftOffset + contentWidth * column / days.size
        if (considerDivider) {
            offset += (DIVIDER_WIDTH_PX / 2f)
        }
        return offset
    }

    internal fun getColumnEnd(column: Int, considerDivider: Boolean): Float {
        val contentWidth: Float = width - leftOffset
        var offset: Float = leftOffset + contentWidth * (column + 1) / days.size
        if (considerDivider) {
            offset -= (DIVIDER_WIDTH_PX / 2)
        }
        return offset
    }

    override fun onMeasure(widthMeasureSpec: Int, hms: Int) {
        val height = topOffsetPx + context.dipToPixeel(getDurationMinutes() * config.stretchingFactor) + paddingBottom
        val heightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(height.roundToInt(), View.MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec2)
    }


    fun setScreenshotMode(screenshotMode: Boolean) {
        isInScreenshotMode = screenshotMode
    }

    companion object {
        private val DIVIDER_WIDTH_PX: Float = 2f // should be a multiple of 2
        private val DIVIDER_COLOR = Color.LTGRAY
    }

    fun updateTimes(startTime: LocalTime, endTime: LocalTime) {
        if (startTime.isAfter(endTime)) throw IllegalArgumentException()
        if (startTime.isBefore(this.startTime)) {
            this.startTime = startTime.truncatedTo(ChronoUnit.HOURS)
        }
        if (endTime.isAfter(this.endTime)) {
            this.endTime = endTime.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        }
    }

    private fun getDurationMinutes(): Long {
        return Duration.between(startTime, endTime).toMinutes()
    }
}
