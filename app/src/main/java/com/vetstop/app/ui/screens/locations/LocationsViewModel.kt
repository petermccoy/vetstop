package com.vetstop.app.ui.screens.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetstop.app.data.db.LocationWithLastVisit
import com.vetstop.app.data.repo.LocationRepository
import com.vetstop.app.domain.model.LocationCategory
import com.vetstop.app.domain.model.VisitFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LocationsViewModel @Inject constructor(
    locationRepository: LocationRepository,
) : ViewModel() {

    private val _visitFilter = MutableStateFlow(VisitFilter.ANY)
    val visitFilter: StateFlow<VisitFilter> = _visitFilter.asStateFlow()

    private val _enabledCategories = MutableStateFlow(LocationCategory.entries.toSet())
    val enabledCategories: StateFlow<Set<LocationCategory>> = _enabledCategories.asStateFlow()

    val locations: StateFlow<List<LocationWithLastVisit>> = combine(
        locationRepository.observeActiveWithLastVisit(),
        _visitFilter,
        _enabledCategories,
    ) { all, filter, categories ->
        val now = System.currentTimeMillis()
        all.filter { location ->
            LocationCategory.fromName(location.category) in categories &&
                filter.matches(location.lastVisitAt, now)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setVisitFilter(filter: VisitFilter) {
        _visitFilter.value = filter
    }

    fun toggleCategory(category: LocationCategory) {
        val current = _enabledCategories.value
        _enabledCategories.value =
            if (category in current && current.size > 1) current - category else current + category
    }
}
