package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Files : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

        val username = intent.getStringExtra("username")
        val id = intent.getIntExtra("id", -1)

        homeIcon = findViewById(R.id.home)
        fileIcon = findViewById(R.id.folder)
        createIcon = findViewById(R.id.image)
        personIcon = findViewById(R.id.person)

        val welcomeMessage = findViewById<TextView>(R.id.welcomeTextView)
        welcomeMessage.text = "Welcome, $username!"

        homeIcon.setOnClickListener {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        }

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
}