package com.example.snapvault_mk1

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter(private val images: MutableList<String>) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView) // Ensure this ID matches your layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false) // Ensure this layout exists
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = images[position]
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.imageView)

        // Set a click listener to open the image viewer
        holder.imageView.setOnClickListener {
            val context = holder.itemView.context
            Log.d("ImageAdapter", "Image URL clicked: $imageUrl")
            val intent = Intent(context, ImageViewerActivity::class.java)
            intent.putExtra("imageUri", imageUrl) // Pass the image URL as an extra
            context.startActivity(intent) // Start the ImageViewerActivity
        }
    }

    override fun getItemCount(): Int = images.size

    // Update the images and notify the adapter
    fun updateImages(newImages: List<String>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    // New method to add a single image and notify the adapter
    fun addImage(imageUrl: String) {
        images.add(imageUrl)
        notifyItemInserted(images.size - 1) // Notify that a new item was added
    }

    // New method to clear images and notify the adapter
    fun clearImages() {
        images.clear()
        notifyDataSetChanged()
    }
}
