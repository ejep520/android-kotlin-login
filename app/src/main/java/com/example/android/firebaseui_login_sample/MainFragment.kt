/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.firebaseui_login_sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContract
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

import com.example.android.firebaseui_login_sample.databinding.FragmentMainBinding


import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse

import com.google.firebase.auth.FirebaseAuth

class MainFragment : Fragment() {

    companion object {
        const val TAG = "MainFragment"
        const val SIGN_IN_RESULT_CODE = 1001
    }

    // Get a reference to the ViewModel scoped to this Fragment
    private val viewModel by viewModels<LoginViewModel>()
    private lateinit var binding: FragmentMainBinding
    private lateinit var signInResult: String
    private val contract = registerForActivityResult(SignInContract()) {
        signInResult = it
        Log.i(TAG, signInResult)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)

        // TODO Remove the two lines below once observeAuthenticationState is implemented.
        binding.welcomeText.text = viewModel.getFactToDisplay(requireContext())
        binding.authButton.text = getString(R.string.login_btn)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthenticationState()

        binding.authButton.setOnClickListener {
            launchSignInFlow()
        }
        binding.settingsBtn.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToSettingsFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Observes the authentication state and changes the UI accordingly.
     * If there is a logged in user: (1) show a logout button and (2) display their name.
     * If there is no logged in user: show a login button
     */
    private fun observeAuthenticationState() {
        val factToDisplay = viewModel.getFactToDisplay(requireContext())

        viewModel.authenticationState.observe(viewLifecycleOwner, { authenticationState ->
            if (authenticationState == LoginViewModel.AuthenticationState.AUTHENTICATED) {
                binding.authButton.text = getText(R.string.logout_button_text)
                binding.welcomeText.text = getFactWithPersonalization(factToDisplay)
                binding.authButton.setOnClickListener { AuthUI.getInstance().signOut(requireContext()) }
            } else {
                binding.authButton.text = getText(R.string.login_button_text)
                binding.authButton.setOnClickListener { launchSignInFlow() }
                binding.welcomeText.text = factToDisplay
            }
        })
    }


    private fun getFactWithPersonalization(fact: String): String {
        return String.format(resources.getString(
                R.string.welcome_message_authed,
                FirebaseAuth.getInstance().currentUser?.displayName,
                Character.toLowerCase(fact[0]) + fact.substring(1)
            ))
    }

    private fun launchSignInFlow() {
        // Create and launch sign-in intent. We listen to the response of this activity with
        // the SIGN_IN_REQUEST_CODE
        contract.launch(SIGN_IN_RESULT_CODE)
    }

    class SignInContract: ActivityResultContract<Int, String>() {
        override fun createIntent(context: Context, input: Int?): Intent {
            // Give users the option to sign in / register with their email or Google account.
            // If users choose to register with their email, they will need to create a password
            // as well.
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
                // This is where you can provide more ways for users to register and sign in.
            )
            return AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String {
            return if (resultCode == Activity.RESULT_OK) "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!"
            else "Activity Failed"
        }
    }
}