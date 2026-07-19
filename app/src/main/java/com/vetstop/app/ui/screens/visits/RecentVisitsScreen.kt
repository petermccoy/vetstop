package com.vetstop.app.ui.screens.visits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetstop.app.data.db.RecentVisit
import com.vetstop.app.data.repo.VisitRepository
import com.vetstop.app.ui.common.formatDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecentVisitsViewModel @Inject constructor(
    private val visitRepository: VisitRepository,
) : ViewModel() {

    val visits: StateFlow<List<RecentVisit>> =
        visitRepository.observeRecentWithLocation(limit = 100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun dismiss(visit: RecentVisit) {
        viewModelScope.launch { visitRepository.delete(visit.visit) }
    }
}

/**
 * Reverse-chronological list of logged visits. Each entry can be edited
 * (opens the visit form pre-filled) or dismissed (deleted).
 */
@Composable
fun RecentVisitsScreen(
    onEditVisit: (placeId: String, visitId: Long) -> Unit,
    viewModel: RecentVisitsViewModel = hiltViewModel(),
) {
    val visits by viewModel.visits.collectAsState()
    var visitPendingDismiss by remember { mutableStateOf<RecentVisit?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Recent visits",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (visits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No visits logged yet.\nLog one from any location's detail page.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(32.dp),
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visits, key = { it.visit.id }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(entry.locationName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatDateTime(entry.visit.visitedAt) +
                                (entry.visit.visitorName
                                    .takeIf { it.isNotBlank() }
                                    ?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "Found ${entry.visit.brochuresRemaining} remaining, " +
                                "left ${entry.visit.brochuresLeft}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (entry.visit.notes.isNotBlank()) {
                            Text(
                                text = entry.visit.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    onEditVisit(entry.visit.placeId, entry.visit.id)
                                },
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                Text("Edit")
                            }
                            TextButton(onClick = { visitPendingDismiss = entry }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }

    visitPendingDismiss?.let { entry ->
        AlertDialog(
            onDismissRequest = { visitPendingDismiss = null },
            title = { Text("Dismiss this visit?") },
            text = {
                Text(
                    "The visit to ${entry.locationName} on " +
                        "${formatDateTime(entry.visit.visitedAt)} will be permanently removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismiss(entry)
                        visitPendingDismiss = null
                    },
                ) { Text("Dismiss visit") }
            },
            dismissButton = {
                TextButton(onClick = { visitPendingDismiss = null }) { Text("Keep") }
            },
        )
    }
}
