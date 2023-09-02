package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemCautionBinding
import com.noque.svampeatlas.databinding.ItemCreditationBinding
import com.noque.svampeatlas.databinding.ItemLoaderBinding
import com.noque.svampeatlas.databinding.ItemReloaderBinding
import com.noque.svampeatlas.databinding.ItemResultBinding
import com.noque.svampeatlas.databinding.ItemTitleBinding
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.view_holders.*

class ResultsAdapter: BaseAdapter<ResultsAdapter.Items, ResultsAdapter.Items.ViewType>() {

    interface Listener {
        fun reloadSelected()
        fun predictionResultSelected(predictionResult: Prediction)
    }

    sealed class Items(viewType: ViewType) : Item<Items.ViewType>(viewType) {
        class Title(val title: Int, val message: Int): Items(ViewType.Title_View)
        class Result(val predictionResult: Prediction): Items(ViewType.RESULTVIEW)
        class TryAgain : Items(ViewType.RETRYVIEW)
        class Caution : Items(ViewType.CAUTIONVIEW)
        class Creditation : Items(ViewType.CREDITATION)

        enum class ViewType : com.noque.svampeatlas.models.ViewType {
            Title_View,
            RESULTVIEW,
            CREDITATION,
            CAUTIONVIEW,
            RETRYVIEW;

            companion object {
                val values = values()
            }
        }
    }
    override val onClickListener = View.OnClickListener {
        when (val viewHolder = it.tag) {
            is ReloaderViewHolder -> {
                listener?.reloadSelected()
            }
            is ResultItemViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Items.Result -> {
                        listener?.predictionResultSelected(item.predictionResult)
                    }
                    else -> {}
                }
            }
        }
    }

    private var listener: Listener? = null

    fun configure(results: List<Prediction>, predictable: Boolean) {
        var highestConfidence = 0.0
        results.forEach {
            if (it.score > highestConfidence) {
                highestConfidence = it.score * 100
            }
        }

        val titleSection: Section<Items> = if (predictable) {
            Section(null, State.Items(listOf(Items.Title(R.string.resultsView_header_title, R.string.resultsView_header_message))))
        } else {
            Section(null, State.Items(listOf(Items.Title(R.string.resultsView_unpredictable_title, R.string.resultsView_unpredictable_message))))
        }

        if (highestConfidence < 50.0) {
            sections.setSections(mutableListOf(
                titleSection,
                Section(null, State.Items(listOf(Items.TryAgain()))),
                Section(null, State.Items(listOf(Items.Caution()))),
                Section(null, State.Items(results.map { Items.Result(it) })),
                Section(null, State.Items(listOf(Items.Creditation())))
            ))
        } else {
            sections.setSections(mutableListOf(
                titleSection,
                Section(null, State.Items(results.map { Items.Result(it) })),
                Section(null, State.Items(listOf(Items.Creditation()))),
                Section(null, State.Items(listOf(Items.TryAgain())))
            ))
        }


        notifyDataSetChanged()
    }

    fun configure(error: AppError) {
        sections.setSections(mutableListOf(
            Section(null, State.Error(error)),
            Section(null, State.Items(listOf(Items.TryAgain())))
        ))

        notifyDataSetChanged()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun createViewTypeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewTypeOrdinal: Int
    ): RecyclerView.ViewHolder {
        return when (Items.ViewType.values[viewTypeOrdinal]) {
            Items.ViewType.RESULTVIEW -> {
                    val binding = ItemResultBinding.inflate(inflater, parent, false)
                    return ResultItemViewHolder(binding)
                    }

            Items.ViewType.CREDITATION -> {
                val binding = ItemCreditationBinding.inflate(inflater, parent, false)
                return CreditationViewHolder(binding)
            }

            Items.ViewType.RETRYVIEW -> {
                val binding = ItemReloaderBinding.inflate(inflater, parent, false)
                        return ReloaderViewHolder(binding)
                    }

            Items.ViewType.CAUTIONVIEW -> {
                val binding = ItemCautionBinding.inflate(inflater, parent, false)
                       return CautionViewHolder(binding)
                    }
            Items.ViewType.Title_View -> {
                        val binding = ItemTitleBinding.inflate(inflater, parent, false)
                        return TitleViewHolder(binding)
                    }
                }
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Items) {
        when (holder) {
            is ResultItemViewHolder -> {
                    when (item) {
                        is Items.Result -> {
                            holder.configure(item.predictionResult.mushroom)
                        }
                        else -> {}
                    }
            }

            is CreditationViewHolder -> {
                holder.configure(CreditationViewHolder.Type.AI)
            }

            is TitleViewHolder -> {
                when (item) {
                    is Items.Title -> holder.configure(item.title, item.message)
                    else -> {}
                }
            }
        }
    }

}