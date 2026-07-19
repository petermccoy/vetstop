package com.vetstop.app.ui.screens.visit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetstop.app.data.db.VisitEntity
import com.vetstop.app.data.prefs.SettingsRepository
import com.vetstop.app.data.repo.LocationRepository
import com.vetstop.app.data.repo.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LogVisitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    locationRepository: LocationRepository,
    private val visitRepository: VisitRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val placeId: String = checkNotNull(savedStateHandle["placeId"])

    /** When > 0, the screen edits this existing visit instead of creating one. */
    val visitId: Long = savedStateHandle["visitId"] ?: -1L
    val isEditing: Boolean = visitId > 0

    private val _existingVisit = MutableStateFlow<VisitEntity?>(null)
    val existingVisit: StateFlow<VisitEntity?> = _existingVisit.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                _existingVisit.value = visitRepository.getById(visitId)
            }
        }
    }

    val locationName: StateFlow<String> =
        locationRepository.observeByIdWithLastVisit(placeId)
            .map { it?.name ?: "" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val defaultVisitorName: StateFlow<String> =
        settingsRepository.settings
            .map { it.visitorName }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun save(
        visitorName: String,
        visitedAt: Long,
        brochuresRemaining: Int,
        brochuresLeft: Int,
        notes: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val visit = VisitEntity(
                id = if (isEditing) visitId else 0,
                placeId = placeId,
                visitorName = visitorName.trim(),
                visitedAt = visitedAt,
                brochuresRemaining = brochuresRemaining,
                brochuresLeft = brochuresLeft,
                notes = notes.trim(),
            )
            if (isEditing) visitRepository.update(visit) else visitRepository.add(visit)
            // Logging a visit resolves any pending "to log" entry for this place.
            visitRepository.clearPending(placeId)
            if (visitorName.isNotBlank()) {
                settingsRepository.setVisitorName(visitorName.trim())
            }
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogVisitScreen(
    onDone: () -> Unit,
    viewModel: LogVisitViewModel = hiltViewModel(),
) {
    val locationName by viewModel.locationName.collectAsState()
    val defaultVisitorName by viewModel.defaultVisitorName.collectAsState()
    val existingVisit by viewModel.existingVisit.collectAsState()

    var visitorName by remember { mutableStateOf("") }
    var visitDate by remember { mutableStateOf(LocalDate.now()) }
    var visitTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var brochuresRemaining by remember { mutableStateOf("") }
    var brochuresLeft by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Prefill the name once settings load (unless the user already typed one).
    LaunchedEffect(defaultVisitorName) {
        if (visitorName.isBlank() && defaultVisitorName.isNotBlank()) {
            visitorName = defaultVisitorName
        }
    }

    // In edit mode, populate the form from the stored visit once it loads.
    LaunchedEffect(existingVisit) {
        existingVisit?.let { visit ->
            visitorName = visit.visitorName
            val dateTime = Instant.ofEpochMilli(visit.visitedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            visitDate = dateTime.toLocalDate()
            visitTime = dateTime.toLocalTime().withSecond(0).withNano(0)
            brochuresRemaining = visit.brochuresRemaining.toString()
            brochuresLeft = visit.brochuresLeft.toString()
            notes = visit.notes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit visit" else "Log visit") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (locationName.isNotBlank()) {
                Text(locationName, style = MaterialTheme.typography.titleLarge)
            }

            OutlinedTextField(
                value = visitorName,
                onValueChange = { visitorName = it },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(visitDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(visitTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                }
            }

            OutlinedTextField(
                value = brochuresRemaining,
                onValueChange = { brochuresRemaining = it.filter(Char::isDigit) },
                label = { Text("Brochures remaining at location (before restock)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = brochuresLeft,
                onValueChange = { brochuresLeft = it.filter(Char::isDigit) },
                label = { Text("Brochures left at location (this visit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val visitedAt = LocalDateTime.of(visitDate, visitTime)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    viewModel.save(
                        visitorName = visitorName,
                        visitedAt = visitedAt,
                        brochuresRemaining = brochuresRemaining.toIntOrNull() ?: 0,
                        brochuresLeft = brochuresLeft.toIntOrNull() ?: 0,
                        notes = notes,
                        onSaved = onDone,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (viewModel.isEditing) "Update visit" else "Save visit")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = visitDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            visitDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = visitTime.hour,
            initialMinute = visitTime.minute,
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Visit time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        visitTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
}
