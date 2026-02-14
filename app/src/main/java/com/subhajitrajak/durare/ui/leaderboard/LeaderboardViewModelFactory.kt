package com.subhajitrajak.durare.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.subhajitrajak.durare.data.repositories.DashboardRepository

class LeaderboardViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
            val repository = DashboardRepository()
            @Suppress("UNCHECKED_CAST")
            return LeaderboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}