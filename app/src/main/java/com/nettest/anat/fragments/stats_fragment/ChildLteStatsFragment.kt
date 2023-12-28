package com.nettest.anat.fragments.stats_fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.FrameMetricsAggregator.MetricType
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.nettest.anat.CellMetrics
import com.nettest.anat.MetricData
import com.nettest.anat.R
import com.nettest.anat.Utility
import com.nettest.anat.databinding.FragmentStatsChildLteBinding
import okhttp3.internal.notify
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetTextI18n")
class ChildLteStatsFragment: Fragment(R.layout.fragment_stats_child_lte) {

    private var _binding: FragmentStatsChildLteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionViewModel by activityViewModels()
    private val chart by lazy { binding.lteChart }
    private val TAG = "ChildLteStatsFragment"
    private var sessionCellMetrics: MutableList<Pair<CellMetrics, Long>> = mutableListOf()
    private var rssiDataSet: LineDataSet? = null
    private var rsrqDataSet: LineDataSet? = null
    private var rsrpDataSet: LineDataSet? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentStatsChildLteBinding.inflate(inflater, container, false)

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setAllCheckBoxes(false)

        viewModel.selectedSession.observe(viewLifecycleOwner) { session ->

            newSessionSelected()
            val cellMetrics = session.metricDataList.filter { it.cellMetrics?.rssi != null }.map { Pair( it.cellMetrics!!, it.timestamp ) }
            sessionCellMetrics.addAll(cellMetrics)

            val cellRssiAvg = session.metricDataList.map { it.cellMetrics?.rssi?: return@observe }.toMutableList().average().toInt()
            val cellRsrpAvg = session.metricDataList.map { it.cellMetrics?.rsrp?: return@observe }.toMutableList().average().toInt()
            val cellRsrqAvg = session.metricDataList.map { it.cellMetrics?.rsrq?: return@observe }.toMutableList().average().toInt()
            val cellBand = session.metricDataList.map { it.cellMetrics?.band ?: return@observe }.toMutableList()
            val band = Utility.mostCommonInList(cellBand)

            updateValueLists()

            setDate(cellMetrics.first().second)
            binding.statsRssiAvg.text = "$cellRssiAvg dBm"
            binding.statsRsrpAvg.text = "$cellRsrpAvg dBm"
            binding.statsRsrqAvg.text = "$cellRsrqAvg dBm"
            binding.statsBand.text      = band.toString()
            setAllCheckBoxes(true)

        }
        
        binding.rssiCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (sessionCellMetrics.isEmpty()) return@setOnCheckedChangeListener
            val entryList = getEntryList(CellType.RSSI)
            if (rssiDataSet == null) rssiDataSet = getLineData(entryList, "RSSI", ContextCompat.getColor(requireContext(), R.color.line_blue))
            if (isChecked) updateChart(sessionCellMetrics, rssiDataSet!!)
            else removeListFromChart(rssiDataSet!!)

        }
        binding.rsrqCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (sessionCellMetrics.isEmpty()) return@setOnCheckedChangeListener
            val entryList = getEntryList(CellType.RSRQ)
            if (rsrqDataSet == null) rsrqDataSet = getLineData(entryList, "RSRP", ContextCompat.getColor(requireContext(), R.color.line_orange))
            if (isChecked) updateChart(sessionCellMetrics, rsrqDataSet!!)
            else removeListFromChart(rsrqDataSet!!)
        }
        binding.rsrpCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (sessionCellMetrics.isEmpty()) return@setOnCheckedChangeListener
            val entryList = getEntryList(CellType.RSRP)
            if (rsrpDataSet == null) rsrpDataSet = getLineData(entryList, "RSRP", ContextCompat.getColor(requireContext(), R.color.line_green))
            if (isChecked) updateChart(sessionCellMetrics, rsrpDataSet!!)
            else removeListFromChart(rsrpDataSet!!)
        }

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
            chart.setScaleMinima(1f, 1f)
        }

        chart.invalidate()

        super.onViewCreated(view, savedInstanceState)
    }

    private fun newSessionSelected() {
        try {
            uncheckAllCheckBoxes()
            chart.data.clearValues()
            chart.notifyDataSetChanged()
            chart.invalidate()
            chart.data.removeDataSet(rssiDataSet)
            chart.data.removeDataSet(rsrpDataSet)
            chart.data.removeDataSet(rsrqDataSet)
            chart.invalidate()
            rssiDataSet?.clear()
            rsrpDataSet?.clear()
            rsrqDataSet?.clear()
            sessionCellMetrics.clear()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun uncheckAllCheckBoxes() {
        binding.rssiCheckbox.isChecked = false
        binding.rsrqCheckbox.isChecked = false
        binding.rsrpCheckbox.isChecked = false
    }

    private fun setAllCheckBoxes(state: Boolean) {
        binding.rssiCheckbox.isClickable = state
        binding.rsrqCheckbox.isClickable = state
        binding.rsrpCheckbox.isClickable = state
    }


    private fun updateValueLists() {
        val rssiEntryList = getEntryList(CellType.RSSI)
        val rsrqEntryList = getEntryList(CellType.RSRQ)
        val rsrpEntryList = getEntryList(CellType.RSRP)
        rssiDataSet = getLineData(rssiEntryList, "RSSI", ContextCompat.getColor(requireContext(), R.color.line_blue))
        rsrqDataSet = getLineData(rsrqEntryList, "RSRQ", ContextCompat.getColor(requireContext(), R.color.line_orange))
        rsrpDataSet = getLineData(rsrpEntryList, "RSRP", ContextCompat.getColor(requireContext(), R.color.line_green))
    }

    private fun setDate(ts: Long) {
        val mFormat = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
        val date = mFormat.format(Date(ts))
        binding.testingDateTextView.text = "Testing Date: $date"
    }

    private fun getEntryList(cellType: CellType): List<Entry> {
        if (sessionCellMetrics.isEmpty()) return listOf()
        var indexCount = 0
        return when (cellType) {
            CellType.RSSI -> { sessionCellMetrics.map { pair -> Entry( indexCount++.toFloat(), pair.first.rssi!!.toFloat() ) } }
            CellType.RSRP -> { sessionCellMetrics.map { pair -> Entry( indexCount++.toFloat(), pair.first.rsrp!!.toFloat() ) } }
            CellType.RSRQ -> { sessionCellMetrics.map { pair -> Entry( indexCount++.toFloat(), pair.first.rsrq!!.toFloat() ) } }
        }
    }

    private fun updateChart(cmList: List<Pair<CellMetrics, Long>>, dataSet: LineDataSet) {

        //Updated version

        val dataSetCount = try { chart.data.dataSetCount } catch (_: Exception) { 0 }
        if ( dataSetCount == 0 ) chart.data = LineData(dataSet)
        else chart.data.addDataSet(dataSet)
        chart.setVisibleXRangeMaximum(7f)
        chart.xAxis.setLabelCount(7, false)
        chart.xAxis.valueFormatter = object : IndexAxisValueFormatter() {

            val format = SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH)

            override fun getFormattedValue(value: Float): String {
                return try {
                    val result = cmList[value.toInt()]
                    val accTs = result.second
                    format.format(Date(accTs))
                } catch (e: Exception) {
                    value.toString()
                }
            }
        }

        chart.axisLeft.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()} dBm"
            }
        }

        chart.notifyDataSetChanged()
        chart.invalidate()


        return
    }

    private fun removeListFromChart(dataSet: LineDataSet) {
        val result = chart.data.removeDataSet(dataSet)
        chart.notifyDataSetChanged()
        Log.d(TAG, "removeListFromChart: $result")
        chart.invalidate()
    }

    private fun getLineData(entryList: List<Entry>, label: String, color: Int): LineDataSet {
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
                return "${value.toInt()}"
            }
        }
        return lineData
    }

    val lteListMetricState = mapOf("RSSI" to ListState.REMOVED, "RSRQ" to ListState.REMOVED, "RSRP" to ListState.REMOVED)
    enum class ListState {
        INSERTED,
        REMOVED
    }

    enum class CellType {
        RSSI,
        RSRQ,
        RSRP
    }





}