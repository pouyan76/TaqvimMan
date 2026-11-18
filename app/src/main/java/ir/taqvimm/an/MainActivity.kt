package ir.taqvimm.an

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.icu.util.Calendar
import android.icu.util.TimeZone
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.GregorianCalendar
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val green = Color.rgb(13, 57, 53)
    private val green2 = Color.rgb(28, 105, 88)
    private val mint = Color.rgb(225, 241, 235)
    private val gold = Color.rgb(218, 171, 91)
    private val bg = Color.rgb(241, 245, 247)
    private val white = Color.WHITE
    private val red = Color.rgb(190, 55, 55)
    private val gray = Color.rgb(111, 124, 130)
    private val blue = Color.rgb(42, 104, 157)

    private lateinit var displayedMonth: Calendar
    private var selectedDay = 1

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private lateinit var liveStatus: TextView
    private lateinit var prayerBox: LinearLayout
    private lateinit var weatherBox: LinearLayout

    private val permissionCode = 501

    private val months = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    private val weekdays = arrayOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    )
    private val gregorianWeekdays = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayedMonth = newPersianCalendar()
        displayedMonth.timeInMillis = System.currentTimeMillis()
        selectedDay = displayedMonth.get(Calendar.DAY_OF_MONTH)
        showHome()
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    private fun newPersianCalendar(): Calendar =
        Calendar.getInstance(
            TimeZone.getTimeZone("Asia/Tehran"),
            Locale.forLanguageTag("fa-IR-u-ca-persian")
        )

    private fun fa(n: Int): String {
        val digits = "۰۱۲۳۴۵۶۷۸۹"
        return n.toString().map {
            if (it in '0'..'9') digits[it - '0'] else it
        }.joinToString("")
    }

    private fun faText(s: String): String =
        s.map {
            if (it in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[it - '0'] else it
        }.joinToString("")

    private fun rounded(color: Int, radius: Int = 20): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun gradient(
        a: Int,
        b: Int,
        radius: Int = 24
    ): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(a, b)
        ).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun label(
        value: String,
        size: Float,
        color: Int = green,
        bold: Boolean = false,
        gravityValue: Int = Gravity.CENTER
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = gravityValue
        textDirection = View.TEXT_DIRECTION_RTL
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(5), dp(5), dp(5), dp(5))
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(16), dp(16), dp(26))
        }
        scroll.addView(root)
        return Pair(scroll, root)
    }

    private fun margins(
        top: Int = 8,
        bottom: Int = 8
    ): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dp(top), 0, dp(bottom))
        }

    private fun sectionTitle(
        title: String,
        subtitle: String = ""
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 19f, green, true, Gravity.RIGHT))
            if (subtitle.isNotEmpty()) {
                addView(label(subtitle, 11f, gray, false, Gravity.RIGHT))
            }
        }
    }

    private fun actionCard(
        icon: String,
        title: String,
        description: String,
        color: Int,
        action: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(white, 22)
            elevation = dp(3).toFloat()
            setPadding(dp(10), dp(13), dp(10), dp(13))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            addView(label(icon, 31f))
            addView(label(title, 15f, green, true))
            addView(label(description, 10f, gray))
            val line = View(this@MainActivity).apply {
                background = rounded(color, 4)
            }
            addView(
                line,
                LinearLayout.LayoutParams(dp(34), dp(4)).apply {
                    topMargin = dp(8)
                }
            )
        }
    }

    private fun addTwoCards(
        root: LinearLayout,
        a: View,
        b: View
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        row.addView(
            a,
            LinearLayout.LayoutParams(0, dp(142), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )
        row.addView(
            b,
            LinearLayout.LayoutParams(0, dp(142), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )
        root.addView(row, margins(5, 5))
    }

    private fun showHome() {
        val (scroll, root) = page()

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradient(green, green2, 28)
            elevation = dp(5).toFloat()
            setPadding(dp(18), dp(22), dp(18), dp(22))
        }
        hero.addView(label("تقویم من", 29f, white, true))
        hero.addView(
            label("مدیریت هوشمند روزهای شما", 13f, Color.rgb(225, 239, 232))
        )
        hero.addView(label("POUYAN  •  TAQVIM MAN", 10f, gold, true))
        root.addView(hero, margins(0, 13))

        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val dateCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(white, 22)
            elevation = dp(2).toFloat()
            setPadding(dp(13), dp(12), dp(13), dp(12))
        }

        val dateInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        dateInfo.addView(
            label(
                "امروز • ${weekdays[today.get(Calendar.DAY_OF_WEEK) % 7]}",
                12f,
                gray,
                true,
                Gravity.RIGHT
            )
        )

        dateInfo.addView(
            label(
                "${fa(today.get(Calendar.DAY_OF_MONTH))} ${months[today.get(Calendar.MONTH)]} ${fa(today.get(Calendar.YEAR))}",
                21f,
                green,
                true,
                Gravity.RIGHT
            )
        )

        dateCard.addView(label("📆", 31f), LinearLayout.LayoutParams(dp(55), dp(60)))
        dateCard.addView(dateInfo, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(dateCard, margins(0, 18))

        root.addView(
            sectionTitle(
                "دسترسی سریع",
                "یکی از بخش‌های برنامه را انتخاب کنید"
            ),
            margins(0, 7)
        )

        addTwoCards(
            root,
            actionCard(
                "📅",
                "تقویم شمسی",
                "نمای ماهانه و انتخاب روز",
                green
            ) { showCalendar() },
            actionCard(
                "🕌",
                "اوقات شرعی",
                "اذان‌ها بر اساس موقعیت",
                gold
            ) { showLiveInfo("prayer") }
        )

        addTwoCards(
            root,
            actionCard(
                "🌤️",
                "هواشناسی",
                "پیش‌بینی هفت روز آینده",
                blue
            ) { showLiveInfo("weather") },
            actionCard(
                "🎮",
                "بازی کلمات",
                "بخش بازی در حال ساخت",
                red
            ) {
                Toast.makeText(
                    this,
                    "بازی کلمات هنوز پیاده‌سازی نشده است.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(mint, 18)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        info.addView(
            label(
                "امکانات آنلاین",
                15f,
                green,
                true,
                Gravity.RIGHT
            )
        )
        info.addView(
            label(
                "برای دریافت اوقات شرعی و پیش‌بینی هوا، موقعیت مکانی و اینترنت را فعال کنید.",
                12f,
                gray,
                false,
                Gravity.RIGHT
            )
        )
        root.addView(info, margins(14, 8))

        setContentView(scroll)
    }
