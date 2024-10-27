package com.example.snapvault_mk1

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import androidx.appcompat.app.AlertDialog

class AlbumImagesActivity : AppCompatActivity() {

    interface AlbumImageService {
        @FormUrlEncoded
        @POST("get_album_images.php")
        fun getAlbumImages(@Field("album_id") albumId: Int): Call<ResponseBody>
    }

    interface AlbumInfoService {
        @FormUrlEncoded
        @POST("get_album_info.php")
        fun getAlbumInfo(@Field("album_id") albumId: Int): Call<ResponseBody>
    }

    interface AlbumDeleteService {
        @FormUrlEncoded
        @POST("delete_album.php")
        fun deleteAlbum(@Field("album_id") albumId: Int): Call<ResponseBody>
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private var albumId: Int = -1
    private lateinit var addimage: ImageView
    private lateinit var info: ImageView
    private lateinit var delete: ImageView
    private lateinit var albumsnameTextView: TextView
    private lateinit var backIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_images)

        // Initialize RecyclerView and TextView for album name
        recyclerView = findViewById(R.id.recyclerView)
        albumsnameTextView = findViewById(R.id.albumsname) // Moved here
        addimage = findViewById(R.id.addimage)
        info = findViewById(R.id.info)
        delete = findViewById(R.id.deletealbum)
        backIcon = findViewById(R.id.back)

        backIcon.setOnClickListener {
            startActivity(Intent(this, Files::class.java))
            finish()
        }

        // Get the album ID from the intent
        albumId = intent.getIntExtra("albumId", -1)

        setupRecyclerView()

        // Set up click listener for add image button
        addimage.setOnClickListener { showPasswordDialog() }

        // Set up click listener for info button
        info.setOnClickListener { fetchAlbumInfo(albumId, albumsnameTextView) }

        // Set up click listener for delete button
        delete.setOnClickListener { showDeleteConfirmationDialog() }
    }

    override fun onStart() {
        super.onStart()
        // Fetch images for the album every time the activity is started
        if (albumId != -1) {
            fetchImagesForAlbum(albumId, albumsnameTextView) // Pass TextView to update album name
        } else {
            Toast.makeText(this, "Invalid album selected.", Toast.LENGTH_SHORT).show()
            finish() // Close activity if album ID is invalid
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 3) // Set the layout manager for a grid view
        imageAdapter = ImageAdapter(mutableListOf()) // Initialize with an empty list
        recyclerView.adapter = imageAdapter // Set the adapter
    }

    private fun fetchImagesForAlbum(albumId: Int, albumsnameTextView: TextView) {
        val service = ApiClient.getRetrofitInstance().create(AlbumImageService::class.java)
        service.getAlbumImages(albumId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val rawResponse = response.body()?.string() ?: "No response"
                    Log.d("RawResponse", rawResponse)

                    try {
                        val jsonResponse = JSONObject(rawResponse)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            // Fetch and display the album name regardless of the images array
                            val albumName = jsonResponse.getString("album_name")
                            albumsnameTextView.text = albumName // Always display the album name

                            // Process images array
                            val imagesJsonArray = jsonResponse.getJSONArray("images")
                            val imagesList = mutableListOf<String>()

                            for (i in 0 until imagesJsonArray.length()) {
                                imagesList.add(imagesJsonArray.getString(i).replace("\\/", "/"))
                            }
                            imagesList.reverse() // Optional: Reverse to show latest images first

                            // Update the adapter with new images
                            imageAdapter.updateImages(imagesList)
                        } else {
                            val albumName = jsonResponse.getString("album_name")
                            albumsnameTextView.text = albumName
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
                Log.e("FetchError", "Error: ${t.message}")
            }
        })
    }

    private fun fetchAlbumInfo(albumId: Int, albumsnameTextView: TextView) {
        val service = ApiClient.getRetrofitInstance().create(AlbumInfoService::class.java)
        service.getAlbumInfo(albumId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val rawResponse = response.body()?.string() ?: "No response"
                    Log.d("RawResponse", rawResponse)

                    try {
                        val jsonResponse = JSONObject(rawResponse)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            // Fetch album name and creation date only
                            val albumName = jsonResponse.getString("album_name")
                            val albumCreationDate = jsonResponse.optString("creation_date", "Unknown date") // Example additional info

                            // Update the TextView with the album name
                            albumsnameTextView.text = albumName

                            // Show an alert dialog with the album info (without password)
                            showAlertDialog("Album Info", "Name: $albumName\nCreated On: $albumCreationDate")
                        } else {
                            Log.e("FetchError", "Failed to fetch album info: $status")
                        }
                    } catch (e: Exception) {
                        Log.e("JSONError", "Error parsing JSON response: ${e.message}")
                    }
                } else {
                    Log.e("FetchError", "Failed to fetch album info. Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchError", "Error: ${t.message}")
            }
        })
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Album")
            .setMessage("Are you sure you want to delete this album?")
            .setPositiveButton("Yes") { dialog, _ ->
                deleteAlbum(albumId) // Call the method to delete the album
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun deleteAlbum(albumId: Int) {
        val service = ApiClient.getRetrofitInstance().create(AlbumDeleteService::class.java)
        service.deleteAlbum(albumId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AlbumImagesActivity, "Album deleted successfully.", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity or navigate as needed
                } else {
                    Toast.makeText(this@AlbumImagesActivity, "Failed to delete album.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@AlbumImagesActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showPasswordDialog() {
        // No longer needed since password setting feature is removed
    }
    override fun onBackPressed() {
        // Create an Intent to navigate back to WelcomeActivity
        val intent = Intent(this, WelcomeActivity::class.java)
        // Clear the current activity and any other activities in the back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Finish the current activity
    }
}
