package com.example.snapvault_mk1

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.MotionEvent
import android.widget.EditText
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SignupService {
    @FormUrlEncoded
    @POST("signup.php") // Make sure this points to your PHP script
    fun signup(
        @Field("email") email: String,
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<ResponseBody>
}

class signup : AppCompatActivity() {
    private var isPopupShown = false

    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        emailEditText = findViewById(R.id.email)
        usernameEditText = findViewById(R.id.username)

        val signinButton: Button = findViewById(R.id.signin)
        val signupButton: Button = findViewById(R.id.signupButton)
        passwordEditText = findViewById(R.id.password)

        val main = findViewById<ConstraintLayout>(R.id.main)
        val heightOfScreen = Resources.getSystem().displayMetrics.heightPixels

        // Setup views
        val popup = listOf<View>(
            findViewById(R.id.background),
            findViewById(R.id.email),
            findViewById(R.id.emailicon),
            findViewById(R.id.username),
            findViewById(R.id.usernameicon),
            findViewById(R.id.password),
            findViewById(R.id.passicon),
            findViewById(R.id.signupButton),
            findViewById(R.id.dhac),
            findViewById(R.id.signin)
        )

        popup.forEach { it.visibility = View.GONE }

        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        signupButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            val email = emailEditText.text.toString()
            val username = usernameEditText.text.toString()

            // Check if any fields are empty
            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Early return if validation fails
            }

            // Check for whitespace only in username
            if (username.trim().isEmpty()) {
                Toast.makeText(this, "Please enter a valid username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Early return if validation fails
            }

            // Check for whitespace and punctuation in username
            if (!isValidUsername(username)) {
                Toast.makeText(this, "Username cannot contain spaces or punctuation", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Early return if validation fails
            }

            // Check for whitespace and punctuation in password
            if (!isValidPassword(password)) {
                Toast.makeText(this, "Password cannot contain spaces or punctuation", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Early return if validation fails
            }

            // Password length validation
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Early return if validation fails
            }

            // All checks passed, send the data
            sendSignupData(email, username, password)
        }

        signinButton.setOnClickListener {
            val intent = Intent(this@signup, Login::class.java)
            startActivity(intent)
        }

        passwordEditText.setOnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2

            if (event.action == MotionEvent.ACTION_UP) {
                val drawableRight = passwordEditText.compoundDrawables[DRAWABLE_RIGHT]

                if (drawableRight != null) {
                    val boundsWidth = drawableRight.bounds.width()
                    val drawableAreaStart = passwordEditText.right - boundsWidth - passwordEditText.paddingRight

                    if (event.rawX >= drawableAreaStart) {
                        togglePasswordVisibility()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    override fun onBackPressed() {
        // Navigate back to WelcomeActivity
        val intent = Intent(this, StartPage::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Optional: Finish the current activity to remove it from the back stack
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

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.eyev, 0)
        } else {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.eyecnot, 0)
        }

        passwordEditText.setSelection(passwordEditText.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun sendSignupData(email: String, username: String, password: String) {
        val apiService = ApiClient.getRetrofitInstance().create(SignupService::class.java)

        apiService.signup(email, username, password).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@signup, "Signup successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@signup, Login::class.java)
                    startActivity(intent)
                } else {
                    // Capture error information and show in Toast
                    val errorBody = response.errorBody()
                    if (errorBody != null) {
                        try {
                            val errorBodyString = errorBody.string()

                            // Extract the error message from the JSON
                            val jsonObject = JSONObject(errorBodyString)
                            val errorMessage = jsonObject.getString("error") // Assuming your PHP returns "error" key

                            Toast.makeText(this@signup, errorMessage, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@signup, "Error parsing response: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@signup, "Response error: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@signup, "Request failed: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun isValidUsername(username: String): Boolean {
        // Check if username contains spaces or punctuation
        return !username.contains(Regex("[\\s\\p{Punct}]"))
    }

    private fun isValidPassword(password: String): Boolean {
        // Check if password contains spaces or punctuation
        return !password.contains(Regex("[\\s\\p{Punct}]"))
    }
}
