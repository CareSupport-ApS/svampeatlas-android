package com.noque.svampeatlas.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.noque.svampeatlas.models.State

import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.BackgroundView
import com.noque.svampeatlas.views.MainActivity
import kotlinx.android.synthetic.main.fragment_login.*
import androidx.core.content.ContextCompat.getSystemService
import android.view.inputmethod.InputMethodManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.noque.svampeatlas.utilities.ToastHelper
import com.noque.svampeatlas.view_models.Session


class LoginFragment : Fragment() {

    companion object {
        const val TAG = "LoginFragment"
    }

    // Views
    private lateinit var backgroundView: BackgroundView
    private lateinit var initialsEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var createAccountButton: Button
    private lateinit var bg: ImageView

    // Listeners

    private val loginButtonClickListener = View.OnClickListener {

        if (initialsEditText.text.isNullOrEmpty()) {
            initialsEditText.error = resources.getString(R.string.loginVC_initialsTextField_error)
        } else if (passwordEditText.text.isNullOrEmpty()) {
            passwordEditText.error = resources.getString(R.string.loginVC_passwordTextField_error)
        } else {
            Session.login(initialsEditText.text.toString(), passwordEditText.text.toString())
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()

    }

    private fun initViews() {
        backgroundView = loginFragment_backgroundView
        initialsEditText = loginFragment_initialsEditText
        passwordEditText = loginFragment_passwordEditText
        loginButton = loginFragment_loginButton
        createAccountButton = loginFragment_createAccountButton
        bg = loginFragment_bg
    }

    private fun setupViews() {
        (requireActivity() as MainActivity).setSupportActionBar(loginFragment_toolbar)

        loginButton.setOnClickListener(loginButtonClickListener)
        createAccountButton.setOnClickListener(createAccountButtonPressed)
        Glide.with(requireContext()).load(R.drawable.background).transition(DrawableTransitionOptions.withCrossFade()).into(bg)
    }

    private fun setupViewModels() {
        Session.loggedInState.observe(viewLifecycleOwner) {
            backgroundView.reset()

            when (it) {
                is State.Error -> ToastHelper.handleError(requireActivity(), it.error)
                is State.Loading -> backgroundView.setLoading()
                else -> {}
            }
        }
    }
}
