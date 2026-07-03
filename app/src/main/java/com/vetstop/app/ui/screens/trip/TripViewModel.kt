package com.vetstop.app.ui.screens.trip

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.vetstop.app.data.prefs.SettingsRepository
import com.vetstop.app.data.repo.LocationAlongRoute
import com.vetstop.app.data.repo.LocationRepository
import com.vetstop.app.data.repo.PlannedRoute
import com.vetstop.app.data.repo.RouteRepository
import com.vetstop.app.domain.geo.GeoUtils
import com.vetstop.app.domain.model.VisitFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TripUiState(
    val origin: String = "",
    val destination: String = "",
    val corridorMiles: Float = SettingsRepository.DEFAULT_CORRIDOR_MILES,
    val visitFilter: VisitFilter = VisitFilter.ANY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val route: PlannedRoute? = null,
    val candidates: List<LocationAlongRoute> = emptyList(),
    val selectedPlaceIds: Set<String> = emptySet(),
) {
    val selectedCandidates: List<LocationAlongRoute>
        get() = candidates.filter { it.location.placeId in selectedPlaceIds }
}

/** Google Maps directions URLs allow at most 9 intermediate waypoints. */
const val MAX_WAYPOINTS = 9

@HiltViewModel
class TripViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.value = _uiState.value.copy(corridorMiles = settings.corridorMiles)
        }
    }

    fun setOrigin(value: String) {
        _uiState.value = _uiState.value.copy(origin = value)
    }

    fun setDestination(value: String) {
        _uiState.value = _uiState.value.copy(destination = value)
    }

    fun setCorridorMiles(miles: Float) {
        _uiState.value = _uiState.value.copy(corridorMiles = miles)
        viewModelScope.launch { settingsRepository.setCorridorMiles(miles) }
        refilterCandidates()
    }

    fun setVisitFilter(filter: VisitFilter) {
        _uiState.value = _uiState.value.copy(visitFilter = filter)
        refilterCandidates()
    }

    /** Caller must have already obtained the location permission. */
    @SuppressLint("MissingPermission")
    fun useCurrentLocationAsOrigin() {
        viewModelScope.launch {
            try {
                val location = fusedLocationClient
                    .getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token,
                    )
                    .await()
                if (location != null) {
                    setOrigin("${location.latitude},${location.longitude}")
                } else {
                    _uiState.value = _uiState.value.copy(error = "Current location unavailable")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Could not get current location: ${e.message}"
                )
            }
        }
    }

    fun planRoute() {
        val state = _uiState.value
        if (state.origin.isBlank() || state.destination.isBlank()) {
            _uiState.value = state.copy(error = "Enter both a start and a destination")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            routeRepository.getRoute(state.origin, state.destination)
                .onSuccess { route ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        route = route,
                        selectedPlaceIds = emptySet(),
                    )
                    refilterCandidates()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Could not plan route",
                    )
                }
        }
    }

    private fun refilterCandidates() {
        val route = _uiState.value.route ?: return
        viewModelScope.launch {
            val all = locationRepository.observeActiveWithLastVisit().first()
            val state = _uiState.value
            val now = System.currentTimeMillis()
            val filtered = all.filter { state.visitFilter.matches(it.lastVisitAt, now) }
            val candidates = routeRepository.locationsAlongRoute(
                route = route,
                locations = filtered,
                maxDistanceMeters = state.corridorMiles * GeoUtils.METERS_PER_MILE,
            )
            val stillVisible = candidates.map { it.location.placeId }.toSet()
            _uiState.value = _uiState.value.copy(
                candidates = candidates,
                selectedPlaceIds = state.selectedPlaceIds intersect stillVisible,
            )
        }
    }

    fun toggleSelection(placeId: String) {
        val state = _uiState.value
        val selected = state.selectedPlaceIds
        _uiState.value = state.copy(
            selectedPlaceIds = when {
                placeId in selected -> selected - placeId
                selected.size >= MAX_WAYPOINTS -> selected // cap reached
                else -> selected + placeId
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Builds a Google Maps directions URL with the selected locations as
     * ordered stops. Opens in the Google Maps app when installed.
     */
    fun buildNavigationUri(): Uri {
        val state = _uiState.value
        val waypoints = state.selectedCandidates.joinToString("|") {
            "${it.location.latitude},${it.location.longitude}"
        }
        val builder = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
            .appendQueryParameter("api", "1")
            .appendQueryParameter("origin", state.origin)
            .appendQueryParameter("destination", state.destination)
            .appendQueryParameter("travelmode", "driving")
        if (waypoints.isNotEmpty()) {
            builder.appendQueryParameter("waypoints", waypoints)
        }
        return builder.build()
    }
}
