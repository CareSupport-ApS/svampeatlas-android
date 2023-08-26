package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewInformationBinding

class InformationView(context: Context?, val attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ViewInformationBinding.inflate(LayoutInflater.from(context), this, false)

    fun configure(information: List<Pair<String, String>>) {
        fun addInformation(info: Pair<String, String>) {
            val linearLayout = LinearLayout(context).apply {
                this.layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )

                this.orientation = HORIZONTAL

                val textViewLeft = TextView(context).apply {
                    this.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    TextViewCompat.setTextAppearance(this, R.style.AppPrimary)
                    this.text = info.first
                    this.maxLines = 1
                }

                val textViewRight = TextView(context).apply {
                    this.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )

                    TextViewCompat.setTextAppearance(this, R.style.AppPrimary)
                    this.text = info.second
                    this.maxLines = 1
                    this.gravity = Gravity.END
                }

                this.addView(textViewLeft)
                this.addView(textViewRight)
            }

            binding.informationViewLinearLayout.addView(linearLayout)
        }

        binding.informationViewLinearLayout.removeAllViews()

        information.forEach {
            addInformation(it)
        }
    }
}