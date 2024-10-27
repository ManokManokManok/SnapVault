package com.example.snapvault_mk1

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

class Files : AppCompatActivity() {

    interface AlbumStuff {
        @FormUrlEncoded
        @POST("get_user_albums.php")
        fun getUserAlbums(@Field("user_id") userId: Int): Call<List<Album>>
    }

    interface CreateAlbumService {
        @FormUrlEncoded
        @POST("create_album.php") // Update this with your actual create album endpoint
        fun createAlbum(
            @Field("user_id") userId: Int,
            @Field("album_name") albumName: String
        ): Call<Album>
    }

    private lateinit var homeIcon: ImageView
    private lateinit var fileIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var newalbum: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var albumRecyclerView: RecyclerView
    private var userId: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)


        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        userId = sharedPreferences.getInt("user_id", -1)
        val username = sharedPreferences.getString("username", "User") // Retrieve username


        findViewById<TextView>(R.id.welcomeTextView).text = "$username's Albums"


        homeIcon = findViewById(R.id.home)
        fileIcon = findViewById(R.id.folder)
        personIcon = findViewById(R.id.person)
        newalbum = findViewById(R.id.newalbum)
        scrollView = findViewById(R.id.scrollView)
        albumRecyclerView = findViewById(R.id.recyclerView)

        albumRecyclerView.layoutManager = LinearLayoutManager(this)

        homeIcon.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        personIcon.setOnClickListener {
            startActivity(Intent(this, User::class.java))
        }

        newalbum.setOnClickListener {
            showCreateAlbumDialog() // Call the dialog function when the new album icon is clicked
        }

        if (userId != -1) {
            fetchAlbums(userId)
        } else {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val user_Id = sharedPreferences.getInt("user_id", -1)

        if (user_Id != -1) {
            fetchAlbums(user_Id) // Fetch images again when returning to the activity
        }
    }

    private fun fetchAlbums(userId: Int) {
        val albumService = ApiClient.getRetrofitInstance().create(AlbumStuff::class.java)

        albumService.getUserAlbums(userId).enqueue(object : Callback<List<Album>> {
            override fun onResponse(call: Call<List<Album>>, response: Response<List<Album>>) {
                if (response.isSuccessful) {
                    val albums = response.body()
                    if (!albums.isNullOrEmpty()) {
                        val adapter = AlbumAdapter(albums) { album -> // Accept Album in lambda
                            // Handle album click, passing the entire Album object
                            onAlbumClick(album)
                        }
                        albumRecyclerView.adapter = adapter
                    } else {
                        Toast.makeText(this@Files, "No albums found.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Files, "Failed to retrieve albums.", Toast.LENGTH_SHORT).show()
                    Log.e("Files", "Response error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Album>>, t: Throwable) {
                Toast.makeText(this@Files, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("Files", "Network error: ${t.message}")
            }
        })
    }

    private fun onAlbumClick(album: Album) {
        // Start AlbumImagesActivity and pass the album ID
        val intent = Intent(this, AlbumImagesActivity::class.java)
        intent.putExtra("albumId", album.album_id) // Assuming album_id is the ID of the album
        startActivity(intent)
    }

    private fun showCreateAlbumDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_album, null)
        val albumNameEditText = dialogView.findViewById<EditText>(R.id.album_name_edittext)

        builder.setTitle("Create New Album with SnapVault")
            .setView(dialogView)
            .setPositiveButton("Create") { dialog, _ -> /* Do nothing here, we will handle it manually */ }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create() // Create the dialog but don't show it yet

        dialog.setOnShowListener {
            val createButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            createButton.setOnClickListener {
                val albumName = albumNameEditText.text.toString().trim() // Trim whitespace

                // Check for maximum length of album name
                if (albumName.length > 25) {
                    Toast.makeText(this, "Album name too long. Max 25 characters allowed.", Toast.LENGTH_SHORT).show()
                } else if (isValidAlbumName(albumName)) { // Validate the album name
                    createAlbum(userId, albumName)
                    dialog.dismiss() // Dismiss the dialog only after successful creation
                } else {
                    Toast.makeText(this, "Invalid album name. Please use letters and numbers only.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show() // Show the dialog
    }



    private fun createAlbum(userId: Int, albumName: String) {
        val createAlbumService = ApiClient.getRetrofitInstance().create(CreateAlbumService::class.java)

        createAlbumService.createAlbum(userId, albumName).enqueue(object : Callback<Album> {
            override fun onResponse(call: Call<Album>, response: Response<Album>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@Files, "Album created successfully.", Toast.LENGTH_SHORT).show()
                    fetchAlbums(userId) // Refresh the album list after creation
                } else {
                    Toast.makeText(this@Files, "Failed to create album.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Album>, t: Throwable) {
                Toast.makeText(this@Files, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to validate the album name
    private fun isValidAlbumName(albumName: String): Boolean {
        // Regular expression to check if album name contains only letters, numbers, and spaces
        val regex = Regex("^[a-zA-Z0-9 ]+\$") // Adjusted regex to include only letters, numbers, and spaces
        return albumName.isNotBlank() && regex.matches(albumName) // Ensure it is not blank
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
