package de.tobiasschuerg.weekview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.View
import de.tobiasschuerg.weekview.util.TimeSpan
import de.tobiasschuerg.weekview.util.dipToPixelF
import de.tobiasschuerg.weekview.util.dipToPixelI
import de.tobiasschuerg.weekview.util.toLocalString
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class WeekBackgroundView constructor(context: Context) : View(context) {

    private val accentPaint: Paint by lazy {
        Paint().apply { strokeWidth = DIVIDER_WIDTH_PX.toFloat() * 2 }
    }

    private val paintDivider: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeWidth = DIVIDER_WIDTH_PX.toFloat()
            color = DIVIDER_COLOR
        }
    }
    private val mPaintLabels: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            textSize = context.dipToPixelF(12f)
            textAlign = Paint.Align.CENTER
        }
    }

    var showNowIndicator = false

    val topOffsetPx: Int = context.dipToPixelI(32f)
    private val leftOffset: Int = context.dipToPixelI(48f)

    private var drawCount = 0

    private var days: List<LocalDate> = listOf(LocalDate.now(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))

    var startTime: LocalTime = LocalTime.of(8, 0)

    private var endTime: LocalTime = LocalTime.of(23, 0)

    var scalingFactor = 1f
        /**
         * Updated the scaling factor and redraws the view.
         */
        set(value) {
            field = max(0.6f, value)
            requestLayout()
            // invalidate()
        }


    fun setDays(days: List<LocalDate>) {
        this.days = days
    }

    fun setAccentColor(color: Int) {
        accentPaint.color = color
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(TAG, "Drawing background for the ${++drawCount}. time.")
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        canvas.drawHorizontalDividers()
        canvas.drawColumnsWithHeaders()

        Log.d(TAG, "Show now indicator? $showNowIndicator")
        if (showNowIndicator && !isInEditMode) {
            drawNowIndicator(canvas)
        }
        Log.d(TAG, "Drawing background completed.")
    }

    private fun drawNowIndicator(canvas: Canvas) {
        if (startTime.isBefore(LocalTime.now()) && endTime.isAfter(LocalTime.now())) {
            Log.v(TAG, "Drawing now indicator")
            val nowOffset = Duration.between(startTime, LocalTime.now())

            val minutes = nowOffset.toMinutes()
            val y = topOffsetPx + context.dipToPixelF(minutes * scalingFactor)
            accentPaint.alpha = 200
            canvas.drawLine(0f, y, canvas.width.toFloat(), y, accentPaint)
        }
    }

    private fun Canvas.drawHorizontalDividers() {
        Log.d(TAG, "Drawing horizontal dividers")
        var localTime = startTime
        var last = LocalTime.MIN
        drawLine(0f, 0f, width.toFloat(), 0f, paintDivider)
        while (localTime.isBefore(endTime) && !last.isAfter(localTime)) {
            val offset = Duration.between(startTime, localTime)
            Log.v(TAG, "Offset $offset")
            val y = topOffsetPx + context.dipToPixelF(offset.toMinutes() * scalingFactor)
            drawLine(0f, y, width.toFloat(), y, paintDivider)

            // final String timeString = localTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
            val timeString = localTime.toLocalString()
            drawMultiLineText(
                this,
                timeString,
                context.dipToPixelF(25f),
                y + context.dipToPixelF(20f),
                mPaintLabels
            )

            last = localTime
            localTime = localTime.plusHours(1)
        }
        val offset = Duration.between(startTime, localTime)
        Log.v(TAG, "Offset + $offset")
        drawLine(0f, bottom.toFloat(), width.toFloat(), bottom.toFloat(), paintDivider)
    }

    private fun Canvas.drawColumnsWithHeaders() {
        Log.v(TAG, "Drawing vertical dividers on canvas")
        val todayDay: LocalDate = LocalDate.now()
        for ((column, dayId) in days.withIndex()) {
            drawLeftColumnDivider(column)
            drawWeekDayName(dayId, column)
            if (dayId == todayDay) {
                drawDayHighlight(column)
            }
        }
    }

    private fun Canvas.drawLeftColumnDivider(column: Int) {
        val left: Int = getColumnStart(column, false)
        drawLine(left.toFloat(), 0f, left.toFloat(), bottom.toFloat(), paintDivider)
    }

    private fun Canvas.drawDayHighlight(column: Int) {
        val left2: Int = getColumnStart(column, true)
        val right: Int = getColumnEnd(column, true)
        val rect = Rect(left2, 0, right, bottom)
        accentPaint.alpha = 32
        drawRect(rect, accentPaint)
    }

    private fun Canvas.drawWeekDayName(day: LocalDate, column: Int) {
        val shortName = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + day.dayOfMonth + " " + day.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + "."
        val xLabel = (getColumnStart(column, false) + getColumnEnd(column, false)) / 2
        drawText(
            shortName.uppercase(),
            xLabel.toFloat(),
            topOffsetPx / 2 + mPaintLabels.descent(),
            mPaintLabels
        )
    }

    private fun drawMultiLineText(
        canvas: Canvas,
        text: String,
        initialX: Float,
        initialY: Float,
        paint: Paint
    ) {
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
    internal fun getColumnStart(column: Int, considerDivider: Boolean): Int {
        val contentWidth: Int = width - leftOffset
        var offset: Int = leftOffset + contentWidth * column / days.size
        if (considerDivider) {
            offset += (DIVIDER_WIDTH_PX / 2)
        }
        return offset
    }

    internal fun getColumnEnd(column: Int, considerDivider: Boolean): Int {
        val contentWidth: Int = width - leftOffset
        var offset: Int = leftOffset + contentWidth * (column + 1) / days.size
        if (considerDivider) {
            offset -= (DIVIDER_WIDTH_PX / 2)
        }
        return offset
    }

    override fun onMeasure(widthMeasureSpec: Int, hms: Int) {
        val height =
            topOffsetPx + context.dipToPixelF(getDurationMinutes() * scalingFactor) + paddingBottom
        val heightMeasureSpec2 =
            MeasureSpec.makeMeasureSpec(height.roundToInt(), MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec2)
    }

    fun updateTimes(timeSpan: TimeSpan) {
        var timesHaveChanged = false
        if (timeSpan.start.isBefore(startTime)) {
            startTime = timeSpan.start.truncatedTo(ChronoUnit.HOURS)
            timesHaveChanged = true
        }
        if (timeSpan.endExclusive.isAfter(endTime)) {
            if (timeSpan.endExclusive.isBefore(LocalTime.of(23, 0))) {
                this.endTime = timeSpan.endExclusive.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            } else {
                this.endTime = LocalTime.MAX
            }
            timesHaveChanged = true
        }
        if (this.startTime.isAfter(this.endTime)) throw IllegalArgumentException()

        if (timesHaveChanged) {
            requestLayout()
        }
    }

    private fun getDurationMinutes(): Long {
        return Duration.between(startTime, endTime).toMinutes()
    }

    companion object {
        /** Thickness of the grid.
         * Should be a multiple of 2 because of rounding. */
        private const val DIVIDER_WIDTH_PX: Int = 2
        private const val DIVIDER_COLOR = Color.LTGRAY
        private const val TAG = "WeekBackgroundView"
    }
}
