package com.example.snapvault_mk1

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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

    @FormUrlEncoded
    @POST("add_image_to_album.php") // Update this with your actual add image to album endpoint
    fun addImageToAlbum(
        @Field("image_uri") imageUri: String,
        @Field("album_id") albumId: Int // Send the album ID to the server
    ): Call<ResponseBody>
}

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var deleteButton: ImageView
    private lateinit var addto: ImageView
    private lateinit var info: ImageView
    private lateinit var downloadbutt: ImageView
    private var imageUri: Uri? = null
    private var imageId: Int? = null // Assuming you have an ID to identify the image
    private var albums: List<Album>? = null // List to hold user albums
    private var imageList: List<String>? = null
    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageView = findViewById(R.id.imageView)
        addto = findViewById(R.id.addto)
        deleteButton = findViewById(R.id.delete) // Assuming you added this button
        info = findViewById(R.id.info)
        downloadbutt = findViewById(R.id.downloadbutton) // Initialize download button
        imageList = intent.getStringArrayListExtra("imageList") // Retrieve the image list
        currentPosition = intent.getIntExtra("imagePosition", 0) // Retrieve the current position

        loadCurrentImage()

        val imageUriString = intent.getStringExtra("imageUri")
        imageId = intent.getIntExtra("imageId", -1) // Get the image ID if available
        Log.d("ImageViewerActivity", "Received Image URI: $imageUriString")

        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString)
            Glide.with(this).load(imageUri).into(imageView)
        }

        findViewById<ImageView>(R.id.nextButton).setOnClickListener {
            goToNextImage()
        }
        findViewById<ImageView>(R.id.previousButton).setOnClickListener {
            goToPreviousImage()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        info.setOnClickListener {
            // Fetch and display upload time when info icon is clicked
            fetchImageInfo()
        }

        addto.setOnClickListener {
            fetchUserAlbums() // Fetch albums when addto button is clicked
        }

        downloadbutt.setOnClickListener {
            downloadImage() // Call the method to download the image
        }
    }

    private fun loadCurrentImage() {
        if (imageList != null && currentPosition in imageList!!.indices) {
            imageUri = Uri.parse(imageList!![currentPosition])
            Glide.with(this).load(imageList!![currentPosition]).into(imageView)
        }
    }

    private fun goToNextImage() {
        if (imageList != null && currentPosition < imageList!!.size - 1) {
            currentPosition++
            loadCurrentImage()
        } else {
            Toast.makeText(this, "No next image available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToPreviousImage() {
        if (imageList != null && currentPosition > 0) {
            currentPosition--
            loadCurrentImage()
        } else {
            Toast.makeText(this, "No previous image available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserAlbums() {
        // Replace with your user ID retrieval logic
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize the API service for getting user albums
        val albumService = ApiClient.getRetrofitInstance().create(Files.AlbumStuff::class.java)

        albumService.getUserAlbums(userId).enqueue(object : Callback<List<Album>> {
            override fun onResponse(call: Call<List<Album>>, response: Response<List<Album>>) {
                if (response.isSuccessful) {
                    albums = response.body()
                    if (!albums.isNullOrEmpty()) {
                        showAlbumSelectionDialog()
                    } else {
                        Toast.makeText(this@ImageViewerActivity, "No albums found.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to retrieve albums.", Toast.LENGTH_SHORT).show()
                    Log.e("FetchAlbumsError", "Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Album>>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("FetchAlbumsError", "Error: ${t.message}")
            }
        })
    }

    private fun showAlbumSelectionDialog() {
        val albumNames = albums?.map { it.album_name }?.toTypedArray() // Get album names

        AlertDialog.Builder(this)
            .setTitle("Select an Album")
            .setItems(albumNames) { dialog, which ->
                val selectedAlbum = albums?.get(which)
                if (selectedAlbum != null) {
                    addImageToAlbum(selectedAlbum.album_id) // Add the image to the selected album
                }
            }
            .show()
    }

    private fun addImageToAlbum(albumId: Int) {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize the API service for adding the image to the album
        val uploadService = ApiClient.getRetrofitInstance().create(ImageEditing::class.java)

        uploadService.addImageToAlbum(imageUri.toString(), albumId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ImageViewerActivity, "Image added to album successfully.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to add image to album.", Toast.LENGTH_SHORT).show()
                    Log.e("AddToAlbumError", "Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("AddToAlbumError", "Error: ${t.message}")
            }
        })
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
        AlertDialog.Builder(this)
            .setTitle("Image Info")
            .setMessage("Upload Time: $uploadTime")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?")
            .setPositiveButton("Yes") { dialog, which -> deleteImage() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteImage() {
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
                    finish() // Close the activity after deletion
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to delete image.", Toast.LENGTH_SHORT).show()
                    Log.e("DeleteImageError", "Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("DeleteImageError", "Error: ${t.message}")
            }
        })
    }

    private fun downloadImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = DownloadManager.Request(imageUri)
        request.setTitle("Image Download")
        request.setDescription("Downloading image...")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "downloaded_image.jpg")

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
    }
}
