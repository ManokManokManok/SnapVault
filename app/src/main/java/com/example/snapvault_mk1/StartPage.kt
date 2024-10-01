package com.example.snapvault_mk1

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StartPage : AppCompatActivity() {
    private var isPopupShown = false // Flag to check if the popup is shown

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.startpage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels
        val startlogo = findViewById<ImageView>(R.id.startlogo)
        val starttitle = findViewById<ImageView>(R.id.starttitle)
        val main = findViewById<ConstraintLayout>(R.id.startpage)

        // Define the popup views
        val popup = listOf<View>(
            findViewById(R.id.welcomebox),
            findViewById(R.id.welcometext),
            findViewById(R.id.signinbutton),
            findViewById(R.id.signupbuttongreen)
        )

        // Hide the popup views initially
        popup.forEach { it.visibility = View.GONE }

        // Set the video URI (assuming you have a VideoView for this)
        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.startbackground}")

        // Fade-in animation for logo
        val logoAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 1000
            fillAfter = true
        }
        startlogo.startAnimation(logoAnimation)

        // Fade-in animation for title
        val fadeInAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 2000
            fillAfter = true
        }
        starttitle.startAnimation(fadeInAnimation)

        // Click listener to show the popup
        main.setOnClickListener {
            if (!isPopupShown) {
                showPopup(popup, heightOfScreen)
                animateLogoAndTitle()
                isPopupShown = true // Set flag that the popup is shown
            }
        }

        // OnClickListener for the sign-in button
        val signinButton = findViewById<Button>(R.id.signinbutton)
        signinButton.setOnClickListener {
            // Handle the sign-in button click
            val intent = Intent(this, Login::class.java) // Replace with your actual activity
            startActivity(intent)
        }

        val signupbutton = findViewById<Button>(R.id.signupbuttongreen)
        signupbutton.setOnClickListener {
            // Handle the sign-in button click
            val intent = Intent(this, signup::class.java) // Replace with your actual activity
            startActivity(intent)
        }
    }



    // Method to show the popup views with animation
    private fun showPopup(popupViews: List<View>, heightOfScreen: Int) {
        popupViews.forEach { view ->
            view.visibility = View.VISIBLE
            view.translationY = heightOfScreen.toFloat() // Start below the screen
            view.animate()
                .translationY(0f) // Move to original position
                .setDuration(500)
                .start()
        }
    }

    // Method to animate the logo and title upward
    private fun animateLogoAndTitle() {
        val startlogo = findViewById<ImageView>(R.id.startlogo)
        val starttitle = findViewById<ImageView>(R.id.starttitle)

        // Animate the logo upward
        startlogo.animate()
            .translationY(-500f) // Adjust as needed
            .setDuration(500) // Duration of the animation
            .start()

        // Animate the title upward
        starttitle.animate()
            .translationY(-500f) // Adjust as needed
            .setDuration(500) // Duration of the animation
            .start()
    }
}
