package com.example.snapvault_mk1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlbumAdapter(
    private val albums: List<Album>,
    private val onAlbumClick: (Album) -> Unit // Accept a click listener that takes an Album
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumName: TextView = itemView.findViewById(R.id.album_name)
        val creationDate: TextView = itemView.findViewById(R.id.creation_date)
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
    }

    override fun getItemCount(): Int = albums.size
}
