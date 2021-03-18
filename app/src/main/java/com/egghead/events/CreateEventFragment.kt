package com.egghead.events

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color.rgb
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.text.SimpleDateFormat
import java.util.*

// Goals:
// Use firebase storage to upload image
// Create popups for opening date/time/etc

class CreateEventFragment : Fragment() {

    lateinit var firebase : FirebaseAuth
    var eventImageView: ImageView? = null
    var eventCardView: CardView? = null
    var startTimeInMilliseconds : Long = 0
    var endTimeInMilliseconds : Long = 0
    var imageUri : Uri? = null
    var imageType : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebase = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_create_event, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser

        val start = view.findViewById<TextView>(R.id.event_start)
        start.setOnClickListener {
            pickDateTime(start)
        }

        val end = view.findViewById<TextView>(R.id.event_end)
        end.setOnClickListener {
            pickDateTime(end)
        }

        view.findViewById<Button>(R.id.upload_image_button).setOnClickListener {
            chooseImage()
        }

        view.findViewById<Button>(R.id.submit_button).setOnClickListener {
            val title = view.findViewById<EditText>(R.id.event_title).text.toString()
            val description = view.findViewById<EditText>(R.id.event_description).text.toString()
            val location = view.findViewById<EditText>(R.id.event_location).text.toString()

            if (title == "" || startTimeInMilliseconds == 0.toLong() || endTimeInMilliseconds == 0.toLong()) {
                makeSnackbar("Missing required information.")
                return@setOnClickListener
            } else if (startTimeInMilliseconds > endTimeInMilliseconds) {
                makeSnackbar("Event cannot start after it ended.")
                return@setOnClickListener
            }

            this.uploadImage { response, downloadUrl ->
                var event: Event? = null

                if (response == ResponseType.SUCCESS) {
                    Log.d("post", "image was successfully uploaded")
                    event = Event(
                        title = title,
                        description = description,
                        location = location, start = Timestamp(Date(startTimeInMilliseconds)),
                        end = Timestamp(Date(endTimeInMilliseconds)),
                        uid = user!!.uid,
                        image = downloadUrl
                    )
                } else {
                    event = Event(
                        title = title,
                        description = description,
                        location = location, start = Timestamp(Date(startTimeInMilliseconds)),
                        end = Timestamp(Date(endTimeInMilliseconds)),
                        uid = user!!.uid
                    )
                }

                EventFirestore.postEvent(event) {
                    if (it == ResponseType.SUCCESS) {
                        Log.d("post", "successfully posted event")
                        val action = R.id.action_createEventFragment_to_eventFeedFragment
                        this.findNavController().navigate(action)
                    } else {
                        Log.d("post", "could not post event")
                    }
                }
            }
        }

        eventImageView = view.findViewById(R.id.event_image)
        eventImageView?.visibility = View.GONE

        eventCardView = view.findViewById(R.id.image_card_view)
        eventCardView?.visibility = View.GONE
    }

    private fun makeSnackbar(message: String) {
        Snackbar
            .make(requireView(), message, Snackbar.LENGTH_LONG)
            .apply { view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 4 }
            .show()
    }

    private fun uploadImage(completion: (response: ResponseType, downloadUrl: String) -> Unit ) {
        imageUri?.let {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("${uid}/${imageUri?.lastPathSegment ?: "unknown.jpg"}")
            val metadata = StorageMetadata.Builder().setContentType(imageType ?: "image/jpeg").build()

            val uploadTask = imageRef.putFile(imageUri!!, metadata)

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                imageRef.downloadUrl
            }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        completion(ResponseType.SUCCESS, task.result.toString())
                    } else {
                        completion(ResponseType.FAILURE, "")
                    }
                }
        } ?: run {
            completion(ResponseType.FAILURE, "")
        }
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        startActivityForResult(intent, 2020)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2020) {
            if (resultCode == Activity.RESULT_OK) {
                imageUri = data?.data
                imageType = data?.type
                eventImageView?.setImageURI(imageUri)
                eventImageView?.visibility = View.VISIBLE
                eventCardView?.visibility = View.VISIBLE
            }
        }
    }

    private fun pickDateTime(textView: TextView) {
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
                        if (textView == view?.findViewById<TextView>(R.id.event_start)) {
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

