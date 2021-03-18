package com.egghead.events

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.*
import kotlin.collections.ArrayList

// Goals:
// Create an easy API for interacting with firebase

enum class ResponseType {
    SUCCESS,
    FAILURE
}

class EventFirestore {
    companion object {
        fun postFavorites(completion: (response: ResponseType) -> Unit) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()

            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val favoriteEvents = EventsSingleton.events.filter { entry ->
                    entry.favorited
                }

                val favorites = favoriteEvents.map { entry ->
                    entry.documentId
                }

                val user = User(ArrayList(favorites))
                firestore.collection("users").document(firebaseUser.uid)
                    .set(user)
                    .addOnSuccessListener {
                        completion(ResponseType.SUCCESS)
                    }
                    .addOnFailureListener {
                        completion(ResponseType.FAILURE)
                    }
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

        fun deleteEvent(event: Event, completion: (response: ResponseType) -> Unit) {
            Log.d("delete", "entered delete event")

            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()

            firestore.collection("events").document(event.documentId)
                .delete()
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
                .whereGreaterThan("end", Timestamp(Date()))
                .get()
                .addOnSuccessListener { documents ->
                    val firebaseUser = FirebaseAuth.getInstance().currentUser

                    if (firebaseUser != null) {
                        firestore.collection("users")
                            .document(firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { userdoc ->
                                val user = userdoc.toObject(User::class.java)

                                for (document in documents) {
                                    val event = document.toObject(Event::class.java)

                                    if (event.documentId in user?.favorites ?: arrayListOf()) {
                                        event.favorited = true
                                    }

                                    events.add(event)
                                }

                                EventsSingleton.events = events

                                setEvents(EventsSingleton.events)
                            }
                    }


                }
                .addOnFailureListener { Log.d("API", "Event failed to write!") }
        }
    }
}