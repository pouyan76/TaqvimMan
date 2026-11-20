
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
    private var prayerData: JSONObject? = null

    private var weatherText = "در حال دریافت اطلاعات هواشناسی..."
    private var currentPage = "home"
    private var secretNumber = Random.nextInt(1, 101)

    // پخش آنلاین اذان
    private var adhanPlayer: MediaPlayer? = null
    private var adhanStatus = "برای پخش اذان دکمه پخش را بزنید"
    private var adhanStatusView: TextView? = null

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

    // هدر مسجد طلایی
    private fun addHeader(body: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(12), dp(10), dp(18))
            background = createBackground(emerald, 22, gold)
        }

        header.addView(
            MosqueHeaderView(),
            LinearLayout.LayoutParams(-1, dp(145))
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

        card.addView(makeText(icon, 30f, gold, true))
        card.addView(makeText(title, 16f, lightGold, true))
        card.addView(makeText(description, 11f, muted))

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

        body.addView(
            makeButton("مشاهده تقویم شمسی") {
                showCalendar()
            },
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

        for (i in 1..31) {
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

    // صفحه اوقات شرعی و پخش اذان
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
            background = createBackground(
                emerald,
                20,
                gold
            )
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

    // شروع پخش آنلاین اذان
    private fun playOnlineAdhan() {

        stopOnlineAdhan()

        updateAdhanStatus("در حال اتصال به فایل صوتی...")

        try {
            val media = MediaPlayer()
            adhanPlayer = media

            media.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_MUSIC
                    )
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
            updateAdhanStatus("اتصال به فایل صوتی ناموفق بود")
        }
    }

    // توقف پخش
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
                        android.os.Looper.getMainLooper()
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

                    if (currentPage == "weather") {
                        showWeather()
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    weatherText = "دریافت هواشناسی ناموفق بود"

                    if (currentPage == "weather") {
                        showWeather()
                    }
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

                    if (currentPage == "prayer") {
                        showPrayer()
                    }
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
        stopOnlineAdhan()
        adhanStatusView = null
        super.onDestroy()
    }

    // رسم مسجد، گنبد، مناره، ماه و ستاره
    private inner class MosqueHeaderView : View(this@MainActivity) {

        private val paint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        )

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f

            paint.style = android.graphics.Paint.Style.FILL

            // پس زمینه
            paint.color = darkGreen

            canvas.drawRoundRect(
                0f, 0f, w, h,
                dp(18).toFloat(),
                dp(18).toFloat(),
                paint
            )

            // ستاره‌ها
            paint.color = gold

            val stars = listOf(
                0.15f to 0.18f,
                0.30f to 0.12f,
                0.70f to 0.15f,
                0.85f to 0.25f,
                0.20f to 0.40f,
                0.78f to 0.42f
            )

            stars.forEach {
                canvas.drawCircle(
                    w * it.first,
                    h * it.second,
                    dp(2).toFloat(),
                    paint
                )
            }

            // ماه
            paint.color = gold

            canvas.drawCircle(
                w * 0.78f,
                h * 0.22f,
                dp(15).toFloat(),
                paint
            )

            paint.color = darkGreen

            canvas.drawCircle(
                w * 0.82f,
                h * 0.17f,
                dp(13).toFloat(),
                paint
            )

            // بدنه مسجد
            paint.color = gold

            canvas.drawRect(
                w * 0.28f,
                h * 0.58f,
                w * 0.72f,
                h * 0.85f,
                paint
            )

            // گنبد اصلی
            val dome = android.graphics.Path().apply {
                moveTo(w * 0.28f, h * 0.60f)
                quadTo(
                    cx,
                    h * 0.08f,
                    w * 0.72f,
                    h * 0.60f
                )
                close()
            }

            canvas.drawPath(dome, paint)

            // گنبد کوچک
            canvas.drawOval(
                w * 0.43f,
                h * 0.40f,
                w * 0.57f,
                h * 0.58f,
                paint
            )

            // مناره چپ
            canvas.drawRect(
                w * 0.19f,
                h * 0.34f,
                w * 0.24f,
                h * 0.85f,
                paint
            )

            canvas.drawCircle(
                w * 0.215f,
                h * 0.32f,
                dp(5).toFloat(),
                paint
            )

            // مناره راست
            canvas.drawRect(
                w * 0.76f,
                h * 0.34f,
                w * 0.81f,
                h * 0.85f,
                paint
            )

            canvas.drawCircle(
                w * 0.785f,
                h * 0.32f,
                dp(5).toFloat(),
                paint
            )

            // در ورودی
            paint.color = darkGreen

            canvas.drawRoundRect(
                w * 0.45f,
                h * 0.65f,
                w * 0.55f,
                h * 0.85f,
                dp(25).toFloat(),
                dp(25).toFloat(),
                paint
            )

            // خط طلایی
            paint.color = gold

            canvas.drawRect(
                w * 0.12f,
                h * 0.88f,
                w * 0.88f,
                h * 0.895f,
                paint
            )
        }
    }
}
