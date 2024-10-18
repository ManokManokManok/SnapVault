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

interface ChangeEmailApi {
    @FormUrlEncoded
    @POST("change_email.php") // DITO YUNG API NATIN ALAM MO NA YAN DAPAT
    fun changeEmail(
        @Field("current_email") currentEmail: String,
        @Field("new_email") newEmail: String
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

       //EDGE TO EDGE SUPP NATEN (SEARCH MO NALANG KUNG DI KA SURE)
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
            findViewById(R.id.emailicon),
            findViewById(R.id.passicon),
            findViewById(R.id.confirmbutton),
            findViewById(R.id.back)
        )

        val backbutton = findViewById<Button>(R.id.back)

        backbutton.setOnClickListener {
            startActivity(Intent(this, User::class.java))
        }



        // ALAM MO NA DAPAT TO JUSKO LAGING GINAGAMIT
        popup.forEach { it.visibility = View.GONE }


        if (!isPopupShown) {
            showPopup(popup, heightOfScreen)
            isPopupShown = true
        }

        val confirmButton = findViewById<Button>(R.id.confirmbutton)
        val currentEmailInput = findViewById<EditText>(R.id.curremail)
        val newEmailInput = findViewById<EditText>(R.id.newemail)

        // CLICK LISTENER NUNG CONFIRM BUTTON
        confirmButton.setOnClickListener {
            val currentEmail = currentEmailInput.text.toString().trim()
            val newEmail = newEmailInput.text.toString().trim()

            // ITO NAG CHE CHECK KUNG YUNG NILAGAY NA EMAIL IS MATCH DUN SA ACCOUNT EMAIL NI USER
            if (currentEmail != accountEmail) {
                Toast.makeText(this, "Current email does not match your account.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // MAKES SURE NA CHAKA LANG IIBAHIN PAG VALID
            changeEmail(currentEmail, newEmail)
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

    private fun changeEmail(currentEmail: String, newEmail: String) {
        // CHECKING KUNG MAY LAMAN YUNG NEW EMAIL FIELD
        if (newEmail.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // RETROFIT NATIN (!!WAG GAGALAWIN KUNG DI KAILANGAN!!)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.43.180/") // IBAHIN AS NEEDED TO
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val changeEmailApi = retrofit.create(ChangeEmailApi::class.java)

        // NETWORK CALLING PRA IBAHIN YUNG EMAIL
        changeEmailApi.changeEmail(currentEmail, newEmail).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val responseMessage = responseBody.string() // Get the response from the PHP file
                        Toast.makeText(this@Settings_Email, responseMessage, Toast.LENGTH_SHORT).show()

                        // ITO NAG CHE CHECK KUNG SUCCESS AND IF SO BALIK SA LOGIN
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
