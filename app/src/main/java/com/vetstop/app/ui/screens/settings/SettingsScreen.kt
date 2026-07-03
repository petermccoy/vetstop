package com.vetstop.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.vetstop.app.data.prefs.Settings
import com.vetstop.app.data.prefs.SettingsRepository
import com.vetstop.app.data.repo.LocationRepository
import com.vetstop.app.ui.common.formatDateTime
import com.vetstop.app.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    locationRepository: LocationRepository,
) : ViewModel() {

    val settings: StateFlow<Settings?> =
        settingsRepository.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val locationCount: StateFlow<Int> =
        locationRepository.observeActiveCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isSyncRunning: StateFlow<Boolean> =
        syncScheduler.observeSyncNow()
            .map { infos -> infos.any { !it.state.isFinished } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setVisitorName(name: String) {
        viewModelScope.launch { settingsRepository.setVisitorName(name) }
    }

    fun syncNow() {
        syncScheduler.syncNow()
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val locationCount by viewModel.locationCount.collectAsState()
    val isSyncRunning by viewModel.isSyncRunning.collectAsState()

    var name by remember { mutableStateOf("") }
    LaunchedEffect(settings?.visitorName) {
        if (name.isBlank()) name = settings?.visitorName.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                viewModel.setVisitorName(it)
            },
            label = { Text("Default visitor name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Location database", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$locationCount active locations",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Last sync: " + (settings?.lastSyncAt?.let { formatDateTime(it) }
                        ?: "never"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                settings?.lastSyncSummary?.let { summary ->
                    Text(summary, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "Search areas are refreshed automatically once a week " +
                        "(requires a network connection).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = { viewModel.syncNow() },
                    enabled = !isSyncRunning,
                ) {
                    Text(if (isSyncRunning) "Syncing…" else "Sync now")
                }
            }
        }
    }
}
