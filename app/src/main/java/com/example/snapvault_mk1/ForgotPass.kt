package com.example.snapvault_mk1

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ForgotPassApi {
    @FormUrlEncoded
    @POST("forgot_password.php") // Make sure this points to your PHP script
    fun forgotPassword(
        @Field("email") email: String // Sending email to the server
    ): Call<ResponseBody>
}

class ForgotPass : AppCompatActivity() {
    private var isPopupShown = false

    private lateinit var emailEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass)

        emailEditText = findViewById(R.id.email)

        val sendForgotPass: Button = findViewById(R.id.sendForgotPass)
        val backToStart: Button = findViewById(R.id.backtostart)

        val main = findViewById<ConstraintLayout>(R.id.main)
        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels

        // Setup popup views
        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.forgotlogo),
            findViewById(R.id.info),
            findViewById(R.id.email),
            findViewById(R.id.changedmind),
            findViewById(R.id.emailicon),
            sendForgotPass,
            backToStart
        )

        // Hide popup views initially
        popup.forEach { it.visibility = View.GONE }

        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        backToStart.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        sendForgotPass.setOnClickListener {
            val email = emailEditText.text.toString()

            if (email.isNotEmpty()) {
                sendForgotPasswordData(email)
            } else {
                Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
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

    private fun sendForgotPasswordData(email: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/") // Replace with your actual IP address or base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val forgotPassApi = retrofit.create(ForgotPassApi::class.java)

        forgotPassApi.forgotPassword(email).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    // Check the response body for the specific message from your PHP
                    response.body()?.let { responseBody ->
                        // Use the response string from the PHP backend
                        val responseMessage = responseBody.string()
                        Toast.makeText(this@ForgotPass, responseMessage, Toast.LENGTH_SHORT).show()
                    } ?: run {
                        // Fallback if no response message is available
                        Toast.makeText(this@ForgotPass, "Unexpected error occurred.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Retrieve the error message from the PHP backend
                    response.errorBody()?.let { errorBody ->
                        Toast.makeText(this@ForgotPass, errorBody.string(), Toast.LENGTH_SHORT).show()
                    } ?: run {
                        // Fallback if no error message is available
                        Toast.makeText(this@ForgotPass, "Failed to send email: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle failure in the network request
                Toast.makeText(this@ForgotPass, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        Log.d("ForgotPassData", "Email: $email")
    }
}
