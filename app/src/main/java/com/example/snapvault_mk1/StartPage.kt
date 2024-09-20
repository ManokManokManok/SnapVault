package com.example.snapvault_mk1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StartPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val startlogo = findViewById<ImageView>(R.id.startlogo)
        val starttitle = findViewById<TextView>(R.id.starttitle)
        val bgVid = findViewById<VideoView>(R.id.background)

        startlogo.setOnClickListener{
            val Intent = Intent (this,MainActivity::class.java)
            startActivity(Intent)
        }

        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.startbackground}")

        //HINDI PA ITO FINAL HA LALAGYAN PA NG MAGANDANG VID

        bgVid.setVideoURI(videoUri)
        bgVid.start()

        bgVid.setOnCompletionListener {
            bgVid.start()
        }

        val animation = AlphaAnimation(0f, 1f).apply {}
        animation.duration = 0
        animation.fillAfter = true

        startlogo.startAnimation(animation)

        val fadeInAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 3000
            fillAfter = true
        }
        starttitle.startAnimation(fadeInAnimation)


    }
}