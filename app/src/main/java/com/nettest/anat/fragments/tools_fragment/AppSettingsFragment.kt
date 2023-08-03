package com.nettest.anat.fragments.tools_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentDiagnosticsAppSettingsViewBinding

class AppSettingsFragment: Fragment(R.layout.fragment_diagnostics_app_settings_view) {

    private var _binding: FragmentDiagnosticsAppSettingsViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentDiagnosticsAppSettingsViewBinding.inflate(inflater, container, false)
        return binding.root
    }






}