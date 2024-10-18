package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class Files : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

        // Retrieve the stored username from SharedPreferences
        val username = sharedPreferences.getString("username", null)

        homeIcon = findViewById(R.id.home)
        fileIcon = findViewById(R.id.folder)
        createIcon = findViewById(R.id.create)
        personIcon = findViewById(R.id.person)

        homeIcon.setOnClickListener {
            // Navigate back to WelcomeActivity
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish() // Optional: Finish Files activity
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

    // Override the onBackPressed method
    override fun onBackPressed() {
        // Navigate back to WelcomeActivity
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Optional: Finish the current activity to remove it from the back stack
    }

}
