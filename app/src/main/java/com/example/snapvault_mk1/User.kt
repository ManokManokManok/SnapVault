package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import java.io.File

class User : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var emailsettings: Button
    private lateinit var usernamesettings: Button
    private lateinit var passwordsettings: Button
    private lateinit var logoutbutton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        // Initialize SharedPreferences to retrieve the username
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        homeIcon = findViewById(R.id.home)
        fileIcon = findViewById(R.id.folder)
        personIcon = findViewById(R.id.person)
        emailsettings = findViewById(R.id.emailsettings)
        usernamesettings = findViewById(R.id.usernamesettings)
        passwordsettings = findViewById(R.id.passwordsettings)
        logoutbutton = findViewById(R.id.logoutbutton)

        // Display the username in the TextView
        val welcomeMessage = findViewById<TextView>(R.id.welcomeTextView)
        welcomeMessage.text = "Settings for \n $username!"

        // Set onClickListeners for the icons
        homeIcon.setOnClickListener {
            val intent = Intent(this, WelcomeActivity::class.java)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        fileIcon.setOnClickListener {
            val intent = Intent(this, Files::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        emailsettings.setOnClickListener {
            val intent = Intent(this, Settings_Email::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        usernamesettings.setOnClickListener {
            val intent = Intent(this, Settings_Username::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        passwordsettings.setOnClickListener {
            val intent = Intent(this, Settings_Password::class.java)
            startActivity(intent)
        }

        logoutbutton.setOnClickListener {
            // Clear SharedPreferences
            sharedPreferences.edit().clear().apply()

            // Clear app cache
            clearAppCache()

            // Verify clearing by checking user_id and username
            val clearedUserId = sharedPreferences.getInt("user_id", -1)
            Log.d("Logout", "Cleared user_id: $clearedUserId") // Should be -1

            // Start the StartPage activity and clear the stack
            val intent = Intent(this, StartPage::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        // Create an Intent to navigate back to WelcomeActivity
        val intent = Intent(this, WelcomeActivity::class.java)
        // Clear the current activity and any other activities in the back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Finish the current activity
    }

    private fun clearAppCache() {
        val cacheDir = cacheDir
        val filesDir = filesDir
        val externalCacheDir = externalCacheDir

        // Clear the cache directory
        if (cacheDir.isDirectory) {
            deleteDir(cacheDir)
        }

        // Clear the files directory
        if (filesDir.isDirectory) {
            deleteDir(filesDir)
        }

        // Clear external cache directory if it exists
        if (externalCacheDir != null && externalCacheDir.isDirectory) {
            deleteDir(externalCacheDir)
        }
    }

    // Helper method to delete a directory and its contents
    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    deleteDir(File(dir, child))
                }
            }
        }
        return dir.delete()
    }
}
