package com.example.ecommerceapp.presentation.home

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    val drawerState = DrawerState(DrawerValue.Closed)

    fun openDrawer(scope: CoroutineScope) {
        scope.launch {
            drawerState.open()
        }
    }

    fun closeDrawer(scope: CoroutineScope) {
        scope.launch {
            drawerState.close()
        }
    }
}