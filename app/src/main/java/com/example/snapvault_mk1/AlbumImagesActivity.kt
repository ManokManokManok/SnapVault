package com.example.snapvault_mk1

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.view.View
import android.content.Intent
import android.content.SharedPreferences
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
        @POST("delete_album.php") // Add the correct endpoint for deleting albums
        fun deleteAlbum(@Field("album_id") albumId: Int): Call<ResponseBody>
    }

    interface AlbumCountService {
        @FormUrlEncoded
        @POST("get_album_count.php")
        fun getAlbumCount(@Field("user_id") userId: Int): Call<ResponseBody>
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private var albumId: Int = -1
    private lateinit var addimage: ImageView
    private lateinit var info: ImageView
    private lateinit var delete: ImageView
    private lateinit var albumsnameTextView: TextView
    private lateinit var backIcon: ImageView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_images)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

        // Initialize RecyclerView and TextView for album name
        recyclerView = findViewById(R.id.recyclerView)
        albumsnameTextView = findViewById(R.id.albumsname)
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

        // Fetch user ID from SharedPreferences
        val userId = sharedPreferences.getInt("user_id", -1)

        val downloadButton: ImageView = findViewById(R.id.downloadbutton)
        downloadButton.setOnClickListener {
            downloadAllImages(imageAdapter.getImages())
        }

        // Fetch album count for the user
        if (userId != -1) {
            fetchAlbumCount(userId)
        } else {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Set up click listener for add image button
        addimage.setOnClickListener { showPasswordDialog() }

        // Set up click listener for info button
        info.setOnClickListener { fetchAlbumInfo(albumId, albumsnameTextView) }

        // Set up click listener for delete button
        delete.setOnClickListener { showDeleteConfirmationDialog() }
    }

    override fun onStart() {
        super.onStart()
        // FETCH IMAGES KADA NASA ACTIVITY
        if (albumId != -1) {
            fetchImagesForAlbum(albumId, albumsnameTextView)
        } else {
            Toast.makeText(this, "Invalid album selected.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    //DOWNLOAD LAHAT NG IMAGE SA ALBUM
    private fun downloadAllImages(images: List<String>) {
        for (imageUrl in images) {
            val uri = Uri.parse(imageUrl)
            val request = DownloadManager.Request(uri)
            request.setTitle("SnapVault")
            request.setDescription("Downloading $imageUrl")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.lastPathSegment)

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        }

        Toast.makeText(this, "Downloading images...", Toast.LENGTH_SHORT).show()
    }

    //RECYCLERVIEW STUFF
    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        imageAdapter = ImageAdapter(mutableListOf())
        recyclerView.adapter = imageAdapter
    }

    //ALBUMCOUNT
    private fun fetchAlbumCount(userId: Int) {
        val service = ApiClient.getRetrofitInstance().create(AlbumCountService::class.java)
        service.getAlbumCount(userId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val rawResponse = response.body()?.string() ?: "No response"
                    Log.d("RawResponse", rawResponse)

                    try {
                        val jsonResponse = JSONObject(rawResponse)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val albumCount = jsonResponse.getInt("album_count")
                            Log.d("AlbumCount", "User has $albumCount albums.")
                            // Store album count in a variable or SharedPreferences if needed
                        } else {
                            Log.e("FetchError", "Failed to fetch album count: $status")
                        }
                    } catch (e: Exception) {
                        Log.e("JSONError", "Error parsing JSON response: ${e.message}")
                    }
                } else {
                    Log.e("FetchError", "Failed to fetch album count. Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchError", "Error: ${t.message}")
            }
        })
    }

        //KUKUNIN IMAGES NA NASA ALBUM
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
                            val albumName = jsonResponse.getString("album_name")
                            albumsnameTextView.text = albumName

                            val imagesJsonArray = jsonResponse.getJSONArray("images")
                            val imagesList = mutableListOf<String>()

                            for (i in 0 until imagesJsonArray.length()) {
                                imagesList.add(imagesJsonArray.getString(i).replace("\\/", "/"))
                            }
                            imagesList.reverse()

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

        //ALBUM INFO
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
                            val albumName = jsonResponse.getString("album_name")
                            val albumCreationDate = jsonResponse.optString("creation_date", "Unknown date")

                            albumsnameTextView.text = albumName
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

    //DELETE CONFIRMATION
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Album")
            .setMessage("Are you sure you want to delete this album?")
            .setPositiveButton("Yes") { dialog, _ ->
                // Fetch album count before deleting
                val userId = sharedPreferences.getInt("user_id", -1)
                if (userId != -1) {
                    fetchAlbumCountForDeletion(userId, albumId) // Check album count before deletion
                } else {
                    Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    //KUNIN ALBUM NA IDE DELETE
    private fun fetchAlbumCountForDeletion(userId: Int, albumId: Int) {
        val service = ApiClient.getRetrofitInstance().create(AlbumCountService::class.java)
        service.getAlbumCount(userId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val rawResponse = response.body()?.string() ?: "No response"
                    Log.d("RawResponse", rawResponse)

                    try {
                        val jsonResponse = JSONObject(rawResponse)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val albumCount = jsonResponse.getInt("album_count")
                            if (albumCount > 1) {
                                deleteAlbum(albumId) // Proceed with deletion
                            } else {
                                Toast.makeText(this@AlbumImagesActivity, "Must have at least 1 album.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("FetchError", "Failed to fetch album count: $status")
                        }
                    } catch (e: Exception) {
                        Log.e("JSONError", "Error parsing JSON response: ${e.message}")
                    }
                } else {
                    Log.e("FetchError", "Failed to fetch album count. Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchError", "Error: ${t.message}")
            }
        })
    }

        //DELETION
    private fun deleteAlbum(albumId: Int) {
        val service = ApiClient.getRetrofitInstance().create(AlbumDeleteService::class.java)
        service.deleteAlbum(albumId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val rawResponse = response.body()?.string() ?: "No response"
                    Log.d("RawResponse", rawResponse)

                    try {
                        val jsonResponse = JSONObject(rawResponse)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            Toast.makeText(this@AlbumImagesActivity, "Album deleted successfully.", Toast.LENGTH_SHORT).show()
                            finish() // Close activity after deletion
                        } else {
                            Toast.makeText(this@AlbumImagesActivity, "Failed to delete album.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("JSONError", "Error parsing JSON response: ${e.message}")
                    }
                } else {
                    Log.e("FetchError", "Failed to delete album. Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchError", "Error: ${t.message}")
            }
        })
    }

    private fun showPasswordDialog() {
        // Your existing logic for showing password dialog and adding images goes here
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
