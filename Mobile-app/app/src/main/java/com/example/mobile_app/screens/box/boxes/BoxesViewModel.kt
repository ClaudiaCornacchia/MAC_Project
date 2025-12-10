package com.example.mobile_app.screens.box.boxes

import com.example.mobile_app.model.service.StorageService
import com.example.mobile_app.screens.BoxAppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BoxesViewModel @Inject constructor(
    storageService: StorageService
) : BoxAppViewModel() {
    // Expose the flow from service directly to the UI
    val boxes = storageService.userBoxes
}