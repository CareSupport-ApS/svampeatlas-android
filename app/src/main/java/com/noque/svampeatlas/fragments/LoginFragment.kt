package com.noque.svampeatlas.fragments


import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentLoginBinding
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.ToastHelper.handleError
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.Session
import com.noque.svampeatlas.views.MainActivity


class LoginFragment : Fragment(R.layout.fragment_login) {

    companion object {
        const val TAG = "LoginFragment"
    }

    // Views
    private val binding by autoClearedViewBinding(FragmentLoginBinding::bind)

    // Listeners

    private val loginButtonClickListener = View.OnClickListener {

        if (binding.loginFragmentInitialsEditText.text.isNullOrEmpty()) {
            binding.loginFragmentInitialsEditText.error = resources.getString(R.string.loginVC_initialsTextField_error)
        } else if (binding.loginFragmentPasswordEditText.text.isNullOrEmpty()) {
            binding.loginFragmentPasswordEditText.error = resources.getString(R.string.loginVC_passwordTextField_error)
        } else {
            Session.login(binding.loginFragmentInitialsEditText.text.toString(), binding.loginFragmentPasswordEditText.text.toString())
        }

        getSystemService(requireContext(), InputMethodManager::class.java)?.hideSoftInputFromWindow(
            requireActivity().currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
        )

        view?.requestFocus()
    }

    private val createAccountButtonPressed = View.OnClickListener {
        try {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://svampe.databasen.org/signup"))
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupViewModels()
    }

    private fun setupViews() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.loginFragmentToolbar)
        binding.loginFragmentLoginButton.setOnClickListener(loginButtonClickListener)
        binding.loginFragmentCreateAccountButton.setOnClickListener(createAccountButtonPressed)
        Glide.with(requireContext()).load(R.drawable.background).transition(DrawableTransitionOptions.withCrossFade()).into(binding.loginFragmentBg)
    }

    private fun setupViewModels() {
        Session.loggedInState.observe(viewLifecycleOwner) {
            binding.loginFragmentBackgroundView.reset()

            when (it) {
                is State.Error -> handleError(it.error)
                is State.Loading -> binding.loginFragmentBackgroundView.setLoading()
                else -> {}
            }
        }
    }
}
