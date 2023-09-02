package com.noque.svampeatlas.adapters.add_observation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemAddImageBinding
import com.noque.svampeatlas.databinding.ItemAddImageExpandedBinding
import com.noque.svampeatlas.databinding.ItemAddedImageBinding
import com.noque.svampeatlas.models.UserObservation
import com.noque.svampeatlas.view_holders.AddImageExpandedViewHolder
import com.noque.svampeatlas.view_holders.AddImageViewHolder
import com.noque.svampeatlas.view_holders.AddedImageViewHolder

class AddImagesAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val TAG = "AddImagesAdapter"
    }

    enum class ViewType {
        ADDIMAGEVIEW,
        ADDIMAGEVIEWEXPANDED,
        ADDEDIMAGEVIEW;

        companion object {
            val values = values()
        }
    }

    var addImageButtonClicked: (() -> Unit)? = null

    private var images = mutableListOf<UserObservation.Image>()

    private val onClickListener = View.OnClickListener { addImageButtonClicked?.invoke() }

    fun configure(images: List<UserObservation.Image>) {
        this.images = images.toMutableList()
        notifyDataSetChanged()
    }

    fun addImage(image: UserObservation.Image) {
        this.images.add(image)
        this.notifyItemInserted(images.lastIndex)
    }

    fun removeImage(atIndex: Int) {
        this.images.removeAt(atIndex)
        this.notifyItemRemoved(atIndex)
    }


    override fun getItemViewType(position: Int): Int {
        if (position < images.count()) {
            return ViewType.ADDEDIMAGEVIEW.ordinal
        } else {
            return if (images.isEmpty()) ViewType.ADDIMAGEVIEWEXPANDED.ordinal else ViewType.ADDIMAGEVIEW.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        when (ViewType.values[viewType]) {
            ViewType.ADDIMAGEVIEW -> {
                val binding = ItemAddImageBinding.inflate(layoutInflater, parent, false)
                return  AddImageViewHolder(binding).apply { itemView.setOnClickListener(onClickListener)  }
            }

            ViewType.ADDIMAGEVIEWEXPANDED -> {
                val binding = ItemAddImageExpandedBinding.inflate(layoutInflater, parent, false)
                return AddImageExpandedViewHolder(binding).apply { itemView.setOnClickListener(onClickListener) }
            }

            ViewType.ADDEDIMAGEVIEW -> {
                val binding = ItemAddedImageBinding.inflate(layoutInflater, parent, false)
                return AddedImageViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int {
        return images.count() + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < images.count()) {
            (holder as? AddedImageViewHolder)?.configure(images[position])
        } else { }
    }
}