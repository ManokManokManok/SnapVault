package com.example.snapvault_mk1

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlbumAdapter(
    private val albums: List<Album>,
    private val onAlbumClick: (Album) -> Unit // Accept a click listener that takes an Album
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumName: TextView = itemView.findViewById(R.id.album_name)
        val creationDate: TextView = itemView.findViewById(R.id.creation_date)
        val dotIcon: View = itemView.findViewById(R.id.dot_icon) // Reference to the dot icon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.album_item, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.albumName.text = album.album_name
        holder.creationDate.text = album.creation_date

        // Set click listener on the item view
        holder.itemView.setOnClickListener {
            onAlbumClick(album) // Pass the whole Album object to the click listener
        }

        // Show popup menu when dot icon is clicked
        holder.dotIcon.setOnClickListener { view ->
            showPopupMenu(view, album)
        }

    }

    override fun getItemCount(): Int = albums.size

    private fun showPopupMenu(view: View, album: Album) {
        val popupMenu = PopupMenu(view.context, view)
        MenuInflater(view.context).inflate(R.menu.album_options_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.rename -> {
                    showRenameDialog(view.context, album)
                    true
                }
                R.id.add_password -> {
                    showSetPasswordDialog(view.context, album)
                    true
                }
                R.id.delete_album -> {
                    showDeleteConfirmationDialog(view.context, album)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showDeleteConfirmationDialog(context: Context, album: Album) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Album")
            .setMessage("Are you sure you want to delete this album?")
            .setPositiveButton("Delete") { _, _ ->
                deleteAlbum(album.album_id, context)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAlbum(albumId: Int, context: Context) {
        val albumService = ApiClient.getRetrofitInstance().create(Files.AlbumStuff::class.java)
        albumService.deleteAlbum(albumId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Album deleted successfully", Toast.LENGTH_SHORT).show()

                    // Remove album from the local list and update the adapter
                    (albums as MutableList).removeIf { it.album_id == albumId }
                    notifyDataSetChanged()

                    // Optionally, refresh from the server again
                    (context as Files).fetchAlbums(context.userId)
                } else {
                    Toast.makeText(context, "Failed to delete album. Server returned error.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun showRenameDialog(context: Context, album: Album) {
        val builder = AlertDialog.Builder(context)
        val input = EditText(context)
        input.hint = "Enter new album name"
        builder.setView(input)

        builder.setTitle("Rename Album")
            .setPositiveButton("Rename") { _, _ ->
                val newAlbumName = input.text.toString().trim()
                if (newAlbumName.isNotEmpty()) {
                    renameAlbum(album.album_id, newAlbumName, context)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameAlbum(albumId: Int, newAlbumName: String, context: Context) {
        val albumService = ApiClient.getRetrofitInstance().create(Files.AlbumStuff::class.java)
        albumService.renameAlbum(albumId, newAlbumName).enqueue(object : Callback<Album> {
            override fun onResponse(call: Call<Album>, response: Response<Album>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Album renamed successfully", Toast.LENGTH_SHORT).show()
                    (context as Files).fetchAlbums(context.userId) // Refresh album list
                } else {
                    Toast.makeText(context, "Failed to rename album", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Album>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showSetPasswordDialog(context: Context, album: Album) {
        // Create EditTexts for password
        val passwordEditText = EditText(context).apply {
            hint = "Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        // Create the AlertDialog
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Set Album Password")
            .setView(passwordEditText)
            .setPositiveButton("Set") { dialog, _ ->
                val password = passwordEditText.text.toString()
                if (isValidPassword(password)) {
                    // Set the password for the album using your API
                    setAlbumPassword(album.album_id, password, context)
                } else {
                    // Show a message and keep the dialog open
                    Toast.makeText(context, "Invalid password. Only letters and numbers are allowed, with no spaces or special characters.", Toast.LENGTH_SHORT).show()
                    // Reopen the dialog for re-entry
                    dialog.dismiss() // Close the dialog, we won't dismiss it here
                    showSetPasswordDialog(context, album) // Show it again
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    private fun isValidPassword(password: String): Boolean {
        // Check if password is not empty and matches the regex for characters only
        return password.isNotEmpty() && password.all { it.isLetterOrDigit() }
    }

    private fun setAlbumPassword(albumId: Int, password: String, context: Context) {
        val albumService = ApiClient.getRetrofitInstance().create(Files.AlbumStuff::class.java)
        albumService.setAlbumPassword(albumId, password).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Password will be active on restart.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to set password.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
