package com.taximeter.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.taximeter.app.databinding.ItemTripHistoryBinding
import com.taximeter.models.TripHistory

class TripHistoryAdapter(private val trips: List<TripHistory>) :
    RecyclerView.Adapter<TripHistoryAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount() = trips.size

    class TripViewHolder(private val binding: ItemTripHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trip: TripHistory) {
            binding.tvDate.text = trip.date
            binding.tvFare.text = String.format("%.2f DH", trip.fare)
            binding.tvStartLocation.text = trip.startLocation
            binding.tvEndLocation.text = trip.endLocation
            binding.tvDistance.text = String.format("📏 %.1f km", trip.distance)
            binding.tvDuration.text = String.format("⏱️ %d min", trip.duration)
        }
    }
}