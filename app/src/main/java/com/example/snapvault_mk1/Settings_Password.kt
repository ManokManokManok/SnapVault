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

interface ChangePasswordApi {
    @FormUrlEncoded
    @POST("change_password.php") // Ensure this matches your PHP file location
    fun changePassword(
        @Field("current_password") currentPassword: String,
        @Field("new_password") newPassword: String
    ): Call<ResponseBody>
}

class Settings_Password : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_password)

        // Handle window insets for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels
        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.currpass),
            findViewById(R.id.newpass),
            findViewById(R.id.confirmbutton),
            findViewById(R.id.back)
        )

        val backButton = findViewById<Button>(R.id.back)

        backButton.setOnClickListener {
            startActivity(Intent(this, User::class.java))
        }

        // Initially hide all popup views
        popup.forEach { it.visibility = View.GONE }

        // Show popup
        showPopup(popup, heightOfScreen)

        val confirmButton = findViewById<Button>(R.id.confirmbutton)
        val currentPasswordInput = findViewById<EditText>(R.id.currpass)
        val newPasswordInput = findViewById<EditText>(R.id.newpass)

        // Set onClickListener for the confirm button
        confirmButton.setOnClickListener {
            val currentPassword = currentPasswordInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()

            // Change password if the current password is valid
            changePassword(currentPassword, newPassword)
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

    private fun changePassword(currentPassword: String, newPassword: String) {
        // Check if the new password is not empty
        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.11/") // Ensure this matches your local server
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val changePasswordApi = retrofit.create(ChangePasswordApi::class.java)

        // Make the network call to change the password
        changePasswordApi.changePassword(currentPassword, newPassword).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val responseMessage = responseBody.string() // Get the response from the PHP file
                        Toast.makeText(this@Settings_Password, responseMessage, Toast.LENGTH_SHORT).show()
                        if (responseMessage == "Password changed successfully.") {
                            // Go back to Login on successful password change
                            startActivity(Intent(this@Settings_Password, Login::class.java))
                            finish() // Finish this activity
                        }
                    } ?: run {
                        Toast.makeText(this@Settings_Password, "Unexpected error occurred.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Settings_Password, "Failed to change password: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@Settings_Password, "Network failure. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
