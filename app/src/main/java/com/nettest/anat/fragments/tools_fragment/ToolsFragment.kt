package com.nettest.anat.fragments.tools_fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentDiagnosticsToolsViewBinding
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.net.InetAddress
import java.text.DecimalFormat

@SuppressLint("SetTextI18n")
class ToolsFragment: Fragment(R.layout.fragment_diagnostics_tools_view) {

    private var _binding: FragmentDiagnosticsToolsViewBinding? = null
    private val binding get() = _binding!!
    private val speedTestSocket     by lazy { SpeedTestSocket().apply { socketTimeout = 5000 } }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentDiagnosticsToolsViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        var inputAddress: String? = null
        val spinner: Spinner = getSpinnerObj()
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?,
                position: Int, id: Long
            ) {
                when (position) {

                    0 -> { } // Do Nothing
                    1 -> {  //Ping
                        viewTextAndButton("Run Ping", resources.getText(R.string.spinner_ping), true)
                    }
                    2 -> {  //Speed Test
                        viewTextAndButton("Start Download", resources.getText(R.string.spinner_speedTest), false)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        binding.diagnosticsUserInputEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) return@setOnEditorActionListener false
            if ( v.text.toString().isEmpty() ) return@setOnEditorActionListener false
            CoroutineScope(Dispatchers.Default).launch {
                if (!isReachable(v.text.toString())) buttonSetError()
                else {
                    activity?.runOnUiThread {
                        binding.diagnosticsToolsButton.setBackgroundColor(resources.getColor(R.color.light_midnight_blue, null))
                        binding.diagnosticsToolsButton.text = "Run Ping"
                        binding.diagnosticsToolsButton.isClickable = true
                        binding.diagnosticsToolsButton.isEnabled = true
                    }
                }
            }

            inputAddress = v.text.toString()
            false
        }

        binding.diagnosticsToolsButton.setOnClickListener {
            //ping
            CoroutineScope(Dispatchers.Default).launch {
                if (spinner.selectedItemPosition == 1) {
                   //TODO: New method for ping, WIP

                    inputAddress?.let {
                        if (!isReachable(it)) { return@let }
                        binding.diagnosticsToolsButton.setBackgroundColor(resources.getColor(R.color.light_midnight_blue, null))
                        activity?.runOnUiThread {
                            binding.diagnosticsUserInputEditText.alpha = .5f
                            binding.diagnosticsUserInputEditText.isEnabled = false
                            binding.diagnosticsTextResult.text = ""
                            binding.diagnosticsToolsButton.text = "Ping in Progress"
                            binding.diagnosticsToolsButton.isClickable = false
                            binding.diagnosticsToolsButton.isEnabled = false
                        }

                        runPing(it)

                        activity?.runOnUiThread {
                            binding.diagnosticsUserInputEditText.alpha = 1f
                            binding.diagnosticsUserInputEditText.isEnabled = true
                            binding.diagnosticsToolsButton.text = "Run Ping"
                            binding.diagnosticsToolsButton.isClickable = true
                            binding.diagnosticsToolsButton.isEnabled = true
                        }

                    }

                }

                if (spinner.selectedItemPosition == 2) {
                    speedTestSocket.startDownload("https://grafana.augmedix.com:1201/v2/api/dl/10MB")
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.diagnosticsToolsButton.text = "Downloading..."
                        binding.diagnosticsToolsButton.isClickable = false
                        binding.diagnosticsToolsButton.isEnabled = false
                    }
                }

            }

        }



        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {
            override fun onCompletion(report: SpeedTestReport?) {
                val speedMb = DecimalFormat("#,###.00").format(report?.transferRateBit?.div(
                    BigDecimal(1000000)
                ))
                val testProgress = getProgressBarString(100)
                val speedTestStatus = "\n[COMPLETED] Speed Rate: $speedMb Mbps"

                activity?.let {
                    it.runOnUiThread {
                        binding.diagnosticsTextResult.text = "$testProgress\n$speedTestStatus"
                        binding.diagnosticsToolsButton.text = "Start Download"
                        binding.diagnosticsToolsButton.isClickable = true
                        binding.diagnosticsToolsButton.isEnabled = true
                    }
                }
            }

            override fun onProgress(percent: Float, report: SpeedTestReport?) {
                val speedMb = DecimalFormat("#,###.00").format(report?.transferRateBit?.div(BigDecimal(1000000)))
                val testProgress = getProgressBarString(percent.toInt())
                val speedTestStatus = "\n[PROGRESS] Rate in mb/s: $speedMb"
                activity?.let {
                    it.runOnUiThread {
                        binding.diagnosticsTextResult.text = "$testProgress\n$speedTestStatus"
                    }
                }
            }

            override fun onError(speedTestError: SpeedTestError?, errorMessage: String?) {
                activity?.let {
                    it.runOnUiThread {
                        binding.diagnosticsTextResult.text = "Error -- $errorMessage"
                    }
                }
            }

        })

        super.onViewCreated(view, savedInstanceState)
    }

    private fun viewTextAndButton(buttonText: String, description: CharSequence, enableInput: Boolean) {
        //TextView
        binding.diagnosticsTextResult.text = ""
        binding.diagnosticsToolDescriptionDynamic.text = description
        binding.diagnosticsTextResult.textAlignment = View.TEXT_ALIGNMENT_CENTER

        //Button
        val buttonColor = ContextCompat.getColor(requireContext(), R.color.light_midnight_blue)
        binding.diagnosticsToolsButton.text = buttonText
        binding.diagnosticsToolsButton.setBackgroundColor(buttonColor)
        binding.diagnosticsToolsButton.isEnabled = true
        binding.diagnosticsToolsButton.isClickable = true
        binding.diagnosticsToolsButton.setTextColor(Color.WHITE)
        binding.diagnosticsToolsButton.visibility = View.VISIBLE

        if (enableInput) binding.diagnosticsUserInputEditText.visibility = View.VISIBLE
        else binding.diagnosticsUserInputEditText.visibility = View.GONE
    }

    private fun getProgressBarString(currentProgress: Int, totalProgress: Int = 100): String {

        // Calculate percentage and bar length
        val percentage = (currentProgress.toDouble() / totalProgress * 100).toInt()
        val barLength = 20

        // Calculate filled and empty bar lengths
        val filledLength = (percentage.toDouble() / 100 * barLength).toInt()
        val emptyLength = barLength - filledLength

        // Build progress bar string
        val bar = "#".repeat(filledLength) + "-".repeat(emptyLength)
        return "$percentage% [$bar] $totalProgress%"


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

    private fun buttonSetError() {
        activity?.runOnUiThread {
            binding.diagnosticsToolsButton.setBackgroundColor(Color.RED)
            binding.diagnosticsToolsButton.text = "Incorrect/Empty Address Entered"
            binding.diagnosticsToolsButton.isClickable = false
            binding.diagnosticsToolsButton.isEnabled = false
        }
    }

    private fun runPing(address: String) {
        val runtime = Runtime.getRuntime()
        try {
            val cmd = runtime.exec("/system/bin/ping -c 4 $address")
            val output = BufferedReader(InputStreamReader(cmd.inputStream))

            var outputCount = 0
            output.forEachLine {

                activity?.runOnUiThread {
                    if (outputCount == 0) binding.diagnosticsTextResult.append("$it\n\n")
                    else binding.diagnosticsTextResult.append("$it\n")
                }
                outputCount++
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isReachable(name: String): Boolean {
        return try {
            InetAddress.getByName(name)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


}