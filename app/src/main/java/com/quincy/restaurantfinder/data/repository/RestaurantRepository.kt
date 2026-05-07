package com.quincy.restaurantfinder.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.quincy.restaurantfinder.models.Restaurant

class RestaurantRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getRestaurants(
        onSuccess: (List<Restaurant>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        db.collection("restaurants")
            .get()
            .addOnSuccessListener { result ->

                val list = result.map { document ->

                    Restaurant(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        rating = document.getDouble("rating") ?: 0.0,
                        location = document.getString("location") ?: "",

                    )
                }

                onSuccess(list)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    fun addRestaurant(
        restaurant: Restaurant,
        onComplete: (() -> Unit)? = null
    ) {
        db.collection("restaurants")
            .add(restaurant)
            .addOnSuccessListener {
                onComplete?.invoke()
            }
    }

    fun deleteRestaurant(
        id: String,
        onComplete: () -> Unit
    ) {

        db.collection("restaurants")
            .document(id)
            .delete()
            .addOnSuccessListener {
                onComplete()
            }
    }
    fun updateRestaurant(
        restaurant: Restaurant,
        onComplete: () -> Unit
    ) {

        db.collection("restaurants")
            .document(restaurant.id)
            .set(restaurant)
            .addOnSuccessListener {
                onComplete()
            }
    }




}