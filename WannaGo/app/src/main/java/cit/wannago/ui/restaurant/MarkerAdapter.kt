package cit.wannago.ui.restaurant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cit.wannago.R

class MarkerAdapter(private val markers: List<MarkerInfo>) :
    RecyclerView.Adapter<MarkerAdapter.MarkerViewHolder>() {

    class MarkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val latLngTextView: TextView = itemView.findViewById(R.id.latLngTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.restaurant_marker_item, parent, false)
        return MarkerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
        val marker = markers[position]
        holder.titleTextView.text = marker.title
        holder.latLngTextView.text =
            "Lat: ${marker.latitude}, Long: ${marker.longitude}"
    }

    override fun getItemCount(): Int {
        return markers.size
    }
}
