package de.tobiasschuerg.timetableview.util

import android.content.Context
import android.view.View.MeasureSpec


object ViewHelper {

    fun dp2px(dip: Float, context: Context): Float = context.dipToPixeel(dip)

    fun debugMeasureSpec(spec: Int): String {
        val mode = MeasureSpec.getMode(spec)
        val size = MeasureSpec.getSize(spec)
        return when (mode) {
            MeasureSpec.EXACTLY -> "Exactly $size px"
            MeasureSpec.AT_MOST -> "At most $size px"
            MeasureSpec.UNSPECIFIED -> "Unspecified ($size px)"
            else -> "? $size px"
        }
    }

}


