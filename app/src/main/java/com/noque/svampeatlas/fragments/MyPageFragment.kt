package com.noque.svampeatlas.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.MyPageAdapter
import com.noque.svampeatlas.databinding.FragmentMyPageBinding
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Notification
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.Session
import com.noque.svampeatlas.views.MainActivity

class MyPageFragment : Fragment(R.layout.fragment_my_page), MenuProvider {
    companion object {
        const val TAG = "MyPageFragment"
    }

    // Views
    private val binding by autoClearedViewBinding(FragmentMyPageBinding::bind)

    // Adapters
    private val adapter by lazy {
        MyPageAdapter().apply {
            setListener(object: MyPageAdapter.Listener {
                override fun observationSelected(observation: Observation) {
                    val action = MyPageFragmentDirections.actionGlobalMushroomDetailsFragment(
                        observation.id,
                        DetailsFragment.TakesSelection.NO,
                        DetailsFragment.Context.OBSERVATION_WITH_SPECIES,
                        null,
                        null
                    )
                    findNavController().navigate(action)
                }

                override fun getAdditionalData(category: MyPageAdapter.Item.Category, atOffset: Int) {
                    when (category) {
                        MyPageAdapter.Item.Category.NOTIFICATIONS -> Session.getAdditionalNotifications(
                            atOffset
                        )
                        MyPageAdapter.Item.Category.OBSERVATIONS -> Session.getAdditionalObservations(
                            atOffset
                        )
                    }
                }

                override fun notificationSelected(notification: Notification) {
                    Session.markNotificationAsRead(notification)
                    val action = MyPageFragmentDirections.actionGlobalMushroomDetailsFragment(
                        notification.observationID,
                        DetailsFragment.TakesSelection.NO,
                        DetailsFragment.Context.OBSERVATION_WITH_SPECIES,
                        null,
                        null
                    )
                    findNavController().navigate(action)
                }
            })
        }
    }

    // Listeners

    private val onRefreshListener = SwipeRefreshLayout.OnRefreshListener { Session.reloadData(true) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupViewModels()
        Session.reloadData(false)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
        menuInflater.inflate(R.menu.my_page_fragment_menu, menu)

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.menu_myPageFragment_logOut -> {
            Session.logout()
            true
        }

        else -> false
    }

    private fun setupViews() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.mypageFragmentToolbar)
        binding.myPageFragmentCollapsingToolbarLayout.setCollapsedTitleTextColor(ResourcesCompat.getColor(resources, R.color.colorWhite, null))
        binding.myPageFragmentCollapsingToolbarLayout.setExpandedTitleColor(ResourcesCompat.getColor(resources, R.color.colorWhite, null))
        binding.myPageFragmentSwipeRefreshLayout.setOnRefreshListener(onRefreshListener)
        binding.myPageFragmentRecyclerView.apply {
            adapter = this@MyPageFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun setupViewModels() {
            Session.user.observe(viewLifecycleOwner, Observer {
                binding.myPageFragmentCollapsingToolbarLayout.title = it?.name
                if (it != null) binding.myPageFragmentProfileImageView.configure(it.initials, it.imageURL, DataService.ImageSize.FULL)
            })

            Session.notificationsState.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is State.Loading -> {
                        binding.myPageFragmentSwipeRefreshLayout.isRefreshing = true
                    }

                    is State.Items -> {
                        if (state.items.first.count() == 0) {
                            adapter.configureNotificationsState(State.Error(AppError(resources.getString(R.string.sessionError_noNotifications_title),
                                resources.getString(R.string.sessionError_noNotifications_message), null)), getString(R.string.myPageScrollView_notificationsHeader))
                        } else {
                            val items: MutableList<MyPageAdapter.Item> = state.items.first.map { MyPageAdapter.Item.Notification(it) }.toMutableList()
                            if (items.count() != state.items.second) items.add(MyPageAdapter.Item.LoadMore(MyPageAdapter.Item.Category.NOTIFICATIONS, items.lastIndex))
                            adapter.configureNotificationsState(State.Items(items), "${state.items.second} ${getText(R.string.myPageScrollView_notificationsHeader)}")
                        }
                    }

                    is State.Error -> {
                        adapter.configureNotificationsState(State.Error(state.error), getString(R.string.myPageScrollView_notificationsHeader))
                    }
                    else -> {}
                }

                evaluateIfFinishedLoading()
            })

            Session.observationsState.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is State.Loading -> {
                        binding.myPageFragmentSwipeRefreshLayout.isRefreshing = true
                    }

                    is State.Items -> {
                        if (state.items.first.count() == 0) {
                            adapter.configureObservationsState(State.Error(AppError(
                                resources.getString(R.string.sessionError_noObservations_title),
                                resources.getString(R.string.sessionError_noObservations_message), null
                            )), getString(R.string.myPageScrollView_observationsHeader))
                        } else {
                            val items: MutableList<MyPageAdapter.Item> = state.items.first.map { MyPageAdapter.Item.Observation(it) }.toMutableList()
                            if (items.count() != state.items.second) items.add(MyPageAdapter.Item.LoadMore(MyPageAdapter.Item.Category.OBSERVATIONS, items.lastIndex))
                            adapter.configureObservationsState(State.Items(items), "${state.items.second} ${getText(
                                R.string.myPageScrollView_observationsHeader
                            )}")
                        }
                    }

                    is State.Error -> {
                        adapter.configureObservationsState(State.Error(state.error), getString(R.string.myPageScrollView_observationsHeader))
                    }
                    else -> {}
                }

                evaluateIfFinishedLoading()
            })
    }

    private fun evaluateIfFinishedLoading() {
        if (Session.observationsState.value !is State.Loading && Session.notificationsState.value !is State.Loading) {
            binding.myPageFragmentSwipeRefreshLayout.isRefreshing = false
        }
    }
}
