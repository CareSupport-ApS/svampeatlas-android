package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewMushroomBinding
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.services.DataService

class MushroomView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    interface Listener {
        fun onClicked(mushroom: Mushroom)
    }


    private var mushroom: Mushroom? = null
    private var listener: Listener? = null

    private val binding = ViewMushroomBinding.inflate(LayoutInflater.from(context), this, false)
    private lateinit var informationLinearLayout: LinearLayout
    init {
        setupView()
    }

    private fun initViews() {
        informationLinearLayout = LinearLayout(context)
        informationLinearLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                informationLinearLayout.orientation = LinearLayout.VERTICAL
    }

    private fun setupView() {
        binding.mushroomViewLinearLayout.addView(informationLinearLayout)
        clipToOutline = true
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
        this.setOnClickListener {
            mushroom?.let { listener?.onClicked(it) }
        }
    }

    fun round(fully: Boolean) {
        outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                val radius = 45F

                view?.let {
                    if (fully) {
                        outline?.setRoundRect(0,0,view.width, view.height, radius)
                    } else {
                        outline?.setRoundRect(0, 0, (view.width + radius).toInt(), view.height, radius)
                    }
                }
            }
        }
    }

    fun configure(mushroom: Mushroom) {
        this.mushroom = mushroom

        if (!mushroom.images.isNullOrEmpty()) {
            binding.mushroomViewImageView.visibility = View.VISIBLE
            binding.mushroomViewImageView.downloadImage(DataService.ImageSize.MINI, mushroom.images.first().url)
        } else {
            binding.mushroomViewImageView.visibility = View.GONE
        }

        if (mushroom.localizedName != null) {
            binding.mushroomViewPrimaryLabel.text = mushroom.localizedName!!.upperCased()
            binding.mushroomViewSecondaryLabel.visibility = View.VISIBLE
            binding.mushroomViewSecondaryLabel.text = mushroom.fullName.italized()
        } else {
            binding.mushroomViewPrimaryLabel.text = mushroom.fullName.italized()
            binding.mushroomViewSecondaryLabel.visibility = View.GONE
        }

        var information: MutableList<Pair<String, String>> = mutableListOf()

        mushroom.statistics?.acceptedObservationsCount?.let {
            information.add(Pair(resources.getString(R.string.mushroomView_numberOfRecords), it.toString()))
        }

        mushroom.statistics?.lastAcceptedObservationDate?.let {
            information.add(Pair(resources.getString(R.string.mushroomView_latestObservation), it.toReadableDate(true, true)))
        }

        binding.mushroomViewInformationView.configure(information)
    }
}
