package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.ResultsAdapter
import com.noque.svampeatlas.databinding.ViewResultsBinding
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Prediction

class ResultsView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ViewResultsBinding.inflate(LayoutInflater.from(context), this, true)

    private val resultsAdapter by lazy { ResultsAdapter() }

    init {
        setupViews()
    }

    private fun setupViews() {
        binding.resultsViewRecyclerView.apply {
            adapter = resultsAdapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    fun setListener(listener: ResultsAdapter.Listener) {
        resultsAdapter.setListener(listener)
    }

    fun showResults(results: List<Prediction>, predictable: Boolean) {
        resultsAdapter.configure(results, predictable)
        binding.resultsViewRecyclerView.scrollTo(0,0)
        binding.resultsViewRecyclerView.layoutManager?.scrollToPosition(0)

        binding.resultsViewRecyclerView.animate().alpha(1F).setDuration(1000).start()
    }

    fun showError(error: AppError) {
        resultsAdapter.configure(error)
        binding.resultsViewRecyclerView.animate().alpha(1F).setDuration(1000).start()
    }

    fun reset() {
        binding.resultsViewRecyclerView.animate().alpha(0F).setDuration(1000).start()
    }
}