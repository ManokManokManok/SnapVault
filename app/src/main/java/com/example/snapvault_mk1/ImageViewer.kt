package com.example.snapvault_mk1

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import android.util.Log
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// Define the API service interface for image editing (deleting and getting info)
interface ImageEditing {
    @FormUrlEncoded
    @POST("delete_image.php")
    fun deleteImage(
        @Field("image_uri") imageUri: String // Send the image URI to the server
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("get_image_info.php")
    fun getImageInfo(
        @Field("image_uri") imageUri: String // Send the image URI to the server
    ): Call<ResponseBody>
}

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var deleteButton: ImageView
    private lateinit var back: ImageView
    private lateinit var info: ImageView
    private var imageUri: Uri? = null
    private var imageId: Int? = null // Assuming you have an ID to identify the image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageView = findViewById(R.id.imageView)
        back = findViewById(R.id.back)
        deleteButton = findViewById(R.id.delete) // Assuming you added this button
        info = findViewById(R.id.info)

        val imageUriString = intent.getStringExtra("imageUri")
        imageId = intent.getIntExtra("imageId", -1) // Get the image ID if available
        Log.d("ImageViewerActivity", "Received Image URI: $imageUriString")

        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString)
            Glide.with(this).load(imageUri).into(imageView)
        }

        back.setOnClickListener {
            finish() // Closes the image viewer when the image is clicked
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        info.setOnClickListener {
            // Fetch and display upload time when info icon is clicked
            fetchImageInfo()
        }
    }

    private fun fetchImageInfo() {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize the API service for getting image info
        val uploadService = ApiClient.getRetrofitInstance().create(ImageEditing::class.java)

        uploadService.getImageInfo(imageUri.toString()).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: return
                    val jsonResponse = JSONObject(responseBody)
                    val status = jsonResponse.getString("status")

                    if (status == "success") {
                        val uploadTime = jsonResponse.getString("upload_time")
                        showUploadTimeDialog(uploadTime)
                    } else {
                        Toast.makeText(this@ImageViewerActivity, "Failed to retrieve image info.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to get image info.", Toast.LENGTH_SHORT).show()
                    Log.e("FetchInfoError", "Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("FetchInfoError", "Error: ${t.message}")
            }
        })
    }

    private fun showUploadTimeDialog(uploadTime: String) {
        // Retrieve the username from SharedPreferences
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE) // Match the preferences name
        val username = sharedPreferences.getString("username", "Unknown User") // Match the key used in WelcomeActivity

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Info")
        builder.setMessage("Uploaded by: $username\n\nUploaded on: $uploadTime")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to delete this image?")
        builder.setPositiveButton("Yes") { dialog, which ->
            deleteImageFromDatabase() // Call the method to delete the image
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss() // Just close the dialog
        }
        builder.show()
    }

    private fun deleteImageFromDatabase() {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize the API service for deleting the image
        val uploadService = ApiClient.getRetrofitInstance().create(ImageEditing::class.java)

        uploadService.deleteImage(imageUri.toString()).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ImageViewerActivity, "Image deleted successfully.", Toast.LENGTH_SHORT).show()
                    finish() // Close the viewer after deletion
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to delete image.", Toast.LENGTH_SHORT).show()
                    Log.e("DeleteError", "Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("DeleteError", "Error: ${t.message}")
            }
        })
    }
}
