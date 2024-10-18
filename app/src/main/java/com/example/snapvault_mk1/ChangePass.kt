package com.example.snapvault_mk1

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ChangePassApi {
    @FormUrlEncoded
    @POST("password_change.php") // Ensure this matches your PHP file location
    fun changePassword(
        @Field("email") email: String,
        @Field("new_password") newPassword: String
    ): Call<ResponseBody>
}

class ChangePass : AppCompatActivity() {
    private var isPopupShown = false
    private lateinit var storedEmail: String // Store the passed email
    private var backPressedTime: Long = 0
    private val backPressedDelay: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_pass)

        // Retrieve the stored email passed from the previous activity
        storedEmail = intent.getStringExtra("email") ?: ""

        // Handle window insets for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels
        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.password),
            findViewById(R.id.passicon),
            findViewById(R.id.sendNewPass),
            findViewById(R.id.email),
            findViewById(R.id.emailicon)
        )

        // Initially hide all popup views
        popup.forEach { it.visibility = View.GONE }

        // Show popup if it hasn't been shown yet
        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        val completeButton = findViewById<Button>(R.id.sendNewPass)
        val emailInput = findViewById<EditText>(R.id.email)
        val newPasswordInput = findViewById<EditText>(R.id.password)

        // Set onClickListener for the complete button
        completeButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()

            // Check if the email input matches the stored email
            if (email != storedEmail) {
                Toast.makeText(this, "This is not your email.", Toast.LENGTH_SHORT).show()
            } else {
                changePassword(email, newPassword) // Proceed with the API call if emails match
            }
        }
    }

    private fun showPopup(popupViews: List<View>, heightOfScreen: Int) {
        popupViews.forEach { view ->
            view.visibility = View.VISIBLE
            view.translationY = heightOfScreen.toFloat()

            view.animate()
                .translationY(0f)
                .setStartDelay(200)
                .setDuration(500)
                .start()
        }
    }

    override fun onBackPressed() {
        if (backPressedTime + backPressedDelay > System.currentTimeMillis()) {
            // Navigate back to another activity instead of closing the app
            val intent = Intent(this, Login::class.java) // Replace with your target activity
            startActivity(intent)
            finish() // Optional: finish the current activity if you want to remove it from the back stack
            return
        } else {
            // Show a toast message for the first back press
            Toast.makeText(this, "Press back again to go back to Sign in", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    private fun changePassword(email: String, newPassword: String) {
        // Check if the email and new password are not empty
        if (email.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/") // Ensure this matches your local server
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val changePassApi = retrofit.create(ChangePassApi::class.java)

        // Make the network call to change the password
        changePassApi.changePassword(email, newPassword).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val responseMessage = responseBody.string()
                        Toast.makeText(this@ChangePass, responseMessage, Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@ChangePass, Login::class.java)
                        startActivity(intent)
                        finish()

                    } ?: run {
                        Toast.makeText(this@ChangePass, "Unexpected error occurred.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ChangePass, "Failed to change password: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ChangePass, "Network failure. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
