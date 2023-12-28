package com.nettest.anat.fragments.stats_fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nettest.anat.R
import com.nettest.anat.SessionData
import com.nettest.anat.databinding.FragmentHomeBinding
import com.nettest.anat.databinding.FragmentStatsViewerBinding
import com.nettest.anat.global_sessionDataList

class StatsViewerFragment: Fragment(R.layout.fragment_stats_viewer) {

    private var _binding: FragmentStatsViewerBinding? = null
    private val binding get() = _binding!!
    private var selectOption: StatsOption = StatsOption.WIFI
    private val viewModel: SessionViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentStatsViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Want to initialize with Wifi stats first, have user select LTE as an option
        val spinner = getSpinnerObject()
        val pattern = Regex(getString(R.string.regex_session_id))
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, positionId: Long) {
                //Change data to display

                val sessionName = parent?.getItemAtPosition(position).toString()
                if ( sessionName ==  "No Previous Sessions Found" ) return
                if ( position == 0 ) return

                val matches     = pattern.find(sessionName) ?: return
                val sessionId   = matches.groupValues[1].toInt()
                val session     = global_sessionDataList.find { it.sessionId == sessionId } ?: return
                viewModel.updateSelectedSession(session)

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //Do Nothing
            }

        }

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

//        loadLastSession(spinner)

        super.onViewCreated(view, savedInstanceState)
    }

    private fun loadLastSession(spinner: Spinner) {
        val count = spinner.adapter.count
        if (count < 1) return
        spinner.setSelection(1)
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

    private fun getSpinnerObject(): Spinner {

        val spinner: Spinner = binding.sessionSelectionSpinner
        val sessionOptions = let {
            if ( global_sessionDataList.isEmpty() ) return@let arrayOf("No Sessions Available")
            val itemList = global_sessionDataList.map { "${it.sessionName} - (${it.sessionId})" }.toMutableList()
            itemList.add(0, "Tap Here To Select Session...")
            return@let itemList.toTypedArray()
        }

        val adapter = object: ArrayAdapter<String>(requireContext(), R.layout.diagnostics_spinner_view, sessionOptions) {

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

enum class StatsOption {
    WIFI,
    LTE,
    NONE
}

class SessionViewModel: ViewModel() {
    val selectedSession = MutableLiveData<SessionData>()
    fun updateSelectedSession(sessionData: SessionData) { selectedSession.value = sessionData }

}