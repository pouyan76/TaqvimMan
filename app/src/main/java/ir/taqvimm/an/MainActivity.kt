
package ir.taqvimm.an

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.icu.util.Calendar
import android.icu.util.TimeZone
import java.util.Locale

class MainActivity : Activity() {

    private val green = Color.rgb(16, 77, 67)
    private val gold = Color.rgb(205, 155, 65)
    private val light = Color.rgb(245, 248, 246)
    private val red = Color.rgb(190, 45, 45)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayedMonth = newPersianCalendar()
        displayedMonth.timeInMillis = System.currentTimeMillis()

        showHome()
    }

    private fun newPersianCalendar(): Calendar {
        return Calendar.getInstance(
            TimeZone.getTimeZone("Asia/Tehran"),
            Locale.forLanguageTag("fa-IR-u-ca-persian")
        )
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
            textDirection = View.TEXT_DIRECTION_RTL
            typeface = Typeface.DEFAULT
            setPadding(4, 8, 4, 8)
        }
    }

    private fun actionButton(
        value: String,
        color: Int = green,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(color, 22f)
            elevation = 4f
            isClickable = true
            isFocusable = true
            setPadding(8, 14, 8, 14)
            minHeight = 58
            setOnClickListener { action() }
        }
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(light)
        scroll.isFillViewport = true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(16, 20, 16, 24)
        }

        scroll.addView(root)
        return Pair(scroll, root)
    }

    private fun buttonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 6, 0, 6)
        }
    }

    private fun showHome() {
        val (scroll, root) = page()

        root.addView(label("🌙 تقویم من", 30f))
        root.addView(
            label("تقویم، آب‌وهوا و سرگرمی در یک جا", 15f)
        )

        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val date = "امروز، ${fa(today.get(Calendar.DAY_OF_MONTH))} " +
            "${months[today.get(Calendar.MONTH)]} " +
            fa(today.get(Calendar.YEAR))

        val dateCard = label(date, 19f, white)
        dateCard.background = rounded(green, 22f)
        root.addView(dateCard, buttonParams())

        root.addView(
            actionButton("📅 تقویم شمسی") {
                showCalendar()
            },
            buttonParams()
        )

        root.addView(
            actionButton("🕌 اوقات شرعی و اذان") {
                Toast.makeText(
                    this,
                    "بخش اوقات شرعی هنوز آماده نشده است",
                    Toast.LENGTH_SHORT
                ).show()
            },
            buttonParams()
        )

        root.addView(
            actionButton("🌤️ هواشناسی") {
                Toast.makeText(
                    this,
                    "بخش هواشناسی هنوز آماده نشده است",
                    Toast.LENGTH_SHORT
                ).show()
            },
            buttonParams()
        )

        root.addView(
            actionButton("🎮 بازی ۵۰۰ مرحله‌ای") {
                Toast.makeText(
                    this,
                    "بازی هنوز آماده نشده است",
                    Toast.LENGTH_SHORT
                ).show()
            },
            buttonParams()
        )

        root.addView(label("طراحی اختصاصی • Pouyan", 13f, gold))
        setContentView(scroll)
    }

    private fun showCalendar() {
        val (scroll, root) = page()

        root.addView(label("📅 تقویم شمسی", 27f))

        val year = displayedMonth.get(Calendar.YEAR)
        val month = displayedMonth.get(Calendar.MONTH)

        root.addView(
            label("${months[month]} ${fa(year)}", 23f)
        )

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val nextButton = actionButton("ماه بعد ←", gold) {
            displayedMonth.add(Calendar.MONTH, 1)
            showCalendar()
        }

        val previousButton = actionButton("→ ماه قبل", gold) {
            displayedMonth.add(Calendar.MONTH, -1)
            showCalendar()
        }

        navigation.addView(
            nextButton,
            LinearLayout.LayoutParams(0, 64, 1f).apply {
                setMargins(4, 4, 4, 4)
            }
        )

        navigation.addView(
            previousButton,
            LinearLayout.LayoutParams(0, 64, 1f).apply {
                setMargins(4, 4, 4, 4)
            }
        )

        root.addView(navigation, buttonParams())

        root.addView(
            actionButton("بازگشت به ماه جاری") {
                displayedMonth.timeInMillis = System.currentTimeMillis()
                showCalendar()
            },
            buttonParams()
        )

        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val grid = GridLayout(this).apply {
            columnCount = 7
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        for (name in weekdays) {
            val color = if (name == "جمعه") red else green

            grid.addView(
                label(name, 10f, color),
                gridParams()
            )
        }

        val firstDay = displayedMonth.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)

        val offset = firstDay.get(Calendar.DAY_OF_WEEK) % 7
        val days = displayedMonth.getActualMaximum(
            Calendar.DAY_OF_MONTH
        )

        repeat(offset) {
            grid.addView(label("", 14f), gridParams())
        }

        for (day in 1..days) {
            val date = displayedMonth.clone() as Calendar
            date.set(Calendar.DAY_OF_MONTH, day)

            val isFriday =
                date.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

            val isToday =
                year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)

            val cell = label(
                fa(day),
                16f,
                when {
                    isToday -> white
                    isFriday -> red
                    else -> green
                }
            )

            if (isToday) {
                cell.background = rounded(green, 16f)
            }

            grid.addView(cell, gridParams())
        }

        root.addView(grid)

        root.addView(
            actionButton("🏠 بازگشت به صفحه اصلی") {
                showHome()
            },
            buttonParams()
        )

        root.addView(
            label("جمعه‌ها با رنگ قرمز مشخص شده‌اند", 13f, red)
        )

        root.addView(label("Pouyan • Taqvim Man", 13f, gold))

        setContentView(scroll)
    }

    private fun gridParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            height = 54
            columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED,
                1f
            )
            setMargins(2, 3, 2, 3)
        }
    }
}
