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
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = images[position]

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ImageViewerActivity::class.java)
            intent.putExtra("imageUri", imageUrl)
            intent.putStringArrayListExtra("imageList", ArrayList(images)) // Pass the list of images
            intent.putExtra("imagePosition", position) // Pass the current position
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = images.size

    fun addImage(imageUrl: String) {
        images.add(imageUrl)
        notifyItemInserted(images.size - 1)
    }

    fun updateImages(newImages: List<String>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }
}
