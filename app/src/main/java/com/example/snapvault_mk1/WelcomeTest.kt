package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity



class WelcomeActivity : AppCompatActivity() {

    private lateinit var fileIcon: ImageView
    private lateinit var imageIcon: ImageView
    private lateinit var personIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_test)

        // Retrieve the intent data
        val username = intent.getStringExtra("username")
        val id = intent.getIntExtra("id", -1)

        fileIcon = findViewById(R.id.folder)
        imageIcon = findViewById(R.id.image)
        personIcon = findViewById(R.id.person)

        // Display the username on the WelcomeActivity
        val welcomeMessage = findViewById<TextView>(R.id.welcomeTextView)
        welcomeMessage.text = "Welcome, $username!"

        fileIcon.setOnClickListener {
            val intent = Intent(this, Files::class.java)
            startActivity(intent)

        }
        imageIcon.setOnClickListener {
            val intent = Intent(this, Image::class.java)
            startActivity(intent)
        }
        personIcon.setOnClickListener {
            val intent = Intent(this, User::class.java)
            startActivity(intent)
        }
    }
}

