package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
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
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nettest.anat.*
import com.nettest.anat.databinding.FragmentTestingBinding
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat


@SuppressLint("SetTextI18n")
class TestingFragment: Fragment(R.layout.fragment_testing)  {

    private val TAG = "TestingFragmentActivity"
    private var _binding: FragmentTestingBinding? = null
    private val binding get() = _binding!!

    private var testingRoomState = false
    private var roomTestingProgressTimeComplete = false

    private var endDate: Long? = null
    private var testingViewModel: TestingViewModel = TestingViewModel()
    private val df = DecimalFormat("#,###.00")

    private var metricsCounter = 0
    private val sessionTimeHandler = Handler(Looper.getMainLooper())
    private val sessionTimeRunn = object: Runnable {
        override fun run() {

            //Every Second
            addSecond()

            //Need to add second to global from here, no other way based off my testing
            global_sessionSeconds++

            if (testingRoomState) global_roomSeconds++

//            if (metricsCounter % global_testingMetricsFrequency == 0) { processNetworkData(wifiManager!!, telephonyManager) }
            metricsCounter++

            if (!global_testingState) sessionTimeHandler.removeCallbacks(this)
            else sessionTimeHandler.postDelayed(this, 1000)
        }
    }

    private var wifiManager: WifiManager? = null
    private var telephonyManager: TelephonyManager? = null


    private val progressTimeHandler = Handler()
    private val progressTimeRunnable = object : Runnable {
        override fun run() {
            if (!roomTestingProgressTimeComplete) {
                (testingViewModel as TestingViewModel).addSecondToRoomProgressTime()
                progressTimeHandler.postDelayed(this, 1000)
            }
            else {
                (testingViewModel as TestingViewModel).resetRoomTestingProgressTime()
                progressTimeHandler.removeCallbacks(this)
            }
        }
    }

    private val inactivityTimeSeconds: Long = ( 60 * 10 * 1000L ) // ( Seconds )  * ( Minutes )  * ( millisecond conversion )

    private var lifecycleLoopRunning = false //To prevent multiple scopes running adding seconds
    private val speedTestSocket by lazy { SpeedTestSocket().apply { socketTimeout = 5000 } }
    private val wifiManagerTemp by lazy { context?.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val telephonyManagerTemp by lazy { context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    override fun onResume() {

        if (global_testingState) {
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

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?

        //Initial
        val setSessionNameDialog = getSessionNameAlertDialog()
        val testRoomDialog = getRoomTestingDialog()
        val endTestingDialog = getEndAlert()
        val portCheckDialog = getPortCheckAlert()
        val sessionInactivityDialog = getInactivityDialog()

        val tempRunn = Runnable {
            if (sessionInactivityDialog.isShowing) {
                sessionInactivityDialog.dismiss()
                endSession()
            }
        }
        val tempHandler = Handler()

        //Start the Inactivity Timer when user starts session
        val inactivityHandler = Handler()
        val inactivityRunnable = Runnable {
            if (global_testingState) {
                requireActivity().runOnUiThread {
                    sessionInactivityDialog.show()
                    tempHandler.postDelayed(tempRunn, 5000)
                }
            }
        }

        //RecyclerView
        val recyclerView = binding.testingRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = TestingAdapter(global_roomList, requireContext())
        recyclerView.adapter = adapter

        testingViewModel = this.run { ViewModelProvider(this)[TestingViewModel::class.java] }

        //11.9 NEW METHODS
        testingViewModel.getSessionData().observe(viewLifecycleOwner) { sessionMap ->

            try {

                val seconds = sessionMap["sessionTotalTime"]?.toInt()
                seconds?.let {
                    val secondsToMinutesString = Utility.getTimeFormat(it)
                    binding.mainTestingTime.text = "${secondsToMinutesString.first}m ${secondsToMinutesString.second}s"
                }


                adapter.notifyDataSetChanged()
                val totalRooms = sessionMap["sessionTotalRooms"]
                totalRooms?.let { binding.testingTotalRooms.text = it }

            } catch (e: Exception) {
                Log.d(TAG, "onViewCreated: $e")
            }

        }
        testingViewModel.getRoomData().observe(viewLifecycleOwner) { roomDataMap ->

            //Room Time Progress
            val progressTime = roomDataMap["sessionRoomProgress"]?.toInt()
            progressTime?.let {

                if (global_roomProgressComplete) return@let
                val progressPercent = getPercentProgress(progressTime)
                (testRoomDialog.findViewById(R.id.testingRemainingProgressBar) as ProgressBar?)?.apply { progress = progressPercent }
                val roomSessionProgressText: TextView = testRoomDialog.findViewById(R.id.testingRemainingText)!!
                when (progressPercent) {
                    in 0..15 ->  {  roomSessionProgressText.text = "Grabbing metrics in room... (${progressPercent}%)" }
                    in 16..29 -> {  roomSessionProgressText.text = "Please standby until completed... (${progressPercent}%)" }
                    in 30..45 -> {  roomSessionProgressText.text = "Almost at the halfway mark... (${progressPercent}%)" }
                    in 46..70 -> {  roomSessionProgressText.text = "Just a few more seconds... (${progressPercent}%)" }
                    in 71..99 -> {  roomSessionProgressText.text = "Finalizing data... (${progressPercent}%)" }
                    100 -> { roomSessionProgressText.text = "Room Testing Complete" }
                }

                if (it == global_testingRoomTimeLimit) {
                    global_roomProgressComplete = true
                    (testRoomDialog.findViewById(R.id.closeButton) as Button?)?.apply {
                        isEnabled = true
                        setTextColor(resources.getColor(R.color.white))
                        setBackgroundColor(resources.getColor(R.color.button_green))
                    }
                }
            }

            //Download Progress
            val progress = roomDataMap["speedTestProgress"]?.toInt() ?: 0
            (testRoomDialog.findViewById(R.id.downloadProgressText) as TextView?)?.let { it.text = "${progress}/100" }
            (testRoomDialog.findViewById(R.id.downloadProgressBar) as ProgressBar?)?.let { tv -> tv.progress = progress }
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
            if (global_testingState) setSessionNameDialog.show()
            true
        }

        binding.startTestingButton.setOnClickListener {

            if ( !global_completedPortChecker && global_portCheckRequired ) {
                portCheckDialog.show()
                return@setOnClickListener
            }

            setSessionNameDialog.show()
            if (!lifecycleLoopRunning) {
                lifecycleLoopRunning = true
                viewLifecycleOwner.lifecycleScope.launch {
                    //if testing add second
                    while (true) {

                        if (global_testingState) {

                            testingViewModel.addSessionData()

                            if (global_roomTestingState) {
                                testingViewModel.addRoomData(wifiManagerTemp, telephonyManagerTemp)
                                if (!global_roomProgressComplete) testingViewModel.addRoomProgressSecond()
                            }

                        }

                        delay(1000L)

                    }
                }
            }


            inactivityHandler.postDelayed(inactivityRunnable, inactivityTimeSeconds)
        }

        binding.endTestingButton.setOnClickListener {
            //End Testing Modal - Confirmation
            endTestingDialog.show()
        }

        binding.addRoomButton.setOnClickListener {

            //Delay Inactivity Timer
            inactivityHandler.removeCallbacks(inactivityRunnable)
            inactivityHandler.postDelayed(inactivityRunnable, inactivityTimeSeconds)

            testingRoomState = true
            testingViewModel.addRoomNetworkData(wifiManagerTemp, telephonyManagerTemp)
            testingViewModel.addRoomCount()
            testRoomDialog.show()
            global_roomTestingState = true
            roomTestingProgressTimeComplete = false
            progressTimeHandler.postDelayed(progressTimeRunnable, 1000)
            processNetworkData(wifiManager!!, telephonyManager)
            CoroutineScope(Dispatchers.IO).launch { speedTestSocket.startDownload("http://speedtest.tele2.net/10MB.zip") }
            CoroutineScope(Dispatchers.Main).launch {
                while(global_roomTestingState) {
                    testingViewModel.addRoomNetworkData(wifiManagerTemp, telephonyManagerTemp)
                    delay(global_testingMetricsFrequency*1000L)
                    if (!global_roomTestingState) this.cancel("GC-Scope")
                }
            }
//            GlobalScope.launch(Dispatchers.IO) { speedTestSocket.startDownload("http://speedtest.tele2.net/10MB.zip") }


        }

        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {

            override fun onCompletion(report: SpeedTestReport?) {
                val speedMb = df.format(report?.transferRateBit?.div(BigDecimal(1000000)))
                testingViewModel.addSessionRoomDownloadProgress(100)
                testingViewModel.addSessionRoomDownloadResult("Result: $speedMb Mbps")
                requireActivity().runOnUiThread {

//
                }
            }

            override fun onProgress(percent: Float, report: SpeedTestReport?) {
                testingViewModel.addSessionRoomDownloadProgress(percent.toInt())
                requireActivity().runOnUiThread {

                }
            }

            override fun onError(speedTestError: SpeedTestError?, errorMessage: String?) {
                Log.d("SpeedTest", "onError: ERROR: $errorMessage")
            }

        })

        //Resume
        if (global_testingState) { resumeSession() }
        else if (global_testName != "") { showPrevSessionName() }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun showPrevSessionName() {

        binding.sessionNameDisplay.text = "Prev. Session Name:"
        binding.sessionName.text = global_testName
        binding.tapHoldLabel.visibility = View.INVISIBLE
        binding.sessionNameContainer.visibility = View.VISIBLE

    }

    private fun showSessionName() {

        binding.sessionNameContainer.visibility = View.VISIBLE
        binding.sessionName.text = global_testName
        binding.tapHoldLabel.visibility = View.VISIBLE
        binding.sessionNameContainer.isClickable = true

    }

    private fun showRoomButtons() {

        //Hide "Start Session" Button
        binding.startTestingButton.visibility = View.GONE

        //View Session Name
        showSessionName()

        //View Room buttons, Session time, and total rooms
        binding.testingSessionTimerContainer.visibility = View.VISIBLE
        binding.testingButtonContainer.visibility = View.VISIBLE
        binding.testingSessionTotalRoomsContainer.visibility = View.VISIBLE

    }

    private fun hideTestingMedia() {

        //Hide room buttons, session time, and total rooms
        binding.testingSessionTimerContainer.visibility = View.GONE
        binding.testingButtonContainer.visibility = View.GONE
        binding.testingSessionTotalRoomsContainer.visibility = View.GONE

    }

    private fun startSessionTimer() {

        sessionTimeHandler.post(sessionTimeRunn)

    }


    private fun startNewSession() {

        global_testStartTimeEpoch = System.currentTimeMillis()
        global_testingState = true
        testingViewModel.resetSessionSeconds()

        hideNavBar()
        showRoomButtons()
        showSessionName()
        startSessionTimer()
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        global_roomList.clear()
        testingViewModel.resetRoomCount()
        testingViewModel.resetTestSessionData()

    }

    private fun hideNavBar() {
        val navBar: BottomNavigationView = requireActivity().findViewById(R.id.nav_view)
        navBar.clearAnimation()
        navBar.animate().translationY(navBar.height.toFloat()).duration = 500

        val controller = requireActivity().window.insetsController
        controller?.hide(WindowInsets.Type.navigationBars())
        controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun showNavBar() {
        val navBar: BottomNavigationView = requireActivity().findViewById(R.id.nav_view)
        navBar.clearAnimation()
        navBar.animate().translationY(0F).duration = 500
        val controller = requireActivity().window.insetsController
        controller?.show(WindowInsets.Type.navigationBars())
    }

    private fun resumeSession() {

        hideNavBar()
        showRoomButtons()
        showSessionName()
        testingViewModel.setSessionSeconds(global_sessionSeconds)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


    }

    private fun endSession() {

        hideTestingMedia()

        //View Start Session Button
        binding.startTestingButton.visibility = View.VISIBLE

        //View Session Name (Prev. version)
        binding.sessionNameDisplay.text = "Prev. Session Name:"
        binding.sessionName.text = global_testName

        //Disable edit functionality for session name
        binding.sessionNameContainer.isClickable = false
        binding.tapHoldLabel.visibility = View.INVISIBLE

        global_testingState = false
        sessionTimeHandler.removeCallbacks(sessionTimeRunn)
        showNavBar()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    }

    private fun getInactivityDialog(): AlertDialog {

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Are You There?")
        builder.setMessage("Testing will automatically end unless you hit \"Continue Testing\"")

        builder.setPositiveButton("Continue Testing") { di, _ ->

            di.dismiss()

        }

        builder.setNegativeButton("End Testing") { di, _ ->

            endSession()
            di.dismiss()

        }

        return builder.create()
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
                                totalTimeSeconds = global_roomSeconds,
                                downloadSpeedResult = global_downloadResult, avgRsrq = global_testingCellModel.rsrqValueList.average().toInt(),
                                avgRsrp = global_testingCellModel.rsrpValueList.average().toInt() )

            global_roomList.add(ri)
            global_roomSeconds = 0
            global_testingCellModel = TestingCellModel()

            testingRoomState = false
            global_roomTestingState = false
            dialog.dismiss()

            testingProgressBar.progress = 0
            testingProgressText.text = "0/100"
            speedTestProgressBar.progress = 0
            resultText.text = "Running Test..."

            roomNameTitle.visibility = View.GONE
            userRoomNameInput.visibility = View.VISIBLE
            userRoomNameInput.text.clear()

            testingViewModel.resetProgressSeconds()
            global_roomProgressComplete = false
            buttonClose.isEnabled = false
            buttonClose.setTextColor(Color.BLACK)
            buttonClose.setBackgroundColor(resources.getColor(R.color.extra_light_gray))

        }

        dialog.setCancelable(false)
        dialog.setContentView(view)

        return dialog
    }

    private fun getSessionNameAlertDialog(): AlertDialog {

        val layoutInflater = LayoutInflater.from(requireContext())
        val startTestingView: View  = layoutInflater.inflate(R.layout.dialog_testing_start_testing, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Session Name")
        builder.setView(startTestingView)

        val input = startTestingView.findViewById<EditText>(R.id.editTextText)
        builder.setPositiveButton("Submit") { di, _ ->
            global_testName = input.text.toString()
            if (!global_testingState) startNewSession()
            di.cancel()
        }

        return builder.create()
    }

    private fun getPortCheckAlert(): AlertDialog {


        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Port Check Required")
        builder.setMessage("Please run a port check before starting a test session")
        builder.setIcon(R.drawable.status_alert)

        builder.setPositiveButton("Got it") { di, _ ->
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
            endSession()
            testingViewModel.resetSessionSeconds()
            di.cancel()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            //Do Nothing
        }

        return builder.create()

    }

    private fun addSecond() {
        Log.d("function", "addSecond: $global_sessionSeconds")
        if (testingRoomState) { testingViewModel?.roomAddSecond() }
        testingViewModel?.sessionAddSecond()

    }

    private fun getPercentProgress(num: Int): Int {

        val result = num.toFloat().div(global_testingRoomTimeLimit)*100
        return result.toInt()


    }

    @SuppressLint("MissingPermission")
    private fun processNetworkData(wm: WifiManager, tm: TelephonyManager?) {

        testingViewModel?.addRssi(wm.connectionInfo.rssi)
        testingViewModel?.addLinkRate(wm.connectionInfo.linkSpeed)
        testingViewModel?.addConnectedBssid(wm.connectionInfo.bssid)

        if (tm == null) {
            Log.d("Result", "processNetworkData: tm is null")
            return
        }
        Log.d("Result", "processNetworkData: processing tm")
        tm.allCellInfo.forEach {
            when(it) {
                is CellInfoLte -> {

                    if (!it.isRegistered) return@forEach
                    testingViewModel?.addRsrp(it.cellSignalStrength.rsrp)
                    testingViewModel?.addRsrq(it.cellSignalStrength.rsrq)
                    testingViewModel?.addConnectedBand(Utility.getCellBand(it.cellIdentity.earfcn))

                    val lteData = lteObject(it.cellSignalStrength.rsrp, it.cellSignalStrength.rsrq, System.currentTimeMillis())
                    global_cellTimeLine.add(lteData)

                    if (testingRoomState) { global_testingCellModel.addMetrics(it.cellSignalStrength.rsrp, it.cellSignalStrength.rsrq, Utility.getCellBand(it.cellIdentity.earfcn)) }

                }
            }
        }
    }

}