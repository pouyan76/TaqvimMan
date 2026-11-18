
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
        "اذان صبح", "اذان ظهر", "اذان عصر",
        "اذان مغرب", "اذان عشا"
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
        minutes(prayer.second) > currentMinutes
    } ?: "اذان صبح"

    val nextPrayer = prayers.first { it.first == upcoming }
    val nextMinutes = minutes(nextPrayer.second)

    val remaining = if (nextMinutes >= 0) {
        val difference =
            if (nextMinutes > currentMinutes)
                nextMinutes - currentMinutes
            else
                24 * 60 - currentMinutes + nextMinutes

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

    header.addView(label("🕌 اوقات شرعی امروز", 17f, white, true))
    header.addView(label("نماز بعدی", 13f, gold))
    header.addView(label(nextPrayer.third, 36f, white))
    header.addView(label(nextPrayer.first, 22f, white, true))
    header.addView(label(fa(nextPrayer.second), 29f, gold, true))
    header.addView(
        label("زمان باقی‌مانده: $remaining", 13f, white)
    )

    prayerBox.addView(header)

    val title = label("برنامه کامل اوقات شرعی", 18f, green, true)
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
                    if (isNext)
                        Color.rgb(255, 244, 218)
                    else
                        Color.rgb(242, 247, 247)
                )
                cornerRadius = dp(17).toFloat()
                if (isNext) {
                    setStroke(dp(2), gold)
                }
            }
            setPadding(dp(7), dp(10), dp(7), dp(10))
            elevation = dp(2).toFloat()
        }

        card.addView(label(prayer.third, 28f))
        card.addView(
            label(
                prayer.first,
                14f,
                if (isNext) green else gray,
                true
            )
        )
        card.addView(
            label(
                fa(prayer.second),
                21f,
                if (isNext) green else green,
                true
            )
        )

        if (isNext) {
            card.addView(label("نماز بعدی", 10f, gold, true))
        }

        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(132)
            columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED,
                1f
            )
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }

        grid.addView(card, params)
    }

    prayerBox.addView(grid)

    prayerBox.addView(
        label(
            "زمان‌ها بر اساس موقعیت مکانی گوشی محاسبه شده‌اند.",
            11f,
            gray
        )
    )
}
