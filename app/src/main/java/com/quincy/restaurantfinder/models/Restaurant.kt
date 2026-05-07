package com.quincy.restaurantfinder.models

import com.quincy.restaurantfinder.R

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val rating: Double = 0.0,
    val location: String = "",
    val imageRes: Int = R.drawable.img,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String="",
    val imageUrl: String="",
)

