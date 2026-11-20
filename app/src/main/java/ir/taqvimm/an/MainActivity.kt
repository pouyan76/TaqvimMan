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
import android.view.ViewGroup
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
import kotlin.concurrent.thread
import kotlin.random.Random

class MainActivity : Activity() {

    private val emerald = Color.rgb(5, 55, 43)
    private val darkGreen = Color.rgb(3, 35, 28)
    private val gold = Color.rgb(225, 184, 92)
    private val lightGold = Color.rgb(248, 222, 160)
    private val white = Color.WHITE
    private val muted = Color.rgb(205, 213, 204)

    private lateinit var root: LinearLayout
    private lateinit var content: FrameLayout

    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null

    private val handler = Handler(Looper.getMainLooper())

    private var prayerData: JSONObject? = null
    private var weatherText = "در حال دریافت اطلاعات هواشناسی..."

    private var currentPage = "home"
    private var secretNumber = Random.nextInt(1, 101)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkGreen
        window.navigationBarColor = darkGreen
        window.decorView.systemUiVisibility = 0

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
            layoutParams = LinearLayout.LayoutParams(
                -1, 0, 1f
            )
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
                layoutParams = LinearLayout.LayoutParams(
                    0, dp(55), 1f
                )
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

    private fun showPage(view: View) {
        content.removeAllViews()
        content.addView(
            view,
            FrameLayout.LayoutParams(-1, -1)
        )
    }

    private fun createScroll(): LinearLayout {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(
                body,
                LinearLayout.LayoutParams(-1, -2)
            )
        }

        content.removeAllViews()
        content.addView(
            scroll,
            FrameLayout.LayoutParams(-1, -1)
        )

        return body
    }

    private fun addHeader(body: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(22), dp(12), dp(22))
            background = createBackground(emerald, 22, gold)
        }

        header.addView(
            makeText("☪", 34f, gold, true)
        )

        header.addView(
            makeText("تقویم من", 27f, lightGold, true)
        )

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
        val text = makeText(
            title,
            18f,
            lightGold,
            true
        )

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
            background = createBackground(
                emerald,
                20,
                gold
            )
            setOnClickListener { action() }
        }

        card.addView(
            makeText(icon, 30f, gold, true)
        )

        card.addView(
            makeText(title, 16f, lightGold, true)
        )

        card.addView(
            makeText(description, 11f, muted)
        )

        val params = LinearLayout.LayoutParams(
            0,
            dp(125),
            1f
        )

        params.setMargins(dp(5), dp(5), dp(5), dp(5))

        body.addView(card, params)
    }
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
            background = createBackground(
                emerald,
                20,
                gold
            )
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
        ) { showPrayer() }

        addCard(
            row1,
            "اذان بعدی",
            "زمان نماز بعدی",
            "🔔"
        ) { showPrayer() }

        body.addView(row1)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        addCard(
            row2,
            "هواشناسی",
            "وضعیت آب و هوا",
            "☀"
        ) { showWeather() }

        addCard(
            row2,
            "بازی",
            "بازی حدس عدد",
            "🎮"
        ) { showGame() }

        body.addView(row2)

        addSectionTitle(body, "امکانات تقویم")

        val calendarButton = makeButton("مشاهده تقویم شمسی") {
            showCalendar()
        }

        body.addView(
            calendarButton,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(8)
            }
        )
    }

    private fun showCalendar() {
        currentPage = "calendar"
        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "تقویم شمسی")

        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        body.addView(
            makeText(
                getPersianDate(year, month, day),
                23f,
                lightGold,
                true
            ),
            LinearLayout.LayoutParams(-1, dp(65))
        )

        val names = arrayOf(
            "شنبه", "یکشنبه", "دوشنبه",
            "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
        )

        val grid = GridLayout(this).apply {
            columnCount = 7
            background = createBackground(emerald, 15, gold)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        names.forEach {
            val text = makeText(
                it.take(1),
                13f,
                gold,
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

        val daysInMonth = 31

        for (i in 1..daysInMonth) {
            val text = makeText(
                i.toString(),
                14f,
                if (i == day) darkGreen else white,
                i == day
            )

            if (i == day) {
                text.background = createBackground(gold, 10)
            }

            grid.addView(
                text,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(42)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
            )
        }

        body.addView(
            grid,
            LinearLayout.LayoutParams(-1, -2)
        )
    }

    private fun showPrayer() {
        currentPage = "prayer"
        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "اوقات شرعی")

        val prayers = listOf(
            "اذان صبح" to "05:00",
            "طلوع آفتاب" to "06:25",
            "اذان ظهر" to "12:10",
            "اذان عصر" to "15:30",
            "غروب آفتاب" to "17:45",
            "اذان مغرب" to "18:05",
            "اذان عشا" to "19:00"
        )

        prayers.forEach { prayer ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background = createBackground(
                    emerald,
                    15,
                    gold
                )
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
                LinearLayout.LayoutParams(dp(100), dp(48))
            )

            body.addView(
                card,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(9)
                }
            )
        }

        body.addView(
            makeText(
                "برای دریافت زمان دقیق، موقعیت مکانی و اینترنت را فعال کنید.",
                12f,
                muted
            )
        )
    }
        private fun showWeather() {
        currentPage = "weather"
        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "هواشناسی")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(25), dp(20), dp(25))
            background = createBackground(emerald, 20, gold)
        }

        card.addView(makeText("☀", 48f, gold))
        card.addView(makeText("وضعیت آب و هوا", 21f, lightGold, true))
        card.addView(makeText(weatherText, 15f, white))

        body.addView(
            card,
            LinearLayout.LayoutParams(-1, dp(190))
        )

        body.addView(
            makeButton("به‌روزرسانی هواشناسی") {
                requestLocation()
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(15)
            }
        )
    }

    private fun showGame() {
        currentPage = "game"
        val body = createScroll()

        addHeader(body)
        addSectionTitle(body, "بازی حدس عدد")

        body.addView(
            makeText(
                "یک عدد بین ۱ تا ۱۰۰ حدس بزن!",
                17f,
                lightGold,
                true
            ),
            LinearLayout.LayoutParams(-1, dp(60))
        )

        val input = EditText(this).apply {
            hint = "عدد مورد نظر"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(white)
            setHintTextColor(muted)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            background = createBackground(emerald, 15, gold)
        }

        body.addView(
            input,
            LinearLayout.LayoutParams(-1, dp(55)).apply {
                bottomMargin = dp(12)
            }
        )

        val result = makeText(
            "منتظر حدس تو هستم!",
            16f,
            lightGold,
            true
        )

        body.addView(
            makeButton("بررسی عدد") {
                val guess = input.text.toString().toIntOrNull()

                if (guess == null || guess !in 1..100) {
                    result.text = "یک عدد بین ۱ تا ۱۰۰ وارد کن"
                } else {
                    when {
                        guess == secretNumber -> {
                            result.text = "آفرین! درست حدس زدی 🎉"
                        }
                        guess < secretNumber -> {
                            result.text = "عدد بزرگ‌تری انتخاب کن"
                        }
                        else -> {
                            result.text = "عدد کوچک‌تری انتخاب کن"
                        }
                    }
                }
            },
            LinearLayout.LayoutParams(-1, dp(52))
        )

        body.addView(
            result,
            LinearLayout.LayoutParams(-1, dp(65))
        )

        body.addView(
            makeButton("شروع بازی جدید") {
                secretNumber = Random.nextInt(1, 101)
                input.text.clear()
                result.text = "عدد جدید انتخاب شد!"
            },
            LinearLayout.LayoutParams(-1, dp(52))
        )
    }

    private fun requestLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
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

        try {
            locationManager =
                getSystemService(LOCATION_SERVICE) as LocationManager

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )

            for (provider in providers) {
                if (locationManager?.isProviderEnabled(provider) == true) {
                    locationManager?.requestSingleUpdate(
                        provider,
                        object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                currentLocation = location
                                loadWeather(
                                    location.latitude,
                                    location.longitude
                                )
                                loadPrayerTimes(
                                    location.latitude,
                                    location.longitude
                                )
                            }
                        },
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "دریافت موقعیت امکان‌پذیر نیست",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadWeather(lat: Double, lon: Double) {
        thread {
            try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val data = connection.inputStream.bufferedReader().use {
                    it.readText()
                }

                val json = JSONObject(data)
                val current = json.getJSONObject("current")
                val temperature = current.getDouble("temperature_2m")

                runOnUiThread {
                    weatherText = "دمای فعلی: $temperature درجه"
                    if (currentPage == "weather") showWeather()
                }

                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    weatherText = "دریافت هواشناسی ناموفق بود"
                    if (currentPage == "weather") showWeather()
                }
            }
        }
    }

    private fun loadPrayerTimes(lat: Double, lon: Double) {
        thread {
            try {
                val date = SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.US
                ).format(Date())

                val url = URL(
                    "https://api.aladhan.com/v1/timings/$date" +
                        "?latitude=$lat&longitude=$lon&method=3"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val data = connection.inputStream.bufferedReader().use {
                    it.readText()
                }

                val json = JSONObject(data)
                val timings = json.getJSONObject("data")
                    .getJSONObject("timings")

                runOnUiThread {
                    prayerData = timings
                }

                connection.disconnect()
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

    private fun getPersianDate(
        gy: Int,
        gm: Int,
        gd: Int
    ): String {
        val gDays = intArrayOf(
            0, 31, 59, 90, 120, 151,
            181, 212, 243, 273, 304, 334
        )

        var gy2 = gy
        if (gm > 2) gy2++

        var days = 355666 +
            365 * gy +
            (gy2 + 3) / 4 -
            (gy2 + 99) / 100 +
            (gy2 + 399) / 400 +
            gd + gDays[gm - 1]

        var jy = -1595 + 33 * (days / 12053).toInt()
        days %= 12053

        jy += 4 * (days / 1461).toInt()
        days %= 1461

        if (days > 365) {
            jy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }

        val jm: Int
        val jd: Int

        if (days < 186) {
            jm = 1 + (days / 31).toInt()
            jd = 1 + (days % 31).toInt()
        } else {
            jm = 7 + ((days - 186) / 30).toInt()
            jd = 1 + ((days - 186) % 30).toInt()
        }

        return "$jd / $jm / $jy"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        try {
            locationManager?.removeUpdates { }
        } catch (_: Exception) {
        }
    }
}
