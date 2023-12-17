package cit.wannago.ui.restaurant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cit.wannago.R
import cit.wannago.databinding.FragmentRestaurantBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale
import java.util.UUID

class RestaurantFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentRestaurantBinding? = null
    private val binding get() = _binding!!

    private lateinit var mMap: GoogleMap
    private var markersList = mutableListOf<MarkerInfo>()
    private val db = Firebase.firestore

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MarkerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurantBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize the map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the RecyclerView
        recyclerView = binding.recyclerView
        adapter = MarkerAdapter(markersList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Enable swipe-to-delete
        enableSwipeToDelete()

        // Load markers from Firebase and update the map
        loadMarkersFromFirebase()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val latitude = 37.422160
        val longitude = -122.084270
        val zoomLevel = 30f

        val homeLatLng = LatLng(latitude, longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
        mMap.addMarker(MarkerOptions().position(homeLatLng))

        // Center the map between all stored markers
        centerMapBetweenMarkers()

        mMap.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            val markerInfo = MarkerInfo("Marker ${markersList.size + 1}", latLng.latitude, latLng.longitude, UUID.randomUUID().toString())
            markersList.add(markerInfo)

            // Add the new marker to Firebase
            db.collection("markers1")
                .add(markerInfo)
                .addOnSuccessListener { documentReference ->
                }
                .addOnFailureListener { e ->
                }

            // Add the new marker to the map
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(markerInfo.title)
                    .snippet(snippet)
            )

            updateRecyclerView()

            centerMapBetweenMarkers()
        }

        loadMarkersFromFirebase()
    }


    private fun loadMarkersFromFirebase() {
        db.collection("markers1")
            .get()
            .addOnSuccessListener { result ->
                markersList.clear()
                for (document in result) {
                    val title = document.getString("title") ?: ""
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0

                    val markerInfo = MarkerInfo(title, latitude, longitude, document.id)
                    markersList.add(markerInfo)

                    // Add markers to the map
                    mMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(latitude, longitude))
                            .title(title)
                    )
                }

                // Update the RecyclerView
                adapter.notifyDataSetChanged()

                // Center the map between all stored markers
                centerMapBetweenMarkers()
            }
            .addOnFailureListener { exception ->
                // Handle errors
            }
    }

    private fun enableSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val markerInfo = markersList[position]

                // Remove the marker from Firestore
                db.collection("markers1")
                    .document(markerInfo.documentId)
                    .delete()
                    .addOnSuccessListener {
                        // Remove the marker from the map
                        mMap.clear()  // Clear all markers
                        loadMarkersFromFirebase()  // Reload markers from Firebase

                        // Remove the marker from the RecyclerView
                        markersList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                    }
                    .addOnFailureListener { exception ->
                        // Handle failure
                    }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun updateRecyclerView() {
        val recyclerView = binding.recyclerView
        val adapter = MarkerAdapter(markersList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun centerMapBetweenMarkers() {
        if (markersList.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            for (markerInfo in markersList) {
                builder.include(LatLng(markerInfo.latitude, markerInfo.longitude))
            }

            val bounds = builder.build()
            val padding = 100 // Adjust padding as needed
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

            mMap.animateCamera(cameraUpdate)
        }
    }
}
