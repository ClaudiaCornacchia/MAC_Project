package com.example.mobile_app.screens.box.box_detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mobile_app.model.Box
import com.example.mobile_app.model.service.StorageService
import com.example.mobile_app.screens.BoxAppViewModel

@HiltViewModel
class BoxDetailViewModel @Inject constructor(
    private val storageService: StorageService,
    savedStateHandle: SavedStateHandle // Here we get navigation arguments
) : BoxAppViewModel() {

    // "boxId" must match the name in the route definition
    private val boxId: String = savedStateHandle["boxId"] ?: ""

    var box by mutableStateOf(Box())
        private set

    init {
        launchCatching {
            // Load box data immediately
            val loadedBox = storageService.getBox(boxId)
            if (loadedBox != null) {
                box = loadedBox
            }
        }
    }
}