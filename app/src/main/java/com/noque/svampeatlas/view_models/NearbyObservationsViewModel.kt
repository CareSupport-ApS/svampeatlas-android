package com.noque.svampeatlas.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.fragments.NearbyFragment
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.api.Geometry

class NearbyObservationsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "NearbyObservationsViewModel"
        const val defaultRadius = 1000
        const val defaultAgeInYears = 1
    }

    private val observations = mutableListOf<Observation>()
    private val geometries = mutableListOf<Geometry>()

    private val _observationsState by lazy { MutableLiveData<State<Pair<List<Observation>, List<Geometry>>>>() }
    private val _radius = MutableLiveData(1000)
    private val _ageInYears = MutableLiveData(1)

     val radius: LiveData<Int> = _radius
     val ageInYears: LiveData<Int> = _ageInYears
    val observationsState: LiveData<State<Pair<List<Observation>, List<Geometry>>>> get() = _observationsState

    init {
        _observationsState.value = State.Empty()
    }

    fun reset(clearAll: Boolean) {
        if (clearAll) {
            observations.clear()
            geometries.clear()
        }

        _observationsState.value = State.Empty()
    }

    fun getObservationsNearby(latLng: LatLng) {
        _observationsState.value = State.Loading()

        val geometry = Geometry(latLng, radius.value ?: defaultRadius, Geometry.Type.CIRCLE)

        DataService.getObservationsWithin(TAG, geometry, null, ageInYears.value ?: defaultAgeInYears) {
            it.onSuccess {
                observations.addAll(it)
                geometries.add(geometry)
                _observationsState.value = State.Items(Pair(observations, geometries))
            }

            it.onError {
                _observationsState.value = State.Error(it)
            }
        }
    }

    fun setRadius(newRadius: Int) {
        _radius.value = newRadius
    }

    fun setAgeInYears(newAge: Int) {
        _ageInYears.value = newAge
    }
}