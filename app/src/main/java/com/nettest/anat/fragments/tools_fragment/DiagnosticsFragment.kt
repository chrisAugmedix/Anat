package com.nettest.anat.fragments.tools_fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentDiagnosticsBinding

class DiagnosticsFragment: Fragment(R.layout.fragment_diagnostics)  {

    private var _binding: FragmentDiagnosticsBinding? = null
    private val binding get() = _binding!!
    private var isViewingSettingsPage = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentDiagnosticsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Initial Set
        setToolsView()
        binding.diagnosticsSettingsButton.setOnClickListener {
            setAppSettingsFragment()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun setAppSettingsFragment() {

        if (isViewingSettingsPage) {

            setToolsView()
            binding.diagnosticsSettingsButton.text = "Settings"
            isViewingSettingsPage = false

        } else {
            val ft: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            ft.replace(R.id.diagnostics_fragmentContainer, AppSettingsFragment())
            ft.commit()
            binding.diagnosticsSettingsButton.text = "Exit Settings"
            isViewingSettingsPage = true
        }

    }

    private fun setToolsView() {
        Log.d("DiagFrag", "setToolsView: Running")
        val ft: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        ft.replace(R.id.diagnostics_fragmentContainer, ToolsFragment())
        ft.commit()
    }

}