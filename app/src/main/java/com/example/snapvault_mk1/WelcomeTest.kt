package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class WelcomeActivity : AppCompatActivity() {

    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_test)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

        // Retrieve the intent data or SharedPreferences
        val username = intent.getStringExtra("username") ?: sharedPreferences.getString("username", null)
        val id = intent.getIntExtra("id", -1)

        // If the username is available, save it to SharedPreferences
        if (username != null) {
            saveUsername(username)
        }

        fileIcon = findViewById(R.id.folder)
        createIcon = findViewById(R.id.create)
        personIcon = findViewById(R.id.person)

        // Display the username on the WelcomeActivity
        val welcomeMessage = findViewById<TextView>(R.id.welcomeTextView)
        welcomeMessage.text = "Welcome, ${username ?: "Guest"}" // If username is null, display "Guest"

        // Set up click listeners for the icons
        fileIcon.setOnClickListener {
            val intent = Intent(this, Files::class.java)
            startActivity(intent)
        }

        createIcon.setOnClickListener {
            val intent = Intent(this, Createalbum::class.java)
            startActivity(intent)
        }

        personIcon.setOnClickListener {
            val intent = Intent(this, User::class.java)
            startActivity(intent)
        }
    }

    // Function to save username in SharedPreferences
    private fun saveUsername(username: String) {
        val editor = sharedPreferences.edit()
        editor.putString("username", username)
        editor.apply() // Commit the changes to SharedPreferences
    }
}
