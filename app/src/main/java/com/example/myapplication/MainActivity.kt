package com.example.myapplication

import androidx.compose.foundation.layout.* // Import necessary layout modifiers
import androidx.compose.material3.* // Import Material 3 components
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import java.util.Locale


class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            LocationInputScreen(fusedLocationClient)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // Permissions granted. Now location updates can be requested.
                    // The setContent call is moved to the permission granted block
                    setContent {
                        LocationInputScreen(fusedLocationClient)
                    }
                } else {
                    // Permissions denied. Handle it appropriately.
                }
            }
        }
    }
}

@Composable
fun LocationInputScreen(fusedLocationClient: FusedLocationProviderClient) {
    var latText by remember { mutableStateOf(TextFieldValue("")) }
    var lngText by remember { mutableStateOf(TextFieldValue("")) }
    var showMap by remember { mutableStateOf(false) }
    var targetLocation by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (!showMap) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Latitude") },
                isError = errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = lngText,
                onValueChange = { lngText = it },
                label = { Text("Longitude") },
                isError = errorMessage != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(onClick = {
                try {
                    val lat = latText.text.toDouble()
                    val lng = lngText.text.toDouble()

                    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                        errorMessage = "Latitude must be between -90 and 90, Longitude between -180 and 180"
                        return@Button
                    }

                    errorMessage = null
                    targetLocation = LatLng(lat, lng)
                    showMap = true
                } catch (e: NumberFormatException) {
                    errorMessage = "Invalid latitude or longitude format"
                }
            }) {
                Text("Show Map")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "Development of mobile applications (First Individual)",
                modifier = Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    } else {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent), // Make background transparent
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Button(
                onClick = { showMap = false },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.Start),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Back")
            }

            MapsDemo(fusedLocationClient, targetLocation)
        }
    }
}

@Composable
fun MapsDemo(fusedLocationClient: FusedLocationProviderClient, targetLocation: LatLng) {
    val cameraPositionState = rememberCameraPositionState()
    var currentLocation by remember { mutableStateOf(targetLocation) }
    var locationName by remember { mutableStateOf("Fetching location...") } // Store area name

    val context = LocalContext.current

    // Fetch location name when targetLocation changes
    LaunchedEffect(targetLocation) {
        currentLocation = targetLocation
        cameraPositionState.position = CameraPosition.fromLatLngZoom(targetLocation, 15f)

        locationName = getLocationName(context, targetLocation.latitude, targetLocation.longitude)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = locationName,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(compassEnabled = false)
        )
    }
}

// Function to get location name from latitude and longitude
fun getLocationName(context: Context, lat: Double, lng: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
            addresses[0].getAddressLine(0) ?: "Unknown location"
        } else {
            "Unknown location"
        }
    } catch (e: Exception) {
        Log.e("Geocoder", "Failed to get address: ${e.localizedMessage}")
        "Location not found"
    }
}