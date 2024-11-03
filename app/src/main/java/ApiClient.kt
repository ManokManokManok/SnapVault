package com.example.snapvault_mk1

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object ApiClient {
    private const val BASE_URL = "http://192.168.1.9/" // Replace with your server's IP or domain

    private var retrofit: Retrofit? = null

    fun getRetrofitInstance(): Retrofit {
        if (retrofit == null) {
            // Create a Gson instance with lenient parsing
            val gson: Gson = GsonBuilder()
                .setLenient() // Enable lenient parsing
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create()) // For plain string responses
                .addConverterFactory(GsonConverterFactory.create(gson)) // Use custom Gson instance
                .build()
        }
        return retrofit!!
    }
}