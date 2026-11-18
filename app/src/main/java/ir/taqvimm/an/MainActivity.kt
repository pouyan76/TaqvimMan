
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val green = Color.rgb(14, 65, 57)
    private val greenLight = Color.rgb(35, 111, 91)
    private val gold = Color.rgb(218, 171, 91)
    private val background = Color.rgb(245, 247, 245)
    private val white = Color.WHITE
    private val red = Color.rgb(190, 55, 55)
    private val gray = Color.rgb(110, 120, 116)

    private lateinit var displayedMonth: Calendar
    private val executor = Executors.newSingleThreadExecutor()

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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
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

    private fun faTime(value: String): String {
        return value.map {
            if (it in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[it - '0'] else it
        }.joinToString("")
    }

    private fun shape(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun gradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(green, greenLight)
        ).apply {
            cornerRadius = dp(26).toFloat()
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = green,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(background)
        scroll.isFillViewport = true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(18), dp(16), dp(28))
        }

        scroll.addView(root)
        return Pair(scroll, root)
    }

    private fun params(
        top: Int = 8,
        bottom: Int = 8
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dp(top), 0, dp(bottom))
        }
    }

    private fun featureCard(
        emoji: String,
        title: String,
        description: String,
        color: Int,
        action: () -> Unit
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(white, 22)
            elevation = dp(3).toFloat()
            setPadding(dp(8), dp(16), dp(8), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        val icon = text(emoji, 34f)
        card.addView(icon)

        card.addView(
            text(title, 16f, green, true)
        )

        card.addView(
            text(description, 11f, gray)
        )

        val line = View(this).apply {
            setBackgroundColor(color)
        }

        card.addView(
            line,
            LinearLayout.LayoutParams(
                dp(38),
                dp(3)
            ).apply {
                topMargin = dp(8)
            }
        )

        return card
    }

    private fun addFeatureRow(
        root: LinearLayout,
        first: View,
        second: View
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        row.addView(
            first,
            LinearLayout.LayoutParams(0, dp(150), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        row.addView(
            second,
            LinearLayout.LayoutParams(0, dp(150), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        root.addView(row, params(5, 5))
    }

    private fun showHome() {
        val (scroll, root) = page()

        // سربرگ برنامه
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradient()
            setPadding(dp(18), dp(22), dp(18), dp(22))
            elevation = dp(4).toFloat()
        }

        header.addView(
            text("تقویم من", 30f, white, true)
        )

        header.addView(
            text("یک همراه هوشمند برای هر روز شما", 14f, white)
        )

        header.addView(
            text("POUYAN  •  TAQVIM MAN", 11f, gold, true)
        )

        root.addView(header, params(0, 12))

        // تاریخ امروز
        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val day = today.get(Calendar.DAY_OF_MONTH)
        val month = today.get(Calendar.MONTH)
        val year = today.get(Calendar.YEAR)

        val dateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(white, 20)
            elevation = dp(2).toFloat()
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        dateCard.addView(
            text("امروز در تقویم شمسی", 13f, gray)
        )

        dateCard.addView(
            text(
                "${fa(day)} ${months[month]} ${fa(year)}",
                23f,
                green,
                true
            )
        )

        val weekDay = today.get(Calendar.DAY_OF_WEEK)
        val weekIndex = (weekDay + 5) % 7

        dateCard.addView(
            text(weekdays[weekIndex], 14f, gold, true)
        )

        root.addView(dateCard, params(0, 14))

        root.addView(
            text("خدمات برنامه", 19f, green, true)
        )

        root.addView(
            text("یکی از بخش‌های زیر را انتخاب کنید", 12f, gray)
        )

        // کارت‌های اصلی
        addFeatureRow(
            root,
            featureCard(
                "📅",
                "تقویم شمسی",
                "روزها و ماه‌های سال",
                green
            ) {
                showCalendar()
            },
            featureCard(
                "🕌",
                "اوقات شرعی",
                "اذان و زمان نماز شیراز",
                gold
            ) {
                showPrayerTimes()
            }
        )

        addFeatureRow(
            root,
            featureCard(
                "🌤️",
                "هواشناسی",
                "آب‌وهوای شیراز",
                Color.rgb(65, 145, 190)
            ) {
                Toast.makeText(
                    this,
                    "بخش هواشناسی در حال آماده‌سازی است",
                    Toast.LENGTH_SHORT
                ).show()
            },
            featureCard(
                "🎮",
                "بازی کلمات",
                "۵۰۰ مرحله سرگرمی",
                Color.rgb(150, 100, 180)
            ) {
                Toast.makeText(
                    this,
                    "بخش بازی در حال آماده‌سازی است",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        // نوار پایینی
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(green, 18)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        footer.addView(
            text("با تقویم من، روزهای خود را بهتر مدیریت کنید", 13f, white)
        )

        footer.addView(
            text("ساخته شده با ❤️ توسط Pouyan", 12f, gold)
        )

        root.addView(footer, params(16, 4))

        setContentView(scroll)
    }

    private fun showPrayerTimes() {
        val (scroll, root) = page()

        root.addView(
            text("🕌 اوقات شرعی شیراز", 25f, green, true),
            params(5, 5)
        )

        root.addView(
            text("استان فارس • ایران", 14f, gold)
        )

        val dateFormat = SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.US
        )

        dateFormat.timeZone =
            java.util.TimeZone.getTimeZone("Asia/Tehran")

        val today = dateFormat.format(Date())

        root.addView(
            text("تاریخ: $today", 14f, gray)
        )

        val status = text(
            "در حال دریافت زمان اذان...",
            15f,
            gold
        )

        root.addView(status, params())

        val prayerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(white, 22)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = dp(3).toFloat()
        }

        root.addView(prayerBox, params())

        val names = listOf(
            "اذان صبح" to "Fajr",
            "طلوع آفتاب" to "Sunrise",
            "اذان ظهر" to "Dhuhr",
            "اذان عصر" to "Asr",
            "غروب آفتاب" to "Sunset",
            "اذان مغرب" to "Maghrib",
            "اذان عشا" to "Isha"
        )

        executor.execute {
            var connection: HttpURLConnection? = null

            try {
                val url = URL(
                    "https://api.aladhan.com/v1/timings/$today" +
                    "?latitude=29.5918&longitude=52.5837&method=7"
                )

                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"

                if (connection.responseCode !in 200..299) {
                    throw Exception("Network error")
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val timings = JSONObject(response)
                    .getJSONObject("data")
                    .getJSONObject("timings")

                val results = names.map {
                    it.first to timings.getString(it.second).take(5)
                }

                runOnUiThread {
                    prayerBox.removeAllViews()

                    for ((name, time) in results) {
                        prayerBox.addView(
                            text(
                                "$name     ${faTime(time)}",
                                18f,
                                green,
                                true
                            )
                        )
                    }

                    status.text = "زمان‌های امروز دریافت شد"
                    status.setTextColor(green)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "دریافت زمان‌ها ناموفق بود"
                    status.setTextColor(red)
                }
            } finally {
                connection?.disconnect()
            }
        }

        root.addView(
            featureCard(
                "🔄",
                "دریافت دوباره",
                "به‌روزرسانی زمان‌ها",
                gold
            ) {
                showPrayerTimes()
            },
            params()
        )

        root.addView(
            featureCard(
                "🏠",
                "صفحه اصلی",
                "بازگشت به خانه",
                green
            ) {
                showHome()
            },
            params()
        )

        setContentView(scroll)
    }

    private fun showCalendar() {
        val (scroll, root) = page()

        root.addView(
            text("📅 تقویم شمسی", 26f, green, true)
        )

        val year = displayedMonth.get(Calendar.YEAR)
        val month = displayedMonth.get(Calendar.MONTH)

        root.addView(
            text("${months[month]} ${fa(year)}", 23f, gold, true)
        )

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val previous = text("ماه قبل", 15f, white, true).apply {
            background = shape(green, 15)
            setOnClickListener {
                displayedMonth.add(Calendar.MONTH, -1)
                showCalendar()
            }
        }

        val next = text("ماه بعد", 15f, white, true).apply {
            background = shape(green, 15)
            setOnClickListener {
                displayedMonth.add(Calendar.MONTH, 1)
                showCalendar()
            }
        }

        navigation.addView(
            previous,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(5)
            }
        )

        navigation.addView(
            next,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(5)
            }
        )

        root.addView(navigation, params())

        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val grid = GridLayout(this).apply {
            columnCount = 7
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = shape(white, 18)
            setPadding(dp(5), dp(8), dp(5), dp(8))
        }

        for (name in weekdays) {
            grid.addView(
                text(
                    name,
                    10f,
                    if (name == "جمعه") red else green,
                    true
                ),
                gridParams()
            )
        }

        val first = displayedMonth.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH, 1)

        val offset = first.get(Calendar.DAY_OF_WEEK) % 7

        val days = displayedMonth.getActualMaximum(
            Calendar.DAY_OF_MONTH
        )

        repeat(offset) {
            grid.addView(text("", 14f), gridParams())
        }

        for (day in 1..days) {
            val date = displayedMonth.clone() as Calendar
            date.set(Calendar.DAY_OF_MONTH, day)

            val friday =
                date.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

            val isToday =
                year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)

            val cell = text(
                fa(day),
                15f,
                when {
                    isToday -> white
                    friday -> red
                    else -> green
                },
                true
            )

            if (isToday) {
                cell.background = shape(green, 12)
            }

            grid.addView(cell, gridParams())
        }

        root.addView(grid, params())

        root.addView(
            featureCard(
                "📍",
                "ماه جاری",
                "بازگشت به امروز",
                gold
            ) {
                displayedMonth.timeInMillis =
                    System.currentTimeMillis()
                showCalendar()
            },
            params()
        )

        root.addView(
            featureCard(
                "🏠",
                "صفحه اصلی",
                "بازگشت به خانه",
                green
            ) {
                showHome()
            },
            params()
        )

        setContentView(scroll)
    }

    private fun gridParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            height = dp(52)
            columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED,
                1f
            )
            setMargins(dp(2), dp(3), dp(2), dp(3))
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
