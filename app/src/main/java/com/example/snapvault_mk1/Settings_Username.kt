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

interface ChangeUsernameApi {
    @FormUrlEncoded
    @POST("change_username.php") // Ensure this matches your PHP file location
    fun changeUsername(
        @Field("current_username") currentUsername: String,
        @Field("new_username") newUsername: String
    ): Call<ResponseBody>
}

class Settings_Username : AppCompatActivity() {
    private var isPopupShown = false
    private lateinit var sharedPreferences: SharedPreferences
    private var accountUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_username)

        // Handle window insets for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        accountUsername = sharedPreferences.getString("username", null)

        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels
        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.curruser),
            findViewById(R.id.newuser),
            findViewById(R.id.confirmbutton),
            findViewById(R.id.back)
        )

        val backbutton = findViewById<Button>(R.id.back)

        backbutton.setOnClickListener {
            startActivity(Intent(this, User::class.java))
        }

        // Initially hide all popup views
        popup.forEach { it.visibility = View.GONE }

        // Show popup if it hasn't been shown yet
        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        val confirmButton = findViewById<Button>(R.id.confirmbutton)
        val currentUsernameInput = findViewById<EditText>(R.id.curruser)
        val newUsernameInput = findViewById<EditText>(R.id.newuser)

        // Set onClickListener for the confirm button
        confirmButton.setOnClickListener {
            val currentUsername = currentUsernameInput.text.toString().trim()
            val newUsername = newUsernameInput.text.toString().trim()

            // Validate that the current username matches the account username
            if (currentUsername != accountUsername) {
                Toast.makeText(this, "Current username does not match your account.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Change username if the current username is valid
            changeUsername(currentUsername, newUsername)
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

    private fun changeUsername(currentUsername: String, newUsername: String) {
        // Check if the new username is not empty
        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/") // Ensure this matches your local server
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val changeUsernameApi = retrofit.create(ChangeUsernameApi::class.java)

        // Make the network call to change the username
        changeUsernameApi.changeUsername(currentUsername, newUsername).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val responseMessage = responseBody.string() // Get the response from the PHP file
                        Toast.makeText(this@Settings_Username, responseMessage, Toast.LENGTH_SHORT).show()

                        // Check if the response indicates a successful username change
                        if (responseMessage.contains("success", ignoreCase = true)) { // Adjust this condition based on your PHP response
                            // Update SharedPreferences with the new username
                            sharedPreferences.edit().putString("username", newUsername).apply()

                            // Navigate back to Login screen
                            startActivity(Intent(this@Settings_Username, Login::class.java)) // Replace with actual LoginActivity name
                            finish() // Optional: finish this activity so it doesn't remain in the back stack
                        }
                    } ?: run {
                        Toast.makeText(this@Settings_Username, "Unexpected error occurred.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Settings_Username, "Failed to change username: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@Settings_Username, "Network failure. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
