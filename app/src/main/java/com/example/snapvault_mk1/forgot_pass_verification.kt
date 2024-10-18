package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// Define the API interface
interface VerificationApi {
    @FormUrlEncoded
    @POST("verify_code.php") // Ensure this matches your PHP script location
    fun verifyCode(
        @Field("verification_code") verificationCode: String,
        @Field("email") email: String
    ): Call<ResponseBody>
}

// Main activity for password verification
class forgot_pass_verification : AppCompatActivity() {
    private lateinit var verificationCodeEditText: EditText
    private lateinit var emailEditText: EditText // This is your existing EditText for email input
    private lateinit var storedEmail: String // Store the email passed from the previous activity
    private var backPressedTime: Long = 0
    private val backPressedDelay: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass_verification)

        // Retrieve the email passed from the Forgot Password activity
        storedEmail = intent.getStringExtra("email") ?: ""

        verificationCodeEditText = findViewById(R.id.emailver) // Replace with your actual EditText ID for verification code
        emailEditText = findViewById(R.id.email) // Use your existing EditText ID for the email input
        val verifyButton: Button = findViewById(R.id.verifyemail) // Replace with your actual Button ID

        // Set onClickListener for verify button
        verifyButton.setOnClickListener {
            val verificationCode = verificationCodeEditText.text.toString()
            val enteredEmail = emailEditText.text.toString() // Get the email entered by the user

            // Check if the entered email matches the stored email
            if (enteredEmail != storedEmail) {
                Toast.makeText(this, "This is not your email.", Toast.LENGTH_SHORT).show()
            } else if (verificationCode.isEmpty()) { // Check if the verification code is empty
                Toast.makeText(this, "Please enter the verification code.", Toast.LENGTH_SHORT).show()
            } else {
                // Proceed with verification code API call if both checks pass
                verifyCode(verificationCode, storedEmail) // Call your verification function here
            }
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

    // Function to verify the code with the API
    private fun verifyCode(verificationCode: String, email: String) {
        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.11/") // Change this to your actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(VerificationApi::class.java)

        // Make the API call
        api.verifyCode(verificationCode, email).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    // Handle successful response
                    Toast.makeText(this@forgot_pass_verification, "Verification successful!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@forgot_pass_verification, ChangePass::class.java)
                    intent.putExtra("email", storedEmail)
                    startActivity(intent) // Start the ChangePass activity
                    finish() // Optionally finish this activity if you don't want to return here

                } else {
                    // Handle failure response
                    Toast.makeText(this@forgot_pass_verification, "Verification failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle failure scenario
                Toast.makeText(this@forgot_pass_verification, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
