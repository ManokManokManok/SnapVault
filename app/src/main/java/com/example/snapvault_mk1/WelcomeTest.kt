package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import android.util.Log

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

        // Retrieve the intent data
        val username = intent.getStringExtra("username") ?: sharedPreferences.getString("username", null)
        val id = intent.getIntExtra("id", -1)
        val email = intent.getStringExtra("email") ?: sharedPreferences.getString("email", null)
        val password = intent.getStringExtra("password") ?: sharedPreferences.getString("password", null)

        // Save username and email if they are available
        username?.let { saveUsername(it) }
        email?.let { saveEmail(it) }
        password?.let { savePassword(it) }

        // Set up the UI components
        fileIcon = findViewById(R.id.folder)
        createIcon = findViewById(R.id.create)
        personIcon = findViewById(R.id.person)

        // Display the welcome message
        val welcomeMessage = findViewById<TextView>(R.id.welcomeTextView)
        welcomeMessage.text = "Welcome, ${username ?: "Guest"}"

        // Click listeners for the icons
        fileIcon.setOnClickListener {
            startActivity(Intent(this, Files::class.java))
        }
        createIcon.setOnClickListener {
            startActivity(Intent(this, Createalbum::class.java))
        }
        personIcon.setOnClickListener {
            startActivity(Intent(this, User::class.java))
        }

        // Logging for debugging
        username?.let { Log.d("SharedPreferences", "Username stored: $it") }
        email?.let { Log.d("SharedPreferences", "Email stored: $it") }
        password?.let { Log.d("SharedPreferences", "Password Stored: $it") }
    }

    // Save username in SharedPreferences
    private fun saveUsername(username: String) {
        val editor = sharedPreferences.edit()
        editor.putString("username", username)
        editor.apply()
        Log.d("SharedPreferences", "Username saved: $username")
        Toast.makeText(this, "Username saved: $username", Toast.LENGTH_SHORT).show()
    }

    // Save email in SharedPreferences
    private fun saveEmail(email: String) {
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.apply()
        Log.d("SharedPreferences", "Email saved: $email")
        Toast.makeText(this, "Email saved: $email", Toast.LENGTH_SHORT).show() // Added Toast message
    }

    private fun savePassword(password: String) {
        val editor = sharedPreferences.edit()
        editor.putString("password", password)
        editor.apply()
        Log.d("SharedPreferences", "Password saved: $password")
        Toast.makeText(this, "Password saved: $password", Toast.LENGTH_SHORT).show() // Added Toast message
    }
}
