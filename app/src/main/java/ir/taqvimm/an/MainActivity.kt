

package ir.taqvimm.an

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val green = Color.rgb(16, 77, 67)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(28, 40, 28, 28)
        root.setBackgroundColor(Color.rgb(245, 248, 246))

        val title = TextView(this)
        title.text = "🌙 تقویم من"
        title.textSize = 30f
        title.setTextColor(green)
        title.gravity = Gravity.CENTER
        root.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "همراه روزهای خوب زندگی"
        subtitle.textSize = 17f
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 15, 0, 25)
        root.addView(subtitle)

        addMenu(root, "📅 تقویم شمسی",
            "بخش تقویم در حال آماده‌سازی است")

        addMenu(root, "🕌 اوقات شرعی و اذان",
            "بخش اوقات شرعی در حال آماده‌سازی است")

        addMenu(root, "🌤️ هواشناسی",
            "بخش هواشناسی در حال آماده‌سازی است")

        addMenu(root, "🎮 بازی کلمات",
            "بازی ۵۰۰ مرحله‌ای در حال آماده‌سازی است")

        addMenu(root, "▶️ شروع پخش اذان", "") {
            val intent = Intent(
                this,
                AdhanService::class.java
            ).setAction(AdhanService.ACTION_START)

            ContextCompat.startForegroundService(this, intent)
        }

        addMenu(root, "⏹ توقف اذان", "") {
            val intent = Intent(
                this,
                AdhanService::class.java
            ).setAction(AdhanService.ACTION_STOP)

            startService(intent)
        }

        setContentView(root)
    }

    private fun addMenu(
        root: LinearLayout,
        title: String,
        message: String,
        action: (() -> Unit)? = null
    ) {
        val button = Button(this)
        button.text = title
        button.textSize = 16f
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(green)

        button.setOnClickListener {
            if (action != null) {
                action()
            } else {
                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_SHORT
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
