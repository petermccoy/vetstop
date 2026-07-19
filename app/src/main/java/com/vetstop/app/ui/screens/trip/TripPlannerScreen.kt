package com.vetstop.app.ui.screens.trip

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.vetstop.app.domain.geo.GeoUtils
import com.vetstop.app.domain.model.LocationCategory
import com.vetstop.app.domain.model.VisitFilter
import com.vetstop.app.ui.common.formatLastVisit
import kotlin.math.roundToInt

/**
 * Plan a trip: origin + destination, find saved locations within an estimated
 * detour time of the route, pick stops, then hand off to Google Maps for
 * navigation. After a route is planned the entry form collapses to a compact
 * summary so the stop list gets the screen space.
 */
@Composable
fun TripPlannerScreen(
    viewModel: TripViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.showTripForm) {
            TripEntryForm(state = state, viewModel = viewModel)
        } else {
            TripSummaryBar(state = state, onEdit = { viewModel.expandTripForm() })
        }

        SnackbarHost(hostState = snackbarHostState)

        val route = state.route
        if (route != null) {
            Text(
                text = "${state.candidates.size} location(s) within " +
                    "~${state.corridorMinutes.roundToInt()} min of route · " +
                    "${state.selectedPlaceIds.size} selected (max $MAX_WAYPOINTS)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            val cameraPositionState = rememberCameraPositionState()
            LaunchedEffect(route) {
                if (route.polyline.isEmpty()) return@LaunchedEffect
                val bounds = GeoUtils.boundingBox(route.polyline)
                try {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 64))
                } catch (_: Exception) {
                    // Map not laid out yet; fall back to centering on the route.
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(bounds.center, 9f)
                    )
                }
            }
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                cameraPositionState = cameraPositionState,
            ) {
                Polyline(points = route.polyline, color = Color(0xFF00696D), width = 8f)
                for (candidate in state.candidates) {
                    val selected = candidate.location.placeId in state.selectedPlaceIds
                    Marker(
                        state = MarkerState(
                            position = LatLng(
                                candidate.location.latitude,
                                candidate.location.longitude,
                            )
                        ),
                        title = candidate.location.name,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (selected) BitmapDescriptorFactory.HUE_GREEN
                            else BitmapDescriptorFactory.HUE_RED
                        ),
                        onInfoWindowClick = {
                            viewModel.toggleSelection(candidate.location.placeId)
                        },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.candidates, key = { it.location.placeId }) { candidate ->
                    val selected = candidate.location.placeId in state.selectedPlaceIds
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = {
                                    viewModel.toggleSelection(candidate.location.placeId)
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    candidate.location.name,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = LocationCategory
                                        .fromName(candidate.location.category).label +
                                        " · ≈${detourMinutes(candidate.distanceFromRouteMeters)}" +
                                        " min off route",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "Last visit: " +
                                        formatLastVisit(candidate.location.lastVisitAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, viewModel.buildNavigationUri())
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Navigation, contentDescription = null)
                Text(
                    if (state.selectedPlaceIds.isEmpty()) {
                        "  Navigate in Google Maps"
                    } else {
                        "  Navigate with ${state.selectedPlaceIds.size} stop(s)"
                    }
                )
            }
        }
    }
}

/** Estimated one-way detour in minutes for a given off-route distance. */
private fun detourMinutes(distanceMeters: Double): Int =
    (distanceMeters / GeoUtils.METERS_PER_DETOUR_MINUTE).roundToInt().coerceAtLeast(1)

@Composable
private fun TripEntryForm(
    state: TripUiState,
    viewModel: TripViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.origin,
            onValueChange = viewModel::setOrigin,
            label = { Text("Start (address or lat,lng)") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { viewModel.useCurrentLocationAsOrigin() }) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Use current location")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.destination,
            onValueChange = viewModel::setDestination,
            label = { Text("Destination") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Stops within ~${state.corridorMinutes.roundToInt()} min of route",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.2f),
            )
            Slider(
                value = state.corridorMinutes,
                onValueChange = viewModel::setCorridorMinutes,
                valueRange = 1f..15f,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (filter in VisitFilter.entries) {
                FilterChip(
                    selected = state.visitFilter == filter,
                    onClick = { viewModel.setVisitFilter(filter) },
                    label = { Text(filter.label) },
                )
            }
        }
        Button(
            onClick = { viewModel.planRoute() },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Plan route")
            }
        }
    }
}

/** Compact one-row summary shown once the form is folded away. */
@Composable
private fun TripSummaryBar(
    state: TripUiState,
    onEdit: () -> Unit,
) {
    val route = state.route
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route?.originLabel ?: state.origin,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "→ " + (route?.destinationLabel ?: state.destination),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (route != null) {
                    Text(
                        text = "${route.distanceText} · ${route.durationText}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit trip")
            }
        }
    }
}
