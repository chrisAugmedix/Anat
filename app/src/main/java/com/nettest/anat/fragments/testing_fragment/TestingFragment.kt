package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nettest.anat.R
import com.nettest.anat.RoomResult
import com.nettest.anat.Utility
import com.nettest.anat.databinding.FragmentTestingBinding
import com.nettest.anat.global_roomList
import com.nettest.anat.global_testEndTimeEpoch
import com.nettest.anat.global_testName
import com.nettest.anat.global_testStartTimeEpoch
import com.nettest.anat.global_testingState
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

    private var roomSeconds: Int = 0

    private var testingRoomState = false

    private var startDate: Long? = null
    private var endDate: Long? = null
    private var model: TestingViewModel? = null
    private val df = DecimalFormat("#,###.00")

    private val handler = Handler()
    private val runnable = object: Runnable {
        override fun run() {

            if (testingRoomState) {
                model?.roomAddSecond()
                roomSeconds++
            }

            if (global_testingState) {
                (model as TestingViewModel).sessionAddSecond()
                handler.postDelayed(this, 1000)
            } else {
                (model as TestingViewModel).resetSessionSeconds()
                handler.removeCallbacks(this)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentTestingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        //Initial
        val changeNameAlert = getStartAlert()
        val testRoomDialog = getRoomTestingDialog()
        val endTestingDialog = getEndAlert()

        //Speed Test Object Initialized
        val speedTestSocket: SpeedTestSocket = SpeedTestSocket()
        speedTestSocket.socketTimeout = 5000


        //RecyclerView
        val recyclerView = binding.testingRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = TestingAdapter(global_roomList, requireContext())
        recyclerView.adapter = adapter

        model = this.run { ViewModelProvider(this)[TestingViewModel::class.java] }

        //Session Timer (minutes and seconds)
        model?.getSessionSeconds()?.observe(viewLifecycleOwner) { seconds ->
            val roomSessionNumber = testRoomDialog.findViewById<TextView>(R.id.testingRoomSessionNumber)
            if (roomSessionNumber != null) roomSessionNumber.text = "${global_roomList + 1}"
            val time = Utility.getTimeFormat(seconds)
            binding.mainTestingTime.text = "${time.first}m ${time.second}s"
        }

        //General Stats Update For List & Testing Dialog
        model?.updateUi()?.observe(viewLifecycleOwner) {_ ->
            adapter.notifyDataSetChanged()
        }

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

        if (global_roomList.isNotEmpty()) {
            model?.resetSessionSeconds()
            Log.d("TestingFragmentSubmitStartTest", "getStartAlert: Running uichange\t${model==null}")
            global_roomList.clear()
            model?.resetRooms()
            model?.updateUi()
        }

        if (!global_testingState) {
            Log.d("startTesting()", "startTesting: Running seconds Runnable")
            global_testingState = true
            handler.postDelayed(runnable, 1000)
        }
    }


    private fun endTesting() {

        binding.startTestingButton.visibility = View.VISIBLE
        hideTestingMedia()
        binding.testNameLabel.text = "Last Session Name: $global_testName"
        global_testingState = false
        handler.removeCallbacks(runnable)

    }


    @SuppressLint("SetTextI18n")
    private fun getRoomTestingDialog(): BottomSheetDialog {

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.testing_bottom_sheet_testing_dialog, null)

        val buttonClose = view.findViewById<Button>(R.id.closeButton)
        val userRoomNameInput = view.findViewById<EditText>(R.id.userSessionRoomNameEditText)
        val roomNameTitle = view.findViewById<TextView>(R.id.roomNameTitle)
        val userInputContainer = view.findViewById<LinearLayout>(R.id.sessionRoomNameContainer)
        var roomName = ""

        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.duration = 100

        userRoomNameInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                roomName = v.text.toString()
                roomNameTitle.text = "Room: ${v.text}"
                roomNameTitle.visibility = View.VISIBLE
                val imm: InputMethodManager = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                userInputContainer.startAnimation(fadeOut)
                userInputContainer.visibility = View.GONE
            }
            false
        }

        roomNameTitle.setOnClickListener {
            userInputContainer.visibility = View.VISIBLE
        }

        buttonClose.setOnClickListener {
            global_roomList.add(RoomInfo(R.drawable.alert_svg, roomName, roomState = RoomResult.ALERT, roomSeconds))
            roomSeconds = 0
            testingRoomState = false
            model!!.forceUiChange()
            model!!.resetRoomSeconds()
            dialog.dismiss()
            userInputContainer.visibility = View.VISIBLE
            roomNameTitle.text = "Room: "
            userRoomNameInput.text.clear()
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


}