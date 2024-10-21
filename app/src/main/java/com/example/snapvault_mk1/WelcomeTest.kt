package com.example.snapvault_mk1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import org.json.JSONObject
import java.io.ByteArrayOutputStream

// Define the API service for uploading image data as Base64
interface UploadService {
    @POST("upload_image.php")
    fun uploadImage(
        @Body imageData: ImageData
    ): Call<ResponseBody> // Expecting a response body for upload

    // Updated method to fetch images using form-encoded parameters
    @FormUrlEncoded // Add this annotation to specify form data
    @POST("get_user_images.php")
    fun fetchImages(
        @Field("user_id") userId: Int // Change to @Field with the name that the PHP script expects
    ): Call<ResponseBody> // Expecting a response body for fetching images
}

// Data class for the image and userId request
data class ImageData(
    val image: String, // Base64 encoded image string
    val user_id: Int
)

class WelcomeActivity : AppCompatActivity() {

    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var uploadIcon: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var uploadService: UploadService

    private var selectedImageUri: Uri? = null

    // Register permission result handler
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission denied. Please enable access from settings.", Toast.LENGTH_SHORT).show()
        }
    }

    // Register image picker result handler
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                Log.d("ImageURI", "Selected Image URI: $uri")
                Toast.makeText(this, "Selected Image URI: $uri", Toast.LENGTH_LONG).show()
                uploadImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

        if (!sharedPreferences.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_welcome_test)

        // Initialize Retrofit service
        initializeRetrofit()

        // Initialize UI components
        initializeUI()

        val user_Id = sharedPreferences.getInt("user_id", -1)
        val username = sharedPreferences.getString("username", "Guest") ?: "Guest"
        val userEmail = sharedPreferences.getString("email", "No email") ?: "No email"

        Log.d("UserId", "Fetched User ID: $user_Id")

        if (user_Id == -1) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        findViewById<TextView>(R.id.welcomeTextView).text = "Welcome, $username\nEmail: $userEmail\nUser ID: $user_Id"

        if (user_Id != -1) {
            fetchImages(user_Id) // Fetch images for the logged-in user
        }

        // Check if permission is already granted
        if (!checkPermission()) {
            requestPermission()
        }

        setClickListeners()
    }

    private fun initializeRetrofit() {
        uploadService = ApiClient.getRetrofitInstance().create(UploadService::class.java)
    }

    private fun initializeUI() {
        fileIcon = findViewById(R.id.folder)
        createIcon = findViewById(R.id.create)
        personIcon = findViewById(R.id.person)
        uploadIcon = findViewById(R.id.uploadicon)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) // Set horizontal layout manager
        imageAdapter = ImageAdapter(mutableListOf())
        recyclerView.adapter = imageAdapter
    }

    private fun setClickListeners() {
        fileIcon.setOnClickListener { startActivity(Intent(this, Files::class.java)) }
        createIcon.setOnClickListener { startActivity(Intent(this, Createalbum::class.java)) }
        personIcon.setOnClickListener { startActivity(Intent(this, User::class.java)) }
        uploadIcon.setOnClickListener {
            if (checkPermission()) {
                openImagePicker()
            } else {
                requestPermission()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
    }

    private fun uploadImage(imageUri: Uri) {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val base64Image = imageUriToBase64(imageUri)
        if (base64Image != null) {
            val imageData = ImageData(image = base64Image, user_id = userId)

            uploadService.uploadImage(imageData).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Log.d("UploadSuccess", "Image uploaded successfully!") // Log success message
                        imageAdapter.addImage(imageUri.toString()) // Convert Uri to String before adding
                        fetchImages(userId)
                    } else {
                        Log.e("UploadError", "Failed to upload image. Response code: ${response.code()}") // Log error message
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("UploadError", t.message.toString()) // Log error message
                }
            })
        } else {
            Log.e("UploadError", "Error converting image to Base64.") // Log error message
        }
    }

    private fun imageUriToBase64(uri: Uri): String? {
        return try {
            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) // Compress the image to JPEG
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT) // Encode to Base64
        } catch (e: Exception) {
            Log.e("ImageError", e.message.toString())
            null // Return null on error
        }
    }

    private fun fetchImages(userId: Int) {
        uploadService.fetchImages(userId).enqueue(object : Callback<ResponseBody> { // Use ResponseBody
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    // Capture the raw response as a string
                    val rawResponse = response.body()?.string() ?: "No response"

                    // Log the raw response for debugging
                    Log.d("RawResponse", rawResponse)

                    try {
                        // Parse the JSON response
                        val jsonResponse = JSONObject(rawResponse)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val imagesJsonArray = jsonResponse.getJSONArray("images")
                            val imagesList = mutableListOf<String>()

                            for (i in 0 until imagesJsonArray.length()) {
                                imagesList.add(imagesJsonArray.getString(i))
                            }

                            // Update the adapter with the fetched images
                            imageAdapter.updateImages(imagesList) // Use a method to update the list in your adapter
                        } else {
                            Log.e("FetchError", "Failed to fetch images: $status") // Log error message
                        }
                    } catch (e: Exception) {
                        Log.e("JSONError", "Error parsing JSON response: ${e.message}") // Log error message
                    }
                } else {
                    Log.e("FetchError", "Failed to fetch images. Response code: ${response.code()}") // Log error message
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchError", "Error: ${t.message}") // Log error message
            }
        })
    }
}
