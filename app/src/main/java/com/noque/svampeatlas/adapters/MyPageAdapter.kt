package com.noque.svampeatlas.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemErrorBinding
import com.noque.svampeatlas.databinding.ItemHeaderBinding
import com.noque.svampeatlas.databinding.ItemLoaderBinding
import com.noque.svampeatlas.databinding.ItemNotificationBinding
import com.noque.svampeatlas.databinding.ItemObservationBinding
import com.noque.svampeatlas.databinding.ItemReloaderBinding
import com.noque.svampeatlas.extensions.difDays
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.view_holders.*

class MyPageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    companion object {
        val TAG = "MyPageAdapter"
    }

    interface Listener {
        fun observationSelected(observation: Observation)
        fun getAdditionalData(category: Item.Category, atOffset: Int)
        fun notificationSelected(notification: Notification)
    }

    sealed class Item(viewType: ViewType) : com.noque.svampeatlas.models.Item<Item.ViewType>(viewType) {

        enum class ViewType : com.noque.svampeatlas.models.ViewType {
            NOTIFICATION,
            OBSERVATION,
            LOADMORE;

            companion object {
                val values = values()
            }
        }

        enum class Category {
            NOTIFICATIONS,
            OBSERVATIONS
        }

        class Notification(val notification: com.noque.svampeatlas.models.Notification) :
            Item(ViewType.NOTIFICATION)

        class Observation(val observation: com.noque.svampeatlas.models.Observation) :
            Item(ViewType.OBSERVATION)

        class LoadMore(val category: Category, val offset: Int) : Item(ViewType.LOADMORE)
    }

    private val sections = Sections<Item.ViewType, Item>()

    private var listener: Listener? = null

    private var notifications = Section<Item>(null)
    private var observations = Section<Item>(null)


    private val onClickListener = View.OnClickListener { view ->
        when (val viewHolder = view.tag) {
            is ReloaderViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.LoadMore -> {
                        listener?.getAdditionalData(item.category, item.offset)
                    }
                    else -> {}
                }
            }
            is NotificationViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.Notification -> {
                        listener?.notificationSelected(item.notification)
                    }
                    else -> {}
                }
            }

            is ObservationViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.Observation -> {
                        listener?.observationSelected(item.observation)
                    }
                    else -> {}
                }
            }
        }
    }

    init {
        sections.addSection(notifications)
        sections.addSection(observations)
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun configureNotificationsState(state: State<List<Item>>, title: String? = null) {
        this.notifications.setTitle(title)
        this.notifications.setState(state)
        notifyDataSetChanged()
    }

    fun configureObservationsState(state: State<List<Item>>, title: String? = null) {
        this.observations.setTitle(title)
        this.observations.setState(state)
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int = sections.getCount()

    override fun getItemViewType(position: Int): Int = sections.getViewTypeOrdinal(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder: RecyclerView.ViewHolder
        when (sections.getSectionViewType(viewType)) {
            Section.ViewType.HEADER -> {
                val binding = ItemHeaderBinding.inflate(layoutInflater, parent, false)
                viewHolder = HeaderViewHolder(binding)
            }
            Section.ViewType.ERROR -> {
                val binding = ItemErrorBinding.inflate(layoutInflater, parent, false)
                viewHolder = ErrorViewHolder(binding)
            }
            Section.ViewType.LOADER -> {
                val binding = ItemReloaderBinding.inflate(layoutInflater, parent, false)
                viewHolder = ReloaderViewHolder(binding)
            }
            Section.ViewType.ITEM -> {
                viewHolder = when (Item.ViewType.values[viewType - Section.ViewType.values.count()]) {
                    Item.ViewType.NOTIFICATION -> {
                        val binding = ItemNotificationBinding.inflate(layoutInflater, parent, false)
                        NotificationViewHolder(binding)
                    }

                    Item.ViewType.OBSERVATION -> {
                        val binding = ItemObservationBinding.inflate(layoutInflater, parent, false)
                        ObservationViewHolder(binding)
                    }

                    Item.ViewType.LOADMORE -> {
                        val binding = ItemReloaderBinding.inflate(layoutInflater, parent, false)
                        ReloaderViewHolder(binding)
                    }
                }
            }
        }

        return viewHolder
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> { sections.getTitle(position)?.let { holder.configure(it) } }
            is ObservationViewHolder -> { when (val item = sections.getItem(position)) {
                is Item.Observation -> { holder.configure(item.observation, true) }
                else -> {}
            } }

            is NotificationViewHolder -> {when (val item = sections.getItem(position)) {
                is Item.Notification -> { holder.configure(item.notification) }
                else -> {}
            }}

            is ErrorViewHolder -> { sections.getError(position)?.let { holder.configure(it) } }

            is ReloaderViewHolder -> { holder.configure(ReloaderViewHolder.Type.LOAD) }
        }
    }
}
