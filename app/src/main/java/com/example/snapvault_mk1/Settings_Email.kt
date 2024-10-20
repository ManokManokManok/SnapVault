package com.example.snapvault_mk1

import android.content.Intent
import android.content.SharedPreferences
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

// Updated API interface to include password field
interface ChangeEmailApi {
    @FormUrlEncoded
    @POST("change_email.php") // Ensure this matches your API endpoint
    fun changeEmail(
        @Field("current_email") currentEmail: String,
        @Field("new_email") newEmail: String,
        @Field("current_password") password: String // Add password field for verification
    ): Call<ResponseBody>
}

class Settings_Email : AppCompatActivity() {
    private var isPopupShown = false
    private lateinit var sharedPreferences: SharedPreferences
    private var accountEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_email)

        // Edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        accountEmail = sharedPreferences.getString("email", null)

        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels
        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.curremail),
            findViewById(R.id.newemail),
            findViewById(R.id.password), // Added password input field
            findViewById(R.id.emailicon),
            findViewById(R.id.passicon),
            findViewById(R.id.confirmbutton),
            findViewById(R.id.back)
        )

        val backbutton = findViewById<Button>(R.id.back)

        backbutton.setOnClickListener {
            startActivity(Intent(this, User::class.java))
        }

        // Hide all popup views initially
        popup.forEach { it.visibility = View.GONE }

        // Show the popup
        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        val confirmButton = findViewById<Button>(R.id.confirmbutton)
        val currentEmailInput = findViewById<EditText>(R.id.curremail)
        val newEmailInput = findViewById<EditText>(R.id.newemail)
        val passwordInput = findViewById<EditText>(R.id.password) // Password input field

        // Set click listener for the confirm button
        confirmButton.setOnClickListener {
            val currentEmail = currentEmailInput.text.toString().trim()
            val newEmail = newEmailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim() // Get password input

            // Check if the current email matches the account email
            if (currentEmail != accountEmail) {
                Toast.makeText(this, "Current email does not match your account.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Change email if everything is valid
            changeEmail(currentEmail, newEmail, password)
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

    // Updated function to handle the password as well
    private fun changeEmail(currentEmail: String, newEmail: String, password: String) {
        // Check if the new email and password fields are not empty
        if (newEmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Retrofit instance
        val retrofit = Retrofit.Builder()

        val changeEmailApi = ApiClient.getRetrofitInstance().create(ChangeEmailApi::class.java)

        // Make network call to change the email and validate the password
        changeEmailApi.changeEmail(currentEmail, newEmail, password).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val responseMessage = responseBody.string() // Get response from the server
                        Toast.makeText(this@Settings_Email, responseMessage, Toast.LENGTH_SHORT).show()

                        // Check if the email change was successful
                        if (responseMessage.contains("Email changed successfully", ignoreCase = true)) {
                            // Navigate back to the login page
                            startActivity(Intent(this@Settings_Email, Login::class.java))
                            finish()
                        }
                    } ?: run {
                        Toast.makeText(this@Settings_Email, "Unexpected error occurred.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Settings_Email, "Failed to change email: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@Settings_Email, "Network failure. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
