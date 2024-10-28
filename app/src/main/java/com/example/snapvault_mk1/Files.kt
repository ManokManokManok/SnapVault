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
import okhttp3.ResponseBody
import org.json.JSONObject
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

        @FormUrlEncoded
        @POST("rename_album.php")
        fun renameAlbum(
            @Field("album_id") albumId: Int,
            @Field("new_album_name") newAlbumName: String
        ): Call<Album>

        @FormUrlEncoded
        @POST("set_album_password.php") // Ensure this matches your PHP script path
        fun setAlbumPassword(
            @Field("album_id") albumId: Int,
            @Field("password") password: String // Include the password as a field
        ): Call<Void>
    }

    interface CreateAlbumService {
        @FormUrlEncoded
        @POST("create_album.php")
        fun createAlbum(
            @Field("user_id") userId: Int,
            @Field("album_name") albumName: String
        ): Call<ResponseBody> // Changed to ResponseBody for parsing JSON
    }

    private lateinit var homeIcon: ImageView
    private lateinit var fileIcon: ImageView
    private lateinit var personIcon: ImageView
    private lateinit var newalbum: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var albumRecyclerView: RecyclerView
    var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        userId = sharedPreferences.getInt("user_id", -1)
        val username = sharedPreferences.getString("username", "User")

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
            showCreateAlbumDialog()
        }

        if (userId != -1) {
            fetchAlbums(userId)
        } else {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = sharedPreferences.getInt("user_id", -1)
        Log.d("Files", "userId in onResume: $userId")
        if (userId != -1) {
            fetchAlbums(userId) // Fetch albums again to get the latest data, including passwords
        }
    }

    fun fetchAlbums(userId: Int) {
        val albumService = ApiClient.getRetrofitInstance().create(AlbumStuff::class.java)

        albumService.getUserAlbums(userId).enqueue(object : Callback<List<Album>> {
            override fun onResponse(call: Call<List<Album>>, response: Response<List<Album>>) {
                if (response.isSuccessful) {
                    val albums = response.body()
                    if (albums != null && albums.isNotEmpty()) {
                        // Update the adapter with the new album list
                        val adapter = AlbumAdapter(albums) { album -> onAlbumClick(album) }
                        albumRecyclerView.adapter = adapter
                    } else {
                        // Handle case where there are no albums
                        Toast.makeText(this@Files, "You have no albums yet. Create one!", Toast.LENGTH_SHORT).show()
                        albumRecyclerView.adapter = null // Clear the adapter
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
        if (album.album_password != null) {
            showPasswordDialog(album)
        } else {
            openAlbumImagesActivity(album.album_id)
        }
    }

    private fun showPasswordDialog(album: Album) {
        val passwordEditText = EditText(this).apply {
            hint = "Enter album password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Album Password")
            .setView(passwordEditText)
            .setPositiveButton("OK", null) // Set to null to handle manually
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val enteredPassword = passwordEditText.text.toString()
                if (enteredPassword == album.album_password) {
                    openAlbumImagesActivity(album.album_id)
                    dialog.dismiss() // Dismiss the dialog if password is correct
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                    // Keep the dialog open
                }
            }
        }

        dialog.show()
    }

    private fun openAlbumImagesActivity(albumId: Int) {
        val intent = Intent(this, AlbumImagesActivity::class.java)
        intent.putExtra("albumId", albumId)
        startActivity(intent)
    }

    private fun showCreateAlbumDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_album, null)
        val albumNameEditText = dialogView.findViewById<EditText>(R.id.album_name_edittext)

        builder.setTitle("Create New Album with SnapVault")
            .setView(dialogView)
            .setPositiveButton("Create") { dialog, _ -> }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val createButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            createButton.setOnClickListener {
                val albumName = albumNameEditText.text.toString().trim()
                if (albumName.length > 25) {
                    Toast.makeText(this, "Album name too long. Max 25 characters allowed.", Toast.LENGTH_SHORT).show()
                } else if (isValidAlbumName(albumName)) {
                    createAlbum(userId, albumName, dialog) // Pass dialog for dismissal
                } else {
                    Toast.makeText(this, "Invalid album name. Please use letters and numbers only.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun createAlbum(userId: Int, albumName: String, dialog: AlertDialog) {
        val createAlbumService = ApiClient.getRetrofitInstance().create(CreateAlbumService::class.java)

        createAlbumService.createAlbum(userId, albumName).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val rawResponse = response.body()?.string() ?: "No response"
                    val jsonResponse = JSONObject(rawResponse)
                    val status = jsonResponse.getString("status")

                    if (status == "success") {
                        // Get the newly created album ID
                        val newAlbumId = jsonResponse.getInt("album_id") // Assuming your API returns this
                        Toast.makeText(this@Files, "Album created successfully.", Toast.LENGTH_SHORT).show()

                        // Navigate to AlbumImagesActivity
                        val intent = Intent(this@Files, AlbumImagesActivity::class.java)
                        intent.putExtra("albumId", newAlbumId) // Pass the new album ID
                        startActivity(intent)
                        finish() // Optionally finish the current activity
                    } else {
                        Toast.makeText(this@Files, "Failed to create album.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Files, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss() // Dismiss the dialog after processing
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@Files, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                dialog.dismiss() // Dismiss the dialog even on failure
            }
        })
    }

    private fun isValidAlbumName(albumName: String): Boolean {
        return albumName.all { it.isLetterOrDigit() || it.isWhitespace() }
    }
}
