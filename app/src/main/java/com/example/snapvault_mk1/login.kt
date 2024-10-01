package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.EditText
import android.text.InputType
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snapvault_mk1.ApiService
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import android.util.Log

interface LoginApi {
    @FormUrlEncoded
    @POST("login.php")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<ResponseBody>
}

class Login : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize the views
        emailEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton) // Make sure this ID matches your XML layout

        // Handle password visibility toggle
        passwordEditText.setOnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordEditText.right - passwordEditText.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Handle login button click
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                sendLoginData(email, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to toggle password visibility
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

    // Function to send login data to the server
    private fun sendLoginData(email: String, password: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val LoginApi = retrofit.create(LoginApi::class.java)
        LoginApi.login(email, password).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {

                if (response.isSuccessful) {
                    val jsonResponse = response.body()?.string() ?:return
                    Log.d("LoginResponse", "Response JSON: $jsonResponse")
                    val jsonObject = JSONObject(jsonResponse)
                    val status = jsonObject.getString("status")

                    if (status == "success") {
                        val username = jsonObject.getString("username")
                        val id = jsonObject.getInt("id")

                        // Show success message and navigate to the next screen
                        Toast.makeText(this@Login, "Welcome, $username!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@Login, WelcomeActivity::class.java)
                        intent.putExtra("username", username)
                        intent.putExtra("id", id)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@Login, "Login failed: ${jsonObject.getString("message")}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Login, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@Login, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
