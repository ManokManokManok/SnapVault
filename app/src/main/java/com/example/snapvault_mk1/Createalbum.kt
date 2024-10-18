package com.example.snapvault_mk1

import android.animation.ObjectAnimator
import android.content.Intent
import android.view.View
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Createalbum : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var closeUpButton: Button
    private var isExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createalbum)

        homeIcon = findViewById(R.id.home)
        fileIcon = findViewById(R.id.folder)
        createIcon = findViewById(R.id.create)
        personIcon = findViewById(R.id.person)

        // Set onClickListener for icons
        homeIcon.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }

        fileIcon.setOnClickListener {
            startActivity(Intent(this, Files::class.java))
        }

        personIcon.setOnClickListener {
            startActivity(Intent(this, User::class.java))
        }

    }


    private fun animateView(viewId: Int, translationY: Float) {
        val view = findViewById<View>(viewId)
        ObjectAnimator.ofFloat(view, "translationY", translationY).apply {
            duration = 600
            start()
        }
    }

    override fun onBackPressed() {
        // Navigate back to WelcomeActivity
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Optional: Finish the current activity to remove it from the back stack
    }
}
