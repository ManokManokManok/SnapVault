package com.example.snapvault_mk1

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_test)

        val username = intent.getStringExtra("username") ?: "User"
        val welcomeTextView: TextView = findViewById(R.id.welcomeTextView)

        welcomeTextView.text = "Hello, $username!"
    }
}