package com.egghead.events

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import io.grpc.okhttp.internal.Platform
import java.text.SimpleDateFormat
import java.util.*

// Goals:
// Allow a user to update an event if they created it

class UpdateEventFragment : Fragment() {

    val args: UpdateEventFragmentArgs by navArgs()

    var startTimeInMilliseconds : Long = 0
    var endTimeInMilliseconds : Long = 0
    var eventImageView: ImageView? = null
    var eventCardView: CardView? = null
    var imageUri : Uri? = null
    var imageType : String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_update_event, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val event : Event = args.event

        startTimeInMilliseconds = event.start.toDate().time
        endTimeInMilliseconds = event.end.toDate().time

        val titleView: TextView = view.findViewById(R.id.event_title)
        val descriptionView: TextView = view.findViewById(R.id.event_description)
        val startTimestampView: TextView = view.findViewById(R.id.event_start)
        val endTimestampView: TextView = view.findViewById(R.id.event_end)
        val locationView : TextView = view.findViewById(R.id.event_location)

        titleView.text = event.title
        descriptionView.text = event.description
        locationView.text = event.location

        val formatter = SimpleDateFormat("MMM dd yyyy HH:mm", Locale.US)
        startTimestampView.text = formatter.format(event.start.toDate())
        endTimestampView.text = formatter.format(event.end.toDate())

        startTimestampView.setOnClickListener {
            pickDateTime(startTimestampView)
        }

        endTimestampView.setOnClickListener {
            pickDateTime(endTimestampView)
        }

        view.findViewById<Button>(R.id.upload_image_button).setOnClickListener {
            chooseImage()
        }

        view.findViewById<Button>(R.id.submit_button).setOnClickListener {


            event.title = titleView.text.toString()
            event.description = descriptionView.text.toString()
            event.location = locationView.text.toString()
            event.start = Timestamp(Date(startTimeInMilliseconds))
            event.end = Timestamp(Date(endTimeInMilliseconds))


            if (event.title == "" || startTimeInMilliseconds == 0.toLong() || endTimeInMilliseconds == 0.toLong()) {
                makeSnackbar("Missing required information.")
                return@setOnClickListener
            } else if (startTimeInMilliseconds > endTimeInMilliseconds) {
                makeSnackbar("Event cannot start after it ended.")
                return@setOnClickListener
            }

            this.uploadImage { response, downloadUrl ->

                if (response == ResponseType.SUCCESS) {
                    event.image = downloadUrl
                }

                EventFirestore.updateEvent(event) { response ->
                    if (response == ResponseType.SUCCESS) {
                        Log.d("post", "successfully updated event")
                        val action : NavDirections = UpdateEventFragmentDirections.actionUpdateEventFragmentToDisplayEventFragment(event)
                        findNavController().navigate(action)
                    } else {
                        Log.d("post", "could not update event")
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