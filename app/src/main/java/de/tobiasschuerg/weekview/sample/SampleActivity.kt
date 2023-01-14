package de.tobiasschuerg.weekview.sample

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.threetenabp.AndroidThreeTen
import de.tobiasschuerg.weekview.data.EventConfig
import de.tobiasschuerg.weekview.view.EventView
import de.tobiasschuerg.weekview.view.WeekView
import kotlin.math.abs

class SampleActivity : AppCompatActivity() {

    private val weekView: WeekView by lazy { findViewById(R.id.week_view) }
    private var x1 = 0f
    private var x2 = 0f
    private val MIN_DISTANCE = 100

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidThreeTen.init(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        val config = EventConfig()
        weekView.eventConfig = config
        weekView.setShowNowIndicator(true)

        weekView.redraw()

        // optional: add an onClickListener for each event
        weekView.setEventClickListener {
            Toast.makeText(applicationContext, "Removing " + it.event.title, Toast.LENGTH_SHORT).show()
            weekView.removeView(it)
        }

        // optional: register a context menu to each event
        registerForContextMenu(weekView)

        weekView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x1 = event.x
                }
                MotionEvent.ACTION_UP -> {
                    x2 = event.x
                    val deltaX = x2 - x1
                    if (abs(deltaX) > MIN_DISTANCE) {
                        if (deltaX < 0) {
                            weekView.advanceInitialDay()
                        } else {
                            weekView.reculInitialDay()
                        }
                    } else {
                        // consider as something else - a screen tap for example
                    }
                }
            }
            when (event.pointerCount) {
                1 -> {
                    Log.d("Scroll", "1-pointer touch")
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                2 -> {
                    Log.d("Zoom", "2-pointer touch")
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            false
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        val (event) = menuInfo as EventView.LessonViewContextInfo
        menu.setHeaderTitle(event.title)
        menu.add("First Option")
        menu.add("Second Option")
        menu.add("Third Option")
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("<").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add(">").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.title) {
            "<" -> {
                Log.i(TAG, "add option clicked")
                weekView.reculInitialDay()
            }
            ">" -> {
                Log.i(TAG, "clear option clicked")
                weekView.advanceInitialDay()
            }
        }
        return true
    }

    companion object {
        private const val TAG = "SampleActivity"
    }
}
