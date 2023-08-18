package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nettest.anat.*
import com.nettest.anat.databinding.FragmentTestingBinding
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat


@SuppressLint("SetTextI18n")
class TestingFragment: Fragment(R.layout.fragment_testing)  {

    private var _binding: FragmentTestingBinding? = null
    private val binding get() = _binding!!

    private var testingRoomState = false
    private var roomTestingProgressTimeComplete = false

    private var startDate: Long? = null
    private var endDate: Long? = null
    private var model: TestingViewModel? = null
    private val df = DecimalFormat("#,###.00")

    private val handler = Handler()
    private val runnable = object: Runnable {
        override fun run() {

            if (global_testingState) {
                addSecond(testingRoomState)
                handler.postDelayed(this, 1000)
                }
            else {
                (model as TestingViewModel).resetSessionSeconds()
                handler.removeCallbacks(this)
                }
            }
        }

    private var wifiManager: WifiManager? = null
    private var telephonyManager: TelephonyManager? = null
    private val metricsFrequencyHandler = Handler()
    private val metricsFrequencyRunnable = object : Runnable {
        override fun run() {
            if (global_testingState) {
                processNetworkData(wifiManager!!, telephonyManager)
                handler.postDelayed(this, global_testingNetworkCadence)
            }
            else { handler.removeCallbacks(this) }
        }

    }

    private val progressTimeHandler = Handler()
    private val progressTimeRunnable = object : Runnable {
        override fun run() {
            if (!roomTestingProgressTimeComplete) {
                (model as TestingViewModel).addSecondToRoomProgressTime()
                progressTimeHandler.postDelayed(this, 1000)
            }
            else {
                (model as TestingViewModel).resetRoomTestingProgressTime()
                progressTimeHandler.removeCallbacks(this)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTestingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        //Initial
        val changeNameAlert = getStartAlert()
        val testRoomDialog = getRoomTestingDialog()
        val endTestingDialog = getEndAlert()

        wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?


        //Speed Test Object Initialized
        val speedTestSocket: SpeedTestSocket = SpeedTestSocket()
        speedTestSocket.socketTimeout = 5000

        //RecyclerView
        val recyclerView = binding.testingRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = TestingAdapter(global_roomList, requireContext())
        recyclerView.adapter = adapter

        model = this.run { ViewModelProvider(this)[TestingViewModel::class.java] }

        //Session Time (minutes and seconds)
        model?.getSessionSeconds()?.observe(viewLifecycleOwner) { seconds ->
            val time = Utility.getTimeFormat(seconds)
            binding.mainTestingTime.text = "${time.first}m ${time.second}s"
        }

        //General Stats Update For List & Testing Dialog
        model?.updateUi()?.observe(viewLifecycleOwner) {_ -> adapter.notifyDataSetChanged() }

        model?.getRoomSeconds()?.observe(viewLifecycleOwner){

            val roomSessionTimer = testRoomDialog.findViewById<TextView>(R.id.roomSessionTimerLbl)
            val time = Utility.getTimeFormat(it)
            roomSessionTimer?.text = "${time.first}m ${time.second}s"
        }

        model?.getDownloadProgress()?.observe(viewLifecycleOwner) {
            val progressBar = testRoomDialog.findViewById<ProgressBar>(R.id.downloadProgressBar)
            progressBar?.progress = it
        }

        model?.getRoomCount()?.observe(viewLifecycleOwner) {
            binding.testingTotalRooms.text = it.toString()
        }

        model?.getRoomTestingProgressTime()?.observe(viewLifecycleOwner) {

            Log.d("ProgressTime", "onViewCreated: progressTime: $it")
            val completeRoomButton: Button? = testRoomDialog.findViewById(R.id.closeButton)
            val roomSessionProgressBar: ProgressBar? = testRoomDialog.findViewById(R.id.testingRemainingProgressBar)
            val roomSessionProgressText: TextView? = testRoomDialog.findViewById(R.id.testingRemainingText)

            if (roomTestingProgressTimeComplete) { return@observe }

            if (roomSessionProgressBar != null) {
                val progressPercent = getPercentProgress(it)
                roomSessionProgressBar.progress = progressPercent

                when (progressPercent) {
                    in 0..12 ->  {  roomSessionProgressText?.text = "Grabbing metrics in room (${progressPercent}%)" }
                    in 13..29 -> {  roomSessionProgressText?.text = "Please standby until completed (${progressPercent}%)" }
                    in 30..45 -> {  roomSessionProgressText?.text = "Almost at the halfway mark... (${progressPercent}%)" }
                    in 46..70 -> {  roomSessionProgressText?.text = "Just a few more seconds (${progressPercent}%)" }
                    in 71..99 -> {  roomSessionProgressText?.text = "Finalizing our data (${progressPercent}%)" }
                    100 -> { roomSessionProgressText?.text = "Room Testing Complete" }
                }

            }
            if (it == global_testingRoomTimeLimit) {
                roomTestingProgressTimeComplete = true
                completeRoomButton?.isEnabled = true
                completeRoomButton?.setTextColor(resources.getColor(R.color.white))
                completeRoomButton?.setBackgroundColor(resources.getColor(R.color.button_green))
            }
        }

        model?.getRssiAvg()?.observe(viewLifecycleOwner) { testRoomDialog.findViewById<TextView>(R.id.rssiAvgLabel)?.text = "$it dBm" }
        model?.getRateAvg()?.observe(viewLifecycleOwner) { testRoomDialog.findViewById<TextView>(R.id.linkRateAvgLabel)?.text = "$it Mbps" }
        model?.getRsrpAvg()?.observe(viewLifecycleOwner) { testRoomDialog.findViewById<TextView>(R.id.rsrpAvgLabel)?.text = "$it dBm" }
        model?.getRsrqAvg()?.observe(viewLifecycleOwner) { testRoomDialog.findViewById<TextView>(R.id.rsrqAvgLabel)?.text = "$it dBm" }
        model?.getConnectedBand()?.observe(viewLifecycleOwner) { testRoomDialog.findViewById<TextView>(R.id.connectedBandLabel)?.text = "( $it )" }
        model?.getConnectedBssid()?.observe(viewLifecycleOwner) { testRoomDialog.findViewById<TextView>(R.id.bssidLabel)?.text = " ( $it )" }

        binding.testNameLabel.setOnLongClickListener {
            if (global_testingState) changeNameAlert.show()
            true
        }

        binding.startTestingButton.setOnClickListener {
            val startTime = System.currentTimeMillis()
            startDate = startTime
            global_testStartTimeEpoch = startTime
            changeNameAlert.show()
        }

        binding.endTestingButton.setOnClickListener {
            //End Testing Modal - Confirmation
            endTestingDialog.show()
        }

        binding.addRoomButton.setOnClickListener {

            testingRoomState = true
            model?.forceUiChange()
            model?.addRoom()
            testRoomDialog.show()
            roomTestingProgressTimeComplete = false
            progressTimeHandler.postDelayed(progressTimeRunnable, 1000)
            GlobalScope.launch(Dispatchers.IO) { speedTestSocket.startDownload("http://speedtest.tele2.net/10MB.zip") }


        }

        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {
            override fun onCompletion(report: SpeedTestReport?) {
                val resultText = testRoomDialog.findViewById<TextView>(R.id.downloadSpeedDynamic)
                val speedMb = df.format(report?.transferRateBit?.div(BigDecimal(1000000)))
                requireActivity().runOnUiThread {
                    model?.setProgressDownload(100)
                    resultText?.text = "Result: $speedMb Mbps"
                }
            }

            override fun onProgress(percent: Float, report: SpeedTestReport?) {
                val progressText = testRoomDialog.findViewById<TextView>(R.id.downloadProgressText)
                requireActivity().runOnUiThread {
                    model?.setProgressDownload(percent.toInt())
                    progressText?.text = "${percent.toInt()}/100"
                }
            }

            override fun onError(speedTestError: SpeedTestError?, errorMessage: String?) {
                Log.d("SpeedTest", "onError: ERROR: $errorMessage")
            }
        })

        //onReload
        if (global_testingState) {
            adapter.notifyDataSetChanged()
            showTestingMedia()
            startTesting()
            model?.setSessionSeconds(global_sessionSeconds)
        }
        else {
            if (global_testName != "") {
                binding.testNameLabel.text = "Last Session Name: $global_testName"
                binding.testNameLabel.visibility = View.VISIBLE
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }


    private fun showTestingMedia() {
        binding.startTestingButton.visibility = View.GONE
        binding.testingSessionTimerContainer.visibility = View.VISIBLE
        binding.testingButtonContainer.visibility = View.VISIBLE
        binding.testNameLabel.text = "Session Name: $global_testName\n(tap and hold to edit)"
        binding.testNameLabel.visibility = View.VISIBLE
        binding.testingSessionTotalRoomsContainer.visibility = View.VISIBLE
    }

    private fun hideTestingMedia() {

        binding.testingSessionTimerContainer.visibility = View.GONE
        binding.testingButtonContainer.visibility = View.GONE
        binding.testingSessionTotalRoomsContainer.visibility = View.GONE

    }

    @SuppressLint("SetTextI18n")
    private fun startTesting() {

        showTestingMedia()
        model?.resetSessionSeconds()

//        telephonyManager!!.listen(PhoneStateListener(), PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        metricsFrequencyHandler.post(metricsFrequencyRunnable)

        if (global_roomList.isNotEmpty()) {
            model?.resetSessionSeconds()
            global_roomList.clear()
            model?.resetRooms()
            model?.updateUi()
        }

        if (!global_testingState) {
            global_testingState = true
            handler.postDelayed(runnable, 1000)
        }
    }


    private fun endTesting() {

        binding.startTestingButton.visibility = View.VISIBLE
        hideTestingMedia()
        binding.testNameLabel.text = "Last Session Name: $global_testName"
        global_testingState = false
        metricsFrequencyHandler.removeCallbacks(metricsFrequencyRunnable)
        handler.removeCallbacks(runnable)

    }


    @SuppressLint("SetTextI18n")
    private fun getRoomTestingDialog(): BottomSheetDialog {

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.testing_room_dialog, null)

        val buttonClose = view.findViewById<Button>(R.id.closeButton)
        val userRoomNameInput = view.findViewById<EditText>(R.id.userSessionRoomNameEditText)
        val roomNameTitle = view.findViewById<TextView>(R.id.roomNameTitle)
        val resultText = view.findViewById<TextView>(R.id.downloadSpeedDynamic)
        val speedTestProgressBar: ProgressBar = view.findViewById(R.id.downloadProgressBar)
        val testingProgressBar: ProgressBar = view.findViewById(R.id.testingRemainingProgressBar)
        val testingProgressText: TextView = view.findViewById(R.id.testingRemainingText)
        var roomName = ""

        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.duration = 100

        userRoomNameInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                roomName = v.text.toString()
                roomNameTitle.text = "${v.text}"
                roomNameTitle.visibility = View.VISIBLE
                val imm: InputMethodManager = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                userRoomNameInput.startAnimation(fadeOut)
                userRoomNameInput.visibility = View.GONE
            }
            false
        }

        roomNameTitle.setOnClickListener {
            userRoomNameInput.visibility = View.VISIBLE
        }

        buttonClose.setOnClickListener {


            if (userRoomNameInput.text.toString() == "") {
                roomName = "Room ${global_roomList.size+1}*"
            }

            val ri = RoomInfo(  lteImage = global_testingCellModel.gradeImage, roomName=roomName,
                                totalTimeSeconds = global_roomSeconds, lteImageColor = global_testingCellModel.gradeColor,
                                downloadSpeedResult = global_downloadResult, avgRsrq = global_testingCellModel.rsrqValueList.average().toInt(),
                                avgRsrp = global_testingCellModel.rsrpValueList.average().toInt() )

            global_roomList.add(ri)
            global_roomSeconds = 0
            global_testingCellModel = TestingCellModel()

            testingRoomState = false
            model!!.forceUiChange()
            model!!.resetRoomSeconds()
            model!!.resetNetworkStats()
            dialog.dismiss()

            testingProgressBar.progress = 0
            testingProgressText.text = "0/100"
            speedTestProgressBar.progress = 0
            resultText.text = "Running Test..."

            roomNameTitle.visibility = View.GONE
            userRoomNameInput.visibility = View.VISIBLE
            userRoomNameInput.text.clear()

            buttonClose.isEnabled = false
            buttonClose.setTextColor(Color.BLACK)
            buttonClose.setBackgroundColor(resources.getColor(R.color.extra_light_gray))

        }

        dialog.setCancelable(false)
        dialog.setContentView(view)

        return dialog
    }


    private fun getStartAlert(): AlertDialog {

        val layoutInflater = LayoutInflater.from(requireContext())
        val startTestingView: View  = layoutInflater.inflate(R.layout.dialog_testing_start_testing, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Physician or Clinic Name")
        builder.setView(startTestingView)

        val input = startTestingView.findViewById<EditText>(R.id.editTextText)

        builder.setPositiveButton("SUBMIT") { di, _ ->
            global_testName = input.text.toString()
            startTesting()
            di.cancel()
        }

        return builder.create()
    }

    private fun getEndAlert(): AlertDialog {

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("End Session?")
        builder.setPositiveButton("Confirm") {di, _ ->
            //End Testing Session
            val ed = System.currentTimeMillis()
            endDate = ed
            global_testEndTimeEpoch = ed
            endTesting()
            model?.resetSessionSeconds()
            di.cancel()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            //Do Nothing
        }

        return builder.create()

    }

    private fun addSecond(addRoomSecond: Boolean) {
        if (addRoomSecond) {
            model?.roomAddSecond()
            global_roomSeconds++
        }
        (model as TestingViewModel).sessionAddSecond()
    }

    private fun getPercentProgress(num: Int): Int {

        val result = num.toFloat().div(global_testingRoomTimeLimit)*100
        return result.toInt()


    }

    @SuppressLint("MissingPermission")
    private fun processNetworkData(wm: WifiManager, tm: TelephonyManager?) {

        model?.addRssi(wm.connectionInfo.rssi)
        model?.addLinkRate(wm.connectionInfo.linkSpeed)
        model?.addConnectedBssid(wm.connectionInfo.bssid)

        if (tm == null) return

        tm.allCellInfo.forEach {
            when(it) {
                is CellInfoLte -> {

                    if (!it.isRegistered) return@forEach
                    model?.addRsrp(it.cellSignalStrength.rsrp)
                    model?.addRsrq(it.cellSignalStrength.rsrq)
                    model?.addConnectedBand(Utility.getCellBand(it.cellIdentity.earfcn))

                    global_testingCellModel.addMetrics(it.cellSignalStrength.rsrp, it.cellSignalStrength.rsrq, Utility.getCellBand(it.cellIdentity.earfcn))

                }
            }
        }
    }

}