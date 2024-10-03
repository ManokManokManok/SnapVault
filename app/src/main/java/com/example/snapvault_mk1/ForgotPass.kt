package com.example.snapvault_mk1

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ForgotPass : AppCompatActivity() {
    private var isPopupShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_pass)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val main = findViewById<ConstraintLayout>(R.id.main)
        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels

        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.forgotlogo),
            findViewById(R.id.info),
            findViewById(R.id.email),
            findViewById(R.id.emailicon),
            findViewById(R.id.sendForgotPass),
            findViewById(R.id.changedmind),
            findViewById(R.id.backtostart)

        )

        popup.forEach { it.visibility = View.GONE }

        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        val backtostart = findViewById<Button>(R.id.backtostart)
        backtostart.setOnClickListener {

            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    private fun showPopup(popupViews: List<View>, heightOfScreen: Int) {
        popupViews.forEach { view ->
            view.visibility = View.VISIBLE
            view.translationY = heightOfScreen.toFloat()

            view.animate()
                .translationY(0f)
                .setStartDelay(200)
                .setDuration(500)
                .start()
        }
    }

}