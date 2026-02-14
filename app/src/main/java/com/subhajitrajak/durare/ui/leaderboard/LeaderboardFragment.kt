package com.subhajitrajak.durare.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.subhajitrajak.durare.R
import com.subhajitrajak.durare.data.models.User
import com.subhajitrajak.durare.databinding.FragmentLeaderboardBinding
import com.subhajitrajak.durare.utils.formatToShortNumber
import com.subhajitrajak.durare.utils.hideWithAnim
import com.subhajitrajak.durare.utils.log
import com.subhajitrajak.durare.utils.show
import com.subhajitrajak.durare.utils.showWithAnim50ms

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaderboardViewModel by activityViewModels {
        LeaderboardViewModelFactory()
    }
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadLeaderboard()

        adapter = LeaderboardAdapter(requireContext())
        binding.leaderboard.adapter = adapter

        viewModel.leaderboard.observe(viewLifecycleOwner) { newUsers ->
            adapter.submitList(newUsers)

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val rank = newUsers.indexOfFirst { it.userData.userId == currentUserId } + 1

            if (rank > 0) {
                val currentUser = newUsers[rank - 1]

                binding.userRankText.text = getString(R.string.you_are_at_rank, rank)
                binding.rankComment.text = getRankComment(rank)
                binding.userRankScore.text = currentUser.pushups.toString()
            }

            setupTop3(newUsers)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.loadingIndicator.showWithAnim50ms()
            } else {
                binding.loadingIndicator.hideWithAnim()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            binding.loadingIndicator.hideWithAnim()
            if (errorMsg != null) {
                log(errorMsg)
            }
        }
    }

    private fun setupTop3(newUsers: List<User>) {
        if (newUsers.size < 3) return
        val list = newUsers.take(3)

        binding.apply {
            firstPersonName.text = list[0].userData.username
            secondPersonName.text = list[1].userData.username
            thirdPersonName.text = list[2].userData.username

            firstPersonScore.text = list[0].pushups.formatToShortNumber()
            secondPersonScore.text = list[1].pushups.formatToShortNumber()
            thirdPersonScore.text = list[2].pushups.formatToShortNumber()

            Glide.with(this@LeaderboardFragment).apply {
                load(list[0].userData.profilePictureUrl).into(firstPersonImage)
                load(list[1].userData.profilePictureUrl).into(secondPersonImage)
                load(list[2].userData.profilePictureUrl).into(thirdPersonImage)
            }

            top3Card.show()
        }
    }

    private fun getRankComment(rank: Int): String {
        return when (rank) {
            1 -> "🏆 You’re on top!"
            in 2..5 -> "🔥 Almost there!"
            in 6..10 -> "💪 Keep pushing!"
            else -> "👏 Stay consistent!"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}