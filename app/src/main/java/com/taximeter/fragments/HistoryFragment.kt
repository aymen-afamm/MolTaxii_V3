
package com.taximeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.taximeter.app.R
import com.taximeter.adapters.TripHistoryAdapter
import com.taximeter.models.TripHistory
import kotlinx.android.synthetic.main.fragment_history.*
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private lateinit var adapter: TripHistoryAdapter
    private val tripList = mutableListOf<TripHistory>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadTripHistory()
    }

    private fun setupRecyclerView() {
        adapter = TripHistoryAdapter(tripList)
        recyclerViewHistory.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewHistory.adapter = adapter
    }

    private fun loadTripHistory() {
        tripList.clear()
        tripList.addAll(getSampleTrips())
        adapter.notifyDataSetChanged()
        updateStatistics()
    }

    private fun getSampleTrips(): List<TripHistory> {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()

        return listOf(
            TripHistory(1, dateFormat.format(calendar.time), 12.5, 25, 25.75, "Centre Ville", "Agdal"),
            TripHistory(2, dateFormat.format(calendar.apply { add(Calendar.DAY_OF_MONTH, -1) }.time), 8.3, 18, 17.45, "Gare", "Hassan"),
            TripHistory(3, dateFormat.format(calendar.apply { add(Calendar.DAY_OF_MONTH, -1) }.time), 15.7, 32, 32.55, "Aéroport", "Souissi"),
            TripHistory(4, dateFormat.format(calendar.apply { add(Calendar.DAY_OF_MONTH, -1) }.time), 5.2, 12, 12.30, "Hay Riad", "Océan"),
            TripHistory(5, dateFormat.format(calendar.apply { add(Calendar.DAY_OF_MONTH, -2) }.time), 20.1, 40, 42.15, "Témara", "Salé")
        )
    }

    private fun updateStatistics() {
        val totalTrips = tripList.size
        val totalDistance = tripList.sumOf { it.distance }
        val totalRevenue = tripList.sumOf { it.fare }

        tvTotalTrips.text = totalTrips.toString()
        tvTotalDistance.text = String.format("%.1f km", totalDistance)
        tvTotalRevenue.text = String.format("%.2f DH", totalRevenue)
        tvAverageFare.text = String.format("%.2f DH", if (totalTrips > 0) totalRevenue / totalTrips else 0.0)
    }
}