package com.noque.svampeatlas.fragments.add_observation

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.add_observation.details_picker.HostsAdapter
import com.noque.svampeatlas.adapters.add_observation.details_picker.PickerAdapter
import com.noque.svampeatlas.adapters.add_observation.details_picker.SubstratesAdapter
import com.noque.svampeatlas.adapters.add_observation.details_picker.VegetationTypesAdapter
import com.noque.svampeatlas.databinding.FragmentDetailsPickerBinding
import com.noque.svampeatlas.extensions.capitalized
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.models.Substrate
import com.noque.svampeatlas.models.VegetationType
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.DetailsPickerViewModel
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.factories.DetailsPickerViewModelFactory
import com.noque.svampeatlas.views.SearchBarListener


class DetailsPickerFragment : DialogFragment(R.layout.fragment_details_picker) {
    enum class Type {
        SUBSTRATEPICKER,
        VEGETATIONTYPEPICKER,
        HOSTPICKER
    }

    // Objects
    private val args by navArgs<DetailsPickerFragmentArgs>()

    // Views
    private val binding by autoClearedViewBinding(FragmentDetailsPickerBinding::bind)

    // Adapters
    private val substratesAdapter: SubstratesAdapter by lazy {
        val adapter = SubstratesAdapter()
        adapter.setListener(object: PickerAdapter.Listener<Substrate> {
            override fun itemSelected(item: Substrate) {
                newObservationViewModel.setSubstrate(item, binding.detailsPickerFragmentSwitch.isChecked)
                dismiss()
            }

            override fun itemDeselected(item: Substrate) {}
        })
        adapter
    }

    private val vegetationTypesAdapter: VegetationTypesAdapter by lazy {
        val adapter = VegetationTypesAdapter()

        adapter.setListener(object: PickerAdapter.Listener<VegetationType> {
            override fun itemDeselected(item: VegetationType) {}
            override fun itemSelected(item: VegetationType) {
                newObservationViewModel.setVegetationType(item, binding.detailsPickerFragmentSwitch.isChecked)
                dismiss()
            }

        })
        adapter
    }

    private val hostsAdapter: HostsAdapter by lazy {
        val adapter = HostsAdapter()
        adapter.setListener(object: PickerAdapter.Listener<Host> {
            override fun itemSelected(item: Host) =
                newObservationViewModel.appendHost(item, binding.detailsPickerFragmentSwitch.isChecked)

            override fun itemDeselected(item: Host) =
                newObservationViewModel.removeHost(item, binding.detailsPickerFragmentSwitch.isChecked)
        })

        adapter
    }


    // View models
    private val newObservationViewModel: NewObservationViewModel by navGraphViewModels(R.id.add_observation_nav)

    private val observationDetailsPickerViewModel by lazy {
        ViewModelProvider(this, DetailsPickerViewModelFactory(args.type, requireActivity().application))[DetailsPickerViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)  {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupViewModels()
    }


    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.70).toInt()
        dialog?.window?.setLayout(width, height)
    }

    private fun setupViews() {
        binding.detailsPickerFragmentCancelButton.apply {
            setOnClickListener {
                when (args.type) {
                    Type.HOSTPICKER -> {
                        newObservationViewModel.setHostsLockedState(binding.detailsPickerFragmentSwitch.isChecked)
                    }
                    else -> {}
                }
                dismiss()
            }
        }

        binding.detailsPickerFragmentRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            if (args.type == Type.HOSTPICKER) {
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (!recyclerView.canScrollVertically(-1)) {
                            binding.detailsPickerFragmentSearchBarView.expand()
                        } else if (dy > 0) {
                            binding.detailsPickerFragmentSearchBarView.collapse()
                        }
                    }
                })

            }

            when (args.type) {
                Type.VEGETATIONTYPEPICKER -> {
                    binding.detailsPickerFragmentRecyclerView.adapter = vegetationTypesAdapter
                    binding.detailsPickerFragmentHeaderTextView.text =
                        resources.getString(R.string.detailsPickerFragment_vegetationTypesPicker)
                    binding.detailsPickerFragmentSearchBarView.visibility = View.GONE
                    binding.detailsPickerFragmentRecyclerView.setPadding(0, 0, 0, 0)
                }

                Type.SUBSTRATEPICKER -> {
                    binding.detailsPickerFragmentRecyclerView.adapter = substratesAdapter
                    binding.detailsPickerFragmentHeaderTextView.text =
                        resources.getString(R.string.detailsPickerFragment_substratePicker)
                    binding.detailsPickerFragmentSearchBarView.visibility = View.GONE
                    binding.detailsPickerFragmentRecyclerView.setPadding(0, 0, 0, 0)
                }

                Type.HOSTPICKER -> {
                    binding.detailsPickerFragmentCancelButton.setImageResource(R.drawable.glyph_checkmark)
                    binding.detailsPickerFragmentRecyclerView.adapter = hostsAdapter
                    binding.detailsPickerFragmentHeaderTextView.text =
                        resources.getString(R.string.detailsPickerFragment_hostsPicker)
                    binding.detailsPickerFragmentRecyclerView.setPadding(
                        0,
                        (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(
                            R.dimen.searchbar_top_margin
                        ) * 2).toInt(),
                        0,
                        0
                    )
                    binding.detailsPickerFragmentSearchBarView.apply {
                        visibility = View.VISIBLE
                        setPlaceholder(resources.getString(R.string.searchVC_searchBar_placeholder))
                        setListener(object : SearchBarListener {
                            override fun newSearch(entry: String) {
                                observationDetailsPickerViewModel.getHosts(entry)
                            }

                            override fun clearedSearchEntry() {
                                observationDetailsPickerViewModel.getHosts(null)
                            }
                        })
                    }
                }
            }
        }
    }


    private fun setupViewModels() {
        when (args.type) {
            Type.VEGETATIONTYPEPICKER -> {
                binding.detailsPickerFragmentSwitch.isChecked = newObservationViewModel.vegetationType.value?.second ?: false
            }
            Type.SUBSTRATEPICKER -> {
                binding.detailsPickerFragmentSwitch.isChecked = newObservationViewModel.substrate.value?.second ?: false
            }
            Type.HOSTPICKER -> {
                binding.detailsPickerFragmentSwitch.isChecked = newObservationViewModel.hosts.value?.second ?: false
            }
        }

        observationDetailsPickerViewModel.hostsState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is State.Loading -> {
                    hostsAdapter.configure(listOf(Section(null, State.Loading())))
                }
                is State.Error -> {
                    hostsAdapter.configure(listOf(Section(null, State.Error(state.error))))
                }
                is State.Items -> {
                    if (state.items.second) {
                        val defaultList = state.items.first.filterNot { it.isUserSelected }
                        val previouslyUsed = state.items.first.filter { it.isUserSelected }
                        hostsAdapter.configure(listOf(
                            Section(null, State.Items(previouslyUsed.map { PickerAdapter.PickerItem(it) })),
                            Section(getString(R.string.observationDetailsCell_mostUsed), State.Items(defaultList.map { PickerAdapter.PickerItem(it) }))
                        ), newObservationViewModel.hosts.value?.first ?: listOf())
                    } else {
                        hostsAdapter.configure(listOf(Section(null, State.Items(state.items.first.map { PickerAdapter.PickerItem(it) }))), newObservationViewModel.hosts.value?.first ?: mutableListOf())
                    }
                }
                else -> {}
            }
        })

        observationDetailsPickerViewModel.substrateGroupsState.observe(
            viewLifecycleOwner,
            Observer { state ->
                when (state) {
                    is State.Loading -> { substratesAdapter.configure(listOf(Section(null, State.Loading()))) }
                    is State.Error -> { substratesAdapter.configure(listOf(Section(null, State.Error(state.error)))) }
                    is State.Items -> {
                        val sections = state.items.map {
                            Section.Builder<PickerAdapter.PickerItem<Substrate>>().title(it.localizedName.capitalized()).items(it.substrates.map { PickerAdapter.PickerItem(it) }).build()
                        }

                        substratesAdapter.configure(sections)
                    }
                    else -> {}
                }
            })

        observationDetailsPickerViewModel.vegetationTypesState.observe(
            viewLifecycleOwner,
            Observer { state ->
                when (state) {
                    is State.Loading -> vegetationTypesAdapter.configure(listOf(Section(null, State.Loading())))
                    is State.Error -> vegetationTypesAdapter.configure(listOf(Section(null, State.Error(state.error))))
                    is State.Items -> {
                        vegetationTypesAdapter.configure(
                            listOf(Section.Builder<PickerAdapter.PickerItem<VegetationType>>().items(state.items.map { PickerAdapter.PickerItem(it) }).build())
                        )
                    }
                    else -> {}
                }
            })
    }
}