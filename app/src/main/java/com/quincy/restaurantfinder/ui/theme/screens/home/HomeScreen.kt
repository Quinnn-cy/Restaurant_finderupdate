package com.quincy.restaurantfinder.ui.theme.screens.home

import android.Manifest
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.quincy.restaurantfinder.data.model.Place
import com.quincy.restaurantfinder.models.LocationViewModel
import com.quincy.restaurantfinder.ui.theme.RestaurantFinderTheme
import com.quincy.restaurantfinder.ui.theme.components.PlaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetails: (String) -> Unit,
    viewModel: LocationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    // Page 0: Hospital Map, Page 1: Hospital List.
    val hospitalPagerState = rememberPagerState(initialPage = 1) { 2 }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    viewModel.updateLocation(location.latitude, location.longitude)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) {
            Log.e("LOCATION", "Permission error: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Health & Dine Finder", fontWeight = FontWeight.Bold) }
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        if (it.isEmpty()) {
                            viewModel.searchRestaurants("")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search places...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.searchRestaurants("")
                            }) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { 
                            if (searchQuery.isNotEmpty()) {
                                viewModel.searchRestaurants(searchQuery)
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (state.isLoading && state.nearbyRestaurants.isEmpty() && state.nearbyHospitals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    // Search Results Section
                    if (searchQuery.isNotEmpty() && state.searchResults.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                "Search Results",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        items(state.searchResults) { place ->
                            PlaceCard(place = place, onClick = { onNavigateToDetails(place.place_id ?: "") })
                        }
                        item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Main Map Section (Restaurants Only)
                    if (state.latitude != null && state.longitude != null) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                "Nearby Restaurants Map",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                        }
                        item(span = { GridItemSpan(2) }) {
                            val userLocation = remember(state.latitude, state.longitude) {
                                LatLng(state.latitude!!, state.longitude!!)
                            }
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(userLocation, 15f)
                            }

                            LaunchedEffect(userLocation) {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(userLocation, 15f)
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState,
                                    properties = MapProperties(isMyLocationEnabled = true)
                                ) {
                                    state.nearbyRestaurants.forEach { restaurant ->
                                        Marker(
                                            state = MarkerState(
                                                position = LatLng(
                                                    restaurant.geometry.location.lat,
                                                    restaurant.geometry.location.lng
                                                )
                                            ),
                                            title = restaurant.name ?: "Restaurant",
                                            snippet = restaurant.vicinity,
                                            onClick = {
                                                onNavigateToDetails(restaurant.place_id ?: "")
                                                true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Nearby Restaurants Section
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "Restaurants Near You",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    if (state.nearbyRestaurants.isEmpty() && !state.isLoading) {
                        item(span = { GridItemSpan(2) }) {
                            Text("No restaurants found nearby.", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(state.nearbyRestaurants) { place ->
                            PlaceCard(place = place, onClick = { onNavigateToDetails(place.place_id ?: "") })
                        }
                    }
                    

    }
}

@Composable
fun PlaceCard(place: Place, isHospital: Boolean = false, onClick: () -> Unit) {
    val apiKey = "AIzaSyAKwniuRGtvdnIBsOI5NnaToJ6wmFtwc6o"
    val photoReference = place.photos?.firstOrNull()?.photo_reference
    val imageUrl = if (photoReference != null) {
        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=$photoReference&key=$apiKey"
    } else {
        null
    }

    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.weight(1f)) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = place.name ?: "Place",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                        error = painterResource(id = android.R.drawable.ic_menu_report_image)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHospital) Icons.Default.LocalHospital else Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                place.rating?.let { rating ->
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                rating.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = place.name ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                place.vicinity?.let { vicinity ->
                    Text(
                        text = vicinity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}}}}


//@Preview(showBackground = false)
//@Composable
// fun HomePreview() {
//    RestaurantFinderTheme {
//        HomeScreen(onNavigateToDetails = {})
//    }}
