package com.noque.svampeatlas.view_models.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.view_models.NewObservationViewModel

@Suppress("UNCHECKED_CAST")
class NewObservationViewModelFactory(val type: AddObservationFragment.Context, val id: Long, private val mushroomId: Int, private val imageFilePath: String?):
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NewObservationViewModel(type, id, mushroomId, imageFilePath) as T
    }

}