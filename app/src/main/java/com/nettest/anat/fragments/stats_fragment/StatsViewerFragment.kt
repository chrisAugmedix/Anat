package com.nettest.anat.fragments.stats_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentHomeBinding
import com.nettest.anat.databinding.FragmentStatsViewerBinding

class StatsViewerFragment: Fragment(R.layout.fragment_stats_viewer) {

    private var _binding: FragmentStatsViewerBinding? = null
    private val binding get() = _binding!!
    private var selectOption: StatsOption = StatsOption.WIFI


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentStatsViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Want to initialize with Wifi stats first, have user select LTE as an option


        setWifiFragment()

        binding.statsWifiButton.setOnClickListener {
            if (selectOption != StatsOption.WIFI) {
                setWifiFragment()
            }
        }

        binding.statsLteButton.setOnClickListener {
            if (selectOption != StatsOption.LTE) {
                setLteFragment()
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun setWifiFragment() {
        selectOption = StatsOption.WIFI

        val ft: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        ft.replace(R.id.statsViewerFragmentContainer, ChildWiFiStatsFragment())
        ft.commit()

        binding.statsWifiButton.setTextColor(resources.getColor(R.color.midnight_blue))
        binding.statsLteButton.setTextColor(resources.getColor(R.color.gray))
    }

    private fun setLteFragment() {
        selectOption = StatsOption.LTE

        val ft: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        ft.replace(R.id.statsViewerFragmentContainer, ChildLteStatsFragment())
        ft.commit()

        binding.statsLteButton.setTextColor(resources.getColor(R.color.midnight_blue))
        binding.statsWifiButton.setTextColor(resources.getColor(R.color.gray))
    }
}

enum class StatsOption {
    WIFI,
    LTE,
    NONE
}