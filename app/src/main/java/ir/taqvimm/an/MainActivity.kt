
package ir.taqvimm.an

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val green = Color.rgb(16, 77, 67)
    private val gold = Color.rgb(205, 155, 65)
    private val light = Color.rgb(245, 248, 246)
    private val white = Color.WHITE

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

    private fun rounded(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun label(
        value: String,
        size: Float,
        color: Int = green
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textDirection = TextView.TEXT_DIRECTION_RTL
            setPadding(10, 12, 10, 12)
        }
    }

    private fun actionButton(
        value: String,
        action: () -> Unit,
        color: Int = green
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 17f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textDirection = TextView.TEXT_DIRECTION_RTL
            background = rounded(color, 28f)
            elevation = 4f
            isClickable = true
            isFocusable = true
            setPadding(12, 16, 12, 16)
            minHeight = 62
            setOnClickListener { action() }
        }
    }

    private fun addGap(root: LinearLayout, height: Int) {
        root.addView(
            android.view.View(this),
            LinearLayout.LayoutParams(1, height)
        )
    }

    private fun buttonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 7, 0, 7)
        }
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(light)
        scroll.isFillViewport = true

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(20, 24, 20, 28)

        scroll.addView(root)
        return Pair(scroll, root)
    }

    private fun showHome() {
        val (scroll, root) = page()

        root.addView(label("🌙 تقویم من", 30f, green))
        root.addView(label("تقویم، آب‌وهوا و سرگرمی در یک جا", 15f))

        addGap(root, 12)

        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val dateCard = label(
            "امروز، ${fa(today.get(Calendar.DAY_OF_MONTH))} " +
            "${months[today.get(Calendar.MONTH)]} " +
            "${fa(today.get(Calendar.YEAR))}",
            19f,
            white
        )
        dateCard.background = rounded(green, 24f)
        root.addView(dateCard, buttonParams())

        addGap(root, 12)

        root.addView(actionButton("📅   تقویم شمسی") {
            showCalendar()
        }, buttonParams())

        root.addView(actionButton("🕌   اوقات شرعی و اذان") {
            android.widget.Toast.makeText(
                this, "بخش اوقات شرعی در حال آماده‌سازی است",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }, buttonParams())

        root.addView(actionButton("🌤️   هواشناسی") {
            android.widget.Toast.makeText(
                this, "بخش هواشناسی در حال آماده‌سازی است",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }, buttonParams())

        root.addView(actionButton("🎮   بازی کلمات") {
            android.widget.Toast.makeText(
                this, "بازی کلمات در حال آماده‌سازی است",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }, buttonParams())

        addGap(root, 12)

        root.addView(actionButton("▶️   شروع پخش اذان") {
            val intent = Intent(
                this, AdhanService::class.java
            ).setAction(AdhanService.ACTION_START)
            ContextCompat.startForegroundService(this, intent)
        }, buttonParams())

        root.addView(actionButton("⏹   توقف اذان") {
            val intent = Intent(
                this, AdhanService::class.java
            ).setAction(AdhanService.ACTION_STOP)
            startService(intent)
        }, buttonParams())

        root.addView(label("طراحی اختصاصی • Pouyan", 13f, gold))

        setContentView(scroll)
    }

    private fun showCalendar() {
        val (scroll, root) = page()

        root.addView(label("📅 تقویم شمسی", 27f, green))

        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val todayCard = label(
            "امروز: ${fa(today.get(Calendar.DAY_OF_MONTH))} " +
            "${months[today.get(Calendar.MONTH)]} " +
            "${fa(today.get(Calendar.YEAR))}",
            18f,
            white
        )
        todayCard.background = rounded(green, 22f)
        root.addView(todayCard, buttonParams())

        addGap(root, 12)

        val navigation = LinearLayout(this)
        navigation.orientation = LinearLayout.HORIZONTAL
        navigation.gravity = Gravity.CENTER
        navigation.layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL

        val previous = actionButton("ماه قبل ←", {
            displayedMonth.add(Calendar.MONTH, -1)
            showCalendar()
        }, gold)

        val next = actionButton("→ ماه بعد", {
            displayedMonth.add(Calendar.MONTH, 1)
            showCalendar()
        }, gold)

        navigation.addView(
            previous,
            LinearLayout.LayoutParams(0, 58, 1f).apply {
                setMargins(4, 4, 4, 4)
            }
        )

        navigation.addView(
            next,
            LinearLayout.LayoutParams(0, 58, 1f).apply {
                setMargins(4, 4, 4, 4)
            }
        )

        root.addView(navigation)

        root.addView(actionButton("بازگشت به ماه جاری", {
            displayedMonth.timeInMillis = System.currentTimeMillis()
            showCalendar()
        }), buttonParams())

        val year = displayedMonth.get(Calendar.YEAR)
        val month = displayedMonth.get(Calendar.MONTH)

        root.addView(
            label("${months[month]} ${fa(year)}", 25f, green)
        )

        addGap(root, 8)

        val grid = GridLayout(this)
        grid.columnCount = 7
        grid.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL

        for (dayName in weekdays) {
            val cell = label(dayName, 11f, green)
            cell.setPadding(1, 8, 1, 8)
            grid.addView(cell, gridParams())
        }

        val firstDay = displayedMonth.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)

        val offset = firstDay.get(Calendar.DAY_OF_WEEK) % 7
        val daysInMonth =
            displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until offset) {
            grid.addView(label("", 14f), gridParams())
        }

        for (day in 1..daysInMonth) {
            val isToday =
                year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)

            val cell = label(
                fa(day),
                16f,
                if (isToday) white else green
            )

            if (isToday) {
                cell.background = rounded(green, 18f)
            }

            grid.addView(cell, gridParams())
        }

        root.addView(grid)

        addGap(root, 16)

        root.addView(actionButton("🏠   صفحه اصلی", {
            showHome()
        }), buttonParams())

        setContentView(scroll)
    }

    private fun gridParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            height = 52
            columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED, 1f
            )
            setMargins(2, 3, 2, 3)
        }
    }
}


            
