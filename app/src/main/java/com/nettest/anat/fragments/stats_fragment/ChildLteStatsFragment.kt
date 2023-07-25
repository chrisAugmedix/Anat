package com.nettest.anat.fragments.stats_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentStatsChildLteBinding

class ChildLteStatsFragment: Fragment(R.layout.fragment_stats_child_lte) {

    private var _binding: FragmentStatsChildLteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentStatsChildLteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}