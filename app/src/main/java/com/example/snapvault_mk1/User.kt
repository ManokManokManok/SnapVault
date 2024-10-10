package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class User : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        // Initialize SharedPreferences to retrieve the username
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        homeIcon = findViewById(R.id.home)
        fileIcon = findViewById(R.id.folder)
        createIcon = findViewById(R.id.image)
        personIcon = findViewById(R.id.person)

        // Display the username in the TextView
        val welcomeMessage = findViewById<TextView>(R.id.welcomeTextView)
        welcomeMessage.text = "Welcome, $username!"

        // Set onClickListeners for the icons
        homeIcon.setOnClickListener {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        fileIcon.setOnClickListener {
            val intent = Intent(this, Files::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        createIcon.setOnClickListener {
            val intent = Intent(this, Createalbum::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        personIcon.setOnClickListener {
            val intent = Intent(this, User::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }
    }
}
