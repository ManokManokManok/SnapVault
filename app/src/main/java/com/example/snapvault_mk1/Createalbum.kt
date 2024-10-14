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
        closeUpButton = findViewById(R.id.close_upbutton)


        findViewById<View>(R.id.welcomebox).translationY = 700f
        findViewById<View>(R.id.albumname).translationY = 1000f
        findViewById<View>(R.id.cancelbutton).translationY = 1000f
        findViewById<View>(R.id.close_upbutton).translationY = 700f
        findViewById<View>(R.id.createbutton).translationY = 1000f

        // Set onClickListener for icons
        homeIcon.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }

        fileIcon.setOnClickListener {
            startActivity(Intent(this, Files::class.java))
        }

        createIcon.setOnClickListener {
            startActivity(Intent(this, Createalbum::class.java))
        }

        personIcon.setOnClickListener {
            startActivity(Intent(this, User::class.java))
        }

        closeUpButton.setOnClickListener {
            if (isExpanded) {

                collapseViews()
            } else {

                expandViews()
            }
            isExpanded = !isExpanded
        }
    }

    private fun expandViews() {

        animateView(R.id.welcomebox, 0f)
        animateView(R.id.albumname, 0f)
        animateView(R.id.cancelbutton, 0f)
        animateView(R.id.close_upbutton, 0f)
        animateView(R.id.createbutton, 0f)
    }

    private fun collapseViews() {

        animateView(R.id.welcomebox, 700f)
        animateView(R.id.albumname, 1000f)
        animateView(R.id.cancelbutton, 1000f)
        animateView(R.id.close_upbutton, 700f)
        animateView(R.id.createbutton, 1000f)
    }

    private fun animateView(viewId: Int, translationY: Float) {
        val view = findViewById<View>(viewId)
        ObjectAnimator.ofFloat(view, "translationY", translationY).apply {
            duration = 600
            start()
        }
    }
}
