package com.egghead.events

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

// Goals:
// Allow user to login using google, facebook, or remain anonymous

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var errorLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        (activity as AppCompatActivity?)?.supportActionBar?.hide()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_signin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        errorLabel = view.findViewById<TextView>(R.id.signin_error)

        val anonymousButton = view.findViewById<Button>(R.id.anonymous_button)
        anonymousButton.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val action = R.id.action_signinFragment_to_eventFeedFragment
                        findNavController().navigate(action)
                    } else {
                        // If sign in fails, display a message to the user.

                    }

                }
        }

        val loginBtn = view.findViewById<Button>(R.id.google_button)
        loginBtn.setOnClickListener{
            startActivityForResult(googleSignInClient.signInIntent, 1)
        }

        callbackManager = CallbackManager.Factory.create();


        val EMAIL = "email"

        val loginButton = view.findViewById<LoginButton>(R.id.login_button)
        loginButton.setReadPermissions("email", "public_profile")
        loginButton.setFragment(this)

        // Callback registration
        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult?> {
            override fun onSuccess(loginResult: LoginResult?) {
                // App code
                Log.d(tag, "Successful login")
                firebaseAuthWithFacebook(loginResult?.accessToken?.token ?: "")
            }

            override fun onCancel() {
                Log.d(tag, "Cancelled login")
                // App code
            }

            override fun onError(exception: FacebookException) {
                Log.d(tag, "Failed login")
                // App code
            }
        })

        val customFacebookButton = view.findViewById<Button>(R.id.custom_facebook_button)
        customFacebookButton.setOnClickListener {
            loginButton.performClick()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("tag", "Result")
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1){
            handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(data))
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) { // Take in a Task API of Type GoogleSignInAccount
        try {
            val account = completedTask.getResult(ApiException::class.java)!! // Use complete task API to check method calls
            Log.w("Google Sign-in", "Sign-in successful")
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Google Sign-in", "signInResult:failed code=" + e.statusCode)
        }
    }

    // https://criticalgnome.com/2019/12/30/more-than-two-lines-in-snackbar/
    // Set snackbar maxlines

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(tag, "signInWithCredential:success")
                    findNavController().navigate(R.id.action_signinFragment_to_eventFeedFragment)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(tag, "signInWithCredential:failure", task.exception)
                    Snackbar
                        .make(requireView(), (task.exception?.message ?: "Signin Failed."), Snackbar.LENGTH_LONG)
                        .apply { view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 4 }
                        .show()
                }
            }
    }

    private fun firebaseAuthWithFacebook(idToken: String) {
        val credential = FacebookAuthProvider.getCredential(idToken)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(tag, "signInWithCredential:success")
                    findNavController().navigate(R.id.action_signinFragment_to_eventFeedFragment)
                } else {
                    // If sign in fails, display a message to the user.
                    LoginManager.getInstance().logOut()
                    Log.w(tag, "signInWithCredential:failure", task.exception)
                    requireView()
                    Snackbar
                        .make(requireView(), (task.exception?.message ?: "Signin Failed."), Snackbar.LENGTH_LONG)
                        .apply { view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 4 }
                        .show()
                }
            }
    }
}