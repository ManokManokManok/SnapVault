package com.example.snapvault_mk1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface UploadService {
    @Multipart
    @POST("upload_image.php")
    fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("user_id") userId: RequestBody
    ): Call<ResponseBody>

    @GET("get_user_images.php")
    fun fetchImages(@Query("user_id") userId: Int): Call<ImagesResponse>
}

data class ImagesResponse(
    val status: String,
    val images: List<String>
)

private const val BASE_URL = "http://10.0.2.2/" // Replace with your server's IP address

class WelcomeActivity : AppCompatActivity() {

    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var uploadIcon: ImageView
    private lateinit var uploadedImageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private var selectedImageUri: Uri? = null

    private val pickImageRequest = 1
    private lateinit var uploadService: UploadService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_test)

        initializeRetrofit()
        initializeUI()

        // Load user information
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        val username = sharedPreferences.getString("username", "Guest") ?: "Guest"
        val userEmail = sharedPreferences.getString("email", "No email") ?: "No email"

        findViewById<TextView>(R.id.welcomeTextView).text =
            "Welcome, $username\nEmail: $userEmail\nUser ID: $userId"

        if (userId != -1) {
            fetchImages(userId)
        }

        // Check and request permissions
        if (!checkPermission()) {
            showPermissionExplanationDialog()
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
        uploadedImageView = findViewById(R.id.uploadedImageView)
        recyclerView = findViewById(R.id.recyclerView)

        uploadedImageView.visibility = View.GONE
        recyclerView.layoutManager = LinearLayoutManager(this)
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
                showPermissionExplanationDialog()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, pickImageRequest)
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setMessage("This app needs permission to access your photos to upload images.")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                requestPermission()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setTitle("Permission Required")
            .show()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                pickImageRequest
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                pickImageRequest
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == pickImageRequest) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission denied. Please allow access in app settings.", Toast.LENGTH_SHORT).show()
                showSettingsDialog()
            }
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setMessage("Permission is needed for the app to function correctly. Please enable it in app settings.")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setTitle("Permission Required")
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageRequest && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let {
                uploadedImageView.setImageURI(it)
                uploadedImageView.visibility = View.VISIBLE
                uploadImage(it)
            }
        }
    }

    private fun uploadImage(imageUri: Uri) {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        contentResolver.openInputStream(imageUri)?.use { inputStream ->
            val fileName = getFileName(imageUri) ?: "uploaded_image.jpg"
            val requestFile = RequestBody.create("image/*".toMediaType(), inputStream.readBytes())
            val body = MultipartBody.Part.createFormData("image", fileName, requestFile)
            val userIdRequestBody = RequestBody.create("text/plain".toMediaType(), userId.toString())

            uploadService.uploadImage(body, userIdRequestBody).enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@WelcomeActivity, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                        fetchImages(userId)
                    } else {
                        Toast.makeText(this@WelcomeActivity, "Failed to upload image. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@WelcomeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchImages(userId: Int) {
        uploadService.fetchImages(userId).enqueue(object : retrofit2.Callback<ImagesResponse> {
            override fun onResponse(call: Call<ImagesResponse>, response: retrofit2.Response<ImagesResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { imagesResponse ->
                        if (imagesResponse.status == "success") {
                            imageAdapter.updateImages(imagesResponse.images)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ImagesResponse>, t: Throwable) {
                Log.e("WelcomeActivity", "Error fetching images: ${t.message}")
            }
        })
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
            }
        }
        return null
    }
}
