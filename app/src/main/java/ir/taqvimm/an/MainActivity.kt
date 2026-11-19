package ir.taqvimm.an

import android.Manifest
import android.app.Activity
import android.content.Context
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
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
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
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : Activity() {

    private val green = Color.rgb(13, 57, 53)
    private val green2 = Color.rgb(28, 105, 88)
    private val mint = Color.rgb(225, 241, 235)
    private val gold = Color.rgb(218, 171, 91)
    private val black = Color.rgb(20, 24, 23)
    private val bg = Color.rgb(241, 245, 242)
    private val white = Color.WHITE
    private val gray = Color.rgb(110, 120, 116)
    private val blue = Color.rgb(55, 125, 190)
    private val red = Color.rgb(190, 65, 65)

    private var displayedMonth = Calendar.getInstance()
    private var selectedDay = 0
    private var currentPage = 0
    private var currentMode = ""

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private var liveStatus: TextView? = null
    private var prayerBox: LinearLayout? = null
    private var weatherBox: LinearLayout? = null

    private var permissionCode = 501
    private var gameNumber = Random.nextInt(1, 6)

    private val pageNames = listOf(
        "خانه",
        "تقویم شمسی",
        "اوقات شرعی",
        "هواشناسی",
        "بازی‌ها"
    )

    private val persianMonths = arrayOf(
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
        window.statusBarColor = green
        window.navigationBarColor = black
        showHome()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun newPersianCalendar(): Calendar {
        val calendar = Calendar.getInstance(
            TimeZone.getDefault(),
            Locale("fa", "IR")
        )
        calendar.setTimeInMillis(System.currentTimeMillis())
        return calendar
    }

    private fun fa(number: Int): String {
        val digits = arrayOf(
            "۰", "۱", "۲", "۳", "۴",
            "۵", "۶", "۷", "۸", "۹"
        )
        return number.toString().map {
            if (it.isDigit()) digits[it.digitToInt()] else it.toString()
        }.joinToString("")
    }

    private fun faText(value: String): String {
        val english = "0123456789"
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        var result = value
        for (i in english.indices) {
            result = result.replace(english[i], persian[i])
        }
        return result
    }

    private fun rounded(
        color: Int,
        radius: Int = 18,
        strokeColor: Int = Color.TRANSPARENT,
        strokeWidth: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeWidth > 0) {
                setStroke(dp(strokeWidth), strokeColor)
            }
        }
    }

    private fun gradient(
        first: Int,
        second: Int,
        radius: Int = 20
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(first, second)
        ).apply {
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun label(
        value: String,
        size: Float = 15f,
        color: Int = black,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun card(
        color: Int = white,
        padding: Int = 16
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
            background = rounded(color, 20)
            elevation = dp(3).toFloat()
        }
    }

    private fun addSpace(
        parent: LinearLayout,
        height: Int = 12
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float = 15f,
        color: Int = black,
        bold: Boolean = false
    ): TextView {
        val view = label(value, size, color, bold)
        parent.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return view
    }

    private fun addButton(
        parent: LinearLayout,
        text: String,
        color: Int = green,
        textColor: Int = white,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            this.text = text
            setTextColor(textColor)
            textSize = 14f
            isAllCaps = false
            background = rounded(color, 14)
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        )
    }

    private fun makeScroll(): Pair<ScrollView, LinearLayout> {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(
                body,
                
                android.widget.FrameLayout.LayoutParams(
    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
)
            )
        }

        return Pair(scroll, body)
    }

    private fun addHeader(
        body: LinearLayout,
        title: String,
        subtitle: String
    ) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient(green, green2, 24)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        addText(header, title, 23f, white, true)
        addSpace(header, 8)
        addText(header, subtitle, 13f, mint)

        body.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        )
    }

    private fun installPage(content: View, index: Int) {
        currentPage = index

        val root = SwipeFrameLayout(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        mainLayout.addView(
            content,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            setBackgroundColor(black)
        }

        val previous = TextView(this).apply {
            text = "❮ قبلی"
            textSize = 13f
            setTextColor(if (index == 0) gray else gold)
            gravity = Gravity.CENTER
            setOnClickListener {
                if (currentPage > 0) navigateTo(currentPage - 1)
            }
        }

        navigation.addView(
            previous,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        val indicator = TextView(this).apply {
            text = "صفحه ${fa(index + 1)} از ${fa(5)}"
            textSize = 13f
            setTextColor(white)
            gravity = Gravity.CENTER
        }

        navigation.addView(
            indicator,
            LinearLayout.LayoutParams(0, dp(48), 1.5f)
        )

        val next = TextView(this).apply {
            text = "بعدی ❯"
            textSize = 13f
            setTextColor(if (index == 4) gray else gold)
            gravity = Gravity.CENTER
            setOnClickListener {
                if (currentPage < 4) navigateTo(currentPage + 1)
            }
        }

        navigation.addView(
            next,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        mainLayout.addView(
            navigation,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(62)
            )
        )

        root.addView(
            mainLayout,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.onSwipe = { direction ->
            navigateTo((currentPage + direction).coerceIn(0, 4))
        }

        setContentView(root)
    }

    private fun navigateTo(index: Int) {
        when (index) {
            0 -> showHome()
            1 -> showCalendar()
            2 -> showLiveInfo("prayer")
            3 -> showLiveInfo("weather")
            4 -> showGames()
        }
    }

    private fun showHome() {
        currentPage = 0

        val (scroll, body) = makeScroll()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient(
                Color.rgb(8, 48, 42),
                Color.rgb(29, 111, 88),
                26
            )
            setPadding(dp(22), dp(24), dp(22), dp(24))
        }

        addText(header, "تقویم من", 27f, white, true)
        addSpace(header, 8)
        addText(
            header,
            "تقویم هوشمند، اوقات شرعی و هواشناسی آنلاین",
            13f,
            mint
        )
        addSpace(header, 18)

        val today = newPersianCalendar()
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH)
        val day = today.get(Calendar.DAY_OF_MONTH)

        val dateText = TextView(this).apply {
            text = "${fa(day)} ${persianMonths[month]} ${fa(year)}"
            textSize = 23f
            setTextColor(gold)
            typeface = Typeface.DEFAULT_BOLD
        }

        header.addView(dateText)

        body.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        )

        addText(body, "امکانات برنامه", 19f, green, true)
        addSpace(body, 12)

        addHomeCard(
            body, "📅", "تقویم شمسی",
            "مشاهده روزها و ماه‌های سال", 1
        )

        addHomeCard(
            body, "🕌", "اوقات شرعی",
            "نمایش آنلاین زمان نمازها بر اساس مکان", 2
        )

        addHomeCard(
            body, "🌤", "هواشناسی",
            "پیش‌بینی وضعیت آب‌وهوا برای هفت روز", 3
        )

        addHomeCard(
            body, "🎮", "بازی‌ها",
            "بازی سرگرم‌کننده و کوتاه", 4
        )

        addSpace(body, 12)

        val footer = card(green, 16)
        addText(footer, "تقویم من", 17f, gold, true)
        addText(
            footer,
            "برای جابه‌جایی صفحات، انگشت خود را به چپ یا راست بکشید.",
            13f,
            white
        )

        body.addView(
            footer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        installPage(scroll, 0)
    }

    private fun addHomeCard(
        parent: LinearLayout,
        emoji: String,
        title: String,
        description: String,
        page: Int
    ) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(white, 18)
            setPadding(dp(15), dp(15), dp(15), dp(15))
            elevation = dp(2).toFloat()
            isClickable = true
            setOnClickListener { navigateTo(page) }
        }

        val icon = TextView(this).apply {
            text = emoji
            textSize = 30f
            gravity = Gravity.CENTER
        }

        item.addView(icon, LinearLayout.LayoutParams(dp(58), dp(58)))

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        addText(texts, title, 16f, green, true)
        addSpace(texts, 5)
        addText(texts, description, 12f, gray)

        item.addView(
            texts,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        parent.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        )
    }

    private inner class SwipeFrameLayout(
        context: Context
    ) : FrameLayout(context) {

        var onSwipe: ((Int) -> Unit)? = null

        private var downX = 0f
        private var downY = 0f
        private var handled = false

        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    handled = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    val dy = event.y - downY

                    if (
                        abs(dx) > dp(65) &&
                        abs(dx) > abs(dy) * 1.35f
                    ) {
                        handled = true
                        return true
                    }
                }
            }

            return super.onInterceptTouchEvent(event)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (handled) {
                        val dx = event.x - downX
                        if (abs(dx) > dp(65)) {
                            onSwipe?.invoke(if (dx < 0) 1 else -1)
                        }
                    }
                    handled = false
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    handled = false
                    return true
                }
            }

            return true
        }
    }    // ==============================
    // صفحه دوم: تقویم شمسی
    // ==============================

    private fun showCalendar() {
        currentPage = 1

        val (scroll, body) = makeScroll()

        addHeader(
            body,
            "تقویم شمسی",
            "نمایش ماه‌ها و انتخاب روزهای سال"
        )

        val calendar = displayedMonth.clone() as Calendar

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val today = newPersianCalendar()

        val monthCard = card(green, 16)

        val monthTitle = TextView(this).apply {
            text = "${persianMonths[month]} ${fa(year)}"
            textSize = 23f
            setTextColor(gold)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        monthCard.addView(
            monthTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val previousMonth = TextView(this).apply {
            text = "❮ ماه قبل"
            textSize = 14f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener {
                displayedMonth.add(Calendar.MONTH, -1)
                selectedDay = 0
                showCalendar()
            }
        }

        val nextMonth = TextView(this).apply {
            text = "ماه بعد ❯"
            textSize = 14f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener {
                displayedMonth.add(Calendar.MONTH, 1)
                selectedDay = 0
                showCalendar()
            }
        }

        controls.addView(
            previousMonth,
            LinearLayout.LayoutParams(0, dp(45), 1f)
        )

        controls.addView(
            nextMonth,
            LinearLayout.LayoutParams(0, dp(45), 1f)
        )

        monthCard.addView(controls)

        val grid = GridLayout(this).apply {
            columnCount = 7
            rowCount = 7
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        val headers = arrayOf(
            "ش", "ی", "د", "س", "چ", "پ", "ج"
        )

        for (dayName in headers) {
            val cell = TextView(this).apply {
                text = dayName
                textSize = 14f
                setTextColor(gold)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            grid.addView(
                cell,
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

        val firstDay = calendar.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)

        // شروع هفته از شنبه
        val offset = firstDay.get(Calendar.DAY_OF_WEEK) % 7
        val maxDays = firstDay.getActualMaximum(
            Calendar.DAY_OF_MONTH
        )

        for (i in 0 until offset) {
            val empty = TextView(this)
            grid.addView(
                empty,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(46)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                }
            )
        }

        for (day in 1..maxDays) {
            val isToday =
                year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)

            val isSelected = selectedDay == day

            val cell = TextView(this).apply {
                text = fa(day)
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(
                    when {
                        isSelected -> white
                        isToday -> gold
                        else -> black
                    }
                )

                background = rounded(
                    when {
                        isSelected -> green2
                        isToday -> green
                        else -> Color.TRANSPARENT
                    },
                    12
                )

                setOnClickListener {
                    selectedDay = day
                    showCalendar()
                }
            }

            grid.addView(
                cell,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(46)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
            )
        }

        monthCard.addView(
            grid,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        body.addView(
            monthCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(body, 16)

        val selectedCard = card(white)

        val selected = if (selectedDay > 0) {
            selectedDay
        } else {
            today.get(Calendar.DAY_OF_MONTH)
        }

        addText(
            selectedCard,
            "روز انتخاب‌شده",
            17f,
            green,
            true
        )

        addSpace(selectedCard, 8)

        addText(
            selectedCard,
            "${fa(selected)} ${persianMonths[month]} ${fa(year)}",
            21f,
            gold,
            true
        )

        addSpace(selectedCard, 8)

        addText(
            selectedCard,
            if (selectedDay == 0) {
                "برای انتخاب روز، روی یکی از خانه‌های تقویم بزنید."
            } else {
                "روز ${fa(selectedDay)} انتخاب شده است."
            },
            13f,
            gray
        )

        body.addView(
            selectedCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(body, 12)

        addButton(body, "بازگشت به ماه جاری") {
            displayedMonth = newPersianCalendar()
            selectedDay = 0
            showCalendar()
        }

        installPage(scroll, 1)
    }

    // ==============================
    // صفحه سوم: اوقات شرعی
    // ==============================

    private fun showLiveInfo(mode: String) {
        currentMode = mode

        val pageIndex = if (mode == "prayer") 2 else 3
        currentPage = pageIndex

        val (scroll, body) = makeScroll()

        if (mode == "prayer") {
            addHeader(
                body,
                "اوقات شرعی",
                "زمان‌بندی نمازها بر اساس موقعیت مکانی"
            )

            val locationCard = card(green, 18)

            addText(
                locationCard,
                "موقعیت مکانی شما",
                17f,
                gold,
                true
            )

            liveStatus = addText(
                locationCard,
                "در حال دریافت موقعیت مکانی...",
                13f,
                white
            )

            addSpace(locationCard, 10)

            addButton(
                locationCard,
                "به‌روزرسانی موقعیت",
                green2,
                white
            ) {
                requestLocation()
            }

            body.addView(
                locationCard,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addSpace(body, 16)

            addText(
                body,
                "برنامه نمازهای روزانه",
                18f,
                green,
                true
            )

            addSpace(body, 10)

            prayerBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val loading = card(white)
            addText(
                loading,
                "برای نمایش زمان نمازها، موقعیت مکانی و اطلاعات آنلاین دریافت می‌شود.",
                14f,
                gray
            )

            prayerBox?.addView(
                loading,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            body.addView(
                prayerBox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

        } else {
            addHeader(
                body,
                "هواشناسی",
                "پیش‌بینی آنلاین وضعیت آب‌وهوا برای هفت روز"
            )

            val locationCard = card(green, 18)

            addText(
                locationCard,
                "وضعیت موقعیت مکانی",
                17f,
                gold,
                true
            )

            liveStatus = addText(
                locationCard,
                "در حال دریافت موقعیت...",
                13f,
                white
            )

            addButton(
                locationCard,
                "به‌روزرسانی هواشناسی",
                green2,
                white
            ) {
                requestLocation()
            }

            body.addView(
                locationCard,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addSpace(body, 16)

            weatherBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val loading = card(white)

            addText(
                loading,
                "در انتظار دریافت اطلاعات هواشناسی...",
                14f,
                gray
            )

            weatherBox?.addView(
                loading,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            body.addView(
                weatherBox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        installPage(scroll, pageIndex)
        requestLocation()
    }

    // ==============================
    // دریافت مجوز مکان
    // ==============================

    private fun requestLocation() {
        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                permissionCode
            )
            return
        }

        findPhoneLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == permissionCode) {
            if (grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }) {
                findPhoneLocation()
            } else {
                liveStatus?.text =
                    "مجوز مکان داده نشد. لطفاً دسترسی مکان را فعال کنید."
            }
        }
    }

    // ==============================
    // دریافت مختصات گوشی
    // ==============================

    private fun findPhoneLocation() {
        try {
            locationManager =
                getSystemService(Context.LOCATION_SERVICE)
                        as LocationManager

            val manager = locationManager ?: return

            var bestLocation: Location? = null

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )

            for (provider in providers) {
                try {
                    if (
                        manager.isProviderEnabled(provider) &&
                        checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        manager.isProviderEnabled(provider) &&
                        checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val location =
                            manager.getLastKnownLocation(provider)

                        if (
                            location != null &&
                            (
                                bestLocation == null ||
                                location.time > bestLocation!!.time
                            )
                        ) {
                            bestLocation = location
                        }
                    }
                } catch (_: Exception) {
                }
            }

            if (bestLocation != null) {
                useLocation(bestLocation!!)
                return
            }

            liveStatus?.text =
                "در حال جست‌وجوی موقعیت فعلی گوشی..."

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    stopLocationUpdates()
                    useLocation(location)
                }
            }

            locationListener = listener

            for (provider in providers) {
                try {
                    if (manager.isProviderEnabled(provider)) {
                        if (
                            checkSelfPermission(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            manager.requestLocationUpdates(
                                provider,
                                3000L,
                                10f,
                                listener,
                                Looper.getMainLooper()
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }

            handler.postDelayed({
                if (locationListener != null) {
                    stopLocationUpdates()
                    liveStatus?.text =
                        "موقعیت پیدا نشد. GPS و اینترنت را بررسی کنید."
                }
            }, 15000)

        } catch (e: Exception) {
            liveStatus?.text =
                "خطا در دریافت موقعیت: ${e.message}"
        }
    }

    private fun useLocation(location: Location) {
        if (isFinishing) return

        liveStatus?.text =
            "موقعیت دریافت شد\n" +
            "عرض جغرافیایی: ${location.latitude}\n" +
            "طول جغرافیایی: ${location.longitude}"

        loadOnlineInformation(
            location.latitude,
            location.longitude,
            currentMode
        )
    }

    private fun stopLocationUpdates() {
        try {
            locationListener?.let {
                locationManager?.removeUpdates(it)
            }
        } catch (_: Exception) {
        }

        locationListener = null
    }    // ==============================
    // صفحه دوم: تقویم شمسی
    // ==============================

    private fun showCalendar() {
        currentPage = 1

        val (scroll, body) = makeScroll()

        addHeader(
            body,
            "تقویم شمسی",
            "نمایش ماه‌ها و انتخاب روزهای سال"
        )

        val calendar = displayedMonth.clone() as Calendar

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val today = newPersianCalendar()

        val monthCard = card(green, 16)

        val monthTitle = TextView(this).apply {
            text = "${persianMonths[month]} ${fa(year)}"
            textSize = 23f
            setTextColor(gold)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        monthCard.addView(
            monthTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val previousMonth = TextView(this).apply {
            text = "❮ ماه قبل"
            textSize = 14f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener {
                displayedMonth.add(Calendar.MONTH, -1)
                selectedDay = 0
                showCalendar()
            }
        }

        val nextMonth = TextView(this).apply {
            text = "ماه بعد ❯"
            textSize = 14f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener {
                displayedMonth.add(Calendar.MONTH, 1)
                selectedDay = 0
                showCalendar()
            }
        }

        controls.addView(
            previousMonth,
            LinearLayout.LayoutParams(0, dp(45), 1f)
        )

        controls.addView(
            nextMonth,
            LinearLayout.LayoutParams(0, dp(45), 1f)
        )

        monthCard.addView(controls)

        val grid = GridLayout(this).apply {
            columnCount = 7
            rowCount = 7
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        val headers = arrayOf(
            "ش", "ی", "د", "س", "چ", "پ", "ج"
        )

        for (dayName in headers) {
            val cell = TextView(this).apply {
                text = dayName
                textSize = 14f
                setTextColor(gold)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            grid.addView(
                cell,
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

        val firstDay = calendar.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)

        // شروع هفته از شنبه
        val offset = firstDay.get(Calendar.DAY_OF_WEEK) % 7
        val maxDays = firstDay.getActualMaximum(
            Calendar.DAY_OF_MONTH
        )

        for (i in 0 until offset) {
            val empty = TextView(this)
            grid.addView(
                empty,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(46)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                }
            )
        }

        for (day in 1..maxDays) {
            val isToday =
                year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)

            val isSelected = selectedDay == day

            val cell = TextView(this).apply {
                text = fa(day)
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(
                    when {
                        isSelected -> white
                        isToday -> gold
                        else -> black
                    }
                )

                background = rounded(
                    when {
                        isSelected -> green2
                        isToday -> green
                        else -> Color.TRANSPARENT
                    },
                    12
                )

                setOnClickListener {
                    selectedDay = day
                    showCalendar()
                }
            }

            grid.addView(
                cell,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(46)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
            )
        }

        monthCard.addView(
            grid,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        body.addView(
            monthCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(body, 16)

        val selectedCard = card(white)

        val selected = if (selectedDay > 0) {
            selectedDay
        } else {
            today.get(Calendar.DAY_OF_MONTH)
        }

        addText(
            selectedCard,
            "روز انتخاب‌شده",
            17f,
            green,
            true
        )

        addSpace(selectedCard, 8)

        addText(
            selectedCard,
            "${fa(selected)} ${persianMonths[month]} ${fa(year)}",
            21f,
            gold,
            true
        )

        addSpace(selectedCard, 8)

        addText(
            selectedCard,
            if (selectedDay == 0) {
                "برای انتخاب روز، روی یکی از خانه‌های تقویم بزنید."
            } else {
                "روز ${fa(selectedDay)} انتخاب شده است."
            },
            13f,
            gray
        )

        body.addView(
            selectedCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(body, 12)

        addButton(body, "بازگشت به ماه جاری") {
            displayedMonth = newPersianCalendar()
            selectedDay = 0
            showCalendar()
        }

        installPage(scroll, 1)
    }

    // ==============================
    // صفحه سوم: اوقات شرعی
    // ==============================

    private fun showLiveInfo(mode: String) {
        currentMode = mode

        val pageIndex = if (mode == "prayer") 2 else 3
        currentPage = pageIndex

        val (scroll, body) = makeScroll()

        if (mode == "prayer") {
            addHeader(
                body,
                "اوقات شرعی",
                "زمان‌بندی نمازها بر اساس موقعیت مکانی"
            )

            val locationCard = card(green, 18)

            addText(
                locationCard,
                "موقعیت مکانی شما",
                17f,
                gold,
                true
            )

            liveStatus = addText(
                locationCard,
                "در حال دریافت موقعیت مکانی...",
                13f,
                white
            )

            addSpace(locationCard, 10)

            addButton(
                locationCard,
                "به‌روزرسانی موقعیت",
                green2,
                white
            ) {
                requestLocation()
            }

            body.addView(
                locationCard,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addSpace(body, 16)

            addText(
                body,
                "برنامه نمازهای روزانه",
                18f,
                green,
                true
            )

            addSpace(body, 10)

            prayerBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val loading = card(white)
            addText(
                loading,
                "برای نمایش زمان نمازها، موقعیت مکانی و اطلاعات آنلاین دریافت می‌شود.",
                14f,
                gray
            )

            prayerBox?.addView(
                loading,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            body.addView(
                prayerBox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

        } else {
            addHeader(
                body,
                "هواشناسی",
                "پیش‌بینی آنلاین وضعیت آب‌وهوا برای هفت روز"
            )

            val locationCard = card(green, 18)

            addText(
                locationCard,
                "وضعیت موقعیت مکانی",
                17f,
                gold,
                true
            )

            liveStatus = addText(
                locationCard,
                "در حال دریافت موقعیت...",
                13f,
                white
            )

            addButton(
                locationCard,
                "به‌روزرسانی هواشناسی",
                green2,
                white
            ) {
                requestLocation()
            }

            body.addView(
                locationCard,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addSpace(body, 16)

            weatherBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val loading = card(white)

            addText(
                loading,
                "در انتظار دریافت اطلاعات هواشناسی...",
                14f,
                gray
            )

            weatherBox?.addView(
                loading,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            body.addView(
                weatherBox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        installPage(scroll, pageIndex)
        requestLocation()
    }

    // ==============================
    // دریافت مجوز مکان
    // ==============================

    private fun requestLocation() {
        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                permissionCode
            )
            return
        }

        findPhoneLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == permissionCode) {
            if (grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }) {
                findPhoneLocation()
            } else {
                liveStatus?.text =
                    "مجوز مکان داده نشد. لطفاً دسترسی مکان را فعال کنید."
            }
        }
    }

    // ==============================
    // دریافت مختصات گوشی
    // ==============================

    private fun findPhoneLocation() {
        try {
            locationManager =
                getSystemService(Context.LOCATION_SERVICE)
                        as LocationManager

            val manager = locationManager ?: return

            var bestLocation: Location? = null

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )

            for (provider in providers) {
                try {
                    if (
                        manager.isProviderEnabled(provider) &&
                        checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        manager.isProviderEnabled(provider) &&
                        checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val location =
                            manager.getLastKnownLocation(provider)

                        if (
                            location != null &&
                            (
                                bestLocation == null ||
                                location.time > bestLocation!!.time
                            )
                        ) {
                            bestLocation = location
                        }
                    }
                } catch (_: Exception) {
                }
            }

            if (bestLocation != null) {
                useLocation(bestLocation!!)
                return
            }

            liveStatus?.text =
                "در حال جست‌وجوی موقعیت فعلی گوشی..."

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    stopLocationUpdates()
                    useLocation(location)
                }
            }

            locationListener = listener

            for (provider in providers) {
                try {
                    if (manager.isProviderEnabled(provider)) {
                        if (
                            checkSelfPermission(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            manager.requestLocationUpdates(
                                provider,
                                3000L,
                                10f,
                                listener,
                                Looper.getMainLooper()
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }

            handler.postDelayed({
                if (locationListener != null) {
                    stopLocationUpdates()
                    liveStatus?.text =
                        "موقعیت پیدا نشد. GPS و اینترنت را بررسی کنید."
                }
            }, 15000)

        } catch (e: Exception) {
            liveStatus?.text =
                "خطا در دریافت موقعیت: ${e.message}"
        }
    }

    private fun useLocation(location: Location) {
        if (isFinishing) return

        liveStatus?.text =
            "موقعیت دریافت شد\n" +
            "عرض جغرافیایی: ${location.latitude}\n" +
            "طول جغرافیایی: ${location.longitude}"

        loadOnlineInformation(
            location.latitude,
            location.longitude,
            currentMode
        )
    }

    private fun stopLocationUpdates() {
        try {
            locationListener?.let {
                locationManager?.removeUpdates(it)
            }
        } catch (_: Exception) {
        }

        locationListener = null
    }
