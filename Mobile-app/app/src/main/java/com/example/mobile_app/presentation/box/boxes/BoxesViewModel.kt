package com.example.mobile_app.presentation.box.boxes

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.viewModelScope
import com.example.mobile_app.domain.model.Box
import com.example.mobile_app.data.repository.StorageRepository
import com.example.mobile_app.presentation.BoxAppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject
import com.example.mobile_app.data.repository.AccountRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@HiltViewModel
class BoxesViewModel @Inject constructor(
    storageRepository: StorageRepository,
    private val accountRepository: AccountRepository
) : BoxAppViewModel() {

    val currentUserId: String
        get() = accountRepository.currentUserId


    // 1. Search Query State (Text typed by user)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 2. Filter Toggle State (Unused boxes > 1 year)
    private val _showUnusedOnly = MutableStateFlow(false)
    val showUnusedOnly = _showUnusedOnly.asStateFlow()

    var ownerNames = mutableStateMapOf<String, String>()
        private set

    // 3. Combined Flow: Merges DB data + Search Query + Filter Toggle
    // This replaces your old 'val boxes = storageService.userBoxes'
    val boxes = combine(
        storageRepository.userBoxes, // Source of truth from Firestore
        _searchQuery,
        _showUnusedOnly
    ) { boxesList, query, unusedOnly ->

        // Apply filters to the list
        val filteredBoxes = boxesList.filter { box ->

            // A. Search Filter (Title or Description)
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                box.title.contains(query, ignoreCase = true) ||
                        box.description.contains(query, ignoreCase = true)
            }

            // B. Unused Filter (> 1 year)
            // If the filter is ON, we check the date. If OFF, we accept everything.
            val matchesUnused = if (unusedOnly) {
                isBoxUnusedForOneYear(box.lastAccess)
            } else {
                true
            }

            // Both conditions must be true
            matchesQuery && matchesUnused
        }

        loadOwnerNames(filteredBoxes)

        filteredBoxes
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun loadOwnerNames(boxes: List<Box>) {
        launchCatching {
            val idsToLoad = boxes
                .filter { it.ownerId != currentUserId && !ownerNames.containsKey(it.ownerId) }
                .map { it.ownerId }
                .distinct()

            if (idsToLoad.isNotEmpty()) {

                val newNames = idsToLoad
                    .map { id ->
                        async { id to accountRepository.getUserName(id) }
                    }
                    .awaitAll()
                    .toMap()


                ownerNames.putAll(newNames)
            }
        }
    }

    // UI ACTIONS
    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleUnusedFilter() {
        _showUnusedOnly.value = !_showUnusedOnly.value
    }

    // HELPER FUNCTIONS

    //Checks if the timestamp is older than 365 days.
    private fun isBoxUnusedForOneYear(date: Date?): Boolean {
        if (date == null) return false

        // 1 Year in Milliseconds (1000ms * 60s * 60m * 24h * 365d)
        val oneYearInMillis = 365L * 24 * 60 * 60 * 1000

        val now = System.currentTimeMillis()
        val boxTime = date.time // Date.time gives milliseconds

        val diff = now - boxTime

        return diff > oneYearInMillis
    }
}