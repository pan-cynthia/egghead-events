package com.egghead.events

import com.google.firebase.firestore.PropertyName

// Goals:
// Allow user to make favorites

data class User(
    @PropertyName("favorites")
    var favorites: ArrayList<String> = arrayListOf()
)
