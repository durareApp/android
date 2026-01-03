package com.subhajitrajak.durare.ui.dashboard

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.slider.Slider
import com.subhajitrajak.durare.R
import com.subhajitrajak.durare.data.models.AiUserStats
import com.subhajitrajak.durare.data.models.User
import com.subhajitrajak.durare.databinding.DialogSetGoalBinding
import com.subhajitrajak.durare.databinding.FragmentDashboardBinding
import com.subhajitrajak.durare.ui.askAi.AskAiFragment
import com.subhajitrajak.durare.utils.Preferences
import com.subhajitrajak.durare.utils.ThemeManager
import com.subhajitrajak.durare.utils.ThemeSwitcher
import com.subhajitrajak.durare.utils.formatToShortNumber
import com.subhajitrajak.durare.utils.formatWithCommas
import com.subhajitrajak.durare.utils.log
import com.subhajitrajak.durare.utils.removeWithAnim
import com.subhajitrajak.durare.utils.show
import com.subhajitrajak.durare.utils.showWithAnim50ms

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var borderAnimator: ValueAnimator? = null
    private var animator: ValueAnimator? = null

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireContext().applicationContext)
    }
    private var isDark: Boolean = false

    private lateinit var pref: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAnimation()

        pref = Preferences.getInstance(requireContext())

        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            // update UI
            binding.apply {
                todayText.text = stats.todayPushups.formatToShortNumber()
                last7Text.text = stats.last7Pushups.formatToShortNumber()
                last30Text.text = stats.last30Pushups.formatToShortNumber()
                lifetimeText.text = stats.lifetimePushups.formatToShortNumber()
                allUsersText.text = stats.allUsersTotal.formatWithCommas()
            }

            binding.statsCard.show()
            binding.globalCard.show()
            binding.globalCardHeader.show()
        }

        viewModel.monthlyPushupCounts.observe(viewLifecycleOwner) { counts ->
            generateLast30DaysHeatmap(counts)
        }

        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            val current = streak.first.formatToShortNumber()
            val highest = streak.second.formatToShortNumber()
            binding.streakText.text = current
            binding.currentStreakText.text = getString(R.string.current, current)
            binding.highestStreakText.text = getString(R.string.highest, highest)
        }

        viewModel.leaderboard.observe(viewLifecycleOwner) { users ->
            updateLeaderboard(users)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.loadingIndicator.showWithAnim50ms()
            } else {
                binding.loadingIndicator.removeWithAnim()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            binding.loadingIndicator.removeWithAnim()
            if (errorMsg != null) {
                log(errorMsg)
            }
        }

        // Trigger loading
        viewModel.loadDashboardStats()
        viewModel.fetchLast30DaysPushupCounts()
        viewModel.loadCurrentStreak()
        viewModel.loadLeaderboard()

        binding.apply {
            isDark = pref.isDarkTheme()
            switchThemesButton.setImageResource(if (isDark) R.drawable.moon else R.drawable.sun)

            switchThemesButton.setOnClickListener {
                if (isDark) {
                    ThemeManager.setDarkMode(requireContext(), false)
                    ThemeSwitcher.switchThemeWithAnimation(requireActivity(), false)
                    switchThemesButton.postDelayed({
                        switchThemesButton.setImageResource(R.drawable.sun)
                    }, ThemeSwitcher.DURATION)
                } else {
                    ThemeManager.setDarkMode(requireContext(), true)
                    ThemeSwitcher.switchThemeWithAnimation(requireActivity(), true)
                    switchThemesButton.postDelayed({
                        switchThemesButton.setImageResource(R.drawable.moon)
                    }, ThemeSwitcher.DURATION)
                }
                isDark = !isDark
            }

            settingsButton.setOnClickListener {
                findNavController().navigate(R.id.action_dashboardFragment_to_settingsFragment)
            }

            swipeRefresh.setOnRefreshListener {
                viewModel.loadDashboardStats()
                viewModel.fetchLast30DaysPushupCounts()
                viewModel.loadLeaderboard()
                viewModel.loadCurrentStreak()
                swipeRefresh.isRefreshing = false
            }

            swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.primary)
            swipeRefresh.setColorSchemeResources(
                R.color.white,
                R.color.black
            )

            askAiButton.setOnClickListener {
                val dashboardStats = viewModel.dashboardStats.value
                val last30PushupsList = viewModel.monthlyPushupCounts.value ?: emptyList()
                val streak = viewModel.currentStreak.value ?: (0 to 0)

                if (dashboardStats != null) {
                    val aiStats = AiUserStats(
                        totalPushups = dashboardStats.lifetimePushups,
                        averagePerDay = (dashboardStats.last30Pushups / 30f), // or calculate more accurately
                        currentStreak = streak.first,
                        highestStreak = streak.second,
                        last7Days = last30PushupsList.takeLast(7),
                        last30Days = last30PushupsList.takeLast(30)
                    )

                    val bundle = Bundle().apply {
                        putParcelable(AskAiFragment.AI_STATS, aiStats)
                    }

                    val extras = FragmentNavigatorExtras( askAiButton to "ask_ai_message")

                    findNavController().navigate(R.id.action_dashboardFragment_to_askAiFragment, bundle, null, extras)
                }
            }

            goalTextView.text = pref.getGoal().toString()
            goalCard.show()
            goalCard.setOnClickListener {
                showGoalDialog(
                    onPositiveClick = { goal ->
                        animator?.isRunning?.let { running ->
                            if (!running) {
                                pref.setGoal(goal)
                                goalTextView.text = goal.toString()
                            }
                        }
                    }
                )
            }
        }
    }

    private fun showGoalDialog(
        onPositiveClick: (Int) -> Unit,
        onNegativeClick: () -> Unit = {}
    ) {
        val dialogBinding = DialogSetGoalBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        animateSlider(dialogBinding.goalSlider, pref.getGoal().toFloat())

        dialogBinding.dialogCancel.setOnClickListener {
            dialog.dismiss()
            onNegativeClick()
        }

        dialogBinding.dialogOk.setOnClickListener {
            dialog.dismiss()
            val goal = dialogBinding.goalSlider.value.toInt()
            onPositiveClick(goal)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun animateSlider(slider: Slider, targetValue: Float) {
        val originalStep = slider.stepSize
        slider.stepSize = 0f

        animator = ValueAnimator.ofFloat(slider.value, targetValue)
        animator?.duration = 600
        animator?.addUpdateListener { anim ->
            slider.value = (anim.animatedValue as Float)
        }

        animator?.doOnEnd {
            slider.stepSize = originalStep
            slider.value = targetValue
        }

        animator?.start()
    }

    private fun setAnimation() {
        val orange = requireContext().getColor(R.color.primary)
        val yellow = "#FFD700".toColorInt()
        val red = requireContext().getColor(R.color.red)

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(orange, yellow, red)
        ).apply {
            cornerRadius = dpToPx(32).toFloat()
        }

        borderAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener { animation ->
                _binding?.let { binding ->
                    val progress = animation.animatedFraction
                    val newColors = intArrayOf(
                        blendColors(orange, yellow, progress),
                        blendColors(yellow, red, progress),
                        blendColors(red, orange, progress)
                    )
                    gradientDrawable.colors = newColors
                    binding.aiBorderContainer.background = gradientDrawable
                }
            }

            start()
        }
    }

    private fun blendColors(from: Int, to: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = Color.red(from) * inverseRatio + Color.red(to) * ratio
        val g = Color.green(from) * inverseRatio + Color.green(to) * ratio
        val b = Color.blue(from) * inverseRatio + Color.blue(to) * ratio
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    private fun updateLeaderboard(users: List<User>) {
        binding.apply {
            val positions = listOf(
                Triple(firstName, firstCount, firstImage),
                Triple(secondName, secondCount, secondImage),
                Triple(thirdName, thirdCount, thirdImage)
            )

            positions.zip(users.take(3)).forEach { (views, user) ->
                val (nameView, countView, imageView) = views

                nameView.text = user.userData.username
                countView.text = user.pushups.formatToShortNumber()

                Glide.with(requireContext())
                    .load(user.userData.profilePictureUrl)
                    .into(imageView)
            }

            leaderboardCardHeader.show()
            leaderboardCard.show()
        }
    }

    private fun generateLast30DaysHeatmap(streaks: List<Int>) {
        val daysInMonth = streaks.size
        val maxValue = streaks.maxOrNull() ?: 0
        val levels = 5

        binding.heatmapLayout.removeAllViews()
        for (day in 1..daysInMonth) {
            val circleView = TextView(requireContext())

            val size = dpToPx(18)
            val params = FlexboxLayout.LayoutParams(size, size)
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            circleView.layoutParams = params

            circleView.text = ""
            circleView.gravity = Gravity.CENTER

            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.day_circle)?.mutate()

            val value = streaks[day - 1]
            // determine level (0–4)
            val level = when {
                value == 0 -> 0
                maxValue == 0 -> 0
                else -> {
                    // scale contributions relative to maxValue
                    val ratio = value.toFloat() / maxValue
                    (ratio * (levels - 1)).toInt().coerceIn(1, levels - 1)
                }
            }

            // map level → color
            val colorRes = when (level) {
                0 -> R.color.level_0
                1 -> R.color.level_1
                2 -> R.color.level_2
                3 -> R.color.level_3
                else -> R.color.level_4
            }
            (drawable as GradientDrawable).setColor(requireContext().getColor(colorRes))

            circleView.background = drawable
            binding.heatmapLayout.addView(circleView)
        }

        binding.thisMonthCard.show()
        binding.thisMonthCardHeader.show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        borderAnimator?.cancel()
        borderAnimator = null
        animator?.cancel()
        animator = null
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.apply {
            loadDashboardStats()
            fetchLast30DaysPushupCounts()
            loadCurrentStreak()
            loadLeaderboard()
        }
    }
}