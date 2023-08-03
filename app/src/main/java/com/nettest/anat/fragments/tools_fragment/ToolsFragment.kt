package com.nettest.anat.fragments.tools_fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentDiagnosticsAppSettingsViewBinding
import com.nettest.anat.databinding.FragmentDiagnosticsToolsViewBinding

class ToolsFragment: Fragment(R.layout.fragment_diagnostics_tools_view) {

    private var _binding: FragmentDiagnosticsToolsViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentDiagnosticsToolsViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val spinner: Spinner = getSpinnerObj()
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?,
                position: Int, id: Long
            ) {
                Log.d("onItemSelected", "onItemSelected: $position")
                when (position) {

                    0 -> { } // Do Nothing
                    1 -> {  //Ping
                        binding.diagnosticsToolDescriptionDynamic.text = resources.getText(R.string.spinner_ping)
                        binding.diagnosticsUserInputEditText.visibility = View.VISIBLE
                    }
                    2 -> {  //Traceroute
                        binding.diagnosticsToolDescriptionDynamic.text = resources.getText(R.string.spinner_traceroute)
                        binding.diagnosticsUserInputEditText.visibility = View.VISIBLE
                    }
                    3 -> {  //Speed Test
                        binding.diagnosticsToolDescriptionDynamic.text = resources.getText(R.string.spinner_speedTest)
                        binding.diagnosticsUserInputEditText.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }



        super.onViewCreated(view, savedInstanceState)
    }

    private fun getSpinnerObj(): Spinner {

        val spinner: Spinner = binding.diagnosticSpinner
        val items = resources.getStringArray(R.array.network_commands)

        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.diagnostics_spinner_view, items) {

            override fun isEnabled(position: Int): Boolean { return position !=0 }

            override fun getDropDownView(
                position: Int, convertView: View?, parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                if (position == 0) view.setTextColor(Color.GRAY)
                else view.setTextColor(Color.BLACK)
                return view
            }

        }

        adapter.setDropDownViewResource(R.layout.diagnostics_spinner_view)

        spinner.adapter = adapter
        return spinner
    }

}