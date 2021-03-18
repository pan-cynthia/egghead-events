package com.egghead.events

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import kotlinx.android.parcel.Parcelize
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// Goals:
// Define an easy to use event type

@Parcelize
data class Event(
        @DocumentId
        var documentId: String = "",

        @PropertyName("title")
        var title: String = "",

        @PropertyName("description")
        var description: String = "",

        @PropertyName("tags")
        var tags: ArrayList<String> = arrayListOf(),

        @PropertyName("start")
        var start: Timestamp = Timestamp(Date()),

        @PropertyName("end")
        var end: Timestamp = Timestamp(Date()),

        @PropertyName("location")
        var location: String = "",

        @PropertyName("uid")
        var uid: String = "",

        @PropertyName("image")
        var image: String? = null,

        var favorited: Boolean = false

) : Parcelable