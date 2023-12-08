package com.nettest.anat.fragments.stats_fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.nettest.anat.R
import com.nettest.anat.SessionData
import com.nettest.anat.WifiMetrics
import com.nettest.anat.databinding.FragmentStatsChildWifiBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@SuppressLint("SetTextI18n")
class ChildWiFiStatsFragment: Fragment(R.layout.fragment_stats_child_wifi) {

    private var _binding: FragmentStatsChildWifiBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionViewModel by activityViewModels()

    private val rssiChart by lazy { binding.rssiChart }
    private val lrDownChart by lazy { binding.rxReceivedChart }
    private val lrUpChart by lazy { binding.txRateChart }
    private val stDownChart by lazy { binding.downloadChart }

    private var sessionWifiMetrics: MutableList<Pair<WifiMetrics, Long>> = mutableListOf()
    private var sessionDownloadMetrics: MutableList<Pair<Double, Long>> = mutableListOf()
    private var rssiDataSet: LineDataSet? = null
    private var lrDownDataSet: LineDataSet? = null
    private var lrUpDataSet: LineDataSet? = null
    private var stDownDataSet: LineDataSet? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentStatsChildWifiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.selectedSession.observe(viewLifecycleOwner) { sessionData ->

            clearAllGraphs()
            updateAllStats(sessionData)
            updateAllGraphs()

        }

        setupChart(rssiChart)
        setupChart(lrDownChart)
        setupChart(lrUpChart)
        setupChart(stDownChart)

        binding.rssiTitleText.setOnClickListener {
            if (!binding.rssiLayoutContainer.isVisible) binding.rssiLayoutContainer.visibility = View.VISIBLE
            else binding.rssiLayoutContainer.visibility = View.GONE
        }

        binding.rxRateTitleText.setOnClickListener {
            if (!binding.rxRateContainer.isVisible) binding.rxRateContainer.visibility = View.VISIBLE
            else binding.rxRateContainer.visibility = View.GONE
        }

        binding.txRateTitleText.setOnClickListener {
            if (!binding.txRateContainer.isVisible) binding.txRateContainer.visibility = View.VISIBLE
            else binding.txRateContainer.visibility = View.GONE
        }

        binding.downloadTestTitleText.setOnClickListener {
            if (!binding.downloadTestContainer.isVisible) binding.downloadTestContainer.visibility = View.VISIBLE
            else binding.downloadTestContainer.visibility = View.GONE
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun updateAllGraphs() {

        if (sessionWifiMetrics.isEmpty()) return

        val mFormat = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
        val ts = sessionWifiMetrics.first().second
        val date = mFormat.format(Date(ts))
        binding.dateTextView.text = "Testing Date: $date"

        val metricTimestamps = sessionWifiMetrics.map { it.second }
        //RSSI
        rssiDataSet?.let { updateChart(rssiChart, metricTimestamps, it, "dBm")  }
        updateRssiText()

        //LINK RATE DOWN
        lrDownDataSet?.let { updateChart(lrDownChart, metricTimestamps, it, "Mbps") }
        updateRxText()

        //LINK RATE UP
        lrUpDataSet?.let { updateChart(lrUpChart, metricTimestamps, it, "Mbps") }
        updateTxText()

        //SPEED TEST
        val speedTestTimestamps = sessionDownloadMetrics.map { it.second }
        if (speedTestTimestamps.isEmpty()) return
        stDownDataSet?.let { updateChart(stDownChart, speedTestTimestamps, it, "Mbps") }
        updateDownloadText()

    }

    private fun updateRssiText() {
        if ( sessionWifiMetrics.isEmpty() ) return
        val rssiList = sessionWifiMetrics.map { it.first.rssi }
        val rssiAvg = rssiList.average().toInt()
        val rssiHigh = rssiList.maxOrNull()
        val rssiLow = rssiList.minBy { it }
        binding.rssiAvg.text = "$rssiAvg dBm"
        binding.rssiLowest.text = "$rssiLow dBm"
        binding.rssiHighest.text = "$rssiHigh dBm"
    }

    private fun updateRxText() {
        if (sessionWifiMetrics.isEmpty()) return
        val rxList = sessionWifiMetrics.map {it.first.linkRateRx}
        val rxAvg = rxList.average().toInt()
        val rxHigh = rxList.maxOrNull()
        val rxLow = rxList.minBy { it }
        binding.rxRateAvg.text = "$rxAvg Mbps"
        binding.rxRateLowest.text = "$rxLow Mbps"
        binding.rxRateHighest.text = "$rxHigh Mbps"
    }

    private fun updateTxText() {
        if (sessionWifiMetrics.isEmpty()) return
        val txList = sessionWifiMetrics.map {it.first.linkRateTx}
        val txAvg = txList.average().toInt()
        val txHigh = txList.maxOrNull()
        val txLow = txList.minBy { it }
        binding.txRateAvg.text = "$txAvg Mbps"
        binding.txRateHighest.text = "$txHigh Mbps"
        binding.txRateLowest.text = "$txLow Mbps"
    }

    private fun updateDownloadText() {
        if (sessionDownloadMetrics.isEmpty()) return

        val resultList = sessionDownloadMetrics.map { it.first }
        val resultAvg = resultList.average().toInt()
        val resultHigh = resultList.maxOrNull()
        val resultLow = resultList.minBy { it }
        binding.downloadAvg.text = "$resultAvg Mbps"
        binding.downloadHighest.text = "$resultHigh Mbps"
        binding.downloadLowest.text = "$resultLow Mbps"


    }

    private fun clearAllGraphs() {
        try { rssiChart.data.clearValues() } catch (_: Exception) {}
        try { lrDownChart.data.clearValues() } catch (_: Exception) {}
        try { lrUpChart.data.clearValues() } catch (_: Exception) {}
        try { stDownChart.data.clearValues() } catch (_: Exception) {}

        rssiChart.notifyDataSetChanged()
        rssiChart.invalidate()
        lrDownChart.notifyDataSetChanged()
        lrDownChart.invalidate()
        lrUpChart.notifyDataSetChanged()
        lrUpChart.invalidate()
        stDownChart.notifyDataSetChanged()
        stDownChart.invalidate()
        sessionDownloadMetrics.clear()
        sessionWifiMetrics.clear()

    }

    private fun setupChart(chart: LineChart) {

        chart.apply {
            xAxis.enableGridDashedLine(10f, 10f, 0f)
            xAxis.setDrawGridLines(true)
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1F
            xAxis.isGranularityEnabled = true
            axisLeft.setDrawGridLines(false)
            axisLeft.disableGridDashedLine()
            axisLeft.granularity = 1f
            axisRight.isEnabled = false
            xAxis.labelRotationAngle = 20f
            extraBottomOffset = 20f
            extraRightOffset = 40f
            chart.setVisibleXRange(6f, 10f)
            chart.legend.isEnabled = false
            chart.setScaleMinima(1f, 1f)
            invalidate()
        }

    }

    private fun updateChart(chart: LineChart, timestampList: List<Long>, dataSet: LineDataSet, metricPostfix: String ) {

        val dataSetCount = try { chart.data.dataSetCount } catch (_: Exception) { 0 }
        if ( dataSetCount == 0 ) chart.data = LineData(dataSet)
        else chart.data.addDataSet(dataSet)
        chart.setVisibleXRangeMaximum(7f)
        chart.xAxis.setLabelCount(7, false)
        chart.xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            val format = SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH)
            override fun getFormattedValue(value: Float): String {
                return try {
                    val result = timestampList[value.toInt()]
                    format.format(Date(result))
                } catch (e: Exception) {
                    value.toString()
                }
            }
        }

        chart.axisLeft.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()} $metricPostfix"
            }
        }




    }

    private fun updateAllStats(sessionData: SessionData) {
        //All metrics except download, since download data is in room list
        val sessionWifiMetricsPair = sessionData.metricDataList.map { Pair(it.wifiMetrics, it.timestamp) }
        sessionWifiMetrics.addAll(sessionWifiMetricsPair)

        //All download metrics
        val downloadMetrics = sessionData.roomDataList.map { Pair( it.getSpeedTestResult().toDouble(), it.getEnd() ) }
        sessionDownloadMetrics.addAll(downloadMetrics)

        rssiDataSet = getLineData(getEntryList(sessionWifiMetrics, WifiMetricName.RSSI), "RSSI", ContextCompat.getColor(requireContext(), R.color.line_blue), WifiMetricName.RSSI)
        lrDownDataSet = getLineData(getEntryList(sessionWifiMetrics, WifiMetricName.LINKDOWN), "Rx Link Rate", ContextCompat.getColor(requireContext(), R.color.line_blue), WifiMetricName.LINKDOWN)
        lrUpDataSet = getLineData(getEntryList(sessionWifiMetrics, WifiMetricName.LINKUP), "Tx Link Rate", ContextCompat.getColor(requireContext(), R.color.line_blue), WifiMetricName.LINKUP)
        stDownDataSet = getLineData(getEntryListFromSpeedTestResult(sessionDownloadMetrics), "Result", ContextCompat.getColor(requireContext(), R.color.line_orange), WifiMetricName.TESTDOWN)

    }

    private fun getEntryList(list: MutableList<Pair<WifiMetrics, Long>>, type: WifiMetricName): List<Entry> {
        if (list.isEmpty()) return emptyList()
        var indexCount = 0
        return when(type) {
            WifiMetricName.RSSI -> {list.map { Entry(indexCount++.toFloat(), (it.first as WifiMetrics).rssi.toFloat()) }}
            WifiMetricName.LINKDOWN -> {list.map { Entry(indexCount++.toFloat(), (it.first as WifiMetrics).linkRateRx.toFloat()) }}
            WifiMetricName.LINKUP -> {list.map { Entry(indexCount++.toFloat(), (it.first as WifiMetrics).linkRateTx.toFloat()) }}
            else -> {return emptyList()
            }
        }
    }

    private fun getEntryListFromSpeedTestResult(list: MutableList<Pair<Double, Long>>): List<Entry> {
        if (list.isEmpty()) return emptyList()
        var indexCount = 0
        return list.map { Entry(indexCount++.toFloat(), it.first.toFloat()) }

    }

    private fun getLineData(entryList: List<Entry>, label: String, color: Int, metricName: WifiMetricName): LineDataSet {
        val lineData = LineDataSet(entryList, label)
        lineData.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineData.valueTextSize = 12f
        lineData.lineWidth = 1f
        lineData.axisDependency = YAxis.AxisDependency.LEFT
        lineData.color = color
        lineData.circleHoleRadius = 1f
        lineData.cubicIntensity = .2f
        lineData.setCircleColor(color)
        lineData.circleHoleColor = color
        lineData.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when(metricName) {
                    WifiMetricName.TESTDOWN -> { "$value"}
                    WifiMetricName.RSSI -> { "${value.toInt()}" }
                    WifiMetricName.LINKDOWN -> { "${value.toInt()}" }
                    WifiMetricName.LINKUP -> { "${value.toInt()} " }
                }
            }
        }
        return lineData
    }

    enum class WifiMetricName {
        RSSI,
        LINKDOWN,
        LINKUP,
        TESTDOWN
    }

}