package com.noque.svampeatlas.fragments

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.NotebookAdapter
import com.noque.svampeatlas.constants.RELOAD_DATA
import com.noque.svampeatlas.databinding.FragmentNotebookBinding
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.extensions.removeTime
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.fragments.modals.DownloaderFragment
import com.noque.svampeatlas.models.NewObservation
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.SwipeToDeleteCallback
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.NotesFragmentViewModel
import com.noque.svampeatlas.views.MainActivity
import java.util.Date

class NotesFragment: Fragment(R.layout.fragment_notebook), PromptFragment.Listener, MenuProvider {

    // Views
    private val binding by autoClearedViewBinding(FragmentNotebookBinding::bind) {
        it?.notebookFragmentRecyclerView?.adapter = null
        deletedCallback.attachToRecyclerView(null)
    }

    private val notebookAdapter by lazy {
        NotebookAdapter().apply {
            listener = object: NotebookAdapter.Listener {
                override fun newObservationSelected(newObservation: NewObservation) {
                   val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment()
                    action.context = AddObservationFragment.Context.EditNote
                    action.id = newObservation.creationDate.time
                    findNavController().navigate(action)
                }

                override fun uploadNewObservation(newObservation: NewObservation) {
                    val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment()
                    action.context = AddObservationFragment.Context.UploadNote
                    action.id = newObservation.creationDate.time
                    findNavController().navigate(action)
                }
            }
        }
    }

private val deletedCallback by lazy {
    ItemTouchHelper(
        SwipeToDeleteCallback(
            { viewHolder ->
                notebookAdapter.sections.getItem(viewHolder.adapterPosition).let {
                    viewModel.deleteNote((it as NotebookAdapter.Items.Note).newObservation, viewHolder.adapterPosition)
                }
            },
            requireContext(),
            resources
        )
    )
}

    private val viewModel by viewModels<NotesFragmentViewModel>()
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.notebook_fragment_menu, menu)
        menu.findItem(R.id.menu_notebookFragment_addEntry)?.let {
            it.actionView?.findViewById<Button>(R.id.actionView_addNotebookEntry)?.apply {
                setOnClickListener {
                    val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment().setContext(AddObservationFragment.Context.Note)
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.menu_notebookFragment_redownloadOffline)
            DownloaderFragment().show(parentFragmentManager, null)
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupViewModel()
    }


    private fun setupViews() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.notebookFragmentToolbar)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        deletedCallback.attachToRecyclerView(binding.notebookFragmentRecyclerView)
        binding.notebookFragmentRecyclerView.apply {
            adapter = notebookAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
            RELOAD_DATA)?.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.getNotes()
            }
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>(RELOAD_DATA)
        })
    }


    private fun setupViewModel() {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                NotesFragmentViewModel.Event.DownloadTaxon -> {
                    val dialog = PromptFragment()
                    dialog.setTargetFragment(this, 10)
                    dialog.arguments = Bundle().apply {
                        putString(PromptFragment.KEY_TITLE, getString(R.string.prompt_taxonData_title))
                        putString(PromptFragment.KEY_MESSAGE, getString(R.string.prompt_taxonData_message))
                        putString(PromptFragment.KEY_POSITIVE, getString(R.string.action_fetchData))
                        putString(PromptFragment.KEY_NEGATIVE, getString(R.string.action_no))
                    }
                    dialog.show(parentFragmentManager, null)
                }
            }
        }

        viewModel.notes.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    val dateSortedNotes = mutableMapOf<Date, MutableList<NewObservation>>()
                    it.items.forEach { note ->
                        if (dateSortedNotes.containsKey(note.creationDate.removeTime())) {
                            dateSortedNotes[note.creationDate.removeTime()]?.add(note)
                        } else {
                            dateSortedNotes[note.creationDate.removeTime()] = mutableListOf(note)
                        }
                    }
                    notebookAdapter.setSections(dateSortedNotes.toSortedMap().map { Section<NotebookAdapter.Items>(resources.getString(R.string.note_createdDate, it.key.toReadableDate(
                        recentFormatting = true,
                        ignoreTime = true
                    )), State.Items(it.value.map { NotebookAdapter.Items.Note(it) }))}.reversed())
                }
                is State.Empty -> notebookAdapter.setSections(listOf(Section(null, State.Empty())))
                is State.Loading -> notebookAdapter.setSections(listOf(Section(null, State.Loading())))
                is State.Error -> notebookAdapter.setSections(listOf(Section(null, State.Error(it.error))))
            }

        })
    }

    override fun positiveButtonPressed() {
        DownloaderFragment().show(parentFragmentManager, null)
    }
    override fun negativeButtonPressed() {}
}