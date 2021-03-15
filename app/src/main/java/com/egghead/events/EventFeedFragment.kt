package com.egghead.events

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_event_feed.*
import java.util.*

class EventFeedFragment : Fragment() {

    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_event_feed, container, false)
        val user = FirebaseAuth.getInstance().currentUser.isAnonymous
        if (user) {
            view.findViewById<FloatingActionButton>(R.id.favorite_button).hide()
            view.findViewById<FloatingActionButton>(R.id.create_event_button).hide()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signoutButton = view.findViewById<Button>(R.id.signout_button)
        signoutButton.setOnClickListener {
            auth.signOut()

            val action = R.id.action_eventFeedFragment_to_signinFragment
            findNavController().navigate(action)
        }

        val eventsRecyclerView: RecyclerView = view.findViewById(R.id.events_recycler_view)
        eventsRecyclerView.layoutManager = LinearLayoutManager(this.context)

        val eventListAdapter = EventListAdapter(arrayListOf(), findNavController())
        eventsRecyclerView.adapter = eventListAdapter

        EventFirestore.getAllEvents {
            eventListAdapter.setData(it)
        }

        create_event_button.setOnClickListener {
            val action = R.id.action_eventFeedFragment_to_createEventFragment
            findNavController().navigate(action)
        }
    }
}