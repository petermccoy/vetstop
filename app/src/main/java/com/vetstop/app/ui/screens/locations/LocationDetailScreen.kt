package com.vetstop.app.ui.screens.locations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetstop.app.data.db.LocationWithLastVisit
import com.vetstop.app.data.db.VisitEntity
import com.vetstop.app.data.repo.LocationRepository
import com.vetstop.app.data.repo.VisitRepository
import com.vetstop.app.domain.model.LocationCategory
import com.vetstop.app.ui.common.formatDateTime
import com.vetstop.app.ui.common.formatLastVisit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    locationRepository: LocationRepository,
    visitRepository: VisitRepository,
) : ViewModel() {

    val placeId: String = checkNotNull(savedStateHandle["placeId"])

    val location: StateFlow<LocationWithLastVisit?> =
        locationRepository.observeByIdWithLastVisit(placeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val visits: StateFlow<List<VisitEntity>> =
        visitRepository.observeForPlace(placeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    onLogVisit: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationDetailViewModel = hiltViewModel(),
) {
    val location by viewModel.location.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(location?.name ?: "Location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = location
        if (current == null) {
            Text(
                text = "Location not found.",
                modifier = Modifier.padding(innerPadding).padding(16.dp),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column {
                    Text(
                        text = LocationCategory.fromName(current.category).label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(current.address, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Last visit: ${formatLastVisit(current.lastVisitAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = { onLogVisit(current.placeId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Text("  Log visit")
                        }
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse(
                                    "google.navigation:q=${current.latitude},${current.longitude}"
                                )
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                "https://www.google.com/maps/dir/?api=1" +
                                                    "&destination=${current.latitude},${current.longitude}"
                                            ),
                                        )
                                    )
                                }
                            },
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = null)
                            Text("  Navigate")
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Visit history (${visits.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (visits.isEmpty()) {
                item { Text("No visits logged yet.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(visits, key = { it.id }) { visit ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = formatDateTime(visit.visitedAt),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (visit.visitorName.isNotBlank()) {
                            Text(
                                text = "By ${visit.visitorName}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = "Brochures: found ${visit.brochuresRemaining} remaining, " +
                                "left ${visit.brochuresLeft}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (visit.notes.isNotBlank()) {
                            Text(visit.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
