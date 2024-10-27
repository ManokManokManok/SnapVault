package com.example.snapvault_mk1

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
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

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private var albumId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_images)

        // Initialize RecyclerView and TextView for album name
        recyclerView = findViewById(R.id.recyclerView)
        val albumsnameTextView = findViewById<TextView>(R.id.albumsname)

        // Get the album ID from the intent
        albumId = intent.getIntExtra("albumId", -1)

        setupRecyclerView()

        if (albumId != -1) {
            fetchImagesForAlbum(albumId, albumsnameTextView) // Pass TextView to update album name
        } else {
            Toast.makeText(this, "Invalid album selected.", Toast.LENGTH_SHORT).show()
            finish() // Close activity if album ID is invalid
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager =
            GridLayoutManager(this, 3) // Set the layout manager for a grid view
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

                    // Show an alert dialog with the server response
                    showAlertDialog("Server Response", rawResponse)

                    try {
                        val jsonResponse = JSONObject(rawResponse)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            // Fetch album name from the JSON response
                            val albumName = jsonResponse.getString("album_name")
                            albumsnameTextView.text = albumName // Display the correct album name

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

    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}

