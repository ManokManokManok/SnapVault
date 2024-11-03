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

// Define the API service interface for image editing (deleting, getting info, adding/removing from albums)
interface ImageEditing {
    @FormUrlEncoded
    @POST("delete_image.php")
    fun deleteImage(
        @Field("image_uri") imageUri: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("get_image_info.php")
    fun getImageInfo(
        @Field("image_uri") imageUri: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("add_image_to_album.php")
    fun addImageToAlbum(
        @Field("image_uri") imageUri: String,
        @Field("album_id") albumId: Int
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("remove_image_from_album.php")
    fun removeImageFromAlbum(
        @Field("image_uri") imageUri: String,
        @Field("album_id") albumId: Int
    ): Call<ResponseBody>
}

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var deleteButton: ImageView
    private lateinit var addto: ImageView
    private lateinit var info: ImageView
    private lateinit var downloadButton: ImageView
    private lateinit var removeFrom: ImageView
    private var imageUri: Uri? = null
    private var imageId: Int? = null
    private var albums: List<Album>? = null
    private var imageList: List<String>? = null
    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageView = findViewById(R.id.imageView)
        addto = findViewById(R.id.addto)
        deleteButton = findViewById(R.id.delete)
        info = findViewById(R.id.info)
        downloadButton = findViewById(R.id.downloadbutton)
        removeFrom = findViewById(R.id.removefromalbum)
        imageList = intent.getStringArrayListExtra("imageList")
        currentPosition = intent.getIntExtra("imagePosition", 0)

        loadCurrentImage()

        val imageUriString = intent.getStringExtra("imageUri")
        imageId = intent.getIntExtra("imageId", -1)
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
            fetchImageInfo()
        }

        addto.setOnClickListener {
            fetchUserAlbumsForAddition()
        }

        removeFrom.setOnClickListener {
            fetchUserAlbumsForRemoval()
        }

        downloadButton.setOnClickListener {
            downloadImage()
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

    private fun fetchUserAlbumsForAddition() {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val albumService = ApiClient.getRetrofitInstance().create(Files.AlbumStuff::class.java)

        albumService.getUserAlbums(userId).enqueue(object : Callback<List<Album>> {
            override fun onResponse(call: Call<List<Album>>, response: Response<List<Album>>) {
                if (response.isSuccessful) {
                    albums = response.body()
                    if (!albums.isNullOrEmpty()) {
                        showAlbumSelectionDialogForAddition()
                    } else {
                        Toast.makeText(this@ImageViewerActivity, "No albums found.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to retrieve albums.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Album>>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserAlbumsForRemoval() {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val albumService = ApiClient.getRetrofitInstance().create(Files.AlbumStuff::class.java)

        albumService.getUserAlbums(userId).enqueue(object : Callback<List<Album>> {
            override fun onResponse(call: Call<List<Album>>, response: Response<List<Album>>) {
                if (response.isSuccessful) {
                    albums = response.body()
                    if (!albums.isNullOrEmpty()) {
                        showAlbumRemovalDialog()
                    } else {
                        Toast.makeText(this@ImageViewerActivity, "No albums found.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to retrieve albums.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Album>>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAlbumSelectionDialogForAddition() {
        val albumNames = albums?.map { it.album_name }?.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select an Album to Add The Image To")
            .setItems(albumNames) { _, which ->
                val selectedAlbum = albums?.get(which)
                selectedAlbum?.let { addImageToAlbum(it.album_id) }
            }
            .show()
    }

    private fun showAlbumRemovalDialog() {
        val albumNames = albums?.map { it.album_name }?.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select an Album to Remove The Image From")
            .setItems(albumNames) { _, which ->
                val selectedAlbum = albums?.get(which)
                selectedAlbum?.let { removeImageFromAlbum(it.album_id) }
            }
            .show()
    }

    private fun addImageToAlbum(albumId: Int) {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val uploadService = ApiClient.getRetrofitInstance().create(ImageEditing::class.java)

        uploadService.addImageToAlbum(imageUri.toString(), albumId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ImageViewerActivity, "Image added to album successfully.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to add image to album.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun removeImageFromAlbum(albumId: Int) {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val uploadService = ApiClient.getRetrofitInstance().create(ImageEditing::class.java)

        uploadService.removeImageFromAlbum(imageUri.toString(), albumId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ImageViewerActivity, "Image removed from album successfully.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to remove image from album.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchImageInfo() {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val uploadService = ApiClient.getRetrofitInstance().create(ImageEditing::class.java)

        uploadService.getImageInfo(imageUri.toString()).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body()?.string() ?: "")
                    // Parse the info you need
                    val imageInfo = jsonResponse.getString("info") // Adjust based on your response
                    showImageInfoDialog(imageInfo)
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to get image info.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showImageInfoDialog(info: String) {
        AlertDialog.Builder(this)
            .setTitle("Image Info")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?")
            .setPositiveButton("Yes") { _, _ -> deleteImage() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val uploadService = ApiClient.getRetrofitInstance().create(ImageEditing::class.java)

        uploadService.deleteImage(imageUri.toString()).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ImageViewerActivity, "Image deleted successfully.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ImageViewerActivity, "Failed to delete image.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ImageViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun downloadImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = DownloadManager.Request(imageUri)
            .setTitle("Downloading Image")
            .setDescription("Image is being downloaded...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "downloaded_image.jpg") // You can customize the file name

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(this, "Download started.", Toast.LENGTH_SHORT).show()
    }
}
