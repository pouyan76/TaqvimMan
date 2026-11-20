package ir.taqvimm.an

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread
import kotlin.random.Random

class MainActivity : Activity() {

    private val emerald = Color.rgb(5, 55, 43)
    private val darkGreen = Color.rgb(3, 35, 28)
    private val gold = Color.rgb(225, 184, 92)
    private val lightGold = Color.rgb(248, 222, 160)
    private val white = Color.WHITE
    private val muted = Color.rgb(205, 213, 204)
    private val holidayRed = Color.rgb(255, 105, 105)

    private lateinit var root: LinearLayout
    private lateinit var content: FrameLayout

    private var currentLocation: Location? = null
    private var prayerData: JSONObject? = null

    private var weatherText = "در حال دریافت اطلاعات هواشناسی..."
    private var weatherForecast: JSONObject? = null

    private var currentPage = "home"
    private var secretNumber = Random.nextInt(1, 101)

    private var adhanPlayer: MediaPlayer? = null
    private var adhanStatus = "برای پخش اذان دکمه پخش را بزنید"
    private var adhanStatusView: TextView? = null

    private var calendarYear = 0
    private var calendarMonth = 0
    private var selectedCalendarDate = ""

    private data class CalendarEvent(
        val title: String,
        val description: String,
        val holiday: Boolean
    )

    private val adhanUrl =
        "https://dl.namaz.ir/s/c/other/azan/rahim_moazenzadeh.mp3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkGreen
        window.navigationBarColor = darkGreen

        createMainLayout()
        showHome()
        requestLocation()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun createBackground(
        color: Int,
        radius: Int,
        strokeColor: Int = Color.TRANSPARENT
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            typeface = Typeface.create(
                "sans-serif",
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    private fun makeButton(
        value: String,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 14f
            setTextColor(darkGreen)
            typeface = Typeface.DEFAULT_BOLD
            background = createBackground(gold, 14)
            setOnClickListener { onClick() }
        }
    }

    private fun createMainLayout() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(darkGreen)
        }

        content = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        root.addView(content)

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(8), dp(4), dp(8))
            background = createBackground(emerald, 18, gold)
        }

        val items = listOf(
            "⌂\nخانه" to "home",
            "▦\nتقویم" to "calendar",
            "☪\nاوقات" to "prayer",
            "☀\nهواشناسی" to "weather",
            "🎮\nبازی" to "game"
        )

        items.forEach { item ->
            val button = TextView(this).apply {
                text = item.first
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(lightGold)
                setPadding(dp(4), dp(5), dp(4), dp(5))
                layoutParams = LinearLayout.LayoutParams(0, dp(55), 1f)
                setOnClickListener {
                    when (item.second) {
                        "home" -> showHome()
                        "calendar" -> showCalendar()
                        "prayer" -> showPrayer()
                        "weather" -> showWeather()
                        "game" -> showGame()
                    }
                }
            }
            navigation.addView(button)
        }

        root.addView(
            navigation,
            LinearLayout.LayoutParams(-1, dp(68)).apply {
                setMargins(dp(10), dp(4), dp(10), dp(8))
            }
        )

        setContentView(root)
    }

    private fun createScroll(): LinearLayout {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(body, LinearLayout.LayoutParams(-1, -2))
        }

        content.removeAllViews()
        content.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        return body
    }

    private fun addHeader(body: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(12), dp(10), dp(18))
            background = createBackground(emerald, 22, gold)
        }

        header.addView(
            MosqueHeaderView(this),
            LinearLayout.LayoutParams(-1, dp(145))
        )

        header.addView(makeText("تقویم من", 27f, lightGold, true))

        header.addView(
            makeText(
                "تقویم شمسی • اوقات شرعی • هواشناسی",
                12f,
                muted
            )
        )

        body.addView(
            header,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(18)
            }
        )
    }

    private fun addSectionTitle(
        body: LinearLayout,
        title: String
    ) {
        val text = makeText(title, 18f, lightGold, true)
        text.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

        body.addView(
            text,
            LinearLayout.LayoutParams(-1, dp(48))
        )
    }

    private fun addCard(
        body: LinearLayout,
        title: String,
        description: String,
        icon: String,
        action: () -> Unit
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(14), dp(10), dp(14))
            background = createBackground(emerald, 20, gold)
            setOnClickListener { action() }
        }

        card.addView(makeText(icon, 30f, gold, true))
        card.addView(makeText(title, 16f, lightGold, true))
        card.addView(makeText(description, 11f, muted))

        val params = LinearLayout.LayoutParams(0, dp(125), 1f)
        params.setMargins(dp(5), dp(5), dp(5), dp(5))

        body.addView(card, params)
    }

    // صفحه خانه
    private fun showHome() {
        currentPage = "home"
        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "تقویم امروز")

        val today = Calendar.getInstance()

        val dateText = getPersianDate(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH)
        )

        val dateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(15), dp(18), dp(15), dp(18))
            background = createBackground(emerald, 20, gold)
        }

        dateCard.addView(
            makeText(dateText, 23f, lightGold, true)
        )

        dateCard.addView(
            makeText(
                SimpleDateFormat(
                    "EEEE، d MMMM yyyy",
                    Locale("fa")
                ).format(Date()),
                14f,
                muted
            )
        )

        body.addView(
            dateCard,
            LinearLayout.LayoutParams(-1, dp(110)).apply {
                bottomMargin = dp(14)
            }
        )

        addSectionTitle(body, "خدمات روزانه")

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        addCard(
            row1,
            "اوقات شرعی",
            "مشاهده زمان نمازها",
            "☪"
        ) {
            showPrayer()
        }

        addCard(
            row1,
            "اذان بعدی",
            "زمان نماز بعدی",
            "🔔"
        ) {
            showPrayer()
        }

        body.addView(row1)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        addCard(
            row2,
            "هواشناسی",
            "پیش‌بینی ۷ روزه",
            "☀"
        ) {
            showWeather()
        }

        addCard(
            row2,
            "بازی",
            "بازی حدس عدد",
            "🎮"
        ) {
            showGame()
        }

        body.addView(row2)

        addSectionTitle(body, "امکانات تقویم")

        body.addView(
            makeButton("مشاهده تقویم شمسی") {
                showCalendar()
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(8)
            }
        )

        body.addView(
            makeText(weatherText, 12f, muted),
            LinearLayout.LayoutParams(-1, dp(45)).apply {
                topMargin = dp(10)
            }
        )
    }
        // مناسبت‌ها و تعطیلات تقویم

    private fun getCalendarEvent(
        year: Int,
        month: Int,
        day: Int
    ): CalendarEvent? {

        return when ("$month-$day") {

            "1-1" -> CalendarEvent(
                "عید نوروز",
                "آغاز سال نو و نخستین روز فروردین",
                true
            )

            "1-2" -> CalendarEvent(
                "عید نوروز",
                "دومین روز تعطیلات نوروز",
                true
            )

            "1-3" -> CalendarEvent(
                "عید نوروز",
                "سومین روز تعطیلات نوروز",
                true
            )

            "1-4" -> CalendarEvent(
                "عید نوروز",
                "چهارمین روز تعطیلات نوروز",
                true
            )

            "1-12" -> CalendarEvent(
                "روز جمهوری اسلامی ایران",
                "سالروز برگزاری همه‌پرسی جمهوری اسلامی ایران",
                true
            )

            "1-13" -> CalendarEvent(
                "روز طبیعت",
                "سیزدهم فروردین و روز طبیعت",
                true
            )

            "3-14" -> CalendarEvent(
                "رحلت امام خمینی",
                "سالروز رحلت امام خمینی",
                true
            )

            "3-15" -> CalendarEvent(
                "قیام ۱۵ خرداد",
                "سالروز قیام پانزدهم خرداد",
                true
            )

            "11-22" -> CalendarEvent(
                "پیروزی انقلاب اسلامی",
                "سالروز پیروزی انقلاب اسلامی ایران",
                true
            )

            "12-29" -> CalendarEvent(
                "روز ملی شدن صنعت نفت",
                "سالروز ملی شدن صنعت نفت ایران",
                true
            )

            else -> null
        }
    }

    // تبدیل تاریخ شمسی به میلادی برای تعیین روز هفته

    private fun persianToGregorian(
        jy: Int,
        jm: Int,
        jd: Int
    ): Calendar {

        val target = "$jy/${jm.toString().padStart(2, '0')}/${jd.toString().padStart(2, '0')}"

        val calendar = Calendar.getInstance()

        calendar.set(Calendar.YEAR, jy + 621)
        calendar.set(Calendar.MONTH, Calendar.MARCH)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        calendar.add(Calendar.DAY_OF_YEAR, -90)

        for (i in 0..370) {

            val gy = calendar.get(Calendar.YEAR)
            val gm = calendar.get(Calendar.MONTH) + 1
            val gd = calendar.get(Calendar.DAY_OF_MONTH)

            if (getPersianDate(gy, gm, gd) == target) {
                return calendar
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return Calendar.getInstance()
    }

    // تعداد روزهای هر ماه شمسی

    private fun getPersianMonthLength(
        year: Int,
        month: Int
    ): Int {

        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> {
                val first = persianToGregorian(year, 12, 29)
                val lastDate = getPersianDate(
                    first.get(Calendar.YEAR),
                    first.get(Calendar.MONTH) + 1,
                    first.get(Calendar.DAY_OF_MONTH)
                )

                if (lastDate.endsWith("/29")) 29 else 30
            }
            else -> 30
        }
    }

    // نام ماه‌های شمسی

    private fun getPersianMonthName(month: Int): String {

        return when (month) {
            1 -> "فروردین"
            2 -> "اردیبهشت"
            3 -> "خرداد"
            4 -> "تیر"
            5 -> "مرداد"
            6 -> "شهریور"
            7 -> "مهر"
            8 -> "آبان"
            9 -> "آذر"
            10 -> "دی"
            11 -> "بهمن"
            12 -> "اسفند"
            else -> ""
        }
    }

    // نمایش تقویم شمسی

    private fun showCalendar() {

        currentPage = "calendar"

        if (calendarYear == 0 || calendarMonth == 0) {

            val today = Calendar.getInstance()

            val todayPersian = getPersianDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH)
            ).split("/")

            calendarYear = todayPersian[0].toInt()
            calendarMonth = todayPersian[1].toInt()
            selectedCalendarDate = todayPersian[2].toInt().toString()
        }

        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "تقویم شمسی")

        // عنوان ماه و سال

        val monthBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = createBackground(emerald, 16, gold)
            setPadding(dp(5), dp(8), dp(5), dp(8))
        }

        val previousButton = makeButton("ماه قبل") {
            calendarMonth--

            if (calendarMonth < 1) {
                calendarMonth = 12
                calendarYear--
            }

            selectedCalendarDate = "1"
            showCalendar()
        }

        val monthTitle = makeText(
            "${getPersianMonthName(calendarMonth)} $calendarYear",
            18f,
            lightGold,
            true
        )

        val nextButton = makeButton("ماه بعد") {
            calendarMonth++

            if (calendarMonth > 12) {
                calendarMonth = 1
                calendarYear++
            }

            selectedCalendarDate = "1"
            showCalendar()
        }

        monthBar.addView(
            previousButton,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        monthBar.addView(
            monthTitle,
            LinearLayout.LayoutParams(0, dp(48), 2f)
        )

        monthBar.addView(
            nextButton,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        body.addView(
            monthBar,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            }
        )

        // روزهای هفته

        val names = arrayOf(
            "ش",
            "ی",
            "د",
            "س",
            "چ",
            "پ",
            "ج"
        )

        val grid = GridLayout(this).apply {
            columnCount = 7
            background = createBackground(emerald, 15, gold)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        names.forEachIndexed { index, name ->

            val text = makeText(
                name,
                14f,
                if (index == 6) holidayRed else gold,
                true
            )

            grid.addView(
                text,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(42)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                }
            )
        }

        // محاسبه روز شروع ماه

        val firstDay = persianToGregorian(
            calendarYear,
            calendarMonth,
            1
        )

        val firstWeekday = when (
            firstDay.get(Calendar.DAY_OF_WEEK)
        ) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }

        // خانه‌های خالی ابتدای ماه

        for (i in 0 until firstWeekday) {

            val empty = TextView(this)

            grid.addView(
                empty,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(48)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                }
            )
        }

        val monthLength = getPersianMonthLength(
            calendarYear,
            calendarMonth
        )

        val todayCalendar = Calendar.getInstance()

        val todayPersian = getPersianDate(
            todayCalendar.get(Calendar.YEAR),
            todayCalendar.get(Calendar.MONTH) + 1,
            todayCalendar.get(Calendar.DAY_OF_MONTH)
        ).split("/")

        val todayYear = todayPersian[0].toInt()
        val todayMonth = todayPersian[1].toInt()
        val todayDay = todayPersian[2].toInt()

        // ساخت روزهای ماه

        for (day in 1..monthLength) {

            val weekday = (firstWeekday + day - 1) % 7

            val isFriday = weekday == 6

            val event = getCalendarEvent(
                calendarYear,
                calendarMonth,
                day
            )

            val isToday =
                calendarYear == todayYear &&
                calendarMonth == todayMonth &&
                day == todayDay

            val isSelected =
                selectedCalendarDate.toIntOrNull() == day

            val text = makeText(
                day.toString(),
                14f,
                when {
                    isToday -> darkGreen
                    isSelected -> darkGreen
                    isFriday -> holidayRed
                    event?.holiday == true -> holidayRed
                    else -> white
                },
                isToday || isSelected
            )

            if (isToday || isSelected) {
                text.background = createBackground(gold, 10)
            } else if (isFriday || event?.holiday == true) {
                text.background = createBackground(
                    Color.rgb(75, 30, 30),
                    10
                )
            }

            text.setOnClickListener {

                selectedCalendarDate = day.toString()

                showCalendar()
            }

            grid.addView(
                text,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(48)

                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )

                    setMargins(
                        dp(2),
                        dp(2),
                        dp(2),
                        dp(2)
                    )
                }
            )
        }

        body.addView(
            grid,
            LinearLayout.LayoutParams(-1, -2)
        )

        // راهنمای رنگ‌ها

        body.addView(
            makeText(
                "🔴 جمعه و تعطیلات رسمی",
                13f,
                holidayRed,
                true
            ),
            LinearLayout.LayoutParams(-1, dp(42)).apply {
                topMargin = dp(8)
            }
        )

        body.addView(
            makeText(
                "🟡 امروز و روز انتخاب‌شده",
                13f,
                lightGold
            ),
            LinearLayout.LayoutParams(-1, dp(35))
        )

        // توضیحات روز انتخاب‌شده

        addSectionTitle(body, "جزئیات روز انتخاب‌شده")

        val selectedDay = selectedCalendarDate.toIntOrNull() ?: 1

        val selectedEvent = getCalendarEvent(
            calendarYear,
            calendarMonth,
            selectedDay
        )

        val selectedWeekday = (
            firstWeekday + selectedDay - 1
        ) % 7

        val weekdayNames = arrayOf(
            "شنبه",
            "یکشنبه",
            "دوشنبه",
            "سه‌شنبه",
            "چهارشنبه",
            "پنجشنبه",
            "جمعه"
        )

        val detailCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(16), dp(14), dp(16))
            background = createBackground(emerald, 18, gold)
        }

        detailCard.addView(
            makeText(
                "$selectedDay ${getPersianMonthName(calendarMonth)} $calendarYear",
                20f,
                lightGold,
                true
            )
        )

        detailCard.addView(
            makeText(
                weekdayNames[selectedWeekday],
                15f,
                if (selectedWeekday == 6) holidayRed else white,
                true
            )
        )

        if (selectedEvent != null) {

            detailCard.addView(
                makeText(
                    selectedEvent.title,
                    17f,
                    holidayRed,
                    true
                )
            )

            detailCard.addView(
                makeText(
                    selectedEvent.description,
                    13f,
                    muted
                )
            )

            if (selectedEvent.holiday) {
                detailCard.addView(
                    makeText(
                        "تعطیل رسمی",
                        13f,
                        holidayRed,
                        true
                    )
                )
            }

        } else if (selectedWeekday == 6) {

            detailCard.addView(
                makeText(
                    "جمعه - تعطیل هفتگی",
                    15f,
                    holidayRed,
                    true
                )
            )

        } else {

            detailCard.addView(
                makeText(
                    "مناسبتی برای این روز ثبت نشده است.",
                    13f,
                    muted
                )
            )
        }

        body.addView(
            detailCard,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            }
        )
    }
        // صفحه اوقات شرعی

    private fun showPrayer() {
        currentPage = "prayer"
        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "اوقات شرعی")

        val timings = prayerData

        val prayers = listOf(
            "اذان صبح" to (timings?.optString("Fajr") ?: "دریافت نشده"),
            "طلوع آفتاب" to (timings?.optString("Sunrise") ?: "دریافت نشده"),
            "اذان ظهر" to (timings?.optString("Dhuhr") ?: "دریافت نشده"),
            "اذان عصر" to (timings?.optString("Asr") ?: "دریافت نشده"),
            "غروب آفتاب" to (timings?.optString("Sunset") ?: "دریافت نشده"),
            "اذان مغرب" to (timings?.optString("Maghrib") ?: "دریافت نشده"),
            "اذان عشا" to (timings?.optString("Isha") ?: "دریافت نشده")
        )

        prayers.forEach { prayer ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background = createBackground(emerald, 15, gold)
            }

            val name = makeText(
                prayer.first,
                16f,
                lightGold,
                true
            )

            name.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

            val time = makeText(
                prayer.second,
                17f,
                white,
                true
            )

            card.addView(
                name,
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )

            card.addView(
                time,
                LinearLayout.LayoutParams(dp(120), dp(48))
            )

            body.addView(
                card,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(9)
                }
            )
        }

        addSectionTitle(body, "پخش اذان ایرانی")

        val audioCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = createBackground(emerald, 20, gold)
        }

        audioCard.addView(
            makeText("☪", 42f, gold, true)
        )

        audioCard.addView(
            makeText(
                "اذان مرحوم رحیم مؤذن‌زاده اردبیلی",
                17f,
                lightGold,
                true
            )
        )

        audioCard.addView(
            makeText(
                "پخش آنلاین اذان معروف ایرانی",
                12f,
                muted
            )
        )

        adhanStatusView = makeText(
            adhanStatus,
            13f,
            lightGold,
            true
        )

        audioCard.addView(
            adhanStatusView,
            LinearLayout.LayoutParams(-1, dp(55))
        )

        audioCard.addView(
            makeButton("▶ پخش اذان") {
                playOnlineAdhan()
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(8)
                bottomMargin = dp(10)
            }
        )

        audioCard.addView(
            makeButton("■ توقف اذان") {
                stopOnlineAdhan()
                updateAdhanStatus("پخش اذان متوقف شد")
            },
            LinearLayout.LayoutParams(-1, dp(52))
        )

        body.addView(
            audioCard,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
                bottomMargin = dp(18)
            }
        )

        body.addView(
            makeText(
                "برای دریافت زمان دقیق، موقعیت مکانی و اینترنت را فعال کنید.",
                12f,
                muted
            )
        )
    }

    // پخش اذان

    private fun playOnlineAdhan() {

        stopOnlineAdhan()

        updateAdhanStatus("در حال اتصال به فایل صوتی...")

        try {

            val media = MediaPlayer()
            adhanPlayer = media

            media.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            media.setOnPreparedListener { mp ->

                if (adhanPlayer === mp) {
                    mp.start()
                    updateAdhanStatus("اذان در حال پخش است 🔊")
                }
            }

            media.setOnCompletionListener { mp ->

                if (adhanPlayer === mp) {
                    mp.release()
                    adhanPlayer = null
                    updateAdhanStatus("پخش اذان به پایان رسید")
                }
            }

            media.setOnErrorListener { mp, _, _ ->

                if (adhanPlayer === mp) {
                    mp.release()
                    adhanPlayer = null
                }

                updateAdhanStatus(
                    "خطا در پخش؛ اینترنت یا آدرس صوت را بررسی کنید"
                )

                true
            }

            media.setDataSource(adhanUrl)
            media.prepareAsync()

        } catch (e: Exception) {

            stopOnlineAdhan()

            updateAdhanStatus(
                "اتصال به فایل صوتی ناموفق بود"
            )
        }
    }

    private fun stopOnlineAdhan() {

        val oldPlayer = adhanPlayer
        adhanPlayer = null

        try {

            oldPlayer?.let {

                if (it.isPlaying) {
                    it.stop()
                }

                it.reset()
                it.release()
            }

        } catch (_: Exception) {
        }
    }

    private fun updateAdhanStatus(message: String) {

        adhanStatus = message

        runOnUiThread {
            adhanStatusView?.text = message
        }
    }
        // آیکون هواشناسی

    private fun weatherIcon(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1, 2 -> "🌤️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            51, 53, 55, 56, 57 -> "🌦️"
            61, 63, 65, 66, 67 -> "🌧️"
            71, 73, 75, 77 -> "❄️"
            80, 81, 82 -> "🌧️"
            85, 86 -> "🌨️"
            95, 96, 99 -> "⛈️"
            else -> "🌤️"
        }
    }

    private fun weatherDescription(code: Int): String {
        return when (code) {
            0 -> "آفتابی"
            1 -> "عمدتاً صاف"
            2 -> "نیمه‌ابری"
            3 -> "ابری"
            45, 48 -> "مه‌آلود"
            51, 53, 55 -> "نم‌نم باران"
            56, 57 -> "باران یخ‌زده"
            61 -> "باران خفیف"
            63 -> "باران متوسط"
            65 -> "باران شدید"
            66, 67 -> "باران یخ‌زده"
            71 -> "برف خفیف"
            73 -> "برف متوسط"
            75 -> "برف شدید"
            77 -> "دانه‌های برف"
            80 -> "رگبار خفیف"
            81 -> "رگبار متوسط"
            82 -> "رگبار شدید"
            85, 86 -> "رگبار برف"
            95 -> "رعد و برق"
            96, 99 -> "رعد و برق و تگرگ"
            else -> "وضعیت نامشخص"
        }
    }

    // صفحه هواشناسی ۷ روزه

    private fun showWeather() {

        currentPage = "weather"
        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "هواشناسی ۷ روزه")

        val forecast = weatherForecast

        if (forecast == null) {

            body.addView(
                makeText(weatherText, 16f, lightGold),
                LinearLayout.LayoutParams(-1, dp(70))
            )

            body.addView(
                makeButton("دریافت دوباره اطلاعات") {
                    requestLocation()
                },
                LinearLayout.LayoutParams(-1, dp(52))
            )

            return
        }

        val current = forecast.optJSONObject("current")

        if (current != null) {

            val temperature = current.optDouble("temperature_2m", 0.0)
            val code = current.optInt("weather_code", 0)

            val currentCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(16), dp(12), dp(16))
                background = createBackground(emerald, 20, gold)
            }

            currentCard.addView(
                makeText(weatherIcon(code), 42f, gold)
            )

            currentCard.addView(
                makeText(
                    "${temperature.toInt()} درجه",
                    28f,
                    lightGold,
                    true
                )
            )

            currentCard.addView(
                makeText(weatherDescription(code), 17f, white)
            )

            body.addView(
                currentCard,
                LinearLayout.LayoutParams(-1, dp(145)).apply {
                    bottomMargin = dp(14)
                }
            )
        }

        addSectionTitle(body, "پیش‌بینی روزانه")

        val daily = forecast.optJSONObject("daily")

        if (daily == null) {

            body.addView(
                makeText("اطلاعات پیش‌بینی موجود نیست", 15f, white)
            )

            return
        }

        val dates = daily.optJSONArray("time") ?: return
        val codes = daily.optJSONArray("weather_code")
        val maxTemps = daily.optJSONArray("temperature_2m_max")
        val minTemps = daily.optJSONArray("temperature_2m_min")
        val rainAmounts = daily.optJSONArray("precipitation_sum")
        val rainChances =
            daily.optJSONArray("precipitation_probability_max")

        for (i in 0 until dates.length()) {

            val date = dates.optString(i)
            val code = codes?.optInt(i, 0) ?: 0
            val maxTemp = maxTemps?.optDouble(i, 0.0) ?: 0.0
            val minTemp = minTemps?.optDouble(i, 0.0) ?: 0.0
            val rain = rainAmounts?.optDouble(i, 0.0) ?: 0.0
            val chance = rainChances?.optInt(i, 0) ?: 0

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = createBackground(emerald, 18, gold)
            }

            card.addView(
                makeText(
                    "${getPersianWeekday(date)} - ${getPersianDateFromIso(date)}",
                    17f,
                    lightGold,
                    true
                )
            )

            card.addView(makeText(weatherIcon(code), 34f, gold))

            card.addView(
                makeText(weatherDescription(code), 15f, white)
            )

            card.addView(
                makeText(
                    "بیشترین دما: ${maxTemp.toInt()} درجه",
                    14f,
                    white
                )
            )

            card.addView(
                makeText(
                    "کمترین دما: ${minTemp.toInt()} درجه",
                    14f,
                    white
                )
            )

            card.addView(
                makeText(
                    "میزان بارش: ${"%.1f".format(Locale.US, rain)} میلی‌متر",
                    14f,
                    lightGold
                )
            )

            card.addView(
                makeText(
                    "احتمال بارندگی: $chance درصد",
                    14f,
                    lightGold
                )
            )

            card.addView(
                makeText(
                    if (rain > 0.0 || chance > 0)
                        "🌧️ احتمال بارندگی وجود دارد"
                    else
                        "☀️ بارندگی پیش‌بینی نشده است",
                    13f,
                    if (rain > 0.0 || chance > 0) gold else muted
                )
            )

            body.addView(
                card,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(10)
                }
            )
        }

        body.addView(
            makeButton("به‌روزرسانی هواشناسی") {
                requestLocation()
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(8)
            }
        )
    }

    private fun getPersianDateFromIso(isoDate: String): String {

        return try {

            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")

            val date = format.parse(isoDate) ?: return isoDate

            val calendar = Calendar.getInstance(
                TimeZone.getTimeZone("UTC")
            )

            calendar.time = date

            getPersianDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )

        } catch (e: Exception) {
            isoDate
        }
    }

    private fun getPersianWeekday(isoDate: String): String {

        return try {

            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")

            val date = format.parse(isoDate) ?: return "روز"

            SimpleDateFormat(
                "EEEE",
                Locale("fa")
            ).format(date)

        } catch (e: Exception) {
            "روز"
        }
    }

    // بازی حدس عدد

    private fun showGame() {

        currentPage = "game"
        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "بازی حدس عدد")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(20), dp(16), dp(20))
            background = createBackground(emerald, 20, gold)
        }

        card.addView(
            makeText(
                "یک عدد بین ۱ تا ۱۰۰ انتخاب شده است.",
                17f,
                lightGold,
                true
            )
        )

        val input = EditText(this).apply {
            hint = "عدد خود را وارد کنید"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 18f
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = createBackground(darkGreen, 12, gold)
        }

        card.addView(
            input,
            LinearLayout.LayoutParams(-1, dp(55)).apply {
                topMargin = dp(16)
                bottomMargin = dp(12)
            }
        )

        val result = makeText(
            "منتظر حدس شما هستم",
            16f,
            lightGold,
            true
        )

        card.addView(
            result,
            LinearLayout.LayoutParams(-1, dp(55))
        )

        card.addView(
            makeButton("بررسی عدد") {

                val guess = input.text.toString().toIntOrNull()

                if (guess == null || guess !in 1..100) {

                    result.text = "عدد بین ۱ تا ۱۰۰ وارد کنید"

                } else {

                    when {
                        guess < secretNumber ->
                            result.text = "عدد بزرگ‌تری حدس بزنید"

                        guess > secretNumber ->
                            result.text = "عدد کوچک‌تری حدس بزنید"

                        else ->
                            result.text = "🎉 آفرین! عدد درست را پیدا کردید"
                    }
                }
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(10)
            }
        )

        card.addView(
            makeButton("شروع بازی جدید") {

                secretNumber = Random.nextInt(1, 101)
                input.setText("")
                result.text = "عدد جدید انتخاب شد؛ حدس بزنید"

            },
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(10)
            }
        )

        body.addView(
            card,
            LinearLayout.LayoutParams(-1, -2)
        )
    }
        // دریافت هواشناسی ۷ روزه

    private fun loadWeather(lat: Double, lon: Double) {

        thread {

            try {

                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code" +
                    "&daily=weather_code,temperature_2m_max," +
                    "temperature_2m_min,precipitation_sum," +
                    "precipitation_probability_max" +
                    "&forecast_days=7&timezone=auto"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val result = JSONObject(response)

                runOnUiThread {

                    weatherForecast = result

                    weatherText.text = "اطلاعات هواشناسی دریافت شد"

                    if (currentPage == "weather") {
                        showWeather()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    weatherText.text = "دریافت اطلاعات هواشناسی ناموفق بود"

                    Toast.makeText(
                        this,
                        "خطا در دریافت هواشناسی",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // دریافت اوقات شرعی

    private fun loadPrayerTimes(lat: Double, lon: Double) {

        thread {

            try {

                val url = URL(
                    "https://api.aladhan.com/v1/timings" +
                    "?latitude=$lat&longitude=$lon&method=8"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val json = JSONObject(response)
                val data = json.getJSONObject("data")
                val timings = data.getJSONObject("timings")

                runOnUiThread {

                    prayerData = timings

                    updateAdhanStatus()

                    if (currentPage == "prayer") {
                        showPrayer()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "دریافت اوقات شرعی ناموفق بود",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // تبدیل تاریخ میلادی به شمسی

    private fun getPersianDate(
        gy: Int,
        gm: Int,
        gd: Int
    ): String {

        val gDaysInMonth = intArrayOf(
            0, 31, 59, 90, 120, 151,
            181, 212, 243, 273, 304, 334
        )

        var jy: Int
        var jm: Int
        var jd: Int

        var gy2 = gy

        if (gm > 2) {
            gy2++
        }

        var days = 355666 +
                (365 * gy) +
                ((gy2 + 3) / 4) -
                ((gy2 + 99) / 100) +
                ((gy2 + 399) / 400) +
                gd +
                gDaysInMonth[gm - 1]

        jy = -1595 + 33 * (days / 12053)
        days %= 12053

        jy += 4 * (days / 1461)
        days %= 1461

        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }

        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }

        return String.format(
            Locale.US,
            "%04d/%02d/%02d",
            jy,
            jm,
            jd
        )
    }

    // آزادسازی منابع هنگام خروج از برنامه

    override fun onDestroy() {

        try {
            stopOnlineAdhan()
        } catch (e: Exception) {
            // جلوگیری از خطا هنگام خروج
        }

        super.onDestroy()
    }

    // هدر گرافیکی مسجد

    inner class MosqueHeaderView(
        context: android.content.Context
    ) : View(context) {

        private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val mosquePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            style = Paint.Style.FILL
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(25).toFloat()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
        }

        override fun onDraw(canvas: Canvas) {

            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()

            backgroundPaint.shader = LinearGradient(
                0f,
                0f,
                w,
                h,
                intArrayOf(
                    Color.rgb(5, 65, 49),
                    Color.rgb(8, 105, 76),
                    Color.rgb(4, 48, 39)
                ),
                null,
                Shader.TileMode.CLAMP
            )

            canvas.drawRoundRect(
                0f,
                0f,
                w,
                h,
                dp(22).toFloat(),
                dp(22).toFloat(),
                backgroundPaint
            )

            backgroundPaint.shader = null

            val centerX = w / 2f
            val baseY = h * 0.76f

            // گنبد وسط

            canvas.drawArc(
                centerX - w * 0.22f,
                baseY - h * 0.55f,
                centerX + w * 0.22f,
                baseY + h * 0.15f,
                180f,
                180f,
                true,
                mosquePaint
            )

            // بدنه مسجد

            canvas.drawRect(
                centerX - w * 0.22f,
                baseY - h * 0.20f,
                centerX + w * 0.22f,
                baseY,
                mosquePaint
            )

            // مناره سمت راست

            canvas.drawRect(
                centerX + w * 0.27f,
                baseY - h * 0.55f,
                centerX + w * 0.31f,
                baseY,
                mosquePaint
            )

            canvas.drawCircle(
                centerX + w * 0.29f,
                baseY - h * 0.58f,
                w * 0.035f,
                mosquePaint
            )

            // مناره سمت چپ

            canvas.drawRect(
                centerX - w * 0.31f,
                baseY - h * 0.55f,
                centerX - w * 0.27f,
                baseY,
                mosquePaint
            )

            canvas.drawCircle(
                centerX - w * 0.29f,
                baseY - h * 0.58f,
                w * 0.035f,
                mosquePaint
            )

            // عنوان برنامه

            canvas.drawText(
                "تقویم من",
                centerX,
                h * 0.27f,
                textPaint
            )
        }
    }

}
    
