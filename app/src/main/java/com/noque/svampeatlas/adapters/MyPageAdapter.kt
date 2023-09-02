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

class MyPageAdapter : BaseAdapter<MyPageAdapter.Items, MyPageAdapter.Items.ViewType>() {


    companion object {
        val TAG = "MyPageAdapter"
    }

    interface Listener {
        fun observationSelected(observation: Observation)
        fun getAdditionalData(category: Items.Category, atOffset: Int)
        fun notificationSelected(notification: Notification)
    }

    sealed class Items(viewType: ViewType) : Item<Items.ViewType>(viewType) {

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
            Items(ViewType.NOTIFICATION)

        class Observation(val observation: com.noque.svampeatlas.models.Observation) :
            Items(ViewType.OBSERVATION)

        class LoadMore(val category: Category, val offset: Int) : Items(ViewType.LOADMORE)
    }

    private var listener: Listener? = null

    private var notifications = Section<Items>(null)
    private var observations = Section<Items>(null)


    override val onClickListener = View.OnClickListener { view ->
        when (val viewHolder = view.tag) {
            is ReloaderViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Items.LoadMore -> {
                        listener?.getAdditionalData(item.category, item.offset)
                    }
                    else -> {}
                }
            }
            is NotificationViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Items.Notification -> {
                        listener?.notificationSelected(item.notification)
                    }
                    else -> {}
                }
            }

            is ObservationViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Items.Observation -> {
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

    fun configureNotificationsState(state: State<List<Items>>, title: String? = null) {
        this.notifications.setTitle(title)
        this.notifications.setState(state)
        notifyDataSetChanged()
    }

    fun configureObservationsState(state: State<List<Items>>, title: String? = null) {
        this.observations.setTitle(title)
        this.observations.setState(state)
        notifyDataSetChanged()
    }

    override fun createViewTypeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewTypeOrdinal: Int
    ): RecyclerView.ViewHolder {
        return when (Items.ViewType.values[viewTypeOrdinal]) {
            Items.ViewType.NOTIFICATION -> {
                val binding = ItemNotificationBinding.inflate(inflater, parent, false)
                NotificationViewHolder(binding)
            }

            Items.ViewType.OBSERVATION -> {
                val binding = ItemObservationBinding.inflate(inflater, parent, false)
                ObservationViewHolder(binding)
            }

            Items.ViewType.LOADMORE -> {
                val binding = ItemReloaderBinding.inflate(inflater, parent, false)
                ReloaderViewHolder(binding)
            }
        }
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Items) {
        when (holder) {
            is ObservationViewHolder -> { when (item) {
                is Items.Observation -> { holder.configure(item.observation, true) }
                else -> {}
            } }
            is NotificationViewHolder -> {when (item) {
                is Items.Notification -> { holder.configure(item.notification) }
                else -> {}
            }}
        }
    }
}
