
package ir.taqvimm.an

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val green = Color.rgb(16, 77, 67)
    private val background = Color.rgb(245, 248, 246)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 35, 28, 28)
            setBackgroundColor(background)
        }

        val title = TextView(this).apply {
            text = "🌙 تقویم من"
            textSize = 30f
            setTextColor(green)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "همراه روزهای خوب زندگی"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 25)
        }
        root.addView(subtitle)

        val date = SimpleDateFormat(
            "yyyy/MM/dd",
            Locale.getDefault()
        ).format(Date())

        val dateText = TextView(this).apply {
            text = "امروز: $date"
            textSize = 19f
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 25)
        }
        root.addView(dateText)

        addMenu(root, "📅 تقویم شمسی", "بخش تقویم در حال آماده‌سازی است")
        addMenu(root, "🕌 اوقات شرعی و اذان", "بخش اوقات شرعی در حال آماده‌سازی است")
        addMenu(root, "🌤️ هواشناسی", "بخش هواشناسی در حال آماده‌سازی است")
        addMenu(root, "🎮 بازی کلمات", "بازی ۵۰۰ مرحله‌ای در حال آماده‌سازی است")

        val footer = TextView(this).apply {
            text = "تقویم من | نسخه ۱"
            textSize = 13f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }
        root.addView(footer)

        setContentView(root)
    }

    private fun addMenu(
        root: LinearLayout,
        title: String,
        message: String
    ) {
        val button = Button(this).apply {
            text = title
            textSize = 17f
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            65
        )
        params.setMargins(0, 8, 0, 8)
        root.addView(button, params)
    }
}
package ir.taqvimm.an

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val green = Color.rgb(16, 77, 67)
    private val background = Color.rgb(245, 248, 246)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 35, 28, 28)
            setBackgroundColor(background)
        }

        val title = TextView(this).apply {
            text = "🌙 تقویم من"
            textSize = 30f
            setTextColor(green)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "همراه روزهای خوب زندگی"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 25)
        }
        root.addView(subtitle)

        val date = SimpleDateFormat(
            "yyyy/MM/dd",
            Locale.getDefault()
        ).format(Date())

        val dateText = TextView(this).apply {
            text = "امروز: $date"
            textSize = 19f
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 25)
        }
        root.addView(dateText)

        addMenu(root, "📅 تقویم شمسی", "بخش تقویم در حال آماده‌سازی است")
        addMenu(root, "🕌 اوقات شرعی و اذان", "بخش اوقات شرعی در حال آماده‌سازی است")
        addMenu(root, "🌤️ هواشناسی", "بخش هواشناسی در حال آماده‌سازی است")
        addMenu(root, "🎮 بازی کلمات", "بازی ۵۰۰ مرحله‌ای در حال آماده‌سازی است")

        val footer = TextView(this).apply {
            text = "تقویم من | نسخه ۱"
            textSize = 13f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }
        root.addView(footer)

        setContentView(root)
    }

    private fun addMenu(
        root: LinearLayout,
        title: String,
        message: String
    ) {
        val button = Button(this).apply {
            text = title
            textSize = 17f
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            65
        )
        params.setMargins(0, 8, 0, 8)
        root.addView(button, params)
    }
}
