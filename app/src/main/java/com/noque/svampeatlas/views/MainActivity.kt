package com.noque.svampeatlas.views
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.google.android.material.navigation.NavigationView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ActivityMainBinding
import com.noque.svampeatlas.fragments.TermsFragment
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.view_models.Session
import www.sanju.motiontoast.MotionToast


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BlankActivity"
        const val KEY_IS_LOGGED_IN = "IsLoggedIn"
    }


    // Objects
    private lateinit var navController: NavController
    private var isLoggedIn: Boolean? = null

    // Views
    private lateinit var binding: ActivityMainBinding
    private lateinit var userView: UserView

    // Listeners
    private val onDestinationChangedListener by lazy {
        NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.addObservationFragment || destination.id != R.id.cameraFragment) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
            binding.navigationView.setCheckedItem(destination.id)
        }
    }

    private val onNavigationItemSelectedListener by lazy {
        NavigationView.OnNavigationItemSelectedListener {
            var setCheckedItem = true
            var closeDrawer = true
            var destinationID: Int? = it.itemId

                when (it.itemId) {
                    R.id.facebook -> {
                        setCheckedItem = false
                        closeDrawer = false
                        destinationID = null

                        val intent = try {
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/groups/svampeatlas")
                            )
                        } catch (e: Exception) {
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.facebook.com/groups/svampeatlas/")
                            )
                        }

                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.d(TAG, e.toString())
                        }
                    }
                }

            if (closeDrawer) binding.drawerLayout.closeDrawer(binding.navigationView, true)
            if (setCheckedItem) binding.navigationView.setCheckedItem(it.itemId)
            if (destinationID != null && destinationID != navController.currentDestination?.id) {
                destinationID?.let {
                    navController.navigate(
                        it,
                        null,
                        NavOptions.Builder().setPopUpTo(navController.graph.startDestinationId, false).build()
                    )
                }
            }
            false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLoggedIn = savedInstanceState?.getBoolean(KEY_IS_LOGGED_IN)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        initViews()
        setupView()
        setupViewModels()
    }

    private fun initViews() {
        navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!.findNavController()
        userView = binding.navigationView.getHeaderView(0).findViewById(R.id.navigationHeader_userView)
    }

    private fun setupView() {
        window.statusBarColor = Color.TRANSPARENT
        binding.navigationView.itemIconTintList = null
        navController.addOnDestinationChangedListener(onDestinationChangedListener)
        binding.navigationView.setNavigationItemSelectedListener(onNavigationItemSelectedListener)
        if (!SharedPreferences.hasSeenWhatsNew) {
            val dialog = TermsFragment()
            dialog.arguments = Bundle().apply { putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.WHATSNEW) }
            dialog.show(supportFragmentManager, null)
            SharedPreferences.databaseShouldUpdate = true
        }


        // Setup toast

        MotionToast.setErrorColor(R.color.colorRed)
        MotionToast.setSuccessColor(R.color.colorGreen)
    }

    private fun setupViewModels() {
        Session.loggedInState.observe(this, Observer {
            when (it) {
                is State.Items -> {
                    binding.navigationView.menu.clear()
                    if (it.items)   binding.navigationView.inflateMenu(R.menu.menu_logged_in) else   binding.navigationView.inflateMenu(
                        R.menu.menu_logged_out
                    )

                    if (it.items != isLoggedIn && it.items) {
                        val newGraph = navController.navInflater.inflate(R.navigation.nav_main)
                        newGraph.setStartDestination(R.id.myPageFragment)
                        binding.navigationView.setCheckedItem(R.id.myPageFragment)
                        navController.graph = newGraph
                    } else if (it.items != isLoggedIn) {
                        val newGraph = navController.navInflater.inflate(R.navigation.nav_main)
                        newGraph.setStartDestination(R.id.mushroomFragment)
                        binding.navigationView.setCheckedItem(R.id.mushroomFragment)
                        navController.graph = newGraph
                    }
                    isLoggedIn = it.items
                }
                else -> {}
            }
        })

        Session.user.observe(this, Observer {
            if (it != null) userView.configure(it) else userView.configureAsGuest()
        })
    }

    fun hideSystemBars() {
        val flags = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LOW_PROFILE
                )

        window.decorView.systemUiVisibility = flags
    }

    fun showSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        isLoggedIn?.let {
            outState.putBoolean(KEY_IS_LOGGED_IN, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        val sharedSet = mutableSetOf(R.id.loginFragment, R.id.myPageFragment, R.id.notesFragment, R.id.mushroomFragment, R.id.nearbyFragment, R.id.cameraFragment, R.id.settingsFragment, R.id.aboutFragment)
        return if (navController.currentDestination?.id == R.id.addObservationFragment && (navController.previousBackStackEntry?.destination?.id == R.id.mushroomDetailsFragment || navController.previousBackStackEntry?.destination?.id == R.id.notesFragment)) {
            navController.navigateUp(AppBarConfiguration(sharedSet, binding.drawerLayout)) || super.onSupportNavigateUp()
        } else  {
            sharedSet.add(R.id.addObservationFragment)
            navController.navigateUp(AppBarConfiguration(sharedSet, binding.drawerLayout)) || super.onSupportNavigateUp()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        if (isFinishing) {
            // Note: this happens only when application is closed by exiting it probably, meaning that potentially a lot of temp images could end up be saved unintentionally
            FileManager.clearTemporaryFiles()
        }

        super.onPause()
    }
}
