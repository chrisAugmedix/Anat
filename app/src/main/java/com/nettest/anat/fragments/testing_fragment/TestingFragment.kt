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
import androidx.core.content.ContextCompat
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
import com.nettest.anat.fragments.NetworkOperations
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

    private var sessionData: SessionData = SessionData()
    private var roomData: RoomData = RoomData()
    private val rvRoomList: MutableList<RoomData> = mutableListOf()

    private var sessionTestingState: Boolean = false
    private var roomTestingState: Boolean = false
    private var roomTestingProgressTimeComplete = false
    private var roomTestingSpeedTestComplete = false

    private var testingViewModel: TestingViewModel = TestingViewModel()
    private var inactivityEpochTime: Long = 0
    private val inactivityTimeSeconds: Long = ( 60 * 1 * 1000L ) // ( Seconds )  * ( Minutes )  * ( millisecond conversion )

    private val speedTestSocket     by lazy { SpeedTestSocket().apply { socketTimeout = 5000 } }
    private val wifiManager         by lazy { context?.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val telephonyManager    by lazy { context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private val roomRecyclerView    by lazy { binding.testingRecyclerView }
    private val recyclerViewAdapter by lazy { TestingAdapter(rvRoomList, requireContext()) }

    override fun onResume() {

        if (sessionTestingState) {
            //Resume Testing
            resumeSession()

        }

        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTestingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Initial
        val setSessionNameDialog = getSessionNameAlertDialog()
        val testRoomDialog = getRoomTestingDialog()
        val endTestingDialog = getEndSessionAlertDialog()
        val portCheckDialog = getPortCheckRequiredDialog()
        val sessionInactivityDialog = getInactivityDialog()
        val progressBar = testRoomDialog.findViewById(R.id.testingRemainingProgressBar) as ProgressBar?
        roomRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        roomRecyclerView.adapter = recyclerViewAdapter
        binding.viewHistoryButton.setOnClickListener {
            if ( binding.viewHistoryButton.text == "View History" ) showHistory()
            else hideHistory()
        }

        testingViewModel = this.run { ViewModelProvider(this)[TestingViewModel::class.java] }

        //11.9 NEW METHODS
        testingViewModel.getSessionData().observe(viewLifecycleOwner) { sessionMap ->

            try {

                val seconds = sessionMap["sessionTotalTime"]?.toInt()
                seconds?.let {
                    val secondsToMinutesString = Utility.getTimeFormat(it)
                    binding.mainTestingTime.text = "${secondsToMinutesString.first}m ${secondsToMinutesString.second}s"
                }

                val totalRooms = sessionMap["sessionTotalRooms"]
                totalRooms?.let { binding.testingTotalRooms.text = it }

            } catch (e: Exception) {
                Log.d(TAG, "onViewCreated: $e")
            }

        }
        testingViewModel.getRoomProgressTime().observe(viewLifecycleOwner) { progress -> progressBar?.setProgress(progress, true) }
        testingViewModel.getRoomData().observe(viewLifecycleOwner) { roomDataMap ->

            //Room Time Progress
            val progressTime = roomDataMap["sessionRoomProgress"]?.toInt()
            progressTime?.let {

                if (roomTestingProgressTimeComplete) return@let
                val progressPercent = getPercentProgress(progressTime)

                val roomSessionProgressText: TextView = testRoomDialog.findViewById(R.id.testingRemainingText)!!
                when (progressPercent) {
                    in 0..15 ->  {  roomSessionProgressText.text = "Grabbing metrics in room... (${progressPercent}%)" }
                    in 16..29 -> {  roomSessionProgressText.text = "Please standby until completed... (${progressPercent}%)" }
                    in 30..45 -> {  roomSessionProgressText.text = "Almost at the halfway mark... (${progressPercent}%)" }
                    in 46..70 -> {  roomSessionProgressText.text = "Just a few more seconds... (${progressPercent}%)" }
                    in 71..99 -> {  roomSessionProgressText.text = "Finalizing data... (${progressPercent}%)" }
                    100 -> { roomSessionProgressText.text = "Allotted Time Required Completed" }
                }

                if (it >= global_testingRoomTimeLimit) {
                    roomTestingProgressTimeComplete = true
                    (testRoomDialog.findViewById(R.id.closeButton) as Button?)?.apply {
                        if (!roomTestingSpeedTestComplete) {
                            text = "Awaiting Speed Test Completion"
                            return@let
                        }
                        text = "Finish/Complete Room"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_green))
                    }
                }
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

        binding.sessionNameContainer.setOnLongClickListener {
            if (sessionTestingState) setSessionNameDialog.show()
            true
        }

        var lifecycleLoopRunning = false
        binding.startTestingButton.setOnClickListener {

            if ( !global_completedPortChecker && global_portCheckRequired ) {
                portCheckDialog.show()
                return@setOnClickListener
            }

            //Alert user to input session name
           showStartSessionDialog(setSessionNameDialog)

            if (!lifecycleLoopRunning) {

                lifecycleLoopRunning = true

                viewLifecycleOwner.lifecycleScope.launch {

                    //
                    // Moved to a function, will confirm if it works then remove
                    //TODO
//                    CoroutineScope(Dispatchers.Default).launch {
//                        var countNotTesting = 0
//                        while (countNotTesting < 8) {
//                            saveNetworkData()
//                            if (!sessionTestingState) countNotTesting++
//                            delay(global_testingMetricsFrequency*1000L)
//                        }
//                    }
                    //Session Loop for adding seconds to session & room
                    //Will also determine if user has been inactive and show inactive dialog
                    //if no response, end the session
                    while (true) {

                        let mainLoop@{
                            if (!sessionTestingState) return@mainLoop

                            let inactiveLoop@{
                                if (!didInactivityExceedThreshold() ) return@inactiveLoop
                                if (!sessionInactivityDialog.isShowing) {
                                    CoroutineScope(Dispatchers.Main).launch inactive@{
                                        sessionInactivityDialog.show()
                                        delay(1000*10) //1000 MS conversion * seconds
                                        if (!sessionInactivityDialog.isShowing) return@inactive
                                        sessionInactivityDialog.dismiss()
                                        endSession()
                                    }
                                }
                            }

                            if (sessionInactivityDialog.isShowing) return@mainLoop

                            testingViewModel.addSessionData()
                            if (!roomTestingState) return@mainLoop

                            roomData.addSecond()
                            testingViewModel.addRoomData()

                            if (roomTestingProgressTimeComplete) return@mainLoop
                            testingViewModel.addRoomProgressSecond()


                        }

                        delay(1000L)

                    }

                }
            }

        }

        binding.endTestingButton.setOnClickListener {
            //End Testing Modal - Confirmation
            endTestingDialog.show()
        }

        binding.addRoomButton.setOnClickListener {

            roomData = RoomData()
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

            val max = 345
            progressBar?.max = max

            CoroutineScope(Dispatchers.Main).launch {
                var stepMax = 1
                while (stepMax < max ) {
                    if (!roomTestingState) {
                        this.cancel()
                        Log.d(TAG, "onViewCreated: progresstime cs cancelled")
                        break
                    }
                    testingViewModel.setRoomProgressTime(stepMax)
                    delay(125)
                    stepMax++
                }
            }


        }

        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {

            override fun onCompletion(report: SpeedTestReport?) {
                val speedMb = DecimalFormat("#,###.00").format(report?.transferRateBit?.div(BigDecimal(1000000)))
                roomData.setSpeedTestResult(speedMb.toBigDecimal())
                roomTestingSpeedTestComplete = true
                activity?.runOnUiThread {
                    (testRoomDialog.findViewById(R.id.closeButton) as Button?)?.apply {
                        if (!roomTestingProgressTimeComplete) {
                            text = "Allotted Time Required Before Completing"
                            return@apply
                        }
                        text = "Finish/Complete Room"
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

    @SuppressLint("MissingPermission")
    private suspend fun saveNetworkData() = withContext(Dispatchers.IO) {

        val pingResultList = mutableListOf<PingResult?>()

        pingHostnameList.forEach {
            val prd = withTimeoutOrNull(1000) { NetworkOperations().pingRequest(it) }
            prd.let { pingResultList.add(it) }
        }

        val ci = wifiManager.connectionInfo
        val bssid = ci.bssid.replace("\"", "")
        val rssi = ci.rssi
        val linkRate = ci.linkSpeed
        val txLinkSpeed = ci.txLinkSpeedMbps
        val rxLinkSpeed = ci.rxLinkSpeedMbps
        val channel = Utility.getWiFiAPChannel(ci.frequency)
        val band = if (channel > 11) 5.0 else 2.4
        val ip = Formatter.formatIpAddress(ci.ipAddress)

        val neighborList = wifiManager.scanResults?.filter{ it.BSSID.replace("\"", "") != bssid }?.take(5)?.
                            map { NeighborData(it.level, it.BSSID.replace("\"", ""), Utility.getWiFiAPChannel(it.frequency)) }?.toMutableList() ?: mutableListOf()

        val wifiMetrics = WifiMetrics(ip, rssi, linkRate, bssid, channel, neighborList, band, txLinkSpeed, rxLinkSpeed )

        Log.d(TAG, "saveNetworkData: $wifiMetrics")

        val cellInfoLte = telephonyManager.allCellInfo.firstOrNull { info -> (info is CellInfoLte) && (info.isRegistered)} as CellInfoLte?

        val cellBand = cellInfoLte?.cellIdentity?.earfcn?.let { return@let Utility.getCellBand(it) }
        val cellMetrics = CellMetrics().also {
            it.rssi = cellInfoLte?.cellSignalStrength?.rssi
            it.pci = cellInfoLte?.cellIdentity?.pci
            it.band = cellBand
            it.rsrp = cellInfoLte?.cellSignalStrength?.rsrp
            it.rsrq = cellInfoLte?.cellSignalStrength?.rsrq
            it.earfcn = cellInfoLte?.cellIdentity?.earfcn
        }

        val metricData = MetricData(wifiMetrics, cellMetrics, ConnectivityMetrics( pingResultList.filterNotNull().toMutableList() ), System.currentTimeMillis())
        if (roomTestingState) roomData.addMetrics(metricData)
        if (sessionTestingState) sessionData.metricDataList.add(metricData)

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

    private fun loadLastSession() {
        val lastSessionData = global_sessionDataList.last()
        if ( lastSessionData.roomDataList.isEmpty() ) return
        showSessionName("Last Session", false, lastSessionData.sessionName )
        rvRoomList.addAll(lastSessionData.roomDataList)
        recyclerViewAdapter.notifyItemRangeInserted(0, rvRoomList.size)
    }
    private fun showSessionName(sessionPrefix: String, showEditOption: Boolean, sessionName: String? = null) {

        binding.sessionNameDisplay.text = sessionPrefix
        if (showEditOption) {
            binding.tapHoldLabel.visibility = View.VISIBLE
            binding.sessionNameContainer.isClickable = true
        } else {
            binding.tapHoldLabel.visibility = View.INVISIBLE
            binding.sessionNameContainer.isClickable = false
        }
        binding.sessionName.text = sessionName ?: sessionData.sessionName
        binding.sessionNameContainer.visibility = View.VISIBLE


    }

    private fun showRoomButtons() {

        //Hide "Start Session" Button
        binding.startTestingButton.visibility = View.GONE
        binding.titleDescription1.visibility = View.GONE

        //View Room buttons, Session time, and total rooms
        binding.testingSessionTimerContainer.visibility = View.VISIBLE
        binding.testingButtonContainer.visibility = View.VISIBLE
        binding.testingSessionTotalRoomsContainer.visibility = View.VISIBLE

    }

    private fun hideRoomButtons() {

        //Hide room buttons, session time, and total rooms
        binding.testingSessionTimerContainer.visibility = View.GONE
        binding.testingButtonContainer.visibility = View.GONE
        binding.testingSessionTotalRoomsContainer.visibility = View.GONE
        binding.titleDescription1.visibility = View.VISIBLE

    }

    private fun startNewSession() {

        sessionData.startEpoch = System.currentTimeMillis()


        sessionTestingState = true
        global_testingState = true

        hideBottomNavBar()
        showRoomButtons()
        showSessionName("Current Session", true)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setInactiveTimeoutEpoch()
        binding.viewHistoryButton.visibility = View.INVISIBLE
        binding.viewHistoryButton.isEnabled = false

        clearRvList()
        testingViewModel.resetTestSessionData()
        CoroutineScope(Dispatchers.Default).launch {
            while (sessionTestingState) {
                saveNetworkData()
                delay(global_testingMetricsFrequency*1000L)
            }
        }

    }

    private fun endSession() {

        hideRoomButtons()
        sessionData.endEpoch = System.currentTimeMillis()
        testingViewModel.resetTestSessionData()

        //View Start Session Button
        binding.startTestingButton.visibility = View.VISIBLE

        //View Session Name (Prev. version)
        if (sessionData.roomDataList.isNotEmpty()) {
            showSessionName("Last Session", false)
            global_sessionDataList.add(sessionData)
        } else binding.sessionNameContainer.visibility = View.INVISIBLE


        binding.viewHistoryButton.visibility = View.VISIBLE
        binding.viewHistoryButton.isEnabled = true

        sessionTestingState = false
        global_testingState = false
        showNavBar()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    }

    private fun showHistory() {


        binding.titleLabel1.text = "Testing History"
        binding.viewHistoryButton.text = "Hide History"
        binding.testingSummaryContainer.visibility = View.VISIBLE
        binding.sessionNameContainer.visibility = View.INVISIBLE
        binding.startTestingButton.visibility = View.INVISIBLE
        binding.viewHistoryButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.midnight_blue))
        binding.titleDescription1.visibility = View.GONE
        val spinner = getSpinnerObject()

        val pattern = Regex(getString(R.string.regex_session_id))
        clearRvList()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, positionId: Long) {

                val sessionName = parent?.getItemAtPosition(position).toString()
                if ( sessionName ==  "No Previous Sessions Found" ) return
                if ( position == 0 ) return

                val matches     = pattern.find(sessionName) ?: return
                val sessionId   = matches.groupValues[1].toInt()
                val session     = global_sessionDataList.find { it.sessionId == sessionId } ?: return
                val sessionRoomList = session.roomDataList
                val sessionTotalRoomTime = Utility.getTimeFormat( sessionRoomList.sumOf { it.getRoomSeconds() } )
                clearRvList()
                rvRoomList.addAll(sessionRoomList)

                if (rvRoomList.isEmpty()) return

                recyclerViewAdapter.notifyItemRangeInserted(0, rvRoomList.size)
                binding.summaryTotalRooms.text = rvRoomList.size.toString()
                binding.summaryTotalTime.text = "${sessionTotalRoomTime.first}m ${sessionTotalRoomTime.second}s"

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //Do Nothing
            }

        }

        binding.historySpinner.visibility = View.VISIBLE

    }

    private fun hideHistory() {

        binding.titleLabel1.text = "Network Testing"
        binding.viewHistoryButton.text = "View History"
        binding.testingSummaryContainer.visibility = View.INVISIBLE
        binding.titleDescription1.visibility = View.VISIBLE
        clearRvList()
        if ( global_sessionDataList.isNotEmpty() ) loadLastSession()
        binding.startTestingButton.visibility = View.VISIBLE
        binding.historySpinner.visibility = View.GONE
        binding.viewHistoryButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))


    }

    private fun getSpinnerObject(): Spinner {

        val spinner: Spinner = binding.historySpinner
        val connectedSsid = wifiManager.connectionInfo?.ssid?.replace("\"", "")

        val items = let {
            if ( global_sessionDataList.isEmpty() ) return@let arrayOf("No Previous Sessions Found")
            val itemList = global_sessionDataList.filter { data -> data.ssid == connectedSsid }.map { "${it.sessionName} - (${it.sessionId})" }.toMutableList()
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
        showRoomButtons()
        showSessionName("Current Session:", true)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.viewHistoryButton.visibility = View.INVISIBLE
        binding.viewHistoryButton.isEnabled = false


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
            sessionData.roomDataList.add(roomData)
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
            buttonClose.setTextColor(Color.BLACK)
            buttonClose.setBackgroundColor(resources.getColor(R.color.extra_light_gray))

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
                    Snackbar.make(it, "Please wait until all tasks are done...", Snackbar.LENGTH_LONG).apply {
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

    private fun getSessionNameAlertDialog(): AlertDialog {


        val layoutInflater = LayoutInflater.from(requireContext())
        val startTestingView: View = layoutInflater.inflate(R.layout.testing_session_name_dialog, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Session Details")
        builder.setMessage("Note: Each entry has a 4 character minimum requirement")
        builder.setView(startTestingView)
        val physicianNameInput = startTestingView.findViewById<EditText>(R.id.physicianEditText)
        val clinicNameInput = startTestingView.findViewById<EditText>(R.id.clinicEditText)

        builder.setPositiveButton("Submit") { di, _ ->

            val userSessionNameInput = "${physicianNameInput.text} - ${clinicNameInput.text}"
            binding.sessionName.text = userSessionNameInput

            //if testing, update name from GUI
            if (sessionTestingState) sessionData.sessionName = userSessionNameInput
            else {
                sessionData = SessionData(sessionName = userSessionNameInput, ssid = getSsid())
                startNewSession()
            }

            physicianNameInput.text.clear()
            clinicNameInput.text.clear()
            di.cancel()
        }
        builder.setNegativeButton("Cancel") { view, _ -> view.cancel() }


        return builder.create()
    }

    private fun showStartSessionDialog(alertDialog: AlertDialog) {

        var clinicEntryPasses = false
        var physicianEntryPasses = false
        alertDialog.setOnShowListener { dialog ->

            val dialogPositiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).apply { isEnabled = false }
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

    private fun getWifiInfo( callback: (WifiInfo?) -> Unit ) {

        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? ?: return
        val networkRequest = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val callbackExecutor = Handler(Looper.getMainLooper())
        val networkCallback = object : ConnectivityManager.NetworkCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
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
