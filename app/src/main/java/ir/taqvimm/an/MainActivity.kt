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
import org.json.JSONArray
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

    // اطلاعات هواشناسی
    private var weatherText = "در حال دریافت اطلاعات هواشناسی..."
    private var weatherForecast: JSONObject? = null

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

    // توقف پخش اذان
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

    // آیکون وضعیت هوا
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

    // توضیح وضعیت هوا
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
    private fun showWeather() {
    currentPage = "weather"
    body.removeAllViews()

    addHeader(body, "هواشناسی ۷ روزه")

    val forecast = weatherForecast

    if (forecast == null) {
        addCard(body, "وضعیت هوا") {
            addText(this, weatherText, 16f, Color.WHITE)
            addButton(this, "دریافت دوباره اطلاعات") {
                requestLocation()
            }
        }
        return
    }

    val current = forecast.optJSONObject("current")

    if (current != null) {
        val temperature = current.optDouble("temperature_2m", 0.0)
        val code = current.optInt("weather_code", 0)

        addCard(body, "وضعیت فعلی هوا") {
            addText(
                this,
                weatherIcon(code),
                42f,
                Color.WHITE
            )

            addText(
                this,
                "${temperature.toInt()} درجه",
                30f,
                Color.WHITE
            )

            addText(
                this,
                weatherDescription(code),
                17f,
                Color.WHITE
            )
        }
    }

    val daily = forecast.optJSONObject("daily")

    if (daily == null) {
        addText(body, "اطلاعات پیش‌بینی در دسترس نیست", 16f, Color.WHITE)
        return
    }

    val dates = daily.optJSONArray("time")
    val codes = daily.optJSONArray("weather_code")
    val maxTemps = daily.optJSONArray("temperature_2m_max")
    val minTemps = daily.optJSONArray("temperature_2m_min")
    val rainAmounts = daily.optJSONArray("precipitation_sum")
    val rainChances = daily.optJSONArray("precipitation_probability_max")

    addText(
        body,
        "پیش‌بینی وضعیت هوا در ۷ روز آینده",
        19f,
        Color.WHITE
    )

    if (dates == null) return

    for (i in 0 until dates.length()) {
        val date = dates.optString(i, "")
        val code = codes?.optInt(i, 0) ?: 0
        val maxTemp = maxTemps?.optDouble(i, 0.0) ?: 0.0
        val minTemp = minTemps?.optDouble(i, 0.0) ?: 0.0
        val rain = rainAmounts?.optDouble(i, 0.0) ?: 0.0
        val chance = rainChances?.optInt(i, 0) ?: 0

        val dayName = getPersianWeekday(date)
        val persianDate = getPersianDateFromIso(date)

        addCard(body, "$dayName - $persianDate") {
            addText(
                this,
                weatherIcon(code),
                36f,
                Color.WHITE
            )

            addText(
                this,
                weatherDescription(code),
                16f,
                Color.WHITE
            )

            addText(
                this,
                "بیشترین دما: ${maxTemp.toInt()} درجه",
                15f,
                Color.WHITE
            )

            addText(
                this,
                "کمترین دما: ${minTemp.toInt()} درجه",
                15f,
                Color.WHITE
            )

            addText(
                this,
                "میزان بارش: ${"%.1f".format(Locale.US, rain)} میلی‌متر",
                15f,
                Color.WHITE
            )

            addText(
                this,
                "احتمال بارندگی: $chance درصد",
                15f,
                if (chance >= 50) Color.YELLOW else Color.WHITE
            )

            if (rain > 0.0 || chance > 0) {
                addText(
                    this,
                    "🌧️ احتمال بارندگی در این روز وجود دارد",
                    14f,
                    Color.YELLOW
                )
            } else {
                addText(
                    this,
                    "☀️ بارندگی قابل توجهی پیش‌بینی نشده است",
                    14f,
                    Color.WHITE
                )
            }
        }
    }

    addButton(body, "به‌روزرسانی هواشناسی") {
        requestLocation()
    }
}

private fun getPersianDateFromIso(isoDate: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val date = format.parse(isoDate) ?: return isoDate
        val calendar = Calendar.getInstance()
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
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val date = format.parse(isoDate) ?: return "روز"
        val dayFormat = SimpleDateFormat("EEEE", Locale("fa"))
        dayFormat.format(date)
    } catch (e: Exception) {
        "روز"
    }
}

private fun showGame() {
    currentPage = "game"
    body.removeAllViews()

    addHeader(body, "بازی حدس عدد")

    addCard(body, "بازی سرگرمی") {
        addText(
            this,
            "یک عدد بین ۱ تا ۱۰۰ انتخاب شده است.",
            17f,
            Color.WHITE
        )

        val input = EditText(this).apply {
            hint = "عدد خود را وارد کنید"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#245B45"))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.parseColor("#D4AF37"))
            }
        }

        addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
        )

        val result = TextView(this).apply {
            text = "منتظر حدس شما هستم"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        addView(result)

        addButton(this, "بررسی عدد") {
            val guess = input.text.toString().toIntOrNull()

            if (guess == null || guess !in 1..100) {
                result.text = "لطفاً عددی بین ۱ تا ۱۰۰ وارد کنید"
            } else {
                val target = gameNumber

                when {
                    guess < target -> result.text = "عدد بزرگ‌تری حدس بزنید"
                    guess > target -> result.text = "عدد کوچک‌تری حدس بزنید"
                    else -> {
                        result.text = "🎉 آفرین! عدد درست را پیدا کردید"
                    }
                }
            }
        }

        addButton(this, "شروع بازی جدید") {
            gameNumber = Random.nextInt(1, 101)
            input.setText("")
            result.text = "عدد جدید انتخاب شد؛ حدس بزنید"
        }
    }
}
private fun requestLocation() {
    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
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
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                loadWeather(location.latitude, location.longitude)
                loadPrayerTimes(location.latitude, location.longitude)
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(
                provider: String?,
                status: Int,
                extras: Bundle?
            ) {}
        }

        var requested = false

        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                0f,
                listener
            )
            requested = true
        }

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                listener
            )
            requested = true
        }

        if (!requested) {
            Toast.makeText(
                this,
                "لطفاً مکان‌یابی گوشی را روشن کنید",
                Toast.LENGTH_LONG
            ).show()
        }

    } catch (e: Exception) {
        Toast.makeText(this, "خطا در دریافت موقعیت", Toast.LENGTH_SHORT).show()
    }
}

override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (requestCode == 100) {
        if (grantResults.any {
                it == PackageManager.PERMISSION_GRANTED
            }) {
            requestLocation()
        } else {
            Toast.makeText(
                this,
                "اجازه دسترسی به موقعیت مکانی داده نشد",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

private fun loadWeather(lat: Double, lon: Double) {
    thread {
        var connection: HttpURLConnection? = null

        try {
            val address =
                "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat" +
                "&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max" +
                "&forecast_days=7" +
                "&timezone=auto"

            connection = URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"

            val response = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            val json = JSONObject(response)
            val current = json.getJSONObject("current")

            val temperature = current.optDouble("temperature_2m", 0.0)
            val code = current.optInt("weather_code", 0)

            weatherForecast = json
            weatherText =
                "دمای فعلی: ${temperature.toInt()} درجه - ${weatherDescription(code)}"

            runOnUiThread {
                if (currentPage == "weather") {
                    showWeather()
                } else if (currentPage == "home") {
                    showHome()
                }
            }

        } catch (e: Exception) {
            weatherForecast = null
            weatherText = "دریافت اطلاعات هواشناسی ناموفق بود"

            runOnUiThread {
                if (currentPage == "weather") {
                    showWeather()
                } else if (currentPage == "home") {
                    showHome()
                }
            }

        } finally {
            connection?.disconnect()
        }
    }
}

private fun loadPrayerTimes(lat: Double, lon: Double) {
    thread {
        var connection: HttpURLConnection? = null

        try {
            val address =
                "https://api.aladhan.com/v1/timings" +
                "?latitude=$lat" +
                "&longitude=$lon" +
                "&method=7"

            connection = URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"

            val response = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            val json = JSONObject(response)
            val data = json.getJSONObject("data")
            val timings = data.getJSONObject("timings")

            prayerData = timings

            runOnUiThread {
                if (currentPage == "prayer" || currentPage == "home") {
                    if (currentPage == "prayer") {
                        showPrayer()
                    } else {
                        showHome()
                    }
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

        } finally {
            connection?.disconnect()
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
        jm = 1 + (days / 31)
        jd = 1 + (days % 31)
    } else {
        jm = 7 + ((days - 186) / 30)
        jd = 1 + ((days - 186) % 30)
    }

    return "$jy/${
        jm.toString().padStart(2, '0')
    }/${
        jd.toString().padStart(2, '0')
    }"
}

override fun onDestroy() {
    try {
        mediaPlayer?.release()
        mediaPlayer = null
    } catch (e: Exception) {
    }

    super.onDestroy()
}

inner class MosqueHeaderView(
    context: android.content.Context
) : View(context) {

    private val paint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    )

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        paint.shader = android.graphics.LinearGradient(
            0f,
            0f,
            w,
            h,
            Color.parseColor("#064B38"),
            Color.parseColor("#10291F"),
            android.graphics.Shader.TileMode.CLAMP
        )

        canvas.drawRoundRect(
            0f,
            0f,
            w,
            h,
            dp(22).toFloat(),
            dp(22).toFloat(),
            paint
        )

        paint.shader = null
        paint.color = Color.parseColor("#D4AF37")
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = dp(2).toFloat()

        val centerX = w / 2f
        val baseY = h * 0.72f

        canvas.drawArc(
            centerX - w * 0.23f,
            h * 0.12f,
            centerX + w * 0.23f,
            h * 0.78f,
            180f,
            180f,
            false,
            paint
        )

        canvas.drawLine(
            centerX - w * 0.23f,
            baseY,
            centerX + w * 0.23f,
            baseY,
            paint
        )

        paint.style = android.graphics.Paint.Style.FILL

        canvas.drawCircle(
            centerX,
            h * 0.16f,
            dp(4).toFloat(),
            paint
        )

        canvas.drawLine(
            centerX,
            h * 0.16f,
            centerX,
            h * 0.05f,
            paint
        )

        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.textSize = dp(20).toFloat()
        paint.typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )

        canvas.drawText(
            "تقویم من",
            centerX,
            h * 0.93f,
            paint
        )

        paint.textAlign = android.graphics.Paint.Align.LEFT
    }
}
