package com.egghead.events

import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isGone
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.milliseconds

// Goals:
// Show all the events as individual tiles with options to modify and filter

class EventListAdapter(private var events: List<Event>) : RecyclerView.Adapter<ItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = events[position]
        holder.apply {
            titleView.text = item.title

            if (item.image != null) {
                Glide.with(holder.itemView)
                    .load(item.image)
                    .into(imageView)
                cardView.visibility = View.VISIBLE
                imageView.visibility = View.VISIBLE
            } else {
                cardView.visibility = View.GONE
                imageView.visibility = View.GONE
            }

            val formatter = SimpleDateFormat("MMM dd yyyy HH:mm", Locale.US)
            startTimestampView.text = formatter.format(item.start.toDate())
            endTimestampView.text = formatter.format(item.end.toDate())

            if (FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true) {
                favoriteButton.visibility = View.GONE
            }

            favoriteButton.setOnClickListener {
                // First update the singleton
                EventsSingleton.events[position].favorited = !EventsSingleton.events[position].favorited

                // then update the actual list on the way back
                EventFirestore.postFavorites {
                    setData(EventsSingleton.events)
                    notifyItemChanged(position)

                    if (events[position].favorited) {
                        favoriteButton.setBackgroundResource(R.drawable.ic_star_filled_24px)
                    } else {
                        favoriteButton.setBackgroundResource(R.drawable.ic_star_unfilled_24px)
                    }
                }
            }

            calendarButton.setOnClickListener {
                // https://stackoverflow.com/questions/14694931/insert-event-to-calendar-using-intent
                val intent = Intent(Intent.ACTION_EDIT)
                intent.type = "vnd.android.cursor.item/event"
                intent.putExtra(CalendarContract.Events.TITLE, item.title)
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.start.toDate().time)
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.end.toDate().time)
                intent.putExtra(CalendarContract.Events.ALL_DAY, false)
                intent.putExtra(CalendarContract.Events.DESCRIPTION, item.description)
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, item.location)

                startActivity(itemView.context, intent, null)
            }

            if (item.favorited) {
                favoriteButton.setBackgroundResource(R.drawable.ic_star_filled_24px)
            } else {
                favoriteButton.setBackgroundResource(R.drawable.ic_star_unfilled_24px)
            }
        }

        holder.fullView.setOnClickListener{
            Log.d("click", "event item clicked on")
            val action: NavDirections = EventFeedFragmentDirections.actionEventFeedFragmentToDisplayEventFragment(item)
            it.findNavController().navigate(action)
        }
    }

    override fun getItemCount() = events.size

    fun setData(newEvents: List<Event>) {
        events = newEvents
        Log.d("EVENTS", events.toString())
        Log.d("EventListAdapter", "Got new posts")
        this.notifyDataSetChanged()
    }

    fun setDataWithSearch(newEvents: List<Event>, search: String){
        val tempevents = mutableListOf<Event>()
        for (anEvent in newEvents){
            if (anEvent.title.contains(search, ignoreCase = true) || anEvent.description.contains(search, ignoreCase = true)){
                tempevents.add(anEvent)
            }
        }
        events = tempevents
        Log.d("EventListAdapter", "Got new posts")
        this.notifyDataSetChanged()
    }

    fun setDataWithFilter(newEvents: List<Event>, search: String, searchlocation: String, starttime: Long , endtime: Long, favoriteFilter: Boolean){
        val tempevents = mutableListOf<Event>()
        for (anEvent in newEvents){
            if (anEvent.title.contains(search, ignoreCase = true) || anEvent.description.contains(search, ignoreCase = true)){
                if (anEvent.location.contains(searchlocation, ignoreCase = true)) {
                    if (anEvent.start > Timestamp(Date(starttime)) || starttime == 0L && anEvent.end < Timestamp(Date(starttime))) {
                        if (anEvent.end < Timestamp(Date(endtime)) || endtime == 0L) {
                            if (anEvent.favorited == true || favoriteFilter == false) {
                                tempevents.add(anEvent)
                            }
                        }
                    }
                }
            }
        }
        events = tempevents
        Log.d("EventListAdapter", "Got new posts")
        this.notifyDataSetChanged()
    }
}

class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val fullView: LinearLayout = view.findViewById(R.id.event_item)
    val titleView: TextView = view.findViewById(R.id.event_title)
    val cardView: CardView = view.findViewById(R.id.image_card_view)
    val imageView: ImageView = view.findViewById(R.id.event_image)
    val favoriteButton: Button = view.findViewById(R.id.event_favorite_button)
    val calendarButton: Button = view.findViewById(R.id.event_calendar_button)
    val startTimestampView: TextView = view.findViewById(R.id.event_start_timestamp)
    val endTimestampView: TextView = view.findViewById(R.id.event_end_timestamp)
}