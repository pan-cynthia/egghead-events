package com.egghead.events

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.*
import kotlin.collections.ArrayList

enum class ResponseType {
    SUCCESS,
    FAILURE
}

class EventFirestore {
    companion object {
        fun postFavorite(event: Event) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()

            val firebaseInstance = FirebaseAuth.getInstance()
            if (firebaseInstance.currentUser != null) {
                firestore.collection("users").document(firebaseInstance.currentUser.uid)
                    .get()
            }

        }

        fun postEvent(event: Event, completion: (response: ResponseType) -> Unit) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()

            firestore.collection("events").add(event)
                .addOnSuccessListener {
                    completion(ResponseType.SUCCESS)
                }
                .addOnFailureListener {
                    completion(ResponseType.FAILURE)
                }
        }

        fun updateEvent(event: Event, completion: (response: ResponseType) -> Unit) {
            Log.d("update", "entered update event")
            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()

            firestore.collection("events").document(event.documentId)
                .set(event)
                .addOnSuccessListener {
                    completion(ResponseType.SUCCESS)
                }
                .addOnFailureListener {
                    completion(ResponseType.FAILURE)
                }
        }

        fun getAllEvents(setEvents: (events: ArrayList<Event>) -> Unit) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()

            val events = arrayListOf<Event>()

            firestore.collection("events")
                //.whereGreaterThan("end", Timestamp(Date()))
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val event = document.toObject(Event::class.java)
                        events.add(event)
                    }
                    setEvents(events)
                }
                .addOnFailureListener { Log.d("API", "Event failed to write!") }
        }
    }
}