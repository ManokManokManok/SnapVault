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
import androidx.recyclerview.widget.GridLayoutManager
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
    @FormUrlEncoded
    @POST("get_user_images.php")
    fun fetchImages(
        @Field("user_id") userId: Int
    ): Call<ResponseBody> // Expecting a response body for fetching images
}

// Data class for the image and userId request
data class ImageData(
    val image: String,
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

    private var backPressedTime: Long = 0
    private val backPressTimeout: Long = 3000 // 3 seconds

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
                Toast.makeText(this, "Uploading Image", Toast.LENGTH_LONG).show()
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

        findViewById<TextView>(R.id.welcomeTextView).text = "$username's Gallery"

        // Fetch images for the logged-in user
        fetchImages(user_Id)

        // Check if permission is already granted
        if (!checkPermission()) {
            requestPermission()
        }

        setClickListeners()
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val user_Id = sharedPreferences.getInt("user_id", -1)

        if (user_Id != -1) {
            fetchImages(user_Id) // Fetch images again when returning to the activity
        }
    }

    private fun initializeRetrofit() {
        uploadService = ApiClient.getRetrofitInstance().create(UploadService::class.java)
    }

    private fun initializeUI() {
        fileIcon = findViewById(R.id.folder)
        personIcon = findViewById(R.id.person)
        uploadIcon = findViewById(R.id.uploadicon)

        recyclerView = findViewById(R.id.recyclerView)

        // Set the GridLayoutManager to display 3 items per row
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        imageAdapter = ImageAdapter(mutableListOf())
        recyclerView.adapter = imageAdapter
    }

    private fun setClickListeners() {
        fileIcon.setOnClickListener { startActivity(Intent(this, Files::class.java)) }
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionLauncher.launch(permission)
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
                        Log.d("UploadSuccess", "Image uploaded successfully!")
                        imageAdapter.addImage(imageUri.toString())
                        fetchImages(userId)
                    } else {
                        Log.e("UploadError", "Failed to upload image. Response code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("UploadError", t.message.toString())
                }
            })
        } else {
            Log.e("UploadError", "Error converting image to Base64.")
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ImageError", e.message.toString())
            null
        }
    }

    private fun fetchImages(userId: Int) {
        uploadService.fetchImages(userId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val rawResponse = response.body()?.string() ?: "No response"
                    Log.d("RawResponse", rawResponse)

                    try {
                        val jsonResponse = JSONObject(rawResponse)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val imagesJsonArray = jsonResponse.getJSONArray("images")
                            val imagesList = mutableListOf<String>()

                            for (i in 0 until imagesJsonArray.length()) {
                                imagesList.add(imagesJsonArray.getString(i))
                            }
                            imagesList.reverse()

                            imageAdapter.updateImages(imagesList)
                        } else {
                            Log.e("FetchError", "Failed to fetch images: $status")
                        }
                    } catch (e: Exception) {
                        Log.e("JSONError", "Error parsing JSON response: ${e.message}")
                    }
                } else {
                    Log.e("FetchError", "Failed to fetch images. Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchError", t.message.toString())
            }
        })
    }

    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < backPressTimeout) {
            super.onBackPressed()
            finish() // Close the app
        } else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
            backPressedTime = currentTime
        }
    }
}
