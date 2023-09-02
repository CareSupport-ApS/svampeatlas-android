package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemNewObservationBinding
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.view_holders.NoteItemViewHolder

class NotebookAdapter: BaseAdapter<NotebookAdapter.Items, NotebookAdapter.Items.ViewTypes>() {

    interface Listener {
        fun newObservationSelected(newObservation: NewObservation)
        fun uploadNewObservation(newObservation: NewObservation)
    }


    sealed class Items(viewType: ViewTypes) : Item<Items.ViewTypes>(viewType) {
        enum class ViewTypes: ViewType {
            NewObservation
        }

        class Note(val newObservation: NewObservation): Items(ViewTypes.NewObservation)
    }


    var listener: Listener? = null

    override val onClickListener: View.OnClickListener
        get() = View.OnClickListener {
            when (val tag = it.tag) {
                is NoteItemViewHolder -> {
                    (sections.getItem(tag.adapterPosition) as? Items.Note)?.let { listener?.newObservationSelected(it.newObservation) }
                }
            }
        }

    override fun createViewTypeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewTypeOrdinal: Int
    ): RecyclerView.ViewHolder {
        when (Items.ViewTypes.values()[viewTypeOrdinal]) {
            Items.ViewTypes.NewObservation -> {
                val binding = ItemNewObservationBinding.inflate(inflater, parent, false)
                return NoteItemViewHolder(binding)
            }
        }
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Items) {
        when (holder) {
            is NoteItemViewHolder -> (item as? Items.Note)?.let { holder.configure(item.newObservation) {
                listener?.uploadNewObservation(item.newObservation)
            } }
        }
    }
}