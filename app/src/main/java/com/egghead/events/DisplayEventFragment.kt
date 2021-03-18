package com.egghead.events

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

// Goals:
// Show additional data

class DisplayEventFragment : Fragment() {

    val args: DisplayEventFragmentArgs by navArgs()
    lateinit var firebase : FirebaseAuth
    private var favoriteFilter: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebase = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_display_event, container, false)
        view.findViewById<FloatingActionButton>(R.id.edit_button).hide()
        view.findViewById<FloatingActionButton>(R.id.delete_button).hide()
        view.findViewById<Button>(R.id.event_favorite_button).visibility= View.VISIBLE
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val event = args.event

        val user = FirebaseAuth.getInstance().currentUser
        if (event.uid == user.uid) {
            view.findViewById<FloatingActionButton>(R.id.edit_button).show()
            view.findViewById<FloatingActionButton>(R.id.delete_button).show()
        }
        if (user.isAnonymous) {
            view.findViewById<Button>(R.id.event_favorite_button).visibility= View.GONE
        }

        Log.d("title", event.title)

        val titleView: TextView = view.findViewById(R.id.event_title)
        val descriptionView: TextView = view.findViewById(R.id.event_description)
        val calendar: Button = view.findViewById(R.id.event_calendar_button)
        val startTimestampView: TextView = view.findViewById(R.id.event_start_timestamp)
        val endTimestampView: TextView = view.findViewById(R.id.event_end_timestamp)
        val locationView : TextView = view.findViewById(R.id.event_location)
        val favorited: Button = view.findViewById(R.id.event_favorite_button)
        val imageView : ImageView = view.findViewById(R.id.event_image)
        val cardView: CardView = view.findViewById(R.id.image_card_view)

        titleView.text = event.title
        descriptionView.text = event.description
        locationView.text = event.location
        if (event.favorited == false){
            favorited.setBackgroundResource(R.drawable.ic_star_unfilled_24px)
            favoriteFilter = false
        }
        if (event.image != null) {
            Glide.with(view)
                .load(event.image)
                .into(imageView)
        } else {
            cardView.visibility = View.GONE
            imageView.visibility = View.GONE
        }

        val formatter = SimpleDateFormat("MMM dd yyyy HH:mm", Locale.US)
        startTimestampView.text = formatter.format(event.start.toDate())
        endTimestampView.text = formatter.format(event.end.toDate())

        view.findViewById<FloatingActionButton>(R.id.edit_button).setOnClickListener {
            val action : NavDirections = DisplayEventFragmentDirections.actionDisplayEventFragmentToUpdateEventFragment(event)
            findNavController().navigate(action)
        }

        view.findViewById<FloatingActionButton>(R.id.delete_button).setOnClickListener {
            EventFirestore.deleteEvent(event) { response ->
                if (response == ResponseType.SUCCESS) {
                    Log.d("delete", "successfully deleted event")
                    val action = R.id.action_displayEventFragment_to_eventFeedFragment
                    this.findNavController().navigate(action)
                } else {
                    Log.d("delete", "could not delete event")
                }
            }
        }

        if (FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true) {
            favorited.visibility = View.GONE
        }

        favorited.setOnClickListener {
            // First update the singleton
            var position = 0
            var found = -1
            for (recur_event in EventsSingleton.events) {
                if (recur_event.documentId == event.documentId) {
                    EventsSingleton.events[position].favorited = !EventsSingleton.events[position].favorited
                    found = position
                }
                position++
            }

            // then update the actual list on the way back
            EventFirestore.postFavorites {
                if (found != -1) {
                    if (EventsSingleton.events[found].favorited) {
                        favorited.setBackgroundResource(R.drawable.ic_star_filled_24px)
                    } else {
                        favorited.setBackgroundResource(R.drawable.ic_star_unfilled_24px)
                    }
                }
            }
        }

        calendar.setOnClickListener {
            // https://stackoverflow.com/questions/14694931/insert-event-to-calendar-using-intent
            val intent = Intent(Intent.ACTION_EDIT)
            intent.type = "vnd.android.cursor.item/event"
            intent.putExtra(CalendarContract.Events.TITLE, event.title)
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.start.toDate().time)
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.toDate().time)
            intent.putExtra(CalendarContract.Events.ALL_DAY, false)
            intent.putExtra(CalendarContract.Events.DESCRIPTION, event.description)
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)

            startActivity(intent, null)
        }
    }
}