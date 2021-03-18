package com.egghead.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.window.isPopupLayout
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.login.LoginManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_event_feed.*
import java.text.SimpleDateFormat
import java.util.*

// Goals:
// Show all events in a feed

class EventFeedFragment : Fragment() {

    private lateinit var auth : FirebaseAuth
    private var favoriteFilter: Boolean = false
    private var eventListAdapter: EventListAdapter? = null

    private lateinit var alertDialog: AlertDialog
    var startTimeInMilliseconds : Long = 0
    var endTimeInMilliseconds : Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        auth = FirebaseAuth.getInstance()

        (activity as AppCompatActivity?)?.supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.standard_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                LoginManager.getInstance().logOut()

                val action = R.id.action_eventFeedFragment_to_signinFragment
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    override fun onResume() {
        super.onResume()

        EventFirestore.getAllEvents {
            eventListAdapter?.setData(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventsRecyclerView: RecyclerView = view.findViewById(R.id.events_recycler_view)
        eventsRecyclerView.layoutManager = LinearLayoutManager(this.context)

        eventListAdapter = EventListAdapter(arrayListOf())
        eventsRecyclerView.adapter = eventListAdapter

        val createEventButton = view.findViewById<FloatingActionButton>(R.id.create_event_button)
        createEventButton.setOnClickListener {
            val action = R.id.action_eventFeedFragment_to_createEventFragment
            findNavController().navigate(action)
        }

        val favoriteButton = view.findViewById<FloatingActionButton>(R.id.favorite_button)
        favoriteButton.setOnClickListener {
            favoriteFilter = !favoriteFilter
            if (favoriteFilter) {
                eventListAdapter?.setData(EventsSingleton.events.filter {
                    it.favorited
                })
                favoriteButton.setImageResource(R.drawable.ic_star_filled_24px)
            } else {
                eventListAdapter?.setData(EventsSingleton.events)
                favoriteButton.setImageResource(R.drawable.ic_star_unfilled_24px)
            }
        }

        val searchButton = view.findViewById<Button>(R.id.searchfilterbutton)
        searchButton.setOnClickListener{
            val searchtext = view.findViewById<TextInputEditText>(R.id.searchbar).text.toString()
            if (searchtext != ""){
                eventListAdapter?.setDataWithSearch(EventsSingleton.events, searchtext)
            } else {
                if (favoriteFilter) {
                    eventListAdapter?.setData(EventsSingleton.events.filter {
                        it.favorited
                    })
                } else {
                    eventListAdapter?.setData(EventsSingleton.events)
                }
            }
        }

        val filterButton = view.findViewById<Button>(R.id.filterfilterbutton)
        filterButton.setOnClickListener {
            startTimeInMilliseconds = 0
            endTimeInMilliseconds = 0
            val inflater: LayoutInflater = this.getLayoutInflater()
            val dialogView: View = inflater.inflate(R.layout.filter_pop_up, null)

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                override fun onDismiss(arg0: DialogInterface) {
                }
            })
            dialogBuilder.setView(dialogView)
            alertDialog = dialogBuilder.create();
            //alertDialog.window!!.getAttributes().windowAnimations =
            alertDialog.show()

            val start = alertDialog.findViewById<TextView>(R.id.filter_time_start)
            start?.setOnClickListener {
                pickDateTime(start, alertDialog)
            }

            val end = alertDialog.findViewById<TextView>(R.id.filter_time_end)
            end?.setOnClickListener {
                pickDateTime(end, alertDialog)
            }


            val filtersearch = alertDialog.findViewById<Button>(R.id.filter_search_button)
            filtersearch?.setOnClickListener {
                val keywords = alertDialog.findViewById<TextInputEditText>(R.id.key_words)?.text.toString()
                val location = alertDialog.findViewById<TextInputEditText>(R.id.filter_location)?.text.toString()
                eventListAdapter?.setDataWithFilter(EventsSingleton.events, keywords, location, startTimeInMilliseconds, endTimeInMilliseconds, favoriteFilter)
                alertDialog.hide()
            }

        }
    }
    private fun pickDateTime(textView: TextView, alertDialog: AlertDialog) {
        val textView = textView
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(
            requireContext(),
            DatePickerDialog.OnDateSetListener { _, year, month, day ->
                TimePickerDialog(
                    requireContext(),
                    TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                        val pickedDateTime = Calendar.getInstance()
                        pickedDateTime.set(year, month, day, hour, minute)
                        if (textView == alertDialog?.findViewById<TextView>(R.id.filter_time_start)) {
                            textView.text = SimpleDateFormat("MMM dd yyyy HH:mm", Locale.US).format(
                                pickedDateTime.time
                            )
                            startTimeInMilliseconds = pickedDateTime.timeInMillis
                        } else {
                            textView.text = SimpleDateFormat("MMM dd yyyy HH:mm", Locale.US).format(
                                pickedDateTime.time
                            )
                            endTimeInMilliseconds = pickedDateTime.timeInMillis
                        }
                    },
                    startHour,
                    startMinute,
                    true
                ).show()
            },
            startYear,
            startMonth,
            startDay
        ).show()
    }
}