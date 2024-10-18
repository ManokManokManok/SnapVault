package com.example.snapvault_mk1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import android.content.SharedPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
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


private const val BASE_URL = "http://192.168.1.11/" // Replace with your server's IP address

class WelcomeActivity : AppCompatActivity() {

    private lateinit var fileIcon: ImageView
    private lateinit var createIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var uploadIcon: ImageView
    private lateinit var uploadedImageView: ImageView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var uploadService: UploadService
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter

    private val pickImageRequest = 1
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

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

        Toast.makeText(this, "Saved User ID: $userId", Toast.LENGTH_SHORT).show()


        findViewById<TextView>(R.id.welcomeTextView).text = "Welcome, $username\nEmail: $userEmail\nUser ID: $userId"

        if (userId != -1) {
            fetchImages(userId) // Fetch images for the logged-in user
        }

        if (!checkPermission()) {
            showPermissionExplanationDialog()
        }

        setClickListeners()
    }

    private fun initializeRetrofit() {
        // Initialize Retrofit
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
        uploadedImageView.visibility = View.GONE

        recyclerView = findViewById(R.id.recyclerView)
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
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), pickImageRequest)
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
        val userId = sharedPreferences.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare the file for upload using the ContentResolver
        contentResolver.openInputStream(imageUri)?.use { inputStream ->
            val fileName = getFileName(imageUri) ?: "uploaded_image.jpg"
            val requestFile = RequestBody.create("image/*".toMediaType(), inputStream.readBytes())
            val body = MultipartBody.Part.createFormData("image", fileName, requestFile)
            val userIdRequestBody = RequestBody.create("text/plain".toMediaType(), userId.toString())

            uploadService.uploadImage(body, userIdRequestBody).enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@WelcomeActivity, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                        fetchImages(userId) // Fetch updated images after upload
                    } else {
                        Toast.makeText(this@WelcomeActivity, "Failed to upload image. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("UploadError", t.message.toString())
                    Toast.makeText(this@WelcomeActivity, "Upload error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: Toast.makeText(this, "Error opening image. Please select a valid image.", Toast.LENGTH_SHORT).show()
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                return it.getString(nameIndex)
            }
        }
        return null
    }

    private fun fetchImages(userId: Int) {
        // Assuming you have an instance of UploadService
        uploadService.fetchImages(userId).enqueue(object : retrofit2.Callback<ImagesResponse> {
            override fun onResponse(call: Call<ImagesResponse>, response: retrofit2.Response<ImagesResponse>) {
                if (response.isSuccessful) {
                    val imagesResponse = response.body()
                    Log.d("API Response", imagesResponse.toString()) // Log entire response
                    if (imagesResponse?.status == "success") {
                        showUrlsAlertDialog(imagesResponse.images)
                    } else {
                        Log.e("API Error", "Status is not success")
                        showAlert("Error", "Failed to fetch images. Status not success.")
                    }
                } else {
                    Log.e("API Error", "Response not successful: ${response.code()}")
                    showAlert("Error", "Error fetching images: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ImagesResponse>, t: Throwable) {
                showAlert("Error", "Fetch error: ${t.message}")
            }
        })
    }


    // Simple Alert function to display any message
    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    // New function to display URLs in an AlertDialog
    private fun showUrlsAlertDialog(imageUrls: List<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Fetched Image URLs")

        // Create a string from the image URLs
        val urlsString = imageUrls.joinToString("\n") // Join the URLs with new line for better readability
        builder.setMessage(urlsString)

        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }



}
