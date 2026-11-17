package ir.taqvimm.an

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 42, 28, 28)
            setBackgroundColor(android.graphics.Color.rgb(245, 248, 246))
        }
        root.addView(TextView(this).apply {
            text = "🌙 تقویم من"
            textSize = 28f
            setTextColor(android.graphics.Color.rgb(16, 77, 67))
        })
        root.addView(TextView(this).apply {
            text = "تقویم شمسی • اذان‌گو • هواشناسی • بازی ۵۰۰ مرحله‌ای"
            textSize = 15f
            setPadding(0, 12, 0, 26)
        })
        root.addView(TextView(this).apply {
            text = "نسخه اولیه اندروید آماده توسعه است. برای پخش اذان، فایل صوتی را به سرویس پخش اضافه کنید."
            textSize = 14f
        })
        root.addView(Button(this).apply {
            text = "شروع پخش سرویس اذان"
            setOnClickListener {
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    Intent(this@MainActivity, AdhanService::class.java).setAction(AdhanService.ACTION_START)
                )
            }
        })
        root.addView(Button(this).apply {
            text = "توقف سرویس اذان"
            setOnClickListener {
                startService(Intent(this@MainActivity, AdhanService::class.java).setAction(AdhanService.ACTION_STOP))
            }
        })
        setContentView(root)
    }
}
