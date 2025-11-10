package com.taximeter.models

data class TripHistory(
    val id: Int,
    val date: String,
    val distance: Double,
    val duration: Int,
    val fare: Double,
    val startLocation: String,
    val endLocation: String
)