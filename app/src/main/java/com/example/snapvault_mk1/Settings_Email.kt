package com.example.snapvault_mk1

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Settings_Email : AppCompatActivity() {
    private var isPopupShown = false

    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_email)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val main = findViewById<ConstraintLayout>(R.id.main)
        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels

        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.confirmbutton),
            findViewById(R.id.email),
            findViewById(R.id.back),
            findViewById(R.id.emailicon),
        )

        popup.forEach { it.visibility = View.GONE }

        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)
        val email = sharedPreferences.getString("email", null)

        val welcomeMessage = findViewById<TextView>(R.id.welcomeTextView)
        welcomeMessage.text = "Settings for \n $username!"

        val currentemail = findViewById<TextView>(R.id.emailtextview)
        currentemail.text = "Current Email: \n $email"


    }

    private fun showPopup(popupViews: List<View>, heightOfScreen: Int) {
        popupViews.forEach { view ->
            view.visibility = View.VISIBLE
            view.translationY = heightOfScreen.toFloat()

            view.animate()
                .translationY(150f)
                .setStartDelay(200)
                .setDuration(500)
                .start()
        }
    }


}