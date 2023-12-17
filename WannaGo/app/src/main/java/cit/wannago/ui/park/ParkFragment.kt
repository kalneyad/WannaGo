package cit.wannago.ui.park
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cit.wannago.R
import cit.wannago.databinding.FragmentParkBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class ParkFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentParkBinding? = null
    private val binding get() = _binding!!

    private lateinit var mMap: GoogleMap
    private var markersList = mutableListOf<MarkerInfo>()
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParkBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize the map
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.parkMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
        val zoomLevel = 15f

        val homeLatLng = LatLng(latitude, longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
        mMap.addMarker(MarkerOptions().position(homeLatLng))

        // Set up a long click listener to add new markers
        mMap.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            val markerInfo = MarkerInfo("Marker ${markersList.size + 1}", latLng.latitude, latLng.longitude)
            markersList.add(markerInfo)

            // Add the new marker to Firebase
            db.collection("markers2")
                .add(markerInfo)
                .addOnSuccessListener { documentReference ->
                }
                .addOnFailureListener { e ->
                }

            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(markerInfo.title)
                    .snippet(snippet)
            )

            updateRecyclerView()
        }
    }

    private fun loadMarkersFromFirebase() {
        db.collection("markers2")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val title = document.getString("title") ?: ""
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0

                    val markerInfo = MarkerInfo(title, latitude, longitude)
                    markersList.add(markerInfo)

                    // Add markers to the map
                    mMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(latitude, longitude))
                            .title(title)
                    )
                }

                // Update the RecyclerView
                updateRecyclerView()
            }
            .addOnFailureListener { exception ->
                // Handle errors
            }
    }

    private fun updateRecyclerView() {
        val recyclerView = binding.parkRecyclerView
        val adapter = MarkerAdapter(markersList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
}
