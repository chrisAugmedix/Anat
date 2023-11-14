package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Display
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat


@SuppressLint("SetTextI18n")
class TestingFragment: Fragment(R.layout.fragment_testing)  {

    private val TAG = "TestingFragmentActivity"
    private var _binding: FragmentTestingBinding? = null
    private val binding get() = _binding!!


    private var inactivityEpochTime: Long? = null
    private var roomTestingProgressTimeComplete = false
    private var endDate: Long? = null
    private var testingViewModel: TestingViewModel = TestingViewModel()
    private val df = DecimalFormat("#,###.00")

    private val inactivityTimeSeconds: Long = ( 1 * 10 * 1000L ) // ( Seconds )  * ( Minutes )  * ( millisecond conversion )
    private val inactivityMessageTimeOut: Long = 10000 // ( seconds ) * (ms to s conversion)
    private var lifecycleLoopRunning = false //To prevent multiple scopes running adding seconds
    private val speedTestSocket by lazy { SpeedTestSocket().apply { socketTimeout = 5000 } }
    private val wifiManager by lazy { context?.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val telephonyManager by lazy { context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

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

        //Initial
        val setSessionNameDialog = getSessionNameAlertDialog()
        val testRoomDialog = getRoomTestingDialog()
        val endTestingDialog = getEndAlert()
        val portCheckDialog = getPortCheckAlert()
        val sessionInactivityDialog = getInactivityDialog()


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
            var showingInactivityModal = false
            setSessionNameDialog.show()
            if (!lifecycleLoopRunning) {

                lifecycleLoopRunning = true
                viewLifecycleOwner.lifecycleScope.launch {
                    //if testing add second
                    while (true) {

                        let {
                            if (!global_testingState) return@let

                        }
                        if (global_testingState) {

//                            keepScreenOn()

                            if ( didInactivityExceedThreshold() ) {

                                if (!showingInactivityModal) {
                                    showingInactivityModal = true
                                    CoroutineScope(Dispatchers.Main).launch {

                                        sessionInactivityDialog.show()
                                        delay(inactivityMessageTimeOut)
                                        sessionInactivityDialog.dismiss()
                                        if (!userInactivityInteracted) endSession()
                                        showingInactivityModal = false
                                        userInactivityInteracted = false

                                    }
                                }
                            } else {

                                testingViewModel.addSessionData()

                                if (global_roomTestingState) {
                                    testingViewModel.addRoomData()
                                    global_roomSeconds++
                                    if (!global_roomProgressComplete) testingViewModel.addRoomProgressSecond()
                                }
                            }

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

            testingViewModel.addDataRoomCount()
            testingViewModel.addRoomNetworkData(wifiManager, telephonyManager)
            testRoomDialog.show()
            global_roomTestingState = true
            roomTestingProgressTimeComplete = false
            CoroutineScope(Dispatchers.IO).launch { speedTestSocket.startDownload("http://speedtest.tele2.net/10MB.zip") }
            CoroutineScope(Dispatchers.Main).launch {
                while(global_roomTestingState) {
                    testingViewModel.addRoomNetworkData(wifiManager, telephonyManager)
                    delay(global_testingMetricsFrequency*1000L)
                    if (!global_roomTestingState) this.cancel("GC-Scope")
                }
            }


        }

        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {

            override fun onCompletion(report: SpeedTestReport?) {
                val speedMb = df.format(report?.transferRateBit?.div(BigDecimal(1000000)))

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
        if (global_testingState) { resumeSession() }
        else if (global_testName != "") { showPrevSessionName() }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun didInactivityExceedThreshold(): Boolean {
        if (inactivityEpochTime == null) return false
        return System.currentTimeMillis() > inactivityEpochTime!!
    }
    private fun setInactiveTimeoutEpoch(idleTime: Boolean = false) {
        val currentTimeMillis = System.currentTimeMillis()
        inactivityEpochTime = currentTimeMillis + ( if (idleTime) 20000 else inactivityTimeSeconds )
    }

    private fun keepScreenOn() {
        val displayManager = context?.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
        displayManager?.displays?.forEach {
            if (it.state != Display.STATE_OFF) return@forEach
            Log.d(TAG, "Screen is Off")

//            val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager?
//            powerManager?.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Anat: Wakeup")?.acquire(1000)
        }
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


    private fun startNewSession() {

        global_testStartTimeEpoch = System.currentTimeMillis()
        global_testingState = true

        hideNavBar()
        showRoomButtons()
        showSessionName()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setInactiveTimeoutEpoch()

        global_roomList.clear()
        testingViewModel.resetTestSessionData()

    }

    private fun showHistory() {

        binding.titleLabel1.text = "Testing History"
        binding.viewHistoryButton.text = "Hide History"
        binding.titleDescription1.visibility = View.GONE
        val spinner = getSpinnerObject()
        val recyclerView = binding.testingRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, positionId: Long) {

                val sessionName = parent?.getItemAtPosition(position).toString()
                if ( sessionName ==  "No History Found" ) return
                val roomDataList = global_sessionDataList.firstOrNull { it.sessionName == sessionName }?.roomDataList
                roomDataList?.let {
                    if (it.isEmpty()) return@let
                    val adapter = TestingAdapter(roomDataList, requireContext())
                    recyclerView.adapter = adapter
                }

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
        binding.titleDescription1.visibility = View.VISIBLE
        binding.historySpinner.visibility = View.GONE

    }

    private fun getSpinnerObject(): Spinner {

        val spinner: Spinner = binding.historySpinner
        val connectedSsid = wifiManager.connectionInfo?.ssid
        val items: Array<String> = if (global_sessionDataList.isEmpty() || ( connectedSsid == null || connectedSsid.contains("unknown")) ) arrayOf("No History Found")
                    else global_sessionDataList.filter { data -> data.ssid == connectedSsid }.map { it.sessionName }.toTypedArray()

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

    private fun hideNavBar() {
        val navBar: BottomNavigationView = activity?.findViewById(R.id.nav_view) ?: return
        navBar.clearAnimation()
        navBar.animate().translationY(navBar.height.toFloat()).duration = 500

        val controller = activity?.window?.insetsController
        controller?.hide(WindowInsets.Type.navigationBars())
        controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun showNavBar() {
        val navBar: BottomNavigationView = activity?.findViewById(R.id.nav_view) ?: return
        navBar.clearAnimation()
        navBar.animate().translationY(0F).duration = 500
        val controller = activity?.window?.insetsController ?: return
        controller.show(WindowInsets.Type.navigationBars())
    }

    private fun resumeSession() {

        hideNavBar()
        showRoomButtons()
        showSessionName()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


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
        showNavBar()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    }

    private var userInactivityInteracted = false
    private fun getInactivityDialog(): AlertDialog {

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Are You There?")
        builder.setMessage("Testing will automatically end unless you hit \"Continue Testing\"")

        builder.setPositiveButton("Continue Testing") { di, _ ->
            userInactivityInteracted = true
            setInactiveTimeoutEpoch(idleTime = true)
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

        roomNameTitle.setOnClickListener { userRoomNameInput.visibility = View.VISIBLE }

        buttonClose.setOnClickListener {

            //Update Inactivity Time
            setInactiveTimeoutEpoch()

            if (userRoomNameInput.text.toString() == "") { roomName = "Room ${global_roomList.size+1}*" }

            val ri = RoomInfo(  lteImage = global_testingCellModel.gradeImage, roomName=roomName,
                                totalTimeSeconds = global_roomSeconds,
                                downloadSpeedResult = global_downloadResult, avgRsrq = global_testingCellModel.rsrqValueList.average().toInt(),
                                avgRsrp = global_testingCellModel.rsrpValueList.average().toInt() )

            global_roomList.add(ri)
            global_roomSeconds = 0
            global_testingCellModel = TestingCellModel()

            global_roomTestingState = false
            dialog.dismiss()

            roomNameTitle.visibility = View.GONE
            userRoomNameInput.visibility = View.VISIBLE
            userRoomNameInput.text.clear()

            testingViewModel.resetRoomValues()
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
        builder.setIcon(R.drawable.room_grade_alert)

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
            testingViewModel.resetTestSessionData()
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


}
