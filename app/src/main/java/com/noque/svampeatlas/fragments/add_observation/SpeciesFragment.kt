package com.noque.svampeatlas.fragments.add_observation

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.add_observation.SpeciesAdapter
import com.noque.svampeatlas.databinding.FragmentAddObservationSpecieBinding
import com.noque.svampeatlas.fragments.AddObservationFragmentDirections
import com.noque.svampeatlas.fragments.DetailsFragment
import com.noque.svampeatlas.models.DeterminationConfidence
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Prediction
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.MushroomsViewModel
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.factories.MushroomsViewModelFactory
import com.noque.svampeatlas.views.SearchBarListener

class SpeciesFragment : Fragment() {

    // Objects
    private var defaultState: Boolean = true

    // Views
    private val binding by autoClearedViewBinding(FragmentAddObservationSpecieBinding::bind) {
        it?.addObservationSpecieFragmentRecyclerView?.adapter = null
        it?.addObservationSpecieFragmentSearchBarView?.setListener(null)
    }

    // View models
    private val mushroomViewModel by lazy {
        ViewModelProvider(this, MushroomsViewModelFactory(
           MushroomsViewModel.Category.FAVORITES, requireActivity().application
        )).get(MushroomsViewModel::class.java)
    }

    private val newObservationViewModel: NewObservationViewModel by navGraphViewModels(R.id.add_observation_nav)
    // Adapters

    private val speciesAdapter: SpeciesAdapter by lazy {
        val adapter = SpeciesAdapter()

        adapter.setListener(object : SpeciesAdapter.Listener {
            override fun mushroomSelected(mushroom: Mushroom) {
                if (mushroom.id == newObservationViewModel.mushroom.value?.first?.id) {
                    val action =
                        AddObservationFragmentDirections.actionGlobalMushroomDetailsFragment(
                            mushroom.id,
                            DetailsFragment.TakesSelection.DESELECT,
                            DetailsFragment.Context.SPECIES,
                            null,
                            null
                        )
                    findNavController().navigate(action)
                } else {
                    val action =
                        AddObservationFragmentDirections.actionGlobalMushroomDetailsFragment(
                            mushroom.id,
                            DetailsFragment.TakesSelection.SELECT,
                            DetailsFragment.Context.SPECIES,
                            null,
                            null
                        )
                    findNavController().navigate(action)
                }
            }

            override fun confidenceSet(confidence: DeterminationConfidence) {
                newObservationViewModel.setConfidence(confidence)
            }

            override fun deselectPressed() {
                newObservationViewModel.setMushroom(null)
            }
        })

        adapter
    }

    // Listeners

    private val searchBarListener = object : SearchBarListener {
        override fun newSearch(entry: String) {
            defaultState = false
            if (!SharedPreferences.databasePresent()) {
                mushroomViewModel.search(entry, detailed = false, allowGenus = true)
            } else {
                mushroomViewModel.searchOffline(entry)
            }
        }

        override fun clearedSearchEntry() {
            defaultState()
        }
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (!recyclerView.canScrollVertically(-1)) {
                binding.addObservationSpecieFragmentSearchBarView.expand()
            } else if (dy > 0) {
                binding.addObservationSpecieFragmentSearchBarView.collapse()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return inflater.inflate(R.layout.fragment_add_observation_specie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupViewModels()
    }


    private fun setupView() {
        binding.addObservationSpecieFragmentSearchBarView.apply {
            setListener(searchBarListener)
        }

        binding.addObservationSpecieFragmentRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = speciesAdapter
            addOnScrollListener(onScrollListener)
        }
    }

    private fun configureLowerSection(state: State<List<Prediction>>) {
        //If a mushroom is selected we should not show this
        if (newObservationViewModel.mushroom.value != null) return
        when (state) {
            is State.Items -> {
                var highestConfidence = 0.0
                state.items.forEach {
                    if (it.score > highestConfidence) {
                        highestConfidence = it.score * 100
                    }
                }

                val items = mutableListOf<SpeciesAdapter.AdapterItem>()

                if (highestConfidence < 50.0) {
                    items.add(SpeciesAdapter.AdapterItem.Caution())
                }

                items.addAll(state.items.map { SpeciesAdapter.AdapterItem.SelectableMushroom(it.mushroom, it.score) })
                items.add(SpeciesAdapter.AdapterItem.Creditation())
                speciesAdapter.configureLowerSectionState(State.Items(items), getString(R.string.observationSpeciesCell_predictionsHeader))
            }

            is State.Error -> {
                speciesAdapter.configureLowerSectionState(State.Error(state.error), getString(R.string.observationSpeciesCell_predictionsHeader))
            }

            is State.Loading -> {
                speciesAdapter.configureLowerSectionState(State.Loading(), getString(R.string.observationSpeciesCell_predictionsHeader))
            }

            is State.Empty -> {
                speciesAdapter.configureLowerSectionState(State.Empty(), null)
            }
        }
    }

    private fun configureMiddleSection(state: State<List<Mushroom>>) {
        // We only want the mushroomState to to anything to the UI if no mushroom has been selected
        if (newObservationViewModel.mushroom.value != null) return

        when (state) {
            is State.Items -> {
                val items = state.items.map { SpeciesAdapter.AdapterItem.SelectableMushroom(it) }

                if (defaultState) {
                    speciesAdapter.configureMiddleSectionState(State.Items(items), getString(R.string.observationSpeciesCell_myFavorites))
                } else {
                    speciesAdapter.configureMiddleSectionState(State.Items(items), getString(R.string.observationSpeciesCell_searchResults))
                }
            }

            is State.Loading -> {
                if (defaultState) {
                    speciesAdapter.configureMiddleSectionState(State.Loading(), getString(R.string.observationSpeciesCell_myFavorites))
                } else {
                    speciesAdapter.configureMiddleSectionState(State.Loading(), getString(R.string.observationSpeciesCell_searchResults))
                }
            }

            is State.Error -> {
                if (defaultState) {
                    speciesAdapter.configureMiddleSectionState(State.Empty(), null)
                } else {
                    speciesAdapter.configureMiddleSectionState(State.Error(state.error), getString(R.string.observationSpeciesCell_searchResults))
                }
            }

            is State.Empty -> {
                defaultState()
            }
        }
    }

    private fun setupViewModels() {
//        newObservationViewModel.event.observe(viewLifecycleOwner) {
//            when (it) {
//                is NewObservationViewModel.Event.Reset -> binding.addObservationSpecieFragmentSearchBarView.resetText()
//                else -> {}
//            }
//        }

        newObservationViewModel.mushroom.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.addObservationSpecieFragmentRecyclerView.setPadding(0, 0, 0, 0)
                binding.addObservationSpecieFragmentSearchBarView.visibility = View.GONE
                speciesAdapter.configureUpperSection(
                    State.Items(
                        listOf(
                            SpeciesAdapter.AdapterItem.SelectedMushroom(
                                it.first,
                                it.second
                            )
                        )
                    ),
                    if (it.first.isGenus) getString(R.string.observationSpeciesCell_choosenGenus) else getString(
                        R.string.observationSpeciesCell_choosenSpecies
                    )
                )
                speciesAdapter.configureMiddleSectionState(State.Empty(), null)
                speciesAdapter.configureLowerSectionState(State.Empty(), null)
            } else {
                binding.addObservationSpecieFragmentRecyclerView.setPadding(
                    0,
                    (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(
                        R.dimen.searchbar_top_margin
                    )).toInt(),
                    0,
                    0
                )
                binding.addObservationSpecieFragmentSearchBarView.visibility = View.VISIBLE
                binding.addObservationSpecieFragmentSearchBarView.expand()
                speciesAdapter.configureUpperSection(
                    State.Items(
                        listOf(
                            SpeciesAdapter.AdapterItem.UnknownSpecies()
                        )
                    ),
                    null
                )
                configureMiddleSection(mushroomViewModel.mushroomsState.value ?: State.Empty())
                configureLowerSection(
                    newObservationViewModel.predictionResultsState.value ?: State.Empty()
                )
            }

            binding.addObservationSpecieFragmentRecyclerView.scrollToPosition(0)
        }

        newObservationViewModel.predictionResultsState.observe(viewLifecycleOwner, Observer {
                configureLowerSection(it)
            })

        mushroomViewModel.mushroomsState.observe(viewLifecycleOwner, Observer {
            configureMiddleSection(it)
        })
    }

    private fun defaultState() {
        binding.addObservationSpecieFragmentSearchBarView.resetText()
        defaultState = true
        mushroomViewModel.selectCategory(MushroomsViewModel.Category.FAVORITES, true)
    }
}