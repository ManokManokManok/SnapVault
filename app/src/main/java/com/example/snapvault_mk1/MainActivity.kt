package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        val loginbutton: Button = findViewById(R.id.loginbutton)
        val signupbutton: Button = findViewById(R.id.signupbutton)

        loginbutton.setOnClickListener{
            val Intent = Intent (this,Login::class.java)
            startActivity(Intent)
        }

        signupbutton.setOnClickListener{
            val Intent = Intent (this,signup::class.java)
            startActivity(Intent)
        }

    }
}