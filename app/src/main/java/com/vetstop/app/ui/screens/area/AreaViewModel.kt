package com.vetstop.app.ui.screens.area

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.vetstop.app.data.db.PolygonCodec
import com.vetstop.app.data.db.SearchAreaEntity
import com.vetstop.app.data.repo.SearchAreaRepository
import com.vetstop.app.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AreaViewModel @Inject constructor(
    private val searchAreaRepository: SearchAreaRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val areas: StateFlow<List<SearchAreaEntity>> =
        searchAreaRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draftVertices = MutableStateFlow<List<LatLng>>(emptyList())
    val draftVertices: StateFlow<List<LatLng>> = _draftVertices.asStateFlow()

    private val _isDrawing = MutableStateFlow(false)
    val isDrawing: StateFlow<Boolean> = _isDrawing.asStateFlow()

    fun startDrawing() {
        _draftVertices.value = emptyList()
        _isDrawing.value = true
    }

    fun cancelDrawing() {
        _draftVertices.value = emptyList()
        _isDrawing.value = false
    }

    fun addVertex(point: LatLng) {
        if (_isDrawing.value) {
            _draftVertices.value = _draftVertices.value + point
        }
    }

    fun undoVertex() {
        _draftVertices.value = _draftVertices.value.dropLast(1)
    }

    /** Persists the draft polygon and kicks off an immediate place search. */
    fun saveDraft(name: String) {
        val vertices = _draftVertices.value
        if (vertices.size < 3) return
        viewModelScope.launch {
            searchAreaRepository.add(
                SearchAreaEntity(
                    name = name.ifBlank { "Area ${System.currentTimeMillis() % 10_000}" },
                    polygon = PolygonCodec.encode(vertices),
                )
            )
            syncScheduler.syncNow()
        }
        cancelDrawing()
    }

    fun deleteArea(area: SearchAreaEntity) {
        viewModelScope.launch { searchAreaRepository.delete(area) }
    }
}
