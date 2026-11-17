
package ir.taqvimm.an

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val green = Color.rgb(16, 77, 67)
    private val light = Color.rgb(245, 248, 246)

    private lateinit var displayedMonth: Calendar

    private val months = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val weekdays = arrayOf(
        "شنبه", "یکشنبه", "دوشنبه",
        "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    )

    private fun newPersianCalendar(): Calendar {
        return Calendar.getInstance(
            TimeZone.getTimeZone("Asia/Tehran"),
            Locale.forLanguageTag("fa-IR-u-ca-persian")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayedMonth = newPersianCalendar()
        displayedMonth.timeInMillis = System.currentTimeMillis()

        showHome()
    }

    private fun fa(number: Int): String {
        val digits = "۰۱۲۳۴۵۶۷۸۹"
        return number.toString().map {
            if (it in '0'..'9') digits[it - '0'] else it
        }.joinToString("")
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int = Color.BLACK
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            textDirection = TextView.TEXT_DIRECTION_RTL
            setPadding(6, 10, 6, 10)
        }
    }

    private fun makeButton(
        value: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { action() }
        }
    }

    private fun buttonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            62
        ).apply {
            setMargins(0, 8, 0, 8)
        }
    }

    private fun showHome() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(24, 35, 24, 24)
        root.setBackgroundColor(light)

        root.addView(makeText("🌙 تقویم من", 30f, green))
        root.addView(makeText("همراه روزهای خوب زندگی", 17f))

        root.addView(makeButton("📅 تقویم شمسی") {
            showCalendar()
        }, buttonParams())

        root.addView(makeButton("🕌 اوقات شرعی و اذان") {
            Toast.makeText(
                this,
                "بخش اوقات شرعی در حال آماده‌سازی است",
                Toast.LENGTH_SHORT
            ).show()
        }, buttonParams())

        root.addView(makeButton("🌤️ هواشناسی") {
            Toast.makeText(
                this,
                "بخش هواشناسی در حال آماده‌سازی است",
                Toast.LENGTH_SHORT
            ).show()
        }, buttonParams())

        root.addView(makeButton("🎮 بازی کلمات") {
            Toast.makeText(
                this,
                "بازی کلمات در حال آماده‌سازی است",
                Toast.LENGTH_SHORT
            ).show()
        }, buttonParams())

        root.addView(makeButton("▶️ شروع پخش اذان") {
            val intent = Intent(
                this,
                AdhanService::class.java
            ).setAction(AdhanService.ACTION_START)

            ContextCompat.startForegroundService(this, intent)
        }, buttonParams())

        root.addView(makeButton("⏹ توقف اذان") {
            val intent = Intent(
                this,
                AdhanService::class.java
            ).setAction(AdhanService.ACTION_STOP)

            startService(intent)
        }, buttonParams())

        setContentView(root)
    }

    private fun showCalendar() {
        val scroll = ScrollView(this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(16, 20, 16, 24)
        root.setBackgroundColor(light)

        scroll.addView(root)

        root.addView(makeText("📅 تقویم شمسی", 27f, green))

        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val todayText = makeText(
            "امروز: ${fa(today.get(Calendar.DAY_OF_MONTH))} " +
            "${months[today.get(Calendar.MONTH)]} " +
            "${fa(today.get(Calendar.YEAR))}",
            19f,
            green
        )

        root.addView(todayText)

        val navigation = LinearLayout(this)
        navigation.orientation = LinearLayout.HORIZONTAL
        navigation.gravity = Gravity.CENTER
        navigation.setPadding(0, 15, 0, 15)

        val previous = makeButton("ماه قبل") {
            displayedMonth.add(Calendar.MONTH, -1)
            showCalendar()
        }

        val next = makeButton("ماه بعد") {
            displayedMonth.add(Calendar.MONTH, 1)
            showCalendar()
        }

        val currentMonth = makeButton("ماه جاری") {
            displayedMonth.timeInMillis =
                System.currentTimeMillis()
            showCalendar()
        }

        navigation.addView(
            previous,
            LinearLayout.LayoutParams(0, 55, 1f)
        )

        navigation.addView(
            currentMonth,
            LinearLayout.LayoutParams(0, 55, 1f)
        )

        navigation.addView(
            next,
            LinearLayout.LayoutParams(0, 55, 1f)
        )

        root.addView(navigation)

        val year = displayedMonth.get(Calendar.YEAR)
        val month = displayedMonth.get(Calendar.MONTH)

        root.addView(
            makeText(
                "${months[month]} ${fa(year)}",
                23f,
                green
            )
        )

        val grid = GridLayout(this)
        grid.columnCount = 7
        grid.rowCount = 7
        grid.useDefaultMargins = true

        for (dayName in weekdays) {
            grid.addView(
                makeText(dayName, 12f, green),
                gridParams()
            )
        }

        val firstDay = displayedMonth.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)

        val offset = firstDay.get(Calendar.DAY_OF_WEEK) % 7

        val daysInMonth =
            displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until offset) {
            grid.addView(
                makeText("", 15f),
                gridParams()
            )
        }

        for (day in 1..daysInMonth) {
            val isToday =
                year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)

            val cell = makeText(
                fa(day),
                17f,
                if (isToday) Color.WHITE else Color.BLACK
            )

            if (isToday) {
                cell.setBackgroundColor(green)
            }

            grid.addView(cell, gridParams())
        }

        root.addView(grid)

        root.addView(
            makeButton("بازگشت به صفحه اصلی") {
                showHome()
            },
            buttonParams()
        )

        setContentView(scroll)
    }

    private fun gridParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            height = 52
            columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED,
                1f
            )
            setMargins(2, 3, 2, 3)
        }
    }
}
