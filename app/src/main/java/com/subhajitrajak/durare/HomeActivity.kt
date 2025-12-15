package com.subhajitrajak.durare

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.subhajitrajak.durare.exercise.ExerciseType
import com.subhajitrajak.durare.databinding.ActivityHomeBinding
import com.subhajitrajak.durare.ui.counter.CounterActivity
import com.subhajitrajak.durare.ui.dashboard.WorkoutSetupDialog
import com.subhajitrajak.durare.utils.Preferences
import com.subhajitrajak.durare.utils.ThemeSwitcher
import com.subhajitrajak.durare.utils.remove
import com.subhajitrajak.durare.utils.show

class HomeActivity : AppCompatActivity() {

    private val binding: ActivityHomeBinding by lazy {
        ActivityHomeBinding.inflate(layoutInflater)
    }

    private var isOffline = false
    private var lastDestinationIsNavScreen = false
    private fun currentFragmentIsNavigationScreen(): Boolean = lastDestinationIsNavScreen

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContentView(binding.root)

        setupNetworkCheck()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // makes sure bottom nav doesn't allocates extra bottom padding
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { _, insets ->
            insets
        }

        ThemeSwitcher.animateActivity(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val areNavigationScreens = (destination.id == R.id.dashboardFragment) || (destination.id == R.id.leaderboardFragment) || (destination.id == R.id.analyticsFragment)
            binding.bottomNav.visibility = if (areNavigationScreens) View.VISIBLE else View.GONE
            binding.view.visibility = if (areNavigationScreens) View.VISIBLE else View.GONE
            binding.startButton.visibility = if (areNavigationScreens) View.VISIBLE else View.GONE

            lastDestinationIsNavScreen = destination.id == R.id.dashboardFragment ||
                    destination.id == R.id.leaderboardFragment ||
                    destination.id == R.id.analyticsFragment
            maybeShowOrHideBanner()
        }

        binding.startButton.setOnClickListener {
            showWorkoutSetupDialog()
        }

        binding.offlineBanner.setOnClickListener {
            isOffline = !isInternetAvailable()
            maybeShowOrHideBanner()
        }
    }

    // opens the workout setup dialog to set custom rep count and rest time using time pickers
    private fun showWorkoutSetupDialog() {
        val dialog = WorkoutSetupDialog(0)
        dialog.onStartClick = { totalReps, restTimeMs ->
            dialog.binding.apply {
                // set preferences
                val prefs = Preferences.getInstance(this@HomeActivity)
                prefs.setTotalReps(totalReps)
                prefs.setRestTime(restTimeMs)

                // Start the workout with the specified parameters
                startWorkout(totalReps, restTimeMs)
                dialog.dismiss()
            }
        }
        dialog.show(supportFragmentManager, WorkoutSetupDialog.TAG)
    }

    // navigates to the counter activity with the specified parameters
    private fun startWorkout(totalReps: Int, restTimeMs: Long) {
        val intent = Intent(this, CounterActivity::class.java).apply {
            putExtra(Preferences.KEY_TOTAL_REPS, totalReps)
            putExtra(Preferences.KEY_REST_TIME, restTimeMs)
            putExtra("exercise_type", ExerciseType.PUSH_UP.name)
        }
        startActivity(intent)
    }

    private fun setupNetworkCheck() {
        connectivityManager = getSystemService(ConnectivityManager::class.java)

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOffline = false
                maybeShowOrHideBanner()
            }

            override fun onLost(network: Network) {
                isOffline = true
                maybeShowOrHideBanner()
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        // initial state
        isOffline = !isInternetAvailable()
    }

    private fun maybeShowOrHideBanner() {
        val isNavScreen = currentFragmentIsNavigationScreen()

        runOnUiThread {
            if (isNavScreen && isOffline) {
                binding.offlineBanner.show()
            } else {
                binding.offlineBanner.remove()
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}