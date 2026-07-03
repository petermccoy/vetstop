package com.vetstop.app.ui.screens.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vetstop.app.data.db.LocationWithLastVisit
import com.vetstop.app.domain.model.LocationCategory
import com.vetstop.app.domain.model.VisitFilter
import com.vetstop.app.ui.common.formatLastVisit

/** Database of found locations, filterable by category and last-visit date. */
@Composable
fun LocationsScreen(
    onLocationClick: (String) -> Unit,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val locations by viewModel.locations.collectAsState()
    val visitFilter by viewModel.visitFilter.collectAsState()
    val enabledCategories by viewModel.enabledCategories.collectAsState()
    var showMap by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${locations.size} locations",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showMap = !showMap }) {
                Icon(
                    imageVector = if (showMap) Icons.AutoMirrored.Filled.List else Icons.Filled.Map,
                    contentDescription = if (showMap) "Show list" else "Show map",
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (category in LocationCategory.entries) {
                FilterChip(
                    selected = category in enabledCategories,
                    onClick = { viewModel.toggleCategory(category) },
                    label = { Text(category.label) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (filter in VisitFilter.entries) {
                FilterChip(
                    selected = visitFilter == filter,
                    onClick = { viewModel.setVisitFilter(filter) },
                    label = { Text(filter.label) },
                )
            }
        }

        if (showMap) {
            LocationsMap(locations = locations, onLocationClick = onLocationClick)
        } else {
            LocationsList(locations = locations, onLocationClick = onLocationClick)
        }
    }
}

@Composable
private fun LocationsList(
    locations: List<LocationWithLastVisit>,
    onLocationClick: (String) -> Unit,
) {
    if (locations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No locations yet.\nDraw a search area on the Area tab to get started.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(locations, key = { it.placeId }) { location ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLocationClick(location.placeId) },
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(location.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = LocationCategory.fromName(location.category).label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(location.address, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Last visit: ${formatLastVisit(location.lastVisitAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationsMap(
    locations: List<LocationWithLastVisit>,
    onLocationClick: (String) -> Unit,
) {
    val center = locations.firstOrNull()
        ?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(39.8283, -98.5795)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, if (locations.isEmpty()) 4f else 10f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
    ) {
        for (location in locations) {
            Marker(
                state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                title = location.name,
                snippet = "Last visit: ${formatLastVisit(location.lastVisitAt)}",
                onInfoWindowClick = { onLocationClick(location.placeId) },
            )
        }
    }
}
