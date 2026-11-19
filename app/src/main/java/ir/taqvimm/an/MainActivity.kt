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
        "اوقات شرعی",
        "هواشناسی",
        "بازی‌ها"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        radius: Int = 18
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun text(
        value: String,
        size: Float = 15f,
        color: Int = white,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER

            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }

            setPadding(
                dp(6),
                dp(7),
                dp(6),
                dp(7)
            )
        }
    }

    private fun button(
        value: String,
        onClick: () -> Unit
    ): Button {

        return Button(this).apply {
            text = value
            textSize = 13f
            setTextColor(white)
            isAllCaps = false
            background = rounded(emerald, 14)

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
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(10)
            )
            background = rounded(darkGreen, 0)
        }

        header.addView(
            text("تقویم من", 23f, gold, true)
        )

        header.addView(
            text(
                "تقویم هوشمند • زمان • آب‌وهوا",
                11f,
                muted
            )
        )

        val content = FrameLayout(this).apply {
            id = View.generateViewId()
            tag = "page_container"
        }

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(
                dp(4),
                dp(7),
                dp(4),
                dp(7)
            )
            background = rounded(darkGreen, 0)
        }

        pageTitles.forEachIndexed { index, title ->

            val item = TextView(this).apply {
                text = title
                textSize = 10.5f
                gravity = Gravity.CENTER

                setTextColor(
                    if (index == 0) gold else muted
                )

                setPadding(
                    dp(3),
                    dp(10),
                    dp(3),
                    dp(10)
                )

                setOnClickListener {
                    navigateTo(index)
                }
            }

            nav.addView(
                item,
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                )
            )
        }

        root.addView(header)

        root.addView(
            content,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(nav)

        setContentView(root)
    }

    private fun pageContainer(): FrameLayout {
        return root.findViewWithTag("page_container")
    }

    private fun makeScroll(): Pair<ScrollView, LinearLayout> {

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(18),
                dp(16),
                dp(24)
            )
        }

        val scroll = ScrollView(this).apply {

            isFillViewport = true
            isVerticalScrollBarEnabled = false

            addView(
                body,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        return Pair(scroll, body)
    }

    private fun newPage(): Pair<ScrollView, LinearLayout> {

        val pair = makeScroll()

        pageContainer().removeAllViews()

        pageContainer().addView(
            pair.first,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
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

        val nav = root.getChildAt(2) as? LinearLayout
            ?: return

        for (i in 0 until nav.childCount) {

            val item = nav.getChildAt(i) as TextView

            item.setTextColor(
                if (i == currentPage) gold else muted
            )
        }
    }

    private fun addTitle(
        body: LinearLayout,
        title: String,
        subtitle: String = ""
    ) {

        body.addView(
            text(title, 21f, gold, true)
        )

        if (subtitle.isNotBlank()) {
            body.addView(
                text(subtitle, 12f, muted)
            )
        }

        body.addView(space(8))
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
        content: String
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cardColor)

            setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
            )
        }

        card.addView(
            text(title, 15f, gold, true)
        )

        card.addView(
            text(content, 14f, white)
        )

        body.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )
    }
        private fun showHome() {

        val (_, body) = newPage()

        addTitle(
            body,
            "داشبورد امروز",
            "نمای کلی روز شما"
        )

        val today = Calendar.getInstance()

        addCard(
            body,
            jalaliDate(today),
            weekday(today)
        )

        addCard(
            body,
            "موقعیت فعلی",
            locationLabel
        )

        addCard(
            body,
            "اوقات شرعی",
            if (prayerJson == null) {
                "برای دریافت اطلاعات، مکان‌یابی و اتصال اینترنت را بررسی کنید."
            } else {
                prayerSummary()
            }
        )

        addCard(
            body,
            "هواشناسی",
            if (weatherJson == null) {
                "در حال دریافت اطلاعات آب‌وهوا..."
            } else {
                weatherSummary()
            }
        )

        body.addView(
            button("به‌روزرسانی اطلاعات آنلاین") {
                loadOnlineInformation()
            },
            LinearLayout.LayoutParams(-1, dp(52))
        )

        body.addView(space(12))

        body.addView(
            button("رفتن به تقویم") {
                navigateTo(1)
            },
            LinearLayout.LayoutParams(-1, dp(48))
        )

        updateNavigationColors()
    }

    private fun showCalendar() {

        val (_, body) = newPage()

        addTitle(
            body,
            "تقویم شمسی",
            "ماه را با دکمه‌های زیر تغییر دهید"
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        header.addView(
            button("ماه قبل") {
                changeMonth(-1)
            },
            LinearLayout.LayoutParams(0, dp(46), 1f)
        )

        header.addView(
            text(
                jalaliMonthTitle(displayedMonth),
                17f,
                gold,
                true
            ),
            LinearLayout.LayoutParams(0, dp(46), 2f)
        )

        header.addView(
            button("ماه بعد") {
                changeMonth(1)
            },
            LinearLayout.LayoutParams(0, dp(46), 1f)
        )

        body.addView(header)
        body.addView(space(10))

        val grid = GridLayout(this).apply {
            columnCount = 7
            background = rounded(cardColor)

            setPadding(
                dp(5),
                dp(8),
                dp(5),
                dp(8)
            )
        }

        val names = listOf(
            "ش", "ی", "د", "س", "چ", "پ", "ج"
        )

        names.forEach {
            grid.addView(gridCell(it, true))
        }

        val first = firstDayOfJalaliMonth(displayedMonth)
        val count = jalaliMonthLength(displayedMonth)

        val offset =
            (first.get(Calendar.DAY_OF_WEEK) + 5) % 7

        repeat(offset) {
            grid.addView(gridCell("", false))
        }

        for (day in 1..count) {

            val cell = gridCell(day.toString(), false)

            val today = Calendar.getInstance()

            if (
                jalaliDate(today) ==
                jalaliDate(first).substringBefore(" ") + " $day"
            ) {
                cell.setTextColor(gold)
                cell.setTypeface(
                    cell.typeface,
                    Typeface.BOLD
                )
            }

            grid.addView(cell)
        }

        body.addView(grid)
        body.addView(space(12))

        addCard(
            body,
            "راهنما",
            "روز جاری با رنگ طلایی مشخص می‌شود. برای جابه‌جایی بین ماه‌ها از دکمه‌های بالا استفاده کنید."
        )
    }

    private fun gridCell(
        value: String,
        header: Boolean
    ): TextView {

        return text(
            value,
            if (header) 12f else 14f,
            if (header) gold else white,
            header
        ).apply {

            background = if (header) {
                rounded(darkGreen, 8)
            } else {
                rounded(
                    Color.rgb(35, 53, 47),
                    8
                )
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

        showCalendar()
    }

    private fun showPrayer() {

        val (_, body) = newPage()

        addTitle(
            body,
            "اوقات شرعی",
            "بر اساس مختصات مکانی و زمان محلی"
        )

        addCard(
            body,
            "مکان",
            locationLabel
        )

        if (prayerJson == null) {

            addCard(
                body,
                "وضعیت",
                "اطلاعات هنوز دریافت نشده است. اینترنت و دسترسی مکان را بررسی کنید."
            )

        } else {

            val timings = prayerJson!!
                .optJSONObject("data")
                ?.optJSONObject("timings")

            val names = listOf(
                "اذان صبح" to "Fajr",
                "طلوع آفتاب" to "Sunrise",
                "اذان ظهر" to "Dhuhr",
                "اذان عصر" to "Asr",
                "غروب آفتاب" to "Sunset",
                "اذان مغرب" to "Maghrib",
                "نیمه‌شب شرعی" to "Midnight"
            )

            names.forEach { (label, key) ->

                addCard(
                    body,
                    label,
                    timings?.optString(key, "--")
                        ?.substringBefore(" ") ?: "--"
                )
            }
        }

        body.addView(
            button("دریافت مجدد اوقات شرعی") {
                loadPrayerTimes()
            },
            LinearLayout.LayoutParams(-1, dp(50))
        )
    }

    private fun showWeather() {

        val (_, body) = newPage()

        addTitle(
            body,
            "هواشناسی هفت‌روزه",
            "پیش‌بینی آنلاین بر اساس موقعیت"
        )

        addCard(
            body,
            "مکان",
            locationLabel
        )

        val json = weatherJson

        if (json == null) {

            addCard(
                body,
                "وضعیت",
                "در حال دریافت اطلاعات یا اتصال اینترنت در دسترس نیست."
            )

        } else {

            val current = json.optJSONObject("current")

            val temp = current?.optDouble(
                "temperature_2m",
                Double.NaN
            ) ?: Double.NaN

            addCard(
                body,
                "دمای فعلی",
                if (temp.isNaN()) "--"
                else "${temp.roundToInt()}°C"
            )

            val daily = json.optJSONObject("daily")

            val dates = daily?.optJSONArray("time")
            val max = daily?.optJSONArray("temperature_2m_max")
            val min = daily?.optJSONArray("temperature_2m_min")
            val codes = daily?.optJSONArray("weather_code")

            if (dates != null) {

                for (i in 0 until dates.length()) {

                    val date = dates.optString(i)

                    val dayName = try {

                        val parsed = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                        ).parse(date)

                        SimpleDateFormat(
                            "EEEE، d MMM",
                            Locale("fa")
                        ).format(parsed ?: Date())

                    } catch (_: Exception) {
                        date
                    }

                    val high = max?.optDouble(
                        i,
                        Double.NaN
                    )

                    val low = min?.optDouble(
                        i,
                        Double.NaN
                    )

                    val condition = weatherDescription(
                        codes?.optInt(i, -1) ?: -1
                    )

                    addCard(
                        body,
                        dayName,
                        "$condition\nبیشینه: ${
                            high?.takeIf { !it.isNaN() }
                                ?.roundToInt() ?: "--"
                        }°  |  کمینه: ${
                            low?.takeIf { !it.isNaN() }
                                ?.roundToInt() ?: "--"
                        }°"
                    )
                }
            }
        }

        body.addView(
            button("به‌روزرسانی هواشناسی") {
                loadWeather()
            },
            LinearLayout.LayoutParams(-1, dp(50))
        )
    }
        private fun showGames() {

        val (_, body) = newPage()

        addTitle(
            body,
            "بازی‌ها",
            "کمی سرگرمی در کنار تقویم"
        )

        var secret = (1..100).random()
        var attempts = 0

        addCard(
            body,
            "بازی حدس عدد",
            "یک عدد بین ۱ تا ۱۰۰ انتخاب شده است. آن را حدس بزن!"
        )

        val input = EditText(this).apply {
            hint = "عدد مورد نظر را وارد کن"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(white)
            setHintTextColor(muted)
            textSize = 18f
            gravity = Gravity.CENTER
            background = rounded(cardColor)
        }

        body.addView(
            input,
            LinearLayout.LayoutParams(-1, dp(55))
        )

        val result = text(
            "منتظر حدس تو هستم",
            16f,
            gold,
            true
        )

        body.addView(result)

        body.addView(
            button("ثبت حدس") {

                val guess = input.text.toString().toIntOrNull()

                if (guess == null || guess !in 1..100) {
                    result.text = "یک عدد بین ۱ تا ۱۰۰ وارد کن"
                    return@button
                }

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
            },
            LinearLayout.LayoutParams(-1, dp(50))
        )

        body.addView(
            button("بازی جدید") {
                secret = (1..100).random()
                attempts = 0
                input.setText("")
                result.text = "عدد جدید انتخاب شد"
            },
            LinearLayout.LayoutParams(-1, dp(50))
        )
    }

    private fun loadOnlineInformation() {
        loadWeather()
        loadPrayerTimes()
    }

    private fun loadWeather() {

        executor.execute {

            try {

                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$latitude" +
                        "&longitude=$longitude" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                        "&forecast_days=7" +
                        "&timezone=auto"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val json = JSONObject(response)

                mainHandler.post {

                    weatherJson = json

                    if (currentPage == 3) {
                        showWeather()
                    } else if (currentPage == 0) {
                        showHome()
                    }
                }

            } catch (e: Exception) {

                mainHandler.post {

                    Toast.makeText(
                        this,
                        "دریافت هواشناسی ناموفق بود",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadPrayerTimes() {

        executor.execute {

            try {

                val date = SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.US
                ).format(Date())

                val url = URL(
                    "https://api.aladhan.com/v1/timings/$date" +
                        "?latitude=$latitude" +
                        "&longitude=$longitude" +
                        "&method=3"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val json = JSONObject(response)

                mainHandler.post {

                    prayerJson = json

                    if (currentPage == 2) {
                        showPrayer()
                    } else if (currentPage == 0) {
                        showHome()
                    }
                }

            } catch (e: Exception) {

                mainHandler.post {

                    Toast.makeText(
                        this,
                        "دریافت اوقات شرعی ناموفق بود",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun weatherSummary(): String {

        val current = weatherJson
            ?.optJSONObject("current")

        val temperature = current?.optDouble(
            "temperature_2m",
            Double.NaN
        ) ?: Double.NaN

        val humidity = current?.optInt(
            "relative_humidity_2m",
            -1
        ) ?: -1

        if (temperature.isNaN()) {
            return "اطلاعات دما در دسترس نیست"
        }

        return "دما: ${temperature.roundToInt()} درجه\nرطوبت: ${if (humidity >= 0) "$humidity درصد" else "--"}"
    }

    private fun prayerSummary(): String {

        val timings = prayerJson
            ?.optJSONObject("data")
            ?.optJSONObject("timings")

        return "صبح: ${timings?.optString("Fajr", "--")}\nظهر: ${timings?.optString("Dhuhr", "--")}\nمغرب: ${timings?.optString("Maghrib", "--")}"
    }

    private fun weatherDescription(code: Int): String {

        return when (code) {
            0 -> "آفتابی"
            1, 2 -> "کمی ابری"
            3 -> "ابری"
            45, 48 -> "مه‌آلود"
            51, 53, 55 -> "نم‌نم باران"
            61, 63, 65 -> "بارانی"
            71, 73, 75 -> "برفی"
            80, 81, 82 -> "رگبار باران"
            95, 96, 99 -> "طوفان رعدوبرق"
            else -> "وضعیت نامشخص"
        }
    }

    private fun requestLocation() {

        val fine = checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )

            return
        }

        findPhoneLocation()
    }

    private fun findPhoneLocation() {

        try {

            val manager = locationManager ?: return

            val provider = when {

                manager.isProviderEnabled(
                    android.location.LocationManager.GPS_PROVIDER
                ) -> android.location.LocationManager.GPS_PROVIDER

                manager.isProviderEnabled(
                    android.location.LocationManager.NETWORK_PROVIDER
                ) -> android.location.LocationManager.NETWORK_PROVIDER

                else -> null
            }

            if (provider == null) {
                loadOnlineInformation()
                return
            }

            val listener =
                object : android.location.LocationListener {

                    override fun onLocationChanged(
                        location: android.location.Location
                    ) {

                        useLocation(location)
                    }

                    override fun onProviderEnabled(
                        provider: String
                    ) {}

                    override fun onProviderDisabled(
                        provider: String
                    ) {}
                }

            locationListener = listener

            val fine = checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarse = checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!fine && !coarse) {
                loadOnlineInformation()
                return
            }

            val last = try {
                manager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            }

            if (last != null) {
                useLocation(last)
            }

            manager.requestLocationUpdates(
                provider,
                10000L,
                100f,
                listener,
                Looper.getMainLooper()
            )

            if (last == null) {
                loadOnlineInformation()
            }

        } catch (_: Exception) {

            loadOnlineInformation()
        }
    }

    private fun useLocation(
        location: android.location.Location
    ) {

        latitude = location.latitude
        longitude = location.longitude

        locationLabel =
            "عرض: ${"%.4f".format(Locale.US, latitude)}، طول: ${
                "%.4f".format(Locale.US, longitude)
            }"

        stopLocationUpdates()

        loadOnlineInformation()

        if (currentPage == 0) {
            showHome()
        }
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

    @Deprecated("Deprecated in Android")
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

        if (requestCode == 1001) {

            if (
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                findPhoneLocation()
            } else {
                loadOnlineInformation()
            }
        }
    }

    // تبدیل تاریخ میلادی به تاریخ شمسی

    
// تبدیل تاریخ میلادی به تقویم شمسی
private fun persianCalendar(
    calendar: Calendar
): android.icu.util.Calendar {
    val persian = android.icu.util.Calendar.getInstance(
        Locale.forLanguageTag("fa-IR-u-ca-persian")
    )
    persian.timeInMillis = calendar.timeInMillis
    return persian
}

private fun toJalali(
    year: Int,
    month: Int,
    day: Int
): IntArray {
    val calendar = Calendar.getInstance()

    calendar.set(
        year,
        month - 1,
        day,
        12,
        0,
        0
    )

    val p = persianCalendar(calendar)

    return intArrayOf(
        p.get(android.icu.util.Calendar.YEAR),
        p.get(android.icu.util.Calendar.MONTH) + 1,
        p.get(android.icu.util.Calendar.DAY_OF_MONTH)
    )
}


private fun toGregorian(
    year: Int,
    month: Int,
    day: Int
): IntArray {
    val p = android.icu.util.Calendar.getInstance(
        Locale.forLanguageTag("fa-IR-u-ca-persian")
    )

    p.clear()
    p.set(android.icu.util.Calendar.YEAR, year)
    p.set(android.icu.util.Calendar.MONTH, month - 1)
    p.set(android.icu.util.Calendar.DAY_OF_MONTH, day)

    val g = Calendar.getInstance()
    g.timeInMillis = p.timeInMillis

    return intArrayOf(
        g.get(Calendar.YEAR),
        g.get(Calendar.MONTH) + 1,
        g.get(Calendar.DAY_OF_MONTH)
    )
}


        
            g.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun jalaliDate(
        calendar: Calendar
    ): String {

        val j = toJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        return "${j[0]}/${j[1]} ${j[2]}"
    }

    private fun weekday(
        calendar: Calendar
    ): String {

        return SimpleDateFormat(
            "EEEE",
            Locale("fa", "IR")
        ).format(calendar.time)
    }

    private fun jalaliMonthTitle(
        calendar: Calendar
    ): String {

        val j = toJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val months = listOf(
            "فروردین",
            "اردیبهشت",
            "خرداد",
            "تیر",
            "مرداد",
            "شهریور",
            "مهر",
            "آبان",
            "آذر",
            "دی",
            "بهمن",
            "اسفند"
        )

        return "${months[j[1] - 1]} ${j[0]}"
    }

    private fun firstDayOfJalaliMonth(
        calendar: Calendar
    ): Calendar {

        val p = persianCalendar(calendar)

        p.set(
            android.icu.util.Calendar.DAY_OF_MONTH,
            1
        )

        val result = Calendar.getInstance()

        result.timeInMillis = p.timeInMillis

        return result
    }

    private fun jalaliMonthLength(
        calendar: Calendar
    ): Int {

        val p = persianCalendar(calendar)

        return p.getActualMaximum(
            android.icu.util.Calendar.DAY_OF_MONTH
        )
    }

}
