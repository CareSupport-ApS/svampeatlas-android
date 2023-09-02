package com.noque.svampeatlas.adapters.add_observation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.BaseAdapter
import com.noque.svampeatlas.databinding.ItemCautionBinding
import com.noque.svampeatlas.databinding.ItemCreditationBinding
import com.noque.svampeatlas.databinding.ItemResultBinding
import com.noque.svampeatlas.databinding.ItemSelectedResultBinding
import com.noque.svampeatlas.databinding.ItemUnknownSpeciesBinding
import com.noque.svampeatlas.models.DeterminationConfidence
import com.noque.svampeatlas.models.Item
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.models.ViewType
import com.noque.svampeatlas.view_holders.CautionViewHolder
import com.noque.svampeatlas.view_holders.CreditationViewHolder
import com.noque.svampeatlas.view_holders.ResultItemViewHolder
import com.noque.svampeatlas.view_holders.SelectedResultItemViewHolder
import com.noque.svampeatlas.view_holders.UnknownSpeciesViewHolder

 class SpeciesAdapter: BaseAdapter<SpeciesAdapter.AdapterItem, SpeciesAdapter.AdapterItem.ViewTypes>() {

    interface Listener {
        fun mushroomSelected(mushroom: Mushroom)
        fun confidenceSet(confidence: DeterminationConfidence)
        fun deselectPressed()
    }

    companion object {
        val TAG = "SpeciesAdapter"
        val DEFAULT_MUSHROOM =
            Mushroom(60212, "Fungi Sp.", null,  null, null, 0, null, null, null,null, null, null, null)
    }

    sealed class AdapterItem(viewType: ViewTypes) : Item<AdapterItem.ViewTypes>(viewType) {
        class Caution: AdapterItem(ViewTypes.CAUTIONCELL)
        class Creditation: AdapterItem(ViewTypes.CREDITATIONCELL)
        class UnknownSpecies: AdapterItem(ViewTypes.UNKNOWNSPECIES)
        class SelectableMushroom(val mushroom: Mushroom, val score: Double? = null) : AdapterItem(ViewTypes.SELECTABLE)
        class SelectedMushroom(
            val mushroom: Mushroom,
            val confidence: DeterminationConfidence
        ) : AdapterItem(ViewTypes.SELECTEDSPECIES)

        enum class ViewTypes : ViewType {
            UNKNOWNSPECIES,
            SELECTEDSPECIES,
            SELECTABLE,
            CAUTIONCELL,
            CREDITATIONCELL;
        }
    }

    private val upperSection = Section<AdapterItem>(null)
    private val middleSection = Section<AdapterItem>(null)
    private val lowerSection = Section<AdapterItem>(null)

    private var listener: Listener? = null

    override val onClickListener = View.OnClickListener { view ->
        when (val viewHolder = view.tag) {
            is SelectedResultItemViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is AdapterItem.SelectedMushroom -> {
                        listener?.mushroomSelected(item.mushroom)
                    }
                    else -> {}
                }
            }
            is UnknownSpeciesViewHolder -> {
                listener?.mushroomSelected(DEFAULT_MUSHROOM)
            }
            is ResultItemViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is AdapterItem.SelectableMushroom -> {
                        listener?.mushroomSelected(item.mushroom)
                    }
                    else -> {}
                }
            }
        }
    }

    init {
        sections.addSection(upperSection)
        sections.addSection(middleSection)
        sections.addSection(lowerSection)
    }


    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun configureUpperSection(state: State<List<AdapterItem>>, title: String?) {
        upperSection.setTitle(title)
        upperSection.setState(state)
        notifyDataSetChanged()
    }

    fun configureMiddleSectionState(state: State<List<AdapterItem>>, title: String?) {
        middleSection.setTitle(title)
        middleSection.setState(state)
        notifyDataSetChanged()
    }

    fun configureLowerSectionState(state: State<List<AdapterItem>>, title: String?) {
        lowerSection.setTitle(title)
        lowerSection.setState(state)
        notifyDataSetChanged()
    }

    override fun createViewTypeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewTypeOrdinal: Int
    ): RecyclerView.ViewHolder {
        when (AdapterItem.ViewTypes.values()[viewTypeOrdinal]) {
            AdapterItem.ViewTypes.UNKNOWNSPECIES -> {
                val binding = ItemUnknownSpeciesBinding.inflate(inflater, parent, false)
                return UnknownSpeciesViewHolder(binding)
            }
            AdapterItem.ViewTypes.SELECTEDSPECIES -> {
                val binding = ItemSelectedResultBinding.inflate(inflater, parent, false)
                return SelectedResultItemViewHolder(binding).apply {
                    confidenceSet = { listener?.confidenceSet(it) }
                    deselectClicked = { listener?.deselectPressed() }
                }
            }
            AdapterItem.ViewTypes.SELECTABLE -> {
                val binding = ItemResultBinding.inflate(inflater, parent, false)
                return ResultItemViewHolder(binding)
            }
            AdapterItem.ViewTypes.CAUTIONCELL -> {
                val binding = ItemCautionBinding.inflate(inflater, parent, false)
                return CautionViewHolder(binding)
            }
            AdapterItem.ViewTypes.CREDITATIONCELL -> {
                val binding = ItemCreditationBinding.inflate(inflater, parent, false)
                return CreditationViewHolder(binding).apply {
                    configure(CreditationViewHolder.Type.AINEWOBSERVATION)
                }
            }
        }
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: AdapterItem) {
        when (holder) {
            is ResultItemViewHolder -> {
                when (item) {
                    is AdapterItem.SelectableMushroom -> {
                        holder.configure(item.mushroom)
                    }
                    else -> {}
                }
            }

            is SelectedResultItemViewHolder -> {
                when (item) {
                    is AdapterItem.SelectedMushroom -> {
                        holder.configure(item.mushroom, item.confidence)
                    }
                    else -> {}
                }
            }
        }
    }
}