package com.subhajitrajak.durare.ui.counter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.subhajitrajak.durare.databinding.NeedHelpTutorialSheetBinding

class NeedHelpTutorialSheet: BottomSheetDialogFragment() {
    private var _binding: NeedHelpTutorialSheetBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NeedHelpTutorialSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}