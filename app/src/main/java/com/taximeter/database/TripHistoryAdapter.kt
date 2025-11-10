
package com.taximeter.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.taximeter.app.R
import com.taximeter.models.TripHistory
import kotlinx.android.synthetic.main.item_trip_history.view.*

class TripHistoryAdapter(private val trips: List<TripHistory>) :
    RecyclerView.Adapter<TripHistoryAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_history, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount() = trips.size

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(trip: TripHistory) {
            itemView.tvDate.text = trip.date
            itemView.tvFare.text = String.format("%.2f DH", trip.fare)
            itemView.tvStartLocation.text = trip.startLocation
            itemView.tvEndLocation.text = trip.endLocation
            itemView.tvDistance.text = String.format("📏 %.1f km", trip.distance)
            itemView.tvDuration.text = String.format("⏱️ %d min", trip.duration)
        }
    }
}