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
import androidx.constraintlayout.widget.ConstraintLayout
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

interface VerificationApi {
    @FormUrlEncoded
    @POST("verify_code.php") // Ensure this matches your PHP script location
    fun verifyCode(
        @Field("verification_code") verificationCode: String,
        @Field("email") email: String
    ): Call<ResponseBody>
}

class forgot_pass_verification : AppCompatActivity() {
    private var isPopupShown = false
    private lateinit var verificationCodeEditText: EditText
    private lateinit var email: String // Store the email passed from the previous activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_pass_verification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve the email passed from the Forgot Password activity
        email = intent.getStringExtra("email") ?: ""

        verificationCodeEditText = findViewById(R.id.emailver) // Replace with your actual EditText ID
        val verifyButton: Button = findViewById(R.id.verifyemail) // Replace with your actual Button ID

        val main = findViewById<ConstraintLayout>(R.id.main)
        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels

        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.forgotlogo),
            findViewById(R.id.info),
            findViewById(R.id.email),
            findViewById(R.id.emailicon),
            findViewById(R.id.emailver),
            findViewById(R.id.vericon),
            findViewById(R.id.verifyemail),
            findViewById(R.id.resend)
        )

        popup.forEach { it.visibility = View.GONE }

        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        verifyButton.setOnClickListener {
            val verificationCode = verificationCodeEditText.text.toString()
            if (verificationCode.isNotEmpty()) {
                verifyCode(verificationCode, email)
            } else {
                Toast.makeText(this, "Please enter the verification code.", Toast.LENGTH_SHORT).show()
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

    private fun verifyCode(verificationCode: String, email: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/") // Replace with your actual IP address or base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val verificationApi = retrofit.create(VerificationApi::class.java)

        verificationApi.verifyCode(verificationCode, email).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val responseMessage = responseBody.string()
                        Toast.makeText(this@forgot_pass_verification, responseMessage, Toast.LENGTH_LONG).show()

                        if (responseMessage.contains("Verification successful", ignoreCase = true)) {
                            val intent = Intent(this@forgot_pass_verification, ChangePass::class.java)
                            intent.putExtra("email", email) // Pass email to the next activity if needed
                            startActivity(intent)
                            finish() // Optional: Finish this activity to remove it from the back stack
                        }
                    // Show response as a Toast
                    } ?: run {
                        Toast.makeText(this@forgot_pass_verification, "Unexpected error occurred.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@forgot_pass_verification, "Failed to verify code: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@forgot_pass_verification, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
