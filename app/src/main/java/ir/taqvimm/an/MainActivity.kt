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
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val green = Color.rgb(14, 65, 57)
    private val greenLight = Color.rgb(35, 111, 91)
    private val gold = Color.rgb(218, 171, 91)
    private val background = Color.rgb(239, 245, 249)
    private val white = Color.WHITE
    private val red = Color.rgb(190, 55, 55)
    private val gray = Color.rgb(110, 120, 130)
    private val sky = Color.rgb(39, 91, 145)

    private lateinit var displayedMonth: Calendar

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private lateinit var liveStatus: TextView
    private lateinit var prayerBox: LinearLayout
    private lateinit var weatherBox: LinearLayout

    private var locationMode = "all"
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

    private val englishDays = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه",
        "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayedMonth = newPersianCalendar()
        displayedMonth.timeInMillis = System.currentTimeMillis()
        showHome()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

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

    private fun shape(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun skyGradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(31, 91, 151),
                Color.rgb(87, 151, 194),
                Color.rgb(172, 202, 218)
            )
        ).apply {
            cornerRadius = dp(24).toFloat()
        }
    }

    private fun greenGradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(green, greenLight)
        ).apply {
            cornerRadius = dp(24).toFloat()
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

        card.addView(text(emoji, 34f))
        card.addView(text(title, 16f, green, true))
        card.addView(text(description, 11f, gray))
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
            LinearLayout.LayoutParams(0, dp(145), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        row.addView(
            second,
            LinearLayout.LayoutParams(0, dp(145), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        root.addView(row, params(5, 5))
    }

    private fun showHome() {
        val (scroll, root) = page()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = greenGradient()
            setPadding(dp(18), dp(22), dp(18), dp(22))
            elevation = dp(4).toFloat()
        }

        header.addView(text("تقویم من", 30f, white, true))
        header.addView(text("همراه هوشمند هر روز شما", 14f, white))
        header.addView(text("POUYAN • TAQVIM MAN", 11f, gold, true))
        root.addView(header, params(0, 12))

        val today = newPersianCalendar()
        today.timeInMillis = System.currentTimeMillis()

        val dateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(white, 20)
            elevation = dp(2).toFloat()
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        dateCard.addView(text("امروز در تقویم شمسی", 13f, gray))
        dateCard.addView(
            text(
                "${fa(today.get(Calendar.DAY_OF_MONTH))} " +
                "${months[today.get(Calendar.MONTH)]} " +
                fa(today.get(Calendar.YEAR)),
                23f,
                green,
                true
            )
        )

        val weekIndex = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7
        dateCard.addView(text(weekdays[weekIndex], 14f, gold, true))
        root.addView(dateCard, params(0, 14))

        root.addView(text("خدمات برنامه", 19f, green, true))

        addFeatureRow(
            root,
            featureCard("📅", "تقویم شمسی", "روزها و ماه‌های سال") {
                showCalendar()
            },
            featureCard("🕌", "اوقات شرعی", "بر اساس مکان گوشی") {
                showLiveInfo("prayer")
            }
        )

        addFeatureRow(
            root,
            featureCard("🌤️", "هواشناسی آنلاین", "پیش‌بینی ۷ روز آینده") {
                showLiveInfo("weather")
            },
            featureCard("🎮", "بازی کلمات", "۵۰۰ مرحله سرگرمی") {
                Toast.makeText(
                    this,
                    "بخش بازی در حال آماده‌سازی است",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = greenGradient()
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        footer.addView(text("اطلاعات آنلاین بر اساس موقعیت شما", 13f, white))
        footer.addView(text("ساخته شده با ❤️ توسط Pouyan", 12f, gold))
        root.addView(footer, params(16, 4))

        setContentView(scroll)
    }

    private fun showLiveInfo(mode: String) {
        locationMode = mode

        val (scroll, root) = page()

        root.addView(
            text(
                if (mode == "weather") "🌤️ هواشناسی ۷ روز آینده"
                else "🕌 اوقات شرعی مکان شما",
                24f,
                green,
                true
            )
        )

        root.addView(
            text("اطلاعات بر اساس موقعیت فعلی گوشی", 13f, gray)
        )

        liveStatus = text(
            "برای دریافت اطلاعات، موقعیت مکانی را تأیید کنید.",
            14f,
            gold
        )
        root.addView(liveStatus, params())

        prayerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(white, 20)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = dp(2).toFloat()
        }

        weatherBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(white, 20)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = dp(2).toFloat()
        }

        if (mode == "weather") {
            root.addView(weatherBox, params())
            root.addView(prayerBox, params())
        } else {
            root.addView(prayerBox, params())
            root.addView(weatherBox, params())
        }

        root.addView(
            featureCard("🔄", "به‌روزرسانی", "دریافت دوباره اطلاعات") {
                beginLocation()
            },
            params(12, 6)
        )

        root.addView(
            featureCard("🏠", "صفحه اصلی", "بازگشت به خانه") {
                showHome()
            },
            params()
        )

        setContentView(scroll)
        beginLocation()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun beginLocation() {
        if (!hasLocationPermission()) {
            liveStatus.text = "لطفاً اجازه دسترسی به مکان را بدهید."

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
            if (hasLocationPermission()) {
                findPhoneLocation()
            } else {
                liveStatus.text =
                    "برای نمایش اطلاعات محلی باید اجازه مکان‌یابی بدهید."
            }
        }
    }

    private fun findPhoneLocation() {
        liveStatus.text = "در حال پیدا کردن موقعیت گوشی..."

        try {
            locationManager =
                getSystemService(LOCATION_SERVICE) as LocationManager

            val manager = locationManager!!
            val providers = manager.getProviders(true)

            if (providers.isEmpty()) {
                liveStatus.text =
                    "مکان‌یابی خاموش است. لطفاً GPS گوشی را روشن کنید."
                return
            }

            var cachedLocation: Location? = null

            for (provider in providers) {
                val cached = try {
                    manager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }

                if (cached != null &&
                    (cachedLocation == null ||
                     cached.accuracy < cachedLocation!!.accuracy)
                ) {
                    cachedLocation = cached
                }
            }

            if (cachedLocation != null &&
                System.currentTimeMillis() - cachedLocation!!.time < 10 * 60 * 1000
            ) {
                loadOnlineInformation(cachedLocation!!)
                return
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    stopLocationUpdates()
                    loadOnlineInformation(location)
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}

                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(
                    provider: String?,
                    status: Int,
                    extras: Bundle?
                ) {}
            }

            locationListener = listener

            for (provider in providers) {
                try {
                    manager.requestLocationUpdates(
                        provider,
                        1000L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    // Permission may have changed.
                }
            }

            handler.postDelayed({
                if (locationListener != null) {
                    stopLocationUpdates()
                    liveStatus.text =
                        "موقعیت پیدا نشد. GPS و اینترنت را بررسی کنید."
                }
            }, 25000)

        } catch (e: Exception) {
            liveStatus.text = "خطا در دریافت موقعیت گوشی."
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationListener?.let {
                locationManager?.removeUpdates(it)
            }
        } catch (e: Exception) {
            // Ignore cleanup errors.
        }
        locationListener = null
    }

    // دریافت آنلاین اوقات شرعی و هواشناسی
    private fun loadOnlineInformation(location: Location) {
        liveStatus.text = "موقعیت پیدا شد؛ اطلاعات آنلاین دریافت می‌شود..."

        val latitude = location.latitude
        val longitude = location.longitude

        executor.execute {
            var prayerData: JSONObject? = null
            var weatherData: JSONObject? = null
            var prayerError = ""
            var weatherError = ""

            try {
                val timestamp = System.currentTimeMillis() / 1000L

                val prayerUrl =
                    "https://api.aladhan.com/v1/timings/$timestamp" +
                    "?latitude=$latitude&longitude=$longitude&method=7"

                val prayerJson = JSONObject(readUrl(prayerUrl))

                prayerData = prayerJson
                    .getJSONObject("data")
                    .getJSONObject("timings")

            } catch (e: Exception) {
                prayerError = "دریافت اوقات شرعی ناموفق بود."
            }

            try {
                val weatherUrl =
                    "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m," +
                    "apparent_temperature,precipitation,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
                    "precipitation_probability_max,wind_speed_10m_max" +
                    "&forecast_days=7&timezone=auto"

                weatherData = JSONObject(readUrl(weatherUrl))

            } catch (e: Exception) {
                weatherError = "دریافت هواشناسی ناموفق بود."
            }

            runOnUiThread {
                prayerBox.removeAllViews()
                weatherBox.removeAllViews()

                if (prayerData != null) {
                    showPrayer(prayerData!!)
                } else {
                    prayerBox.addView(
                        text(prayerError, 14f, red)
                    )
                }

                if (weatherData != null) {
                    displayWeather(weatherData!!)
                } else {
                    weatherBox.addView(
                        text(weatherError, 14f, red)
                    )
                }

                liveStatus.text =
                    "موقعیت فعلی: " +
                    String.format(Locale.US, "%.3f", latitude) +
                    " , " +
                    String.format(Locale.US, "%.3f", longitude)

                liveStatus.setTextColor(green)
            }
        }
    }

    private fun readUrl(address: String): String {
        val connection =
            URL(address).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode !in 200..299) {
                throw Exception("HTTP ${connection.responseCode}")
            }

            return connection.inputStream
                .bufferedReader()
                .use { it.readText() }

        } finally {
            connection.disconnect()
        }
    }

    // نمایش پیشرفته اوقات شرعی
    private fun showPrayer(data: JSONObject) {
        prayerBox.removeAllViews()

        val items = listOf(
            Triple("اذان صبح", "Fajr", "🌅"),
            Triple("طلوع آفتاب", "Sunrise", "☀️"),
            Triple("اذان ظهر", "Dhuhr", "🌞"),
            Triple("اذان عصر", "Asr", "🌤️"),
            Triple("غروب آفتاب", "Sunset", "🌇"),
            Triple("اذان مغرب", "Maghrib", "🌆"),
            Triple("اذان عشا", "Isha", "🌙")
        )

        val prayers = items.map {
            Triple(
                it.first,
                data.optString(it.second, "--").take(5),
                it.third
            )
        }

        val now = java.util.Calendar.getInstance()
        val currentMinutes =
            now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            now.get(java.util.Calendar.MINUTE)

        val prayerKeys = listOf(
            "اذان صبح",
            "اذان ظهر",
            "اذان عصر",
            "اذان مغرب",
            "اذان عشا"
        )

        fun minutes(time: String): Int {
            return try {
                val parts = time.split(":")
                parts[0].toInt() * 60 + parts[1].toInt()
            } catch (e: Exception) {
                -1
            }
        }

        val upcoming = prayerKeys.firstOrNull { name ->
            val prayer = prayers.first { it.first == name }
            val prayerTime = minutes(prayer.second)
            prayerTime >= 0 && prayerTime > currentMinutes
        } ?: "اذان صبح"

        val nextPrayer = prayers.first { it.first == upcoming }
        val nextMinutes = minutes(nextPrayer.second)

        val remaining = if (nextMinutes >= 0) {
            val difference =
                if (nextMinutes > currentMinutes) {
                    nextMinutes - currentMinutes
                } else {
                    24 * 60 - currentMinutes + nextMinutes
                }

            "${fa(difference / 60)} ساعت و ${fa(difference % 60)} دقیقه"
        } else {
            "نامشخص"
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(15, 52, 66),
                    Color.rgb(35, 111, 91)
                )
            ).apply {
                cornerRadius = dp(22).toFloat()
            }
            setPadding(dp(15), dp(18), dp(15), dp(18))
        }

        header.addView(
            text("🕌 اوقات شرعی امروز", 17f, white, true)
        )
        header.addView(
            text("نماز بعدی", 13f, white)
        )
        header.addView(
            text(nextPrayer.third, 36f, white)
        )
        header.addView(
            text(nextPrayer.first, 22f, white, true)
        )
        header.addView(
            text(faTime(nextPrayer.second), 29f, gold, true)
        )
        header.addView(
            text("زمان باقی‌مانده: $remaining", 13f, white)
        )

        prayerBox.addView(header)

        val title = text(
            "برنامه کامل اوقات شرعی",
            18f,
            green,
            true
        )
        title.setPadding(0, dp(16), 0, dp(8))
        prayerBox.addView(title)

        val grid = GridLayout(this).apply {
            columnCount = 2
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        for (prayer in prayers) {
            val isNext = prayer.first == upcoming

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(
                        if (isNext) {
                            Color.rgb(255, 244, 218)
                        } else {
                            Color.rgb(242, 247, 247)
                        }
                    )
                    cornerRadius = dp(17).toFloat()

                    if (isNext) {
                        setStroke(dp(2), gold)
                    }
                }
                setPadding(dp(7), dp(10), dp(7), dp(10))
                elevation = dp(2).toFloat()
            }

            card.addView(text(prayer.third, 28f))

            card.addView(
                text(
                    prayer.first,
                    14f,
                    if (isNext) green else gray,
                    true
                )
            )

            card.addView(
                text(
                    faTime(prayer.second),
                    21f,
                    green,
                    true
                )
            )

            if (isNext) {
                card.addView(
                    text("نماز بعدی", 10f, gold, true)
                )
            }

            val cardParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(132)
                columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED,
                    1f
                )
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }

            grid.addView(card, cardParams)
        }

        prayerBox.addView(grid)

        prayerBox.addView(
            text(
                "زمان‌ها بر اساس موقعیت مکانی گوشی محاسبه شده‌اند.",
                11f,
                gray
            )
        )
    }

    // ساخت کارت هواشناسی و پیش‌بینی ۷ روزه
    private fun displayWeather(json: JSONObject) {
        weatherBox.removeAllViews()

        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")

        val temperature = current.getDouble("temperature_2m")
        val feels = current.getDouble("apparent_temperature")
        val humidity = current.getInt("relative_humidity_2m")
        val wind = current.getDouble("wind_speed_10m")
        val currentCode = current.getInt("weather_code")

        val currentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = skyGradient()
            setPadding(dp(14), dp(18), dp(14), dp(18))
            elevation = dp(3).toFloat()
        }

        currentCard.addView(
            text("وضعیت فعلی هوا", 16f, white, true)
        )

        currentCard.addView(
            text(weatherIcon(currentCode), 48f, white)
        )

        currentCard.addView(
            text(
                "${faTime(String.format(Locale.US, "%.1f", temperature))}°",
                43f,
                white,
                true
            )
        )

        currentCard.addView(
            text(weatherDescription(currentCode), 18f, white, true)
        )

        val maxToday =
            daily.getJSONArray("temperature_2m_max").getDouble(0)
        val minToday =
            daily.getJSONArray("temperature_2m_min").getDouble(0)

        currentCard.addView(
            text(
                "بیشینه ${faTime(String.format(Locale.US, "%.0f", maxToday))}°  " +
                " |  کمینه ${faTime(String.format(Locale.US, "%.0f", minToday))}°",
                15f,
                white
            )
        )

        currentCard.addView(
            text(
                "احساسی ${faTime(String.format(Locale.US, "%.1f", feels))}°" +
                "   •   رطوبت ${fa(humidity)}٪" +
                "   •   باد ${faTime(String.format(Locale.US, "%.0f", wind))}",
                12f,
                white
            )
        )

        weatherBox.addView(currentCard, params(4, 14))

        weatherBox.addView(
            text("پیش‌بینی ۷ روز آینده", 19f, green, true),
            params(8, 4)
        )

        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxTemps = daily.getJSONArray("temperature_2m_max")
        val minTemps = daily.getJSONArray("temperature_2m_min")
        val rain = daily.getJSONArray("precipitation_probability_max")
        val winds = daily.getJSONArray("wind_speed_10m_max")

        for (i in 0 until dates.length()) {
            val date = dates.getString(i)
            val dayName = getDayName(date)

            val code = codes.getInt(i)
            val max = maxTemps.getDouble(i)
            val min = minTemps.getDouble(i)
            val rainChance =
                if (rain.isNull(i)) 0 else rain.getInt(i)
            val windSpeed = winds.getDouble(i)

            val dayCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = shape(
                    if (i == 0) Color.rgb(224, 239, 246) else white,
                    16
                )
                elevation = dp(2).toFloat()
                setPadding(dp(9), dp(12), dp(9), dp(12))
            }

            dayCard.addView(
                text(dayName, 12f, green, true)
            )

            dayCard.addView(
                text(weatherIcon(code), 27f)
            )

            dayCard.addView(
                text(
                    "${faTime(String.format(Locale.US, "%.0f", max))}°",
                    16f,
                    green,
                    true
                )
            )

            dayCard.addView(
                text(
                    "${faTime(String.format(Locale.US, "%.0f", min))}°",
                    13f,
                    gray
                )
            )

            dayCard.addView(
                text("☔ ${fa(rainChance)}٪", 11f, sky)
            )

            dayCard.addView(
                text(
                    "باد ${faTime(String.format(Locale.US, "%.0f", windSpeed))}",
                    10f,
                    gray
                )
            )

            row.addView(
                dayCard,
                LinearLayout.LayoutParams(
                    dp(100),
                    dp(170)
                ).apply {
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }
            )
        }

        horizontal.addView(row)

        weatherBox.addView(horizontal, params(0, 12))

        weatherBox.addView(
            text(
                "دمای فعلی بر اساس موقعیت شما • پیش‌بینی آنلاین",
                11f,
                gray
            )
        )
    }

    private fun getDayName(date: String): String {
        return try {
            val format = java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")

            val parsed = format.parse(date) ?: return date
            val calendar = java.util.Calendar.getInstance()
            calendar.time = parsed

            englishDays[
                calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1
            ]
        } catch (e: Exception) {
            date
        }
    }

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
            0 -> "آسمان صاف"
            1 -> "عمدتاً صاف"
            2 -> "نیمه‌ابری"
            3 -> "ابری"
            45, 48 -> "مه‌آلود"
            51, 53, 55 -> "نم‌نم باران"
            56, 57 -> "باران یخ‌زده"
            61, 63, 65 -> "بارانی"
            66, 67 -> "باران یخی"
            71, 73, 75 -> "برفی"
            77 -> "دانه‌های برف"
            80, 81, 82 -> "رگبار باران"
            85, 86 -> "رگبار برف"
            95 -> "رعد و برق"
            96, 99 -> "رعد و برق و تگرگ"
            else -> "وضعیت نامشخص"
        }
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
        val days =
            displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

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
            featureCard("🏠", "صفحه اصلی", "بازگشت به خانه") {
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
        stopLocationUpdates()
        handler.removeCallbacksAndMessages(null)
        executor.shutdown()
        super.onDestroy()
    }
}
