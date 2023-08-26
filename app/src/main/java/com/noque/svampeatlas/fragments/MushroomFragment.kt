package com.noque.svampeatlas.fragments


import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.MushroomListAdapter
import com.noque.svampeatlas.databinding.FragmentMushroomBinding
import com.noque.svampeatlas.extensions.changeColor
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.ToastHelper.handleSuccess
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.MushroomsViewModel
import com.noque.svampeatlas.view_models.factories.MushroomsViewModelFactory
import com.noque.svampeatlas.views.MainActivity
import com.noque.svampeatlas.views.SearchBarListener


class MushroomFragment : Fragment() {

    companion object {
        const val TAG = "MushroomFragment"
    }


    // Views

    private val binding by autoClearedViewBinding(FragmentMushroomBinding::bind) {
        it?.mushroomFragmentRecyclerView?.adapter = null
    }

    // View models

    private val mushroomsViewModel by lazy {
        ViewModelProvider(this, MushroomsViewModelFactory(MushroomsViewModel.Category.SPECIES, requireActivity().application))[MushroomsViewModel::class.java]
    }


    // Adapters

    private val mushroomListAdapter: MushroomListAdapter by lazy {
        val adapter = MushroomListAdapter()

        adapter.setOnClickListener { mushroom ->
            val action = MushroomFragmentDirections.actionGlobalMushroomDetailsFragment(
                mushroom.id,
                DetailsFragment.TakesSelection.NO,
                DetailsFragment.Context.SPECIES,
                null,
                null
            )
            findNavController().navigate(action)
        }

        adapter
    }

    // Listeners

    private val searchBarListener by lazy {
        object : SearchBarListener {
            override fun newSearch(entry: String) {
                mushroomsViewModel.search(entry, true)
            }

            override fun clearedSearchEntry() {
                mushroomsViewModel.reloadData()
            }
        }
    }

    private val onScrollListener by lazy {
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(-1)) {
                    binding.mushroomFragmentSearchBarView.expand()
                } else if (dy > 0) {
                    binding.mushroomFragmentSearchBarView.collapse()
                }
            }
        }
    }

    private val onRefreshListener  by lazy {
        SwipeRefreshLayout.OnRefreshListener {
            mushroomsViewModel.reloadData()
            binding.mushroomFragmentSwipeRefreshLayout.isRefreshing = false
        }
    }

    private val onTapSelectedListener by lazy {
        object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                when (MushroomsViewModel.Category.values[tab.position]) {
                    MushroomsViewModel.Category.FAVORITES -> {
                        binding.mushroomFragmentSearchBarView.visibility = View.GONE
                        binding.mushroomFragmentRecyclerView.setPadding(0, 0, 0, 0)

                    }
                    MushroomsViewModel.Category.SPECIES -> {
                        binding.mushroomFragmentSearchBarView.visibility = View.VISIBLE
                        binding.mushroomFragmentRecyclerView.setPadding(
                            0,
                            (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(
                                R.dimen.searchbar_top_margin
                            )).toInt(),
                            0,
                            0
                        )
                    }
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = MushroomsViewModel.Category.values[tab.position]
                mushroomsViewModel.selectCategory(category)

                when (category) {
                    MushroomsViewModel.Category.FAVORITES -> {
                        binding.mushroomFragmentSearchBarView.visibility = View.GONE
                        binding.mushroomFragmentRecyclerView.setPadding(0, 0, 0, 0)

                    }
                    MushroomsViewModel.Category.SPECIES -> {
                        binding.mushroomFragmentSearchBarView.visibility = View.VISIBLE
                        binding.mushroomFragmentRecyclerView.setPadding(
                            0,
                            (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(
                                R.dimen.searchbar_top_margin
                            )).toInt(),
                            0,
                            0
                        )
                    }
                }
            }
        }
    }

    private val imageSwipedCallback by lazy {
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onChildDrawOver(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder?,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDrawOver(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return super.getMoveThreshold(viewHolder)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {

                val rightMargin = 32.dpToPx(requireContext())
                val iconSize = 30.dpToPx(requireContext())
                val icon: Drawable
                val background: ColorDrawable

                if (mushroomsViewModel.selectedCategory.value == MushroomsViewModel.Category.FAVORITES) {
                    background =
                        ColorDrawable(ResourcesCompat.getColor(resources, R.color.colorRed, null))
                    icon = resources.getDrawable(R.drawable.icon_favorite_remove, null)
                } else {
                    background =
                        ColorDrawable(ResourcesCompat.getColor(resources, R.color.colorGreen, null))
                    icon = resources.getDrawable(R.drawable.icon_favorite_make, null)
                }


                icon.bounds = Rect(
                    viewHolder.itemView.right - iconSize * 2 - rightMargin,
                    viewHolder.itemView.top + (viewHolder.itemView.height / 2) - iconSize,
                    viewHolder.itemView.right - rightMargin,
                    viewHolder.itemView.bottom - (viewHolder.itemView.height / 2) + iconSize
                )


                background.bounds = Rect(
                    viewHolder.itemView.right + dX.toInt(),
                    viewHolder.itemView.top + resources.getDimension(R.dimen.item_mushroom_top_margin).toInt(),
                    viewHolder.itemView.right,
                    viewHolder.itemView.bottom - resources.getDimension(R.dimen.item_mushroom_bottom_margin).toInt()
                )

                background.draw(c)
                icon.draw(c)

                binding.mushroomFragmentSwipeRefreshLayout.isEnabled = !isCurrentlyActive

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }


            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (mushroomsViewModel.selectedCategory.value == MushroomsViewModel.Category.FAVORITES) {
                    mushroomsViewModel.unFavoriteMushroomAt(viewHolder.adapterPosition)
                } else {
                    mushroomsViewModel.favoriteMushroomAt(viewHolder.adapterPosition)
                    mushroomListAdapter.notifyItemChanged(viewHolder.adapterPosition)
                }

            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mushroom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupViewModels()
    }


    private fun setupView() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.mushroomFragmentToolbar)

        binding.mushroomFragmentTabLayout.apply {
            MushroomsViewModel.Category.values.forEach {
                val tab = this.newTab()

                when (it) {
                    MushroomsViewModel.Category.FAVORITES -> {
                        tab.text = resources.getText(R.string.mushroomVC_category_favorites)
                    }
                    MushroomsViewModel.Category.SPECIES -> {
                        tab.text = resources.getText(R.string.mushroomVC_category_species)
                    }
                }

                tab.tag = it
                this.addTab(tab)

            }

            this.addOnTabSelectedListener(onTapSelectedListener)
        }


       binding.mushroomFragmentRecyclerView.apply {
            val myHelper = ItemTouchHelper(imageSwipedCallback)
            myHelper.attachToRecyclerView(this)


            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = RecyclerView.VERTICAL
            this.layoutManager = layoutManager
            this.addOnScrollListener(onScrollListener)
            this.adapter = mushroomListAdapter
            runLayoutAnimation()
        }

       binding.mushroomFragmentSwipeRefreshLayout.apply {
            setOnRefreshListener(onRefreshListener)
        }

        binding.mushroomFragmentSearchBarView.apply {
            setListener(searchBarListener)
        }
    }

    private fun setupViewModels() {
        mushroomsViewModel.selectedCategory.observe(viewLifecycleOwner, Observer {
            binding.mushroomFragmentTabLayout.getTabAt(it.ordinal)?.select()
        })

        mushroomsViewModel.mushroomsState.observe(viewLifecycleOwner, Observer {
           binding.mushroomFragmentBackgroundView.reset()

            when (it) {
                is State.Loading -> {
                    mushroomListAdapter.updateData(listOf())
                    binding.mushroomFragmentBackgroundView.setLoading()
                }
                is State.Items -> {
                    runLayoutAnimation()
                    mushroomListAdapter.updateData(it.items)

                }

                is State.Error -> {
                    binding.mushroomFragmentBackgroundView.setError(it.error)
                }
                else -> {}
            }
        })

        mushroomsViewModel.favoringState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Error -> {
                    mushroomsViewModel.resetFavoritizingState()
                    BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure).changeColor(ResourcesCompat.getColor(resources, R.color.colorRed, null))
                    handleSuccess(it.error.title, it.error.message)
                }

                is State.Items -> {
                    mushroomsViewModel.resetFavoritizingState()
                    BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure).changeColor(ResourcesCompat.getColor(resources, R.color.colorGreen, null))
                    handleSuccess(getString(R.string.mushroomVC_favoriteSucces_title, it.items.localizedName ?: it.items.fullName.italized()), getString(R.string.mushroomVC_favoriteSucces_message))
                }
                else -> {}
            }
        })
    }

    private fun runLayoutAnimation() {
        val controller = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.item_animation)

        binding.mushroomFragmentRecyclerView.layoutAnimation = controller
        binding.mushroomFragmentRecyclerView.scheduleLayoutAnimation()
    }
}