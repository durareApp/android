package com.subhajitrajak.durare.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.subhajitrajak.durare.R
import com.subhajitrajak.durare.data.models.DailyPushStats
import com.subhajitrajak.durare.databinding.FragmentAnalyticsBinding
import com.subhajitrajak.durare.ui.daySummary.DaySummaryFragment
import com.subhajitrajak.durare.utils.getMarker
import com.subhajitrajak.durare.utils.log
import com.subhajitrajak.durare.utils.removeWithAnim
import com.subhajitrajak.durare.utils.showWithAnim50ms

class AnalyticsFragment : Fragment() {
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by activityViewModels {
        AnalyticsViewModelFactory()
    }

    private lateinit var adapter: DailyRecordsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DailyRecordsAdapter { stats ->
            binding.loadingIndicator.showWithAnim50ms()
            navigateToNextScreen(stats)
        }
        binding.recordsRv.adapter = adapter

        setupCharts()

        viewModel.loadDailyStats()
        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            adapter.submitList(stats)
        }

        // Attach chart model producer
        binding.chartView.modelProducer = viewModel.modelProducer

        // Observe LiveData
        viewModel.loadThisMonthPushups()
        viewModel.pushupCounts.observe(viewLifecycleOwner) { counts ->
            if (counts.isNotEmpty()) {
                // Optionally animate or refresh
                binding.chartView.invalidate()
            }
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

        binding.swipeRefresh.apply {
            setOnRefreshListener {
                viewModel.loadDailyStats()
                viewModel.loadThisMonthPushups()
                isRefreshing = false
            }

            setProgressBackgroundColorSchemeResource(R.color.primary)
            setColorSchemeResources(
                R.color.white,
                R.color.black
            )
        }
    }

    private fun setupCharts() {
        val color = resources.getColor(R.color.primary, null)
        val lineProvider = LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(color)),
                areaFill = LineCartesianLayer.AreaFill.single(
                    Fill(
                        // gradient for chart fill
                        ShaderProvider.verticalGradient(
                            ColorUtils.setAlphaComponent(color, 102),
                            Color.TRANSPARENT
                        )
                    )
                ),
                pointConnector = LineCartesianLayer.PointConnector.cubic(0.5f)
            )
        )

        binding.chartView.apply {
            chart = chart!!.copy(
                (chart!!.layers[0] as LineCartesianLayer).copy(
                    lineProvider = lineProvider,
                    rangeProvider = CartesianLayerRangeProvider.auto()
                ),
                startAxis = (chart!!.startAxis as VerticalAxis).copy(
                    title = "Pushups Counts"
                ),
                bottomAxis = (chart!!.bottomAxis as HorizontalAxis).copy(
                    title = "Days of the month",
                    valueFormatter = { _, value, _ ->
                        (value.toInt() + 1).toString()
                    }
                ),
                marker = getMarker(requireContext())
            )

            this.modelProducer = modelProducer
        }
    }

    // navigates to the day summary screen
    private fun navigateToNextScreen(stats: DailyPushStats) {
        val bundle = Bundle().apply {
            putParcelable(DaySummaryFragment.STATS, stats)
        }
        findNavController().navigate(R.id.action_analyticsFragment_to_daySummaryFragment, bundle)
        binding.loadingIndicator.removeWithAnim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}