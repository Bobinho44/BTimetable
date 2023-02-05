package de.tobiasschuerg.weekview.sample

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.jakewharton.threetenabp.AndroidThreeTen
import de.tobiasschuerg.weekview.data.EventConfig
import de.tobiasschuerg.weekview.view.EventView
import de.tobiasschuerg.weekview.view.WeekView
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import kotlin.math.abs

class SampleActivity : AppCompatActivity() {

    private val weekView: WeekView by lazy { findViewById(R.id.week_view) }
    private val weekInfoView: WeekView by lazy { findViewById(R.id.textView2) }
    private var actualDay: LocalDate = LocalDate.now(Clock.systemUTC())
    private var x1 = 0f
    private var x2 = 0f

    private fun showPopup(weekView: WeekView) {
        val popupView = layoutInflater.inflate(R.layout.popup_layout, null)
        val popupWindow = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        )

        val textView = popupView.findViewById<TextView>(R.id.textView)
        textView.text = "Ceci est un exemple de popup prenant tout l'écran"


        val closeButton = popupView.findViewById<ImageButton>(R.id.closeButton)
        closeButton.setOnClickListener { popupWindow.dismiss() }

        popupWindow.showAtLocation(weekView, Gravity.CENTER, 0, 0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidThreeTen.init(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        val config = EventConfig()
        weekView.initTextView(findViewById(R.id.textView2))
        weekView.eventConfig = config
        weekView.setShowNowIndicator(true)

        weekView.redraw()

        // optional: add an onClickListener for each event
        weekView.setEventClickListener {
            showPopup(weekView)
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
                    if (abs(deltaX) > 200) {
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

        findViewById<ImageButton>(R.id.imageButton).setOnClickListener { weekView.advanceInitialDay() }
        findViewById<ImageButton>(R.id.imageButton2).setOnClickListener { weekView.reculInitialDay() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("settings").setIcon(android.R.drawable.ic_menu_info_details).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.title) {
            "settings" -> {
                Log.i(TAG, "settings")
            }
        }
        return true
    }

    private fun redraw() {

    }

    companion object {
        private const val TAG = "SampleActivity"
    }

}
