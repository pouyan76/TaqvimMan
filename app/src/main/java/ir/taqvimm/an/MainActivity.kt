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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.text.InputType
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private val emerald = Color.rgb(10, 91, 68)
    private val darkGreen = Color.rgb(5, 48, 39)
    private val gold = Color.rgb(226, 184, 91)
    private val black = Color.rgb(14, 20, 19)
    private val cardColor = Color.rgb(25, 39, 35)
    private val softCard = Color.rgb(31, 49, 43)
    private val white = Color.WHITE
    private val muted = Color.rgb(190, 202, 196)

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var root: LinearLayout
    private var currentPage = 0
    private var displayedMonth = Calendar.getInstance()

    private var latitude = 35.6892
    private var longitude = 51.3890
    private var locationLabel = "تهران (مکان پیش‌فرض)"

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var weatherJson: JSONObject? = null
    private var prayerJson: JSONObject? = null

    private val pageTitles = listOf(
        "خانه",
        "تقویم",
        "اوقات",
        "هواشناسی",
        "بازی"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkGreen
        window.navigationBarColor = black

        locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        displayedMonth = Calendar.getInstance()

        buildShell()
        showHome()
        requestLocation()
    }

    override fun onDestroy() {
        stopLocationUpdates()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private fun rounded(
        color: Int,
        radius: Int = 18,
        stroke: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (stroke != 0) {
                setStroke(dp(1), stroke)
            }
        }
    }

    private fun label(
        value: String,
        size: Float = 14f,
        color: Int = white,
        bold: Boolean = false,
        gravityValue: Int = Gravity.CENTER
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = gravityValue

            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }

            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
    }

    private fun actionButton(
        value: String,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 13f
            setTextColor(white)
            isAllCaps = false
            stateListAnimator = null
            background = rounded(emerald, 14)

            setPadding(dp(10), 0, dp(10), 0)

            setOnClickListener {
                onClick()
            }
        }
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(black)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = rounded(darkGreen, 0)
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        brand.addView(
            label("تقویم من", 22f, gold, true, Gravity.START)
        )

        brand.addView(
            label(
                "همراه هوشمند روزهای شما",
                10.5f,
                muted,
                false,
                Gravity.START
            )
        )

        header.addView(
            brand,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        header.addView(
            label("✦", 25f, gold, true)
        )

        val content = FrameLayout(this).apply {
            tag = "page_container"
        }

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(5), dp(5), dp(5))
            background = rounded(darkGreen, 0)
        }

        pageTitles.forEachIndexed { index, title ->

            val item = TextView(this).apply {
                text = title
                textSize = 10.5f
                gravity = Gravity.CENTER

                setPadding(
                    dp(2),
                    dp(10),
                    dp(2),
                    dp(10)
                )

                setOnClickListener {
                    navigateTo(index)
                }
            }

            nav.addView(
                item,
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, dp(66))
        )

        root.addView(
            content,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        root.addView(
            nav,
            LinearLayout.LayoutParams(-1, dp(60))
        )

        setContentView(root)
        updateNavigationColors()
    }

    private fun pageContainer(): FrameLayout {
        return root.findViewWithTag("page_container")
    }

    private fun makeScroll(): Pair<ScrollView, LinearLayout> {

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(17),
                dp(16),
                dp(24)
            )
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false

            addView(
                body,
                ScrollView.LayoutParams(-1, -2)
            )
        }

        return Pair(scroll, body)
    }

    private fun newPage(): Pair<ScrollView, LinearLayout> {

        val pair = makeScroll()

        pageContainer().removeAllViews()

        pageContainer().addView(
            pair.first,
            FrameLayout.LayoutParams(-1, -1)
        )

        return pair
    }

    private fun navigateTo(page: Int) {

        currentPage = page

        when (page) {
            0 -> showHome()
            1 -> showCalendar()
            2 -> showPrayer()
            3 -> showWeather()
            4 -> showGames()
        }

        updateNavigationColors()
    }

    private fun updateNavigationColors() {

        if (!::root.isInitialized || root.childCount < 3) {
            return
        }

        val nav = root.getChildAt(2) as? LinearLayout ?: return

        for (i in 0 until nav.childCount) {

            val item = nav.getChildAt(i) as TextView

            item.setTextColor(
                if (i == currentPage) gold else muted
            )

            item.setTypeface(
                null,
                if (i == currentPage) Typeface.BOLD
                else Typeface.NORMAL
            )
        }
    }

    private fun addHeading(
        body: LinearLayout,
        title: String,
        subtitle: String = ""
    ) {

        body.addView(
            label(title, 21f, gold, true, Gravity.START)
        )

        if (subtitle.isNotBlank()) {
            body.addView(
                label(subtitle, 11.5f, muted, false, Gravity.START)
            )
        }

        body.addView(space(12))
    }

    private fun space(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        }
    }

    private fun addCard(
        body: LinearLayout,
        title: String,
        content: String,
        accent: Boolean = false
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            background = rounded(
                if (accent) darkGreen else cardColor,
                18,
                if (accent) gold else 0
            )

            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )
        }

        card.addView(
            label(title, 13f, gold, true, Gravity.START)
        )

        card.addView(
            label(content, 14f, white, false, Gravity.START)
        )

        body.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            }
        )
    }

    private fun addSectionTitle(
        body: LinearLayout,
        title: String
    ) {

        body.addView(
            label(title, 16f, gold, true, Gravity.START)
        )

        body.addView(space(5))
    }
        private fun showHome() {
        val (_, body) = newPage()

        val today = Calendar.getInstance()

        val j = toJalali(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH)
        )

        val monthName = jalaliMonthName(j[1])

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(darkGreen, 24, gold)
            setPadding(dp(16), dp(18), dp(16), dp(18))
        }

        hero.addView(
            label("امروز • ${weekday(today)}", 13f, muted, true)
        )

        hero.addView(
            label(j[2].toString(), 48f, gold, true)
        )

        hero.addView(
            label("$monthName ${j[0]}", 19f, white, true)
        )

        hero.addView(
            label(gregorianDate(today), 11f, muted)
        )

        body.addView(
            hero,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(14)
            }
        )

        val locationCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(cardColor, 15)
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }

        locationCard.addView(
            label("⌖", 22f, gold, true)
        )

        locationCard.addView(
            label(locationLabel, 11.5f, white, false, Gravity.START),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        locationCard.addView(
            label("مکان", 10f, muted)
        )

        body.addView(
            locationCard,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(14)
            }
        )

        addSectionTitle(body, "اوقات شرعی امروز")

        val timings = prayerJson
            ?.optJSONObject("data")
            ?.optJSONObject("timings")

        val prayerItems = listOf(
            "صبح" to "Fajr",
            "طلوع" to "Sunrise",
            "ظهر" to "Dhuhr",
            "عصر" to "Asr",
            "مغرب" to "Maghrib"
        )

        val prayerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        prayerItems.forEach { (name, key) ->

            val value = timings
                ?.optString(key, "--")
                ?.substringBefore(" ") ?: "--"

            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(cardColor, 13)

                setPadding(
                    dp(3),
                    dp(9),
                    dp(3),
                    dp(9)
                )

                addView(
                    label(name, 10f, muted)
                )

                addView(
                    label(value, 12f, gold, true)
                )
            }

            prayerRow.addView(
                tile,
                LinearLayout.LayoutParams(0, dp(68), 1f).apply {
                    marginEnd = dp(4)
                }
            )
        }

        body.addView(
            prayerRow,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(14)
            }
        )

        if (timings == null) {

            body.addView(
                label(
                    "برای دریافت زمان‌های دقیق، اینترنت و دسترسی مکان را بررسی کنید.",
                    11f,
                    muted,
                    false,
                    Gravity.START
                )
            )

            body.addView(space(8))
        }

        addSectionTitle(body, "آب‌وهوای فعلی")

        val current = weatherJson?.optJSONObject("current")

        val temperature = current
            ?.optDouble("temperature_2m", Double.NaN)
            ?: Double.NaN

        val humidity = current
            ?.optInt("relative_humidity_2m", -1)
            ?: -1

        val condition = weatherDescription(
            current?.optInt("weather_code", -1) ?: -1
        )

        val weatherHero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(cardColor, 18)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        weatherHero.addView(
            label(
                if (temperature.isNaN()) "☁" else "☀",
                34f,
                gold,
                true
            )
        )

        val weatherInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        weatherInfo.addView(
            label(
                if (temperature.isNaN()) {
                    "در انتظار دریافت اطلاعات"
                } else {
                    "${temperature.roundToInt()}°C"
                },
                25f,
                white,
                true,
                Gravity.START
            )
        )

        weatherInfo.addView(
            label(
                if (temperature.isNaN()) {
                    "اتصال اینترنت را بررسی کنید"
                } else {
                    "$condition  •  رطوبت ${if (humidity >= 0) "$humidity٪" else "--"}"
                },
                11f,
                muted,
                false,
                Gravity.START
            )
        )

        weatherHero.addView(
            weatherInfo,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        weatherHero.addView(
            label("۷ روز", 11f, gold, true)
        )

        body.addView(
            weatherHero,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(14)
            }
        )

        body.addView(
            actionButton("به‌روزرسانی اطلاعات آنلاین") {
                loadOnlineInformation()
            },
            LinearLayout.LayoutParams(-1, dp(50))
        )

        body.addView(space(8))

        body.addView(
            actionButton("مشاهده تقویم شمسی") {
                navigateTo(1)
            },
            LinearLayout.LayoutParams(-1, dp(48))
        )

        updateNavigationColors()
    }

    private fun showCalendar() {

        val (_, body) = newPage()

        addHeading(
            body,
            "تقویم شمسی",
            "ماه موردنظر را انتخاب کنید"
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        header.addView(
            actionButton("ماه قبل") {
                changeMonth(-1)
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        header.addView(
            label(
                jalaliMonthTitle(displayedMonth),
                16f,
                gold,
                true
            ),
            LinearLayout.LayoutParams(0, dp(44), 2f)
        )

        header.addView(
            actionButton("ماه بعد") {
                changeMonth(1)
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        body.addView(header)
        body.addView(space(12))

        val grid = GridLayout(this).apply {
            columnCount = 7
            background = rounded(cardColor, 17)

            setPadding(
                dp(5),
                dp(8),
                dp(5),
                dp(8)
            )
        }

        listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach {
            grid.addView(
                gridCell(it, true, false)
            )
        }

        val first = firstDayOfJalaliMonth(displayedMonth)
        val count = jalaliMonthLength(displayedMonth)

        val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7

        repeat(offset) {
            grid.addView(
                gridCell("", false, false)
            )
        }

        val today = Calendar.getInstance()

        val todayJ = toJalali(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH)
        )

        val monthJ = toJalali(
            displayedMonth.get(Calendar.YEAR),
            displayedMonth.get(Calendar.MONTH) + 1,
            1
        )

        for (day in 1..count) {

            val isToday =
                todayJ[0] == monthJ[0] &&
                todayJ[1] == monthJ[1] &&
                todayJ[2] == day

            grid.addView(
                gridCell(day.toString(), false, isToday)
            )
        }

        body.addView(
            grid,
            LinearLayout.LayoutParams(-1, -2)
        )

        body.addView(space(12))

        addCard(
            body,
            "راهنمای تقویم",
            "روز جاری با رنگ طلایی مشخص شده است. برای رفتن به ماه‌های قبل و بعد از دکمه‌های بالا استفاده کنید."
        )
    }

    private fun gridCell(
        value: String,
        header: Boolean,
        selected: Boolean
    ): TextView {

        return label(
            value,
            if (header) 11f else 13f,
            when {
                selected -> black
                header -> gold
                else -> white
            },
            header || selected
        ).apply {

            background = when {
                selected -> rounded(gold, 9)
                header -> rounded(darkGreen, 8)
                else -> rounded(softCard, 8)
            }

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(43)

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
        }
    }

    private fun changeMonth(amount: Int) {

        val j = toJalali(
            displayedMonth.get(Calendar.YEAR),
            displayedMonth.get(Calendar.MONTH) + 1,
            1
        )

        val absolute =
            j[0] * 12 + j[1] - 1 + amount

        val jy = Math.floorDiv(absolute, 12)
        val jm = Math.floorMod(absolute, 12) + 1

        val greg = toGregorian(jy, jm, 1)

        displayedMonth.set(
            greg[0],
            greg[1] - 1,
            greg[2]
        )

        displayedMonth.set(
            Calendar.HOUR_OF_DAY,
            12
        )

        showCalendar()
    }
        private fun showPrayer() {
        val (_, body) = newPage()

        addHeading(
            body,
            "اوقات شرعی",
            "زمان‌بندی محاسبه‌شده بر اساس موقعیت شما"
        )

        addCard(body, "موقعیت", locationLabel, true)

        val timings = prayerJson
            ?.optJSONObject("data")
            ?.optJSONObject("timings")

        if (timings == null) {

            addCard(
                body,
                "اطلاعات در دسترس نیست",
                "برای دریافت اوقات شرعی، اتصال اینترنت و دسترسی مکان را بررسی کنید."
            )

        } else {

            val entries = listOf(
                "اذان صبح" to "Fajr",
                "طلوع آفتاب" to "Sunrise",
                "اذان ظهر" to "Dhuhr",
                "اذان عصر" to "Asr",
                "غروب آفتاب" to "Sunset",
                "اذان مغرب" to "Maghrib",
                "نیمه‌شب شرعی" to "Midnight"
            )

            entries.forEachIndexed { index, item ->

                val time = timings
                    .optString(item.second, "--")
                    .substringBefore(" ")

                addPrayerTile(
                    body,
                    item.first,
                    time,
                    index
                )
            }
        }

        body.addView(
            actionButton("دریافت مجدد اوقات شرعی") {
                loadPrayerTimes()
            },
            LinearLayout.LayoutParams(-1, dp(50))
        )
    }

    private fun addPrayerTile(
        body: LinearLayout,
        title: String,
        time: String,
        index: Int
    ) {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            background = rounded(
                if (index == 0 || index == 5) darkGreen else cardColor,
                15,
                if (index == 0 || index == 5) gold else 0
            )

            setPadding(
                dp(15),
                dp(11),
                dp(15),
                dp(11)
            )
        }

        row.addView(
            label("✦", 17f, gold, true)
        )

        row.addView(
            label(
                title,
                14f,
                white,
                true,
                Gravity.START
            ),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        row.addView(
            label(time, 17f, gold, true)
        )

        body.addView(
            row,
            LinearLayout.LayoutParams(-1, dp(57)).apply {
                bottomMargin = dp(8)
            }
        )
    }

    private fun showWeather() {

        val (_, body) = newPage()

        addHeading(
            body,
            "هواشناسی هفت‌روزه",
            "پیش‌بینی آنلاین بر اساس موقعیت فعلی"
        )

        addCard(body, "موقعیت", locationLabel, true)

        val json = weatherJson

        if (json == null) {

            addCard(
                body,
                "در انتظار اطلاعات",
                "هنوز داده‌ای دریافت نشده است. اتصال اینترنت را بررسی یا دوباره تلاش کنید."
            )

        } else {

            val current = json.optJSONObject("current")

            val temp = current?.optDouble(
                "temperature_2m",
                Double.NaN
            ) ?: Double.NaN

            val feels = current?.optDouble(
                "apparent_temperature",
                Double.NaN
            ) ?: Double.NaN

            val humidity = current?.optInt(
                "relative_humidity_2m",
                -1
            ) ?: -1

            val wind = current?.optDouble(
                "wind_speed_10m",
                Double.NaN
            ) ?: Double.NaN

            val code = current?.optInt("weather_code", -1) ?: -1

            val now = if (temp.isNaN()) {
                "--"
            } else {
                "${temp.roundToInt()}°C"
            }

            addCard(
                body,
                "$now  •  ${weatherDescription(code)}",
                "دمای محسوس: ${if (feels.isNaN()) "--" else "${feels.roundToInt()}°C"}\n" +
                    "رطوبت: ${if (humidity < 0) "--" else "$humidity٪"}\n" +
                    "سرعت باد: ${if (wind.isNaN()) "--" else "${wind.roundToInt()} کیلومتر بر ساعت"}",
                true
            )

            val daily = json.optJSONObject("daily")

            val dates = daily?.optJSONArray("time")
            val max = daily?.optJSONArray("temperature_2m_max")
            val min = daily?.optJSONArray("temperature_2m_min")
            val codes = daily?.optJSONArray("weather_code")

            if (dates != null) {

                addSectionTitle(body, "پیش‌بینی روزانه")

                for (i in 0 until dates.length()) {

                    val date = dates.optString(i)

                    val dayName = try {

                        val parsed = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                        ).parse(date)

                        SimpleDateFormat(
                            "EEEE، d MMM",
                            Locale("fa", "IR")
                        ).format(parsed ?: Date())

                    } catch (_: Exception) {
                        date
                    }

                    val high = max?.optDouble(
                        i,
                        Double.NaN
                    ) ?: Double.NaN

                    val low = min?.optDouble(
                        i,
                        Double.NaN
                    ) ?: Double.NaN

                    val description = weatherDescription(
                        codes?.optInt(i, -1) ?: -1
                    )

                    val highText = if (high.isNaN()) {
                        "--"
                    } else {
                        "${high.roundToInt()}°"
                    }

                    val lowText = if (low.isNaN()) {
                        "--"
                    } else {
                        "${low.roundToInt()}°"
                    }

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = rounded(cardColor, 14)

                        setPadding(
                            dp(10),
                            dp(8),
                            dp(10),
                            dp(8)
                        )
                    }

                    row.addView(
                        label(
                            dayName,
                            12f,
                            white,
                            true,
                            Gravity.START
                        ),
                        LinearLayout.LayoutParams(0, -2, 1.2f)
                    )

                    row.addView(
                        label(
                            description,
                            10.5f,
                            muted,
                            false
                        ),
                        LinearLayout.LayoutParams(0, -2, 1f)
                    )

                    row.addView(
                        label(
                            "$highText / $lowText",
                            12f,
                            gold,
                            true
                        ),
                        LinearLayout.LayoutParams(0, -2, 1f)
                    )

                    body.addView(
                        row,
                        LinearLayout.LayoutParams(-1, dp(55)).apply {
                            bottomMargin = dp(7)
                        }
                    )
                }
            }
        }

        body.addView(
            actionButton("به‌روزرسانی هواشناسی") {
                loadWeather()
            },
            LinearLayout.LayoutParams(-1, dp(50))
        )
    }

    private fun showGames() {

        val (_, body) = newPage()

        addHeading(
            body,
            "بازی‌ها",
            "سرگرمی کوتاه در کنار تقویم"
        )

        var secret = (1..100).random()
        var attempts = 0

        addCard(
            body,
            "بازی حدس عدد",
            "یک عدد بین ۱ تا ۱۰۰ انتخاب شده است. عدد درست را پیدا کن!",
            true
        )

        val input = EditText(this).apply {

            hint = "عدد موردنظر را وارد کن"

            inputType = InputType.TYPE_CLASS_NUMBER

            setTextColor(white)
            setHintTextColor(muted)

            textSize = 18f
            gravity = Gravity.CENTER

            background = rounded(cardColor, 14)

            setPadding(
                dp(10),
                0,
                dp(10),
                0
            )
        }

        body.addView(
            input,
            LinearLayout.LayoutParams(-1, dp(54))
        )

        body.addView(space(8))

        val result = label(
            "منتظر حدس تو هستم",
            15f,
            gold,
            true
        )

        body.addView(result)

        body.addView(
            actionButton("ثبت حدس") {

                val guess = input.text
                    .toString()
                    .toIntOrNull()

                if (guess == null || guess !in 1..100) {

                    result.text = "یک عدد بین ۱ تا ۱۰۰ وارد کن"

                } else {

                    attempts++

                    when {
                        guess == secret -> {
                            result.text =
                                "آفرین! عدد درست بود 🎉\nتعداد تلاش: $attempts"
                        }

                        guess < secret -> {
                            result.text = "عدد بزرگ‌تر است"
                        }

                        else -> {
                            result.text = "عدد کوچک‌تر است"
                        }
                    }
                }

            },
            LinearLayout.LayoutParams(-1, dp(50))
        )

        body.addView(space(7))

        body.addView(
            actionButton("شروع بازی جدید") {

                secret = (1..100).random()
                attempts = 0

                input.setText("")

                result.text =
                    "عدد جدید انتخاب شد؛ دوباره حدس بزن"
            },
            LinearLayout.LayoutParams(-1, dp(50))
        )
    }

    private fun loadOnlineInformation() {
                loadWeather()
        loadPrayerTimes()
    }

    private fun requestLocation() {

        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
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

        if (requestCode == 100) {
            if (grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                findPhoneLocation()
            } else {
                loadOnlineInformation()
            }
        }
    }

    private fun findPhoneLocation() {

        try {
            val manager = locationManager ?: return

            val gpsEnabled = manager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )

            val networkEnabled = manager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )

            val provider = when {
                gpsEnabled -> LocationManager.GPS_PROVIDER
                networkEnabled -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider == null) {
                loadOnlineInformation()
                return
            }

            val lastLocation = try {
                manager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            }

            if (lastLocation != null) {
                useLocation(lastLocation)
                return
            }

            val listener = object : LocationListener {

                override fun onLocationChanged(location: Location) {
                    useLocation(location)
                }

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {}
            }

            locationListener = listener

            manager.requestLocationUpdates(
                provider,
                3000L,
                10f,
                listener,
                Looper.getMainLooper()
            )

            mainHandler.postDelayed({
                if (locationListener != null) {
                    stopLocationUpdates()
                    loadOnlineInformation()
                }
            }, 15000L)

        } catch (_: Exception) {
            loadOnlineInformation()
        }
    }

    private fun useLocation(location: Location) {

        latitude = location.latitude
        longitude = location.longitude

        locationLabel = String.format(
            Locale.US,
            "%.4f, %.4f",
            latitude,
            longitude
        )

        stopLocationUpdates()

        mainHandler.post {
            when (currentPage) {
                0 -> showHome()
                2 -> showPrayer()
                3 -> showWeather()
            }
        }

        loadOnlineInformation()
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

    private fun loadWeather() {

        executor.execute {

            var connection: HttpURLConnection? = null

            try {

                val address =
                    "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto" +
                    "&forecast_days=7"

                connection = URL(address)
                    .openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val result = JSONObject(response)

                mainHandler.post {
                    weatherJson = result

                    when (currentPage) {
                        0 -> showHome()
                        3 -> showWeather()
                    }
                }

            } catch (_: Exception) {

                mainHandler.post {
                    Toast.makeText(
                        this,
                        "دریافت هواشناسی ناموفق بود",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun loadPrayerTimes() {

        executor.execute {

            var connection: HttpURLConnection? = null

            try {

                val date = SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.US
                ).format(Date())

                val address =
                    "https://api.aladhan.com/v1/timings/$date" +
                    "?latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&method=3"

                connection = URL(address)
                    .openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val result = JSONObject(response)

                mainHandler.post {
                    prayerJson = result

                    when (currentPage) {
                        0 -> showHome()
                        2 -> showPrayer()
                    }
                }

            } catch (_: Exception) {

                mainHandler.post {
                    Toast.makeText(
                        this,
                        "دریافت اوقات شرعی ناموفق بود",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun weatherDescription(code: Int): String {

        return when (code) {
            0 -> "آسمان صاف"
            1, 2 -> "کمی ابری"
            3 -> "ابری"
            45, 48 -> "مه‌آلود"
            51, 53, 55 -> "نم‌نم باران"
            61, 63, 65 -> "بارانی"
            71, 73, 75 -> "برفی"
            80, 81, 82 -> "رگبار باران"
            95, 96, 99 -> "رعد و برق"
            else -> "وضعیت نامشخص"
        }
    }

    private fun weekday(calendar: Calendar): String {

        val names = listOf(
            "یکشنبه",
            "دوشنبه",
            "سه‌شنبه",
            "چهارشنبه",
            "پنجشنبه",
            "جمعه",
            "شنبه"
        )

        return names[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }

    private fun gregorianDate(calendar: Calendar): String {

        val format = SimpleDateFormat(
            "yyyy/MM/dd",
            Locale.US
        )

        return "تاریخ میلادی: ${format.format(calendar.time)}"
    }

    private fun toJalali(
        gy: Int,
        gm: Int,
        gd: Int
    ): IntArray {

        val gDays = intArrayOf(
            0, 31, 59, 90, 120, 151,
            181, 212, 243, 273, 304, 334
        )

        val gy2 = if (gm > 2) gy + 1 else gy

        var days =
            355666 +
            365 * gy +
            (gy2 + 3) / 4 -
            (gy2 + 99) / 100 +
            (gy2 + 399) / 400 +
            gd +
            gDays[gm - 1]

        var jy = -1595 + 33 * (days / 12053)

        days %= 12053

        jy += 4 * (days / 1461)

        days %= 1461

        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }

        val jm: Int
        val jd: Int

        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }

        return intArrayOf(jy, jm, jd)
    }

    private fun jalaliMonthName(month: Int): String {

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

    private fun jalaliMonthTitle(calendar: Calendar): String {

        val j = toJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            1
        )

        return "${jalaliMonthName(j[1])} ${j[0]}"
    }

    private fun firstDayOfJalaliMonth(
        calendar: Calendar
    ): Calendar {

        val j = toJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            1
        )

        val greg = toGregorian(j[0], j[1], 1)

        return Calendar.getInstance().apply {
            set(
                greg[0],
                greg[1] - 1,
                greg[2],
                12,
                0,
                0
            )
        }
    }

    private fun jalaliMonthLength(calendar: Calendar): Int {

        val j = toJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            1
        )

        return when {
            j[1] <= 6 -> 31
            j[1] <= 11 -> 30
            else -> {
                val nextYear = toGregorian(j[0] + 1, 1, 1)
                val currentYear = toGregorian(j[0], 1, 1)

                val first = Calendar.getInstance().apply {
                    set(
                        currentYear[0],
                        currentYear[1] - 1,
                        currentYear[2],
                        12,
                        0,
                        0
                    )
                }

                val next = Calendar.getInstance().apply {
                    set(
                        nextYear[0],
                        nextYear[1] - 1,
                        nextYear[2],
                        12,
                        0,
                        0
                    )
                }

                val days = (
                    (next.timeInMillis - first.timeInMillis) /
                    (24L * 60L * 60L * 1000L)
                ).toInt()

                if (days > 365) 30 else 29
            }
        }
    }

    private fun toGregorian(
        jy: Int,
        jm: Int,
        jd: Int
    ): IntArray {

        val year = jy + 1595

        var days =
            -355668 +
            365 * year +
            (year / 33) * 8 +
            ((year % 33 + 3) / 4)

        if (jm <= 6) {
            days += (jm - 1) * 31
        } else {
            days += 186 + (jm - 7) * 30
        }

        days += jd - 1

        var gy = 400 * (days / 146097)

        days %= 146097

        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524

            if (days >= 365) {
                days++
            }
        }

        gy += 4 * (days / 1461)

        days %= 1461

        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }

        val gd = days + 1

        val leap =
            gy % 4 == 0 &&
            (gy % 100 != 0 || gy % 400 == 0)

        val monthDays = intArrayOf(
            0,
            31,
            if (leap) 29 else 28,
            31, 30, 31, 30,
            31, 31, 30, 31, 30, 31
        )

        var gm = 1
        var remaining = gd

        while (
            gm <= 12 &&
            remaining > monthDays[gm]
        ) {
            remaining -= monthDays[gm]
            gm++
        }

        return intArrayOf(
            gy,
            gm,
            remaining
        )
    }
}
