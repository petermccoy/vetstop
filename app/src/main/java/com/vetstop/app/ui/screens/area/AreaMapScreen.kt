package com.vetstop.app.ui.screens.area

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.vetstop.app.data.db.PolygonCodec
import com.vetstop.app.data.db.SearchAreaEntity

/**
 * Map screen where the user draws polygon search areas. Tap points while in
 * drawing mode, then save; the weekly sync searches each saved area.
 */
@Composable
fun AreaMapScreen(
    viewModel: AreaViewModel = hiltViewModel(),
) {
    val areas by viewModel.areas.collectAsState()
    val draftVertices by viewModel.draftVertices.collectAsState()
    val isDrawing by viewModel.isDrawing.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var areaPendingDelete by remember { mutableStateOf<SearchAreaEntity?>(null) }

    val context = LocalContext.current
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Default camera: center of the most recent area, else continental US.
    val initialCenter = areas.firstOrNull()
        ?.let { PolygonCodec.decode(it.polygon).firstOrNull() }
        ?: LatLng(39.8283, -98.5795)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, if (areas.isEmpty()) 4f else 10f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            onMapClick = { point -> viewModel.addVertex(point) },
        ) {
            // Saved areas
            for (area in areas) {
                val points = PolygonCodec.decode(area.polygon)
                if (points.size >= 3) {
                    Polygon(
                        points = points,
                        fillColor = Color(0x3300696D),
                        strokeColor = Color(0xFF00696D),
                        strokeWidth = 4f,
                        clickable = true,
                        onClick = { areaPendingDelete = area },
                    )
                }
            }
            // Draft polygon being drawn
            if (draftVertices.isNotEmpty()) {
                if (draftVertices.size >= 3) {
                    Polygon(
                        points = draftVertices,
                        fillColor = Color(0x33F4BE48),
                        strokeColor = Color(0xFF7A5900),
                        strokeWidth = 4f,
                    )
                } else {
                    Polyline(
                        points = draftVertices,
                        color = Color(0xFF7A5900),
                        width = 4f,
                    )
                }
                for ((index, vertex) in draftVertices.withIndex()) {
                    Marker(
                        state = MarkerState(position = vertex),
                        title = "Point ${index + 1}",
                    )
                }
            }
        }

        if (isDrawing) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Tap the map to add boundary points " +
                            "(${draftVertices.size} placed, need at least 3)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { viewModel.undoVertex() }) { Text("Undo") }
                        OutlinedButton(onClick = { viewModel.cancelDrawing() }) { Text("Cancel") }
                        Button(
                            onClick = { showNameDialog = true },
                            enabled = draftVertices.size >= 3,
                        ) { Text("Save area") }
                    }
                }
            }
        } else {
            ExtendedFloatingActionButton(
                onClick = { viewModel.startDrawing() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Draw area") },
            )
        }
    }

    if (showNameDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Name this area") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Area name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveDraft(name)
                        showNameDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            },
        )
    }

    areaPendingDelete?.let { area ->
        AlertDialog(
            onDismissRequest = { areaPendingDelete = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete \"${area.name}\"?") },
            text = {
                Text(
                    "Locations already found stay in your database, but this " +
                        "area will no longer be searched weekly."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteArea(area)
                        areaPendingDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { areaPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}
