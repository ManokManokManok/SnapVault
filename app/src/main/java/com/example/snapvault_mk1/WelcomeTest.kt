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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.ByteArrayOutputStream

// Define the API service for uploading image data as Base64
interface UploadService {
    @POST("upload_image.php")
    fun uploadImage(
        @Body imageData: ImageData
    ): Call<ResponseBody>
}

// Data class for the image and userId request
data class ImageData(
    val image: String, // Base64 encoded image string
    val user_id: Int
)

private const val BASE_URL = "http://192.168.1.32/" // Replace with your server's IP address

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

        val userId = sharedPreferences.getInt("user_id", -1)
        val username = sharedPreferences.getString("username", "Guest") ?: "Guest"
        val userEmail = sharedPreferences.getString("email", "No email") ?: "No email"

        findViewById<TextView>(R.id.welcomeTextView).text = "Welcome, $username\nEmail: $userEmail\nUser ID: $userId"

        if (userId != -1) {
            fetchImages(userId) // Fetch images for the logged-in user
        }

        // Check if permission is already granted
        if (!checkPermission()) {
            requestPermission()
        }

        setClickListeners()
    }

    private fun initializeRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        uploadService = retrofit.create(UploadService::class.java)
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
                        Toast.makeText(this@WelcomeActivity, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                        imageAdapter.addImage(imageUri.toString()) // Convert Uri to String before adding
                        fetchImages(userId)
                    } else {
                        Toast.makeText(this@WelcomeActivity, "Failed to upload image. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("UploadError", t.message.toString())
                    Toast.makeText(this@WelcomeActivity, "Upload error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Error converting image to Base64. Please try again.", Toast.LENGTH_SHORT).show()
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
        // Implementation for fetching images associated with the user from the server.
        // You need to add your API call logic here.
    }
}
