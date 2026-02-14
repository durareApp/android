package com.subhajitrajak.durare.ui.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhajitrajak.durare.data.models.User
import com.subhajitrajak.durare.data.repositories.DashboardRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LeaderboardViewModel(private val repository: DashboardRepository) : ViewModel() {
    private val _leaderboard = MutableLiveData<List<User>>()
    val leaderboard: LiveData<List<User>> get() = _leaderboard

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadLeaderboard() {
        _loading.value = true
        viewModelScope.launch {
            repository.fetchLeaderboard()
                .catch { e ->
                    _error.value = e.message
                    _loading.value = false
                }
                .collect { users ->
                    _leaderboard.value = users
                    _loading.value = false
                }
        }
    }
}