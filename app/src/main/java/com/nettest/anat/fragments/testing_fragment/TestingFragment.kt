package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.nettest.anat.*
import com.nettest.anat.databinding.FragmentTestingBinding
import com.nettest.anat.NetworkOperations
import com.nettest.anat.fragments.pc_fragment.PingResult
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.text.DecimalFormat


@SuppressLint("SetTextI18n")
class TestingFragment: Fragment(R.layout.fragment_testing)  {

    private val TAG = "TestingFragmentActivity"
    private var _binding: FragmentTestingBinding? = null
    private val binding get() = _binding!!

    private var sessionData: SessionData? = null
    private var roomData: RoomData? = null
    private val rvRoomList: MutableList<RoomData> = mutableListOf()
    private val progressBarMax = global_testingRoomTimeLimit*8

    private var sessionTestingState: Boolean = false
    private var roomTestingState: Boolean = false
    private var roomTestingProgressTimeComplete = false
    private var roomTestingSpeedTestComplete = false

    private var snackBarMessage: String = "2 Tasks remaining..."

    private var testingViewModel: TestingViewModel = TestingViewModel()
    private var inactivityEpochTime: Long = 0
    private val inactivityTimeSeconds: Long = ( 60 * 10 * 1000L ) // ( Seconds )  * ( Minutes )  * ( millisecond conversion )

    private val speedTestSocket     by lazy { SpeedTestSocket().apply { socketTimeout = 5000 } }
    private val wifiManager         by lazy { context?.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val telephonyManager    by lazy { context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private val roomRecyclerView    by lazy { binding.testingRecyclerView }
    private val recyclerViewAdapter by lazy { TestingAdapter(rvRoomList, requireContext()) }
    private val testRoomDialog      by lazy { getRoomTestingDialog() }
    private val inactiveDialog      by lazy { getInactivityDialog() }

    override fun onResume() {


        if (sessionTestingState) {
            //Resume Testing
            resumeSession()
            return
        }

        if (global_sessionDataList.isNotEmpty()) showHistory()

        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTestingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        reportNetworkInfo()
        val endTestingDialog = getEndSessionAlertDialog()
        val portCheckDialog = getPortCheckRequiredDialog()
        val progressBar = testRoomDialog.findViewById(R.id.testingRemainingProgressBar) as ProgressBar?
        roomRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        roomRecyclerView.adapter = recyclerViewAdapter

        testingViewModel = this.run { ViewModelProvider(this)[TestingViewModel::class.java] }

        //12.28 NEW
        testingViewModel.getSessionSeconds().observe(viewLifecycleOwner) {
            val secondsToMinutesString = Utility.getTimeFormat(it)
            binding.mainTestingTime.text = "${secondsToMinutesString.first}m ${secondsToMinutesString.second}s"
        }

        testingViewModel.getSessionRoomsCount().observe(viewLifecycleOwner) {binding.testingTotalRooms.text = it.toString() }

        //11.9 NEW METHODS <-- this method is inefficient
        testingViewModel.getSessionData().observe(viewLifecycleOwner) { sessionMap ->

//            try {
//
//                val seconds = sessionMap["sessionTotalTime"]?.toInt()
//                seconds?.let {
//                    val secondsToMinutesString = Utility.getTimeFormat(it)
//                    binding.mainTestingTime.text = "${secondsToMinutesString.first}m ${secondsToMinutesString.second}s"
//                }
//
//                val totalRooms = sessionMap["sessionTotalRooms"]
//                totalRooms?.let { binding.testingTotalRooms.text = it }
//
//            } catch (e: Exception) {
//                Log.d(TAG, "onViewCreated: $e")
//            }

        }

        testingViewModel.getRoomProgressTime().observe(viewLifecycleOwner) { progress ->
            progressBar?.setProgress(progress, true)
            val roomSessionProgressText: TextView = testRoomDialog.findViewById(R.id.testingRemainingText)!!
            when (val percentage = ((progress.toDouble() / progressBarMax) * 100).toInt()) {
                in 0..15 ->  {  roomSessionProgressText.text = "Grabbing metrics in room... (${percentage}%)" }
                in 16..29 -> {  roomSessionProgressText.text = "Please standby until completed... (${percentage}%)" }
                in 30..45 -> {  roomSessionProgressText.text = "Almost at the halfway mark... (${percentage}%)" }
                in 46..70 -> {  roomSessionProgressText.text = "Just a few more seconds... (${percentage}%)" }
                in 71..99 -> {  roomSessionProgressText.text = "Finalizing data... (${percentage}%)" }
                in 100..105 -> {
                    roomSessionProgressText.text = "Allotted Time Required Completed"
                    roomTestingProgressTimeComplete = true
                    (testRoomDialog.findViewById(R.id.closeButton) as Button?)?.apply {
                        if (!roomTestingSpeedTestComplete) {
                            text = "Awaiting Speed Test Completion"
                            snackBarMessage = resources.getString(R.string.test_button_speed_msg)
                            return@apply
                        }
                        text = "Finish/Complete Room"
                        snackBarMessage = resources.getString(R.string.test_button_both_msg)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_green))
                    }
                }
            }
        }
        testingViewModel.getRoomData().observe(viewLifecycleOwner) { roomDataMap ->

            //Room Time Progress
            val progressTime = roomDataMap["sessionRoomProgress"]?.toInt()
            progressTime?.let {
                return@let
//                if (roomTestingProgressTimeComplete) return@let
//                val progressPercent = getPercentProgress(progressTime)
//
//                val roomSessionProgressText: TextView = testRoomDialog.findViewById(R.id.testingRemainingText)!!
//                when (progressPercent) {
//                    in 0..15 ->  {  roomSessionProgressText.text = "Grabbing metrics in room... (${progressPercent}%)" }
//                    in 16..29 -> {  roomSessionProgressText.text = "Please standby until completed... (${progressPercent}%)" }
//                    in 30..45 -> {  roomSessionProgressText.text = "Almost at the halfway mark... (${progressPercent}%)" }
//                    in 46..70 -> {  roomSessionProgressText.text = "Just a few more seconds... (${progressPercent}%)" }
//                    in 71..99 -> {  roomSessionProgressText.text = "Finalizing data... (${progressPercent}%)" }
//                    100 -> { roomSessionProgressText.text = "Allotted Time Required Completed" }
//                }
//
//                if (it >= global_testingRoomTimeLimit) {
//                    roomTestingProgressTimeComplete = true
//                    (testRoomDialog.findViewById(R.id.closeButton) as Button?)?.apply {
//                        if (!roomTestingSpeedTestComplete) {
//                            text = "Awaiting Speed Test Completion"
//                            snackBarMessage = resources.getString(R.string.test_button_speed_msg)
//                            return@let
//                        }
//                        text = "Finish/Complete Room"
//                        snackBarMessage = resources.getString(R.string.test_button_both_msg)
//                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
//                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_green))
//                    }
//                }
            }

            //Download Progress
            val progress = roomDataMap["speedTestProgress"]?.toInt() ?: 0
            (testRoomDialog.findViewById(R.id.downloadProgressText) as TextView?)?.let { it.text = "${progress}/100" }
//            (testRoomDialog.findViewById(R.id.downloadProgressBar) as ProgressBar?)?.let { tv -> tv.progress = progress }
            (testRoomDialog.findViewById(R.id.downloadProgressBar) as ProgressBar?)?.setProgress(progress, true)
            val speedTestResult = roomDataMap["speedTestResult"]
            (testRoomDialog.findViewById(R.id.downloadSpeedDynamic) as TextView?)?.let { tv -> speedTestResult?.let { tv.text = speedTestResult } }


            //Network Data
            //Wifi
            (testRoomDialog.findViewById(R.id.rssiAvgLabel) as TextView?)?.let      { it.text = roomDataMap["wifiRssiAvg"] ?: "N/A" }
            (testRoomDialog.findViewById(R.id.linkRateAvgLabel) as TextView?)?.let  { it.text = roomDataMap["wifiRateAvg"] ?: "N/A" }
            (testRoomDialog.findViewById(R.id.bssidLabel) as TextView?)?.let        { it.text = "( ${ roomDataMap["wifiBssid"] ?: "N/A" } )" }

            //LTE
            (testRoomDialog.findViewById(R.id.rsrpAvgLabel) as TextView?)?.let { it.text = roomDataMap["lteRsrpAvg"] ?: "N/A" }
            (testRoomDialog.findViewById(R.id.rsrqAvgLabel) as TextView?)?.let { it.text = roomDataMap["lteRsrqAvg"] ?: "N/A" }
            (testRoomDialog.findViewById(R.id.lteRssiAvgLabel) as TextView?)?.let { it.text = roomDataMap["lteRssiAvg"] ?: "N/A" }
            (testRoomDialog.findViewById(R.id.connectedBandLabel) as TextView?)?.let { it.text = "( ${ roomDataMap["lteBand"] ?: "N/A" } )" }

        }

        binding.startTestingButton.setOnClickListener {

            if ( !global_completedPortChecker && global_portCheckRequired ) {
                portCheckDialog.show()
                return@setOnClickListener
            }

           showSessionNameEntryDialog()

        }

        binding.endTestingButton.setOnClickListener {
            //End Testing Modal - Confirmation
            endTestingDialog.show()
        }

        binding.addRoomButton.setOnClickListener {

            roomData = RoomData(sessionData!!.sessionName, sessionData!!.sessionId)
            testingViewModel.addRoomNetworkData(wifiManager, telephonyManager)
            testRoomDialog.show()
            roomTestingState = true
            roomTestingProgressTimeComplete = false
            roomTestingSpeedTestComplete = false
            CoroutineScope(Dispatchers.IO).launch { speedTestSocket.startDownload("https://grafana.augmedix.com:1201/v2/api/dl/10MB") }
            CoroutineScope(Dispatchers.Main).launch {

                while(roomTestingState) {
                    testingViewModel.addRoomNetworkData(wifiManager, telephonyManager)
                    getWifiInfo { Log.d(TAG, "onViewCreated: $it") }
                    if (!roomTestingState) this.cancel("GC-Scope")
                    delay(global_testingMetricsFrequency*1000L)
                }

            }

            progressBar?.max = progressBarMax
            CoroutineScope(Dispatchers.Main).launch {
                var progress = 1
                while (progress <= progressBarMax ) {

                    if (!roomTestingState) {
                        this.cancel()
                        break
                    }
                    testingViewModel.setRoomProgressTime(progress)
                    delay(125)
                    progress++
                }
            }
        }

        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {

            override fun onCompletion(report: SpeedTestReport?) {
                val speedMb = DecimalFormat("#,###.00").format(report?.transferRateBit?.div(BigDecimal(1000000)))
                roomData!!.setSpeedTestResult(speedMb.toBigDecimal())
                roomTestingSpeedTestComplete = true
                activity?.runOnUiThread {
                    (testRoomDialog.findViewById(R.id.closeButton) as Button?)?.apply {
                        if (!roomTestingProgressTimeComplete) {
                            text = "Allotted Time Required Before Completing"
                            snackBarMessage = resources.getString(R.string.test_button_time_msg)
                            return@apply
                        }
                        text = "Finish/Complete Room"
                        snackBarMessage = resources.getString(R.string.test_button_both_msg)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_green))
                    }
                }
                CoroutineScope(Dispatchers.Main).launch {
                    testingViewModel.addSessionRoomDownloadProgress(100)
                    testingViewModel.addSessionRoomDownloadResult("Result: $speedMb Mbps")
                }

            }

            override fun onProgress(percent: Float, report: SpeedTestReport?) {
                CoroutineScope(Dispatchers.Main).launch {
                    testingViewModel.addSessionRoomDownloadProgress(percent.toInt())
                }
            }

            override fun onError(speedTestError: SpeedTestError?, errorMessage: String?) {
                Log.d("SpeedTest", "onError: ERROR: $errorMessage")
            }

        })

        //Resume
        if (global_testingState) resumeSession()
//        else if ( global_sessionDataList.isNotEmpty() ) loadLastSession()

        super.onViewCreated(view, savedInstanceState)
    }

    private fun startNetworkReportLoop() {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                reportNetworkInfo()
                if (!sessionTestingState) break
                delay(global_testingMetricsFrequency*1000L)
            }
        }
    }

    private fun startSessionTimeCounterLoop() {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {

                let inactiveLoop@{
                    if ( !didInactivityExceedThreshold() ) return@inactiveLoop

                    if (!inactiveDialog.isShowing) {
                        CoroutineScope(Dispatchers.Main).launch inactive@{
                            inactiveDialog.show()
                            delay(1000*10) //1000 MS conversion * seconds
                            if (!inactiveDialog.isShowing) return@inactive
                            inactiveDialog.dismiss()
                            endSession()
                        }
                    }
                }

                let countLoop@{
                    if (inactiveDialog.isShowing) return@countLoop
                    testingViewModel.addSecondToSession()
                    if (roomTestingState) testingViewModel.addSecondToRoom()
                }

                if (!sessionTestingState) break
                delay(999L)
            }
        }
    }

    private fun addRoom() {

        val progressBar: ProgressBar? = testRoomDialog.findViewById(R.id.testingRemainingProgressBar) as ProgressBar?

        roomData = RoomData(sessionData!!.sessionName, sessionData!!.sessionId)
        testingViewModel.addRoomNetworkData(wifiManager, telephonyManager)
        testRoomDialog.show()
        roomTestingState = true
        roomTestingProgressTimeComplete = false
        roomTestingSpeedTestComplete = false
        CoroutineScope(Dispatchers.IO).launch { speedTestSocket.startDownload("https://grafana.augmedix.com:1201/v2/api/dl/10MB") }


        progressBar?.max = progressBarMax
        CoroutineScope(Dispatchers.Main).launch {
            var progress = 1
            while (progress <= progressBarMax ) {

                if (!roomTestingState) {
                    this.cancel()
                    break
                }
                testingViewModel.setRoomProgressTime(progress)
                delay(125)
                progress++
            }
        }
    }

    private fun reportNetworkInfo() {

        val lte: CellInfoLte? = NetworkOperations().getLteStats(context)
        val pingResultList = mutableListOf<PingResult>()
        val metricData = MetricData(timestamp = System.currentTimeMillis())

        CoroutineScope(Dispatchers.IO).launch {
            pingHostnameList.forEach {
                var prd = withTimeoutOrNull(1000) { NetworkOperations().pingRequest(it) }
                if (prd == null) prd = PingResult(it, -1, false)
                pingResultList.add(prd)
            }
        }

        NetworkOperations().getWifiStats(context) { wifiInfo ->

            metricData.connectivityMetrics = ConnectivityMetrics(pingResultList)

            let wifi@{
                if (wifiInfo == null) return@wifi

                val ipAddress = NetworkOperations().getIpAddress(context)?: "N/A"
                val channel = Utility.getWiFiAPChannel(wifiInfo.frequency) ?: 0
                metricData.wifiMetrics = WifiMetrics(
                    ip = ipAddress, ssid = wifiInfo.ssid.replace("\"", ""), bssid = wifiInfo.bssid.replace("\"", ""),
                    rssi = wifiInfo.rssi, wifiInfo.linkSpeed, channel, ( if (channel > 11) 5.0 else 2.4 ) , wifiInfo.txLinkSpeedMbps, wifiInfo.rxLinkSpeedMbps
                )
            }

            let lte@{
                if (lte == null) return@lte
                val band = Utility.getCellBand(lte.cellIdentity.earfcn)
                metricData.cellMetrics = CellMetrics(
                    lte.cellSignalStrength.rssi, lte.cellSignalStrength.rsrp, lte.cellSignalStrength.rsrq, band,
                    lte.cellIdentity.earfcn, lte.cellIdentity.pci
                )
            }

            //Report Network Data
            if (sessionTestingState) sessionData!!.metricDataList.add(metricData)
            if (roomTestingState) roomData.addMetrics(metricData)

        }


    }

    @SuppressLint("MissingPermission")
    private suspend fun saveNetworkData() = withContext(Dispatchers.IO) {

//        val pingResultList = mutableListOf<PingResult?>()
//
//        pingHostnameList.forEach {
//            var prd = withTimeoutOrNull(1000) { NetworkOperations().pingRequest(it) }
//            if (prd == null) prd = PingResult(it, -1, false)
//            pingResultList.add(prd)
//        }
//
//        val ci = wifiManager.connectionInfo
//        val connectedSsid = wifiManager.connectionInfo?.ssid?.replace("\"", "") ?: "N/A"
//        val bssid = ci.bssid.replace("\"", "")
//        val rssi = ci.rssi
//        val linkRate = ci.linkSpeed
//        val txLinkSpeed = ci.txLinkSpeedMbps
//        val rxLinkSpeed = ci.rxLinkSpeedMbps
//        val channel = Utility.getWiFiAPChannel(ci.frequency)
//        val band = if (channel > 11) 5.0 else 2.4
//        val ip = Formatter.formatIpAddress(ci.ipAddress)
//
//        val neighborList = wifiManager.scanResults?.filter{ it.BSSID.replace("\"", "") != bssid }?.take(5)?.
//                            map { NeighborData(it.level, it.BSSID.replace("\"", ""), Utility.getWiFiAPChannel(it.frequency)) }?.toMutableList() ?: mutableListOf()
//
//        val wifiMetrics = WifiMetrics(ip, rssi, linkRate, bssid, channel, band, txLinkSpeed, rxLinkSpeed, connectedSsid )
//
//        Log.d(TAG, "saveNetworkData: $wifiMetrics")
//
//        val cellInfoLte = telephonyManager.allCellInfo.firstOrNull { info -> (info is CellInfoLte) && (info.isRegistered)} as CellInfoLte?
//
//        val cellBand = cellInfoLte?.cellIdentity?.earfcn?.let { return@let Utility.getCellBand(it) }
//        val cellMetrics = CellMetrics().also {
//            it.rssi = cellInfoLte?.cellSignalStrength?.rssi
//            it.pci = cellInfoLte?.cellIdentity?.pci
//            it.band = cellBand
//            it.rsrp = cellInfoLte?.cellSignalStrength?.rsrp
//            it.rsrq = cellInfoLte?.cellSignalStrength?.rsrq
//            it.earfcn = cellInfoLte?.cellIdentity?.earfcn
//        }
//
//        val metricData = MetricData(wifiMetrics, cellMetrics, ConnectivityMetrics( pingResultList.filterNotNull().toMutableList() ), System.currentTimeMillis())
//        if (roomTestingState) roomData.addMetrics(metricData)
//        if (sessionTestingState) sessionData.metricDataList.add(metricData)

    }

    private fun didInactivityExceedThreshold(): Boolean {
        if ( inactivityEpochTime == 0L ) return false
        return System.currentTimeMillis() > inactivityEpochTime
    }
    private fun setInactiveTimeoutEpoch(idleTime: Boolean = false) {
        //Add the time statically OR
        //If user hit's "Continue testing" in inactive dialog, set a shorter time frame to ensure they aren't inactive
        val currentTimeMillis = System.currentTimeMillis()
        inactivityEpochTime = currentTimeMillis + ( if (idleTime) 20000 else inactivityTimeSeconds )
    }

    private fun startSession() {

        //Change Testing Flags
        sessionTestingState = true
        global_testingState = true //This one is solely for the BroadcastReceiver -- KEEP SCREEN ON

        //Hide "Start Session" Button
        binding.startTestingButton.visibility = View.GONE

        //View Room buttons, Session time, and total rooms
        binding.testingSessionViewContainer.visibility = View.VISIBLE

        //Constraint List to Testing Media
        binding.testingRecyclerView.layoutParams.apply {
            this as ConstraintLayout.LayoutParams
            this.bottomToTop = binding.testingSessionViewContainer.id
        }

        if (binding.historyViewContainer.isVisible) binding.historyViewContainer.visibility = View.GONE

        binding.sessionName.text = sessionData!!.sessionName

        hideBottomNavBar()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setInactiveTimeoutEpoch()

        clearRvList()
        testingViewModel.resetTestSessionData()

        //TODO: Investigate this
        CoroutineScope(Dispatchers.Default).launch {
            while (sessionTestingState) {
                saveNetworkData()
                delay(global_testingMetricsFrequency*1000L)
            }
        }

    }

    private fun endSession() {
        //Change Testing Flags
        sessionTestingState = false
        global_testingState = false

        binding.testingSessionViewContainer.visibility = View.GONE
        testingViewModel.resetTestSessionData()

        if (testRoomDialog.isShowing) testRoomDialog.dismiss()

        //View Start Session Button
        binding.startTestingButton.visibility = View.VISIBLE
        binding.testingRecyclerView.apply {
            layoutParams.apply {
                this as ConstraintLayout.LayoutParams
                this.bottomToTop = binding.startTestingButton.id
            }
        }

        //View Session Name (Prev. version)
        if ( sessionData!!.roomList.isNotEmpty() ) { global_sessionDataList.add(sessionData!!) }
        if ( global_sessionDataList.isNotEmpty() ) showHistory(false)

        showNavBar()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    }

    private fun showHistory(clearRvList: Boolean = true) {

        binding.historyViewContainer.visibility = View.VISIBLE
        binding.startTestingButton.text = "Start New Session"
        val spinner = getSpinnerObject()

        val pattern = Regex(getString(R.string.regex_session_id))

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, positionId: Long) {

                val sessionName = parent?.getItemAtPosition(position).toString()
                if ( position == 0 ) return

                val matches     = pattern.find(sessionName) ?: return
                val sessionId   = matches.groupValues[1].toInt()
                val session     = global_sessionDataList.find { it.sessionId == sessionId } ?: return
                val sessionRoomList = session.roomList
                val sessionTotalRoomTime = Utility.getTimeFormat( sessionRoomList.sumOf { it.getRoomSeconds() } )
                if ( clearRvList ) {
                    if ( recyclerViewAdapter.itemCount > 0 ) clearRvList()
                    rvRoomList.addAll(sessionRoomList)
                    recyclerViewAdapter.notifyItemRangeInserted(0, rvRoomList.size)
                }

                binding.historySessionName.text = sessionName
                binding.historyRooms.text = rvRoomList.size.toString()
                binding.historyDuration.text = "${sessionTotalRoomTime.first}m ${sessionTotalRoomTime.second}s"

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //Do Nothing
            }

        }

        spinner.setSelection(global_sessionDataList.size, true)

    }

    private fun getSpinnerObject(): Spinner {

        val spinner: Spinner = binding.historySpinner
        val connectedSsid = wifiManager.connectionInfo?.ssid?.replace("\"", "")

        val items = let {
            if ( global_sessionDataList.isEmpty() ) return@let arrayOf("No Previous Sessions Found")
            val itemList = global_sessionDataList.map { "${it.sessionName} - (${it.sessionId})" }.toMutableList()
            itemList.add(0, "Select Session...")
            return@let itemList.toTypedArray()
        }

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

    private fun hideBottomNavBar() {
        activity?.let {
            it.findViewById<BottomNavigationView>(R.id.nav_view).apply {
                clearAnimation()
                animate().translationY(this.height.toFloat()).duration = 500
            }
            it.window?.insetsController?.apply {
                hide(WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun showNavBar() {
        val navBar: BottomNavigationView = activity?.findViewById(R.id.nav_view) ?: return
        navBar.clearAnimation()
        navBar.animate().translationY(0F).duration = 500
        val controller = activity?.window?.insetsController ?: return
        controller.show(WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
    }

    private fun resumeSession() {

        hideBottomNavBar()
//        showRoomButtons()
//        showSessionName("Current Session:", true)
//        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        binding.viewHistoryButton.visibility = View.INVISIBLE
//        binding.viewHistoryButton.isEnabled = false


    }

    private fun getInactivityDialog(): AlertDialog {

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Are You There?")
        builder.setMessage("Testing will automatically end unless you hit \"Continue Testing\"")

        builder.setPositiveButton("Continue Testing") { di, _ ->
            setInactiveTimeoutEpoch(idleTime = true)
            di.dismiss()
        }

        builder.setNegativeButton("End Testing") { di, _ ->
            endSession()
            di.dismiss()
        }

        return builder.create()
    }

    private fun getRoomTestingDialog(): BottomSheetDialog {

        val dialog = BottomSheetDialog(requireContext())
        val view = View.inflate(requireContext(), R.layout.testing_room_dialog, null)

        dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        val buttonClose = view.findViewById<Button>(R.id.closeButton)
        val userRoomNameInput = view.findViewById<EditText>(R.id.userSessionRoomNameEditText)
        val roomNameTitle = view.findViewById<TextView>(R.id.roomNameTitle)
        var roomName: String? = null

        fun closeDialog() {

            setInactiveTimeoutEpoch()
            if (roomName == null || roomName!!.isEmpty() ) roomName = "Room ${rvRoomList.size+1}*"

            roomData.updateRoomName(roomName!!)
            roomData.finalizeRoom()
            sessionData!!.roomList.add(roomData)
            rvRoomList.add(roomData)

            roomTestingState = false

            dialog.dismiss()
            roomNameTitle.visibility = View.GONE
            userRoomNameInput.visibility = View.VISIBLE
            userRoomNameInput.text.clear()

            testingViewModel.addDataRoomCount()
            testingViewModel.resetRoomValues()
            recyclerViewAdapter.notifyItemInserted(rvRoomList.size)
            roomTestingProgressTimeComplete = false
            buttonClose.text = "2 Tasks Required Before Completing"
            buttonClose.setTextColor(resources.getColor(R.color.gray, null))
            buttonClose.setBackgroundColor(resources.getColor(R.color.extra_light_gray, null))

            Log.d(TAG, "${Utility.getGson(roomData)}")
            roomName = null

        }

        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.duration = 100

        userRoomNameInput.setOnEditorActionListener { v, actionId, _ ->

            if (actionId != EditorInfo.IME_ACTION_DONE) return@setOnEditorActionListener false
            if ( v.text.toString().isEmpty() ) return@setOnEditorActionListener false
            roomName = v.text.toString()
            Log.d(TAG, "getRoomTestingDialog - Submitted: $roomName")
            roomNameTitle.text = "${v.text}"
            roomNameTitle.visibility = View.VISIBLE
            val imm: InputMethodManager = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            userRoomNameInput.startAnimation(fadeOut)
            userRoomNameInput.visibility = View.GONE

            false
        }

        roomNameTitle.setOnClickListener { userRoomNameInput.visibility = View.VISIBLE }

        buttonClose.setOnLongClickListener {
            //Update Inactivity Time
            if (!roomTestingSpeedTestComplete && !roomTestingProgressTimeComplete) {
                dialog.window?.decorView?.let {
                    Snackbar.make(it, "1 task remaining...", Snackbar.LENGTH_LONG).apply {
                        this.view.setBackgroundColor(Color.GRAY)
                        this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                        setTextColor(Color.WHITE)
                    }.show()
                }
                return@setOnLongClickListener true
            }
            closeDialog()
            true
        }

        buttonClose.setOnClickListener {
            if ( buttonClose.text != "Finish/Complete Room" ) {
                dialog.window?.decorView?.let {
                    Snackbar.make(it, snackBarMessage, Snackbar.LENGTH_LONG).apply {
                    setActionTextColor(Color.BLUE)
                    this.view.setBackgroundColor(Color.LTGRAY)
                    this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                    setTextColor(Color.BLACK)
                    }.show()
                }

                return@setOnClickListener
            }

            //Update Inactivity Time 
            closeDialog()

        }

        dialog.setCancelable(false)
        dialog.setContentView(view)

        return dialog
    }

    private fun showSessionNameEntryDialog() {

        val layoutInflater = LayoutInflater.from(requireContext())
        val startTestingView: View = layoutInflater.inflate(R.layout.testing_session_name_dialog, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Session Details")
        builder.setMessage("Note: Each entry has a 4 character minimum requirement, <strong>this cannot be changed</strong>.")
        builder.setView(startTestingView)
        val physicianNameInput = startTestingView.findViewById<EditText>(R.id.physicianEditText)
        val clinicNameInput = startTestingView.findViewById<EditText>(R.id.clinicEditText)

        builder.setPositiveButton("Submit") { di, _ ->
            val userSessionNameInput = "${physicianNameInput.text} - ${clinicNameInput.text}"
            binding.sessionName.text = userSessionNameInput
            sessionData = SessionData(sessionName = userSessionNameInput)
            startSession()
            physicianNameInput.text.clear()
            clinicNameInput.text.clear()
            di.dismiss()
        }

        builder.setNegativeButton("Cancel") { view, _ -> view.cancel() }
        val alertDialog = builder.create()

        var clinicEntryPasses = false
        var physicianEntryPasses = false
        alertDialog.setOnShowListener { dialog ->
            dialog as AlertDialog
            val dialogPositiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply { isEnabled = false }
            dialog.findViewById<TextView>(android.R.id.message)?.let {
                it.textSize = 12f
                it.setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
            }
            //physician
            dialog.findViewById<EditText>(R.id.physicianEditText)?.let { editText ->

                editText.addTextChangedListener {
                    if ( editText.text.toString().isEmpty() ) dialog.findViewById<TextView>(R.id.physicianEntryTV)?.visibility = View.INVISIBLE
                    else if (editText.text.isNotEmpty()) dialog.findViewById<TextView>(R.id.physicianEntryTV)?.visibility = View.VISIBLE
                    if ( editText.text.toString().length < 4 ) {
                        dialogPositiveButton.isEnabled = false
                        return@addTextChangedListener
                    }
                    physicianEntryPasses = true
                    if (clinicEntryPasses) dialogPositiveButton.isEnabled = true
                }
            }
            //clinic
            dialog.findViewById<EditText>(R.id.clinicEditText)?.let { editText ->

                editText.addTextChangedListener {
                    if ( editText.text.isNotEmpty() ) dialog.findViewById<TextView>(R.id.clinicEntryTV)?.visibility = View.VISIBLE
                    if ( editText.text.toString().isEmpty() ) dialog.findViewById<TextView>(R.id.clinicEntryTV)?.visibility = View.INVISIBLE
                    if ( editText.text.toString().length < 4 ) {
                        dialogPositiveButton.isEnabled = false
                        return@addTextChangedListener
                    }

                    clinicEntryPasses = true
                    if (physicianEntryPasses) dialogPositiveButton.isEnabled = true
                }
            }

        }

        alertDialog.show()

    }

    private fun getPingResults( callback: (List<PingResult>) -> Unit) {

    }

    private fun getWifiInfo( callback: (WifiInfo?) -> Unit ) {

        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? ?: return
        val networkRequest = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val callbackExecutor = Handler(Looper.getMainLooper())
        val networkCallback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
//
//            override fun onAvailable(network: Network) {
//                super.onAvailable(network)
//                connectivityManager.unregisterNetworkCallback(this)
//            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val wifiInfo = networkCapabilities.transportInfo as WifiInfo? ?: return
                callback(wifiInfo)
                connectivityManager.unregisterNetworkCallback(this)
            }
//
//            override fun onUnavailable() {
//                super.onUnavailable()
//                callback(null)
//                connectivityManager.unregisterNetworkCallback(this)
//            }

        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback, callbackExecutor)
        //This method does not provide SSID nor BSSID, only RSSI + link rate and other network information
//        val activeNetwork = connectivityManager.activeNetwork
//        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
//        val wifiInfo = networkCapabilities?.transportInfo as WifiInfo? ?: return
//        Log.d(TAG, "getWifiInfo: $wifiInfo")
        //==

    }

    private fun getSsid(): String = wifiManager.connectionInfo?.ssid?.replace("\"", "") ?: "N/A"

    private fun getPortCheckRequiredDialog(): AlertDialog {


        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Port Check Required")
        builder.setMessage("Please run a port check before starting a test session")
        builder.setIcon(R.drawable.room_grade_alert)

        builder.setPositiveButton("Got it") { di, _ ->
            di.cancel()
        }

        return builder.create()
    }

    private fun getEndSessionAlertDialog(): AlertDialog {

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("End Session?")
        builder.setPositiveButton("Confirm") {di, _ ->
            //End Testing Session
            endSession()
            di.cancel()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            //Do Nothing
        }

        return builder.create()

    }

    private fun getPercentProgress(num: Int): Int {
        val result = num.toFloat().div(global_testingRoomTimeLimit)*100
        return result.toInt()
    }

    private fun clearRvList() {
        val listSize = rvRoomList.size
        rvRoomList.clear()
        recyclerViewAdapter.notifyItemRangeRemoved(0, listSize)
    }


}
