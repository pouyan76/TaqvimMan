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

class MainActivity : Activity() {

    private val green = Color.rgb(13, 57, 53)
    private val green2 = Color.rgb(28, 105, 88)
    private val mint = Color.rgb(225, 241, 235)
    private val gold = Color.rgb(218, 171, 91)
    private val bg = Color.rgb(241, 245, 247)
    private val white = Color.WHITE
    private val gray = Color.rgb(111, 124, 130)
    private val blue = Color.rgb(42, 104, 157)
    private val red = Color.rgb(190, 55, 55)

    private lateinit var displayedMonth: Calendar
    private var selectedDay = 1
    private var currentMode = "prayer"

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private lateinit var liveStatus: TextView
    private lateinit var prayerBox: LinearLayout
    private lateinit var weatherBox: LinearLayout

    private val permissionCode = 501

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
        selectedDay = displayedMonth.get(Calendar.DAY_OF_MONTH)

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

    private fun faText(value: String): String {
        return value.map {
            if (it in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[it - '0'] else it
        }.joinToString("")
    }

    private fun rounded(
        color: Int,
        radius: Int = 20
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun gradient(
        first: Int,
        second: Int
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(first, second)
        ).apply {
            cornerRadius = dp(24).toFloat()
        }
    }

    private fun label(
        value: String,
        size: Float,
        color: Int = green,
        bold: Boolean = false,
        gravityValue: Int = Gravity.CENTER
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = gravityValue
            textDirection = View.TEXT_DIRECTION_RTL
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(16), dp(16), dp(25))
        }

        scroll.addView(root)
        return Pair(scroll, root)
    }

    private fun margins(
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
            setPadding(dp(8), dp(12), dp(8), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }

            addView(label(icon, 30f))
            addView(label(title, 15f, green, true))
            addView(label(description, 10f, gray))

            val line = View(this@MainActivity).apply {
                background = rounded(color, 4)
            }

            addView(
                line,
                LinearLayout.LayoutParams(dp(35), dp(4)).apply {
                    topMargin = dp(8)
                }
            )
        }
    }

    private fun addTwoCards(
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
            LinearLayout.LayoutParams(0, dp(140), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        row.addView(
            second,
            LinearLayout.LayoutParams(0, dp(140), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        root.addView(row, margins(5, 5))
    }

    // صفحه اصلی

    private fun showHome() {
        val (scroll, root) = page()

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradient(green, green2)
            elevation = dp(4).toFloat()
            setPadding(dp(18), dp(22), dp(18), dp(22))
        }

        hero.addView(label("تقویم من", 29f, white, true))
        hero.addView(
            label("مدیریت هوشمند روزهای شما", 13f, white)
        )
        hero.addView(
            label("POUYAN • TAQVIM MAN", 10f, gold, true)
        )

        root.addView(hero, margins(0, 13))

        val today = newPersianCalendar().apply {
            timeInMillis = System.currentTimeMillis()
        }

        val dateCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(white, 22)
            elevation = dp(2).toFloat()
            setPadding(dp(13), dp(12), dp(13), dp(12))
        }

        val dateInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        dateInfo.addView(
            label(
                "امروز • ${weekdays[today.get(Calendar.DAY_OF_WEEK) % 7]}",
                12f, gray, true, Gravity.RIGHT
            )
        )

        dateInfo.addView(
            label(
                "${fa(today.get(Calendar.DAY_OF_MONTH))} " +
                    "${months[today.get(Calendar.MONTH)]} " +
                    fa(today.get(Calendar.YEAR)),
                21f, green, true, Gravity.RIGHT
            )
        )

        dateCard.addView(
            label("📆", 30f),
            LinearLayout.LayoutParams(dp(55), dp(60))
        )

        dateCard.addView(
            dateInfo,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        root.addView(dateCard, margins(0, 18))

        root.addView(
            label("امکانات برنامه", 19f, green, true, Gravity.RIGHT)
        )

        addTwoCards(
            root,
            actionCard(
                "📅", "تقویم شمسی",
                "نمای ماهانه و انتخاب روز",
                green
            ) {
                showCalendar()
            },
            actionCard(
                "🕌", "اوقات شرعی",
                "اذان بر اساس موقعیت گوشی",
                gold
            ) {
                showLiveInfo("prayer")
            }
        )

        addTwoCards(
            root,
            actionCard(
                "🌤️", "هواشناسی",
                "پیش‌بینی هفت روز آینده",
                blue
            ) {
                showLiveInfo("weather")
            },
            actionCard(
                "🎮", "بازی کلمات",
                "بخش بازی در حال ساخت",
                red
            ) {
                Toast.makeText(
                    this,
                    "بازی هنوز پیاده‌سازی نشده است.",
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
                15f, green, true, Gravity.RIGHT
            )
        )

        info.addView(
            label(
                "برای دریافت اطلاعات آنلاین، اینترنت و موقعیت مکانی را فعال کنید.",
                12f, gray, false, Gravity.RIGHT
            )
        )

        root.addView(info, margins(14, 8))

        root.addView(
            label("ساخته شده با ❤️ توسط Pouyan", 11f, gray)
        )

        setContentView(scroll)
    }

    // صفحه اوقات شرعی و هواشناسی

    private fun showLiveInfo(mode: String) {
        currentMode = mode

        val (scroll, root) = page()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient(green, green2)
            setPadding(dp(15), dp(16), dp(15), dp(16))
        }

        header.addView(
            label(
                if (mode == "prayer") "🕌 اوقات شرعی" else "🌤️ هواشناسی",
                23f, white, true
            )
        )

        header.addView(
            label("اطلاعات آنلاین بر اساس موقعیت گوشی", 12f, white)
        )

        root.addView(header, margins(0, 12))

        val back = label(
            "→ بازگشت به صفحه اصلی",
            14f, green2, true, Gravity.RIGHT
        )

        back.setOnClickListener { showHome() }
        root.addView(back)

        liveStatus = label(
            "در حال بررسی موقعیت مکانی...",
            13f, gray, true
        )

        root.addView(liveStatus, margins(10, 10))

        prayerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        weatherBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (mode == "prayer") {
            root.addView(prayerBox)
        } else {
            root.addView(weatherBox)
        }

        val refresh = actionCard(
            "🔄", "به‌روزرسانی",
            "دریافت دوباره اطلاعات", blue
        ) {
            requestLocation()
        }

        root.addView(refresh, margins(12, 8))

        setContentView(scroll)
        requestLocation()
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
                permissionCode
            )
            liveStatus.text = "لطفاً اجازه مکان‌یابی را تأیید کنید."
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
            val granted = grantResults.any {
                it == PackageManager.PERMISSION_GRANTED
            }

            if (granted) {
                findPhoneLocation()
            } else {
                liveStatus.text = "اجازه دسترسی به مکان داده نشد."
            }
        }
    }

    private fun findPhoneLocation() {
        try {
            locationManager =
                getSystemService(LOCATION_SERVICE) as LocationManager

            val manager = locationManager!!
            val providers = manager.getProviders(true)

            if (providers.isEmpty()) {
                liveStatus.text = "لطفاً GPS گوشی را روشن کنید."
                return
            }

            var last: Location? = null

            for (provider in providers) {
                val location = try {
                    manager.getLastKnownLocation(provider)
                } catch (_: Exception) {
                    null
                }

                if (
                    location != null &&
                    (last == null || location.time > last.time)
                ) {
                    last = location
                }
            }

            if (last != null) {
                loadOnlineInformation(
                    last.latitude,
                    last.longitude
                )
                return
            }

            liveStatus.text = "در حال دریافت موقعیت گوشی..."

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    stopLocationUpdates()

                    loadOnlineInformation(
                        location.latitude,
                        location.longitude
                    )
                }
            }

            for (provider in providers) {
                try {
                    manager.requestLocationUpdates(
                        provider,
                        1000L,
                        10f,
                        locationListener!!,
                        Looper.getMainLooper()
                    )
                } catch (_: Exception) {
                }
            }

            handler.postDelayed({
                if (locationListener != null) {
                    stopLocationUpdates()
                    liveStatus.text =
                        "موقعیت دریافت نشد. GPS را بررسی کنید."
                }
            }, 25000L)

        } catch (_: Exception) {
            liveStatus.text = "خطا در دریافت موقعیت مکانی."
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

    // دریافت اطلاعات آنلاین

    private fun readUrl(address: String): String {
        val connection =
            URL(address).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode !in 200..299) {
                throw Exception("Network error")
            }

            return connection.inputStream
                .bufferedReader()
                .use { it.readText() }

        } finally {
            connection.disconnect()
        }
    }

    private fun loadOnlineInformation(
        latitude: Double,
        longitude: Double
    ) {
        runOnUiThread {
            liveStatus.text = "موقعیت پیدا شد؛ دریافت اطلاعات آنلاین..."
        }

        executor.execute {
            try {
                if (currentMode == "prayer") {
                    val timestamp = System.currentTimeMillis() / 1000L

                    val address =
                        "https://api.aladhan.com/v1/timings/$timestamp" +
                            "?latitude=$latitude&longitude=$longitude&method=7"

                    val json = JSONObject(readUrl(address))
                    val data = json.getJSONObject("data")
                    val timings = data.getJSONObject("timings")

                    val hijri = data.getJSONObject("date")
                        .getJSONObject("hijri")

                    val hijriDate =
                        "${hijri.optString("day")} " +
                            "${hijri.getJSONObject("month").optString("ar")} " +
                            hijri.optString("year")

                    runOnUiThread {
                        displayPrayer(timings, hijriDate)
                    }

                } else {
                    val address =
                        "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$latitude&longitude=$longitude" +
                            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                            "&forecast_days=7&timezone=auto"

                    val json = JSONObject(readUrl(address))

                    runOnUiThread {
                        displayWeather(json)
                    }
                }

            } catch (_: Exception) {
                runOnUiThread {
                    liveStatus.text =
                        "دریافت اطلاعات ناموفق بود. اینترنت را بررسی کنید."
                }
            }
        }
    }

    // نمایش اوقات شرعی

    private fun displayPrayer(
        timings: JSONObject,
        hijriDate: String
    ) {
        prayerBox.removeAllViews()

        liveStatus.text = "اوقات شرعی دریافت شد"

        val items = listOf(
            Triple("اذان صبح", "Fajr", "🌅"),
            Triple("طلوع آفتاب", "Sunrise", "☀️"),
            Triple("اذان ظهر", "Dhuhr", "🌞"),
            Triple("اذان عصر", "Asr", "🌤️"),
            Triple("غروب آفتاب", "Sunset", "🌇"),
            Triple("اذان مغرب", "Maghrib", "🌆"),
            Triple("اذان عشا", "Isha", "🌙")
        )

        val prayerTimes = items.map {
            Triple(
                it.first,
                timings.optString(it.second, "--").take(5),
                it.third
            )
        }

        val now = java.util.Calendar.getInstance()
        val currentMinutes =
            now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                now.get(java.util.Calendar.MINUTE)

        fun toMinutes(value: String): Int {
            return try {
                val parts = value.split(":")
                parts[0].toInt() * 60 + parts[1].toInt()
            } catch (_: Exception) {
                -1
            }
        }

        val prayerNames = listOf(
            "اذان صبح", "اذان ظهر",
            "اذان عصر", "اذان مغرب", "اذان عشا"
        )

        val upcoming = prayerNames.firstOrNull { name ->
            val item = prayerTimes.first { it.first == name }
            toMinutes(item.second) > currentMinutes
        } ?: "اذان صبح"

        val next = prayerTimes.first { it.first == upcoming }
        val nextMinutes = toMinutes(next.second)

        val remaining = if (nextMinutes >= 0) {
            val difference = if (nextMinutes > currentMinutes) {
                nextMinutes - currentMinutes
            } else {
                1440 - currentMinutes + nextMinutes
            }

            "${fa(difference / 60)} ساعت و ${fa(difference % 60)} دقیقه"
        } else {
            "نامشخص"
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradient(green, green2)
            setPadding(dp(15), dp(18), dp(15), dp(18))
        }

        header.addView(label("نماز بعدی", 14f, white))
        header.addView(label(next.third, 35f, white))
        header.addView(label(next.first, 21f, white, true))
        header.addView(
            label(faText(next.second), 29f, gold, true)
        )
        header.addView(
            label("زمان باقی‌مانده: $remaining", 12f, white)
        )

        prayerBox.addView(header, margins(5, 10))

        prayerBox.addView(
            label(
                "تاریخ قمری: $hijriDate",
                12f, gray, false, Gravity.RIGHT
            )
        )

        prayerBox.addView(
            label(
                "برنامه کامل اوقات شرعی",
                18f, green, true, Gravity.RIGHT
            )
        )

        val grid = GridLayout(this).apply {
            columnCount = 2
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        for (item in prayerTimes) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(
                    if (item.first == upcoming) {
                        Color.rgb(255, 244, 218)
                    } else {
                        white
                    },
                    17
                )
                setPadding(dp(7), dp(10), dp(7), dp(10))
                elevation = dp(2).toFloat()
            }

            card.addView(label(item.third, 27f))
            card.addView(label(item.first, 14f, green, true))
            card.addView(
                label(faText(item.second), 20f, green2, true)
            )

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(120)
                columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED, 1f
                )
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }

            grid.addView(card, params)
        }

        prayerBox.addView(grid)
    }

    // نمایش هواشناسی هفت روزه

    private fun weatherIcon(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1, 2 -> "🌤️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            in 51..67 -> "🌧️"
            in 71..77 -> "❄️"
            in 80..82 -> "🌦️"
            in 95..99 -> "⛈️"
            else -> "🌤️"
        }
    }

    private fun weatherDescription(code: Int): String {
        return when (code) {
            0 -> "آفتابی"
            1, 2 -> "نیمه ابری"
            3 -> "ابری"
            45, 48 -> "مه‌آلود"
            in 51..67 -> "بارانی"
            in 71..77 -> "برفی"
            in 80..82 -> "رگبار"
            in 95..99 -> "طوفانی"
            else -> "متغیر"
        }
    }

    private fun displayWeather(json: JSONObject) {
        weatherBox.removeAllViews()

        liveStatus.text = "هواشناسی دریافت شد"

        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")

        val temperature = current.optDouble("temperature_2m", 0.0)
        val humidity = current.optInt("relative_humidity_2m", 0)
        val wind = current.optDouble("wind_speed_10m", 0.0)
        val code = current.optInt("weather_code", 0)

        val currentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradient(blue, Color.rgb(100, 165, 205))
            setPadding(dp(15), dp(18), dp(15), dp(18))
            elevation = dp(3).toFloat()
        }

        currentCard.addView(
            label("وضعیت فعلی هوا", 17f, white, true)
        )

        currentCard.addView(
            label(weatherIcon(code), 48f, white)
        )

        currentCard.addView(
            label(
                "${faText(String.format(Locale.US, "%.1f", temperature))}°",
                40f, white, true
            )
        )

        currentCard.addView(
            label(weatherDescription(code), 17f, white, true)
        )

        currentCard.addView(
            label(
                "رطوبت: ${fa(humidity)}٪   باد: ${fa(wind.toInt())} کیلومتر بر ساعت",
                12f, white
            )
        )

        weatherBox.addView(currentCard, margins(5, 12))

        weatherBox.addView(
            label(
                "پیش‌بینی هفت روز آینده",
                19f, green, true, Gravity.RIGHT
            )
        )

        val dates = daily.getJSONArray("time")
        val maximum = daily.getJSONArray("temperature_2m_max")
        val minimum = daily.getJSONArray("temperature_2m_min")
        val codes = daily.getJSONArray("weather_code")
        val rain = daily.getJSONArray(
            "precipitation_probability_max"
        )

        for (i in 0 until dates.length()) {
            val date = dates.getString(i)
            val shortDate = if (date.length >= 10) {
                date.substring(5, 10)
            } else {
                date
            }

            val dayCode = codes.getInt(i)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(white, 16)
                elevation = dp(1).toFloat()
                setPadding(dp(7), dp(10), dp(7), dp(10))
            }

            row.addView(
                label(
                    if (i == 0) "امروز" else faText(shortDate),
                    12f, green, true
                ),
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            row.addView(
                label(weatherIcon(dayCode), 24f),
                LinearLayout.LayoutParams(dp(45), -2)
            )

            row.addView(
                label(
                    "${fa(maximum.getInt(i))}° / ${fa(minimum.getInt(i))}°",
                    13f, green2, true
                ),
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            row.addView(
                label(
                    "💧${fa(rain.getInt(i))}٪",
                    11f, blue
                ),
                LinearLayout.LayoutParams(dp(58), -2)
            )

            weatherBox.addView(row, margins(4, 5))
        }
    }

    // تقویم شمسی

    private fun showCalendar() {
        val (scroll, root) = page()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient(green, green2)
            setPadding(dp(15), dp(16), dp(15), dp(16))
        }

        header.addView(
            label("📅 تقویم شمسی", 23f, white, true)
        )

        header.addView(
            label("تقویم ماهانه فارسی", 12f, white)
        )

        root.addView(header, margins(0, 12))

        val back = label(
            "→ بازگشت به صفحه اصلی",
            14f, green2, true, Gravity.RIGHT
        )

        back.setOnClickListener { showHome() }
        root.addView(back)

        val monthBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(white, 18)
            setPadding(dp(5), dp(7), dp(5), dp(7))
        }

        val previous = label("❮", 20f, green, true)
        previous.setOnClickListener {
            displayedMonth.add(Calendar.MONTH, -1)
            selectedDay = 1
            showCalendar()
        }

        val next = label("❯", 20f, green, true)
        next.setOnClickListener {
            displayedMonth.add(Calendar.MONTH, 1)
            selectedDay = 1
            showCalendar()
        }

        val title = label(
            "${months[displayedMonth.get(Calendar.MONTH)]} " +
                fa(displayedMonth.get(Calendar.YEAR)),
            18f, green, true
        )

        monthBar.addView(
            previous,
            LinearLayout.LayoutParams(dp(45), dp(45))
        )

        monthBar.addView(
            title,
            LinearLayout.LayoutParams(0, dp(45), 1f)
        )

        monthBar.addView(
            next,
            LinearLayout.LayoutParams(dp(45), dp(45))
        )

        root.addView(monthBar, margins(8, 12))

        val grid = GridLayout(this).apply {
            columnCount = 7
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(white, 18)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        for (day in weekdays) {
            grid.addView(
                label(day.take(1), 12f, green2, true),
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(38)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED, 1f
                    )
                }
            )
        }

        val first = newPersianCalendar().apply {
            timeInMillis = displayedMonth.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val offset = first.get(Calendar.DAY_OF_WEEK) % 7
        val totalDays =
            first.getActualMaximum(Calendar.DAY_OF_MONTH)

        repeat(offset) {
            grid.addView(
                View(this),
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(46)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED, 1f
                    )
                }
            )
        }

        val today = newPersianCalendar().apply {
            timeInMillis = System.currentTimeMillis()
        }

        for (day in 1..totalDays) {
            val isToday =
                day == today.get(Calendar.DAY_OF_MONTH) &&
                    displayedMonth.get(Calendar.MONTH) ==
                    today.get(Calendar.MONTH) &&
                    displayedMonth.get(Calendar.YEAR) ==
                    today.get(Calendar.YEAR)

            val isSelected = day == selectedDay

            val cell = TextView(this).apply {
                text = fa(day)
                textSize = 14f
                gravity = Gravity.CENTER

                setTextColor(
                    if (isSelected || isToday) white else green
                )

                background = rounded(
                    when {
                        isSelected -> green2
                        isToday -> gold
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
                        GridLayout.UNDEFINED, 1f
                    )
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
            )
        }

        root.addView(grid, margins(5, 12))

        val selectedInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(mint, 18)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        selectedInfo.addView(
            label(
                "روز انتخاب‌شده",
                13f, gray, true, Gravity.RIGHT
            )
        )

        selectedInfo.addView(
            label(
                "${fa(selectedDay)} " +
                    "${months[displayedMonth.get(Calendar.MONTH)]} " +
                    fa(displayedMonth.get(Calendar.YEAR)),
                20f, green, true, Gravity.RIGHT
            )
        )

        root.addView(selectedInfo, margins(5, 10))

        setContentView(scroll)
    }

    override fun onDestroy() {
        stopLocationUpdates()
        executor.shutdownNow()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
