package com.example.snapvault_mk1

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.EditText
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
                    // Handle add password action here
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
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
}
