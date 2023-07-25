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

@SuppressLint("SetTextI18n")
class TestingFragment: Fragment(R.layout.fragment_testing)  {

    private var _binding: FragmentTestingBinding? = null
    private val binding get() = _binding!!

    private var roomSeconds: Int = 0

    private var testingRoomState = false

    private var startDate: Long? = null
    private var endDate: Long? = null
    private var model: TestingViewModel? = null
    private var testTotalRooms = 0

    private val handler = Handler()
    private val runnable = object: Runnable {
        override fun run() {

            if (testingRoomState) {
                model?.roomAddSecond()
                roomSeconds++
            }

            if (global_testingState) {
                (model as TestingViewModel).testingAddSecond()
                handler.postDelayed(this, 1000)
            } else {
                (model as TestingViewModel).testingResetSeconds()
                handler.removeCallbacks(this)
                }
            }

        }

    private val handlerRoom = Handler()
    private val runnableRoom = object : Runnable {
        override fun run() {
            if (testingRoomState) {
                model?.roomAddSecond()
                handlerRoom.postDelayed(this, 1000)
            } else { handlerRoom.removeCallbacks(this) }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentTestingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        Log.d("TestingActivity", "Is Testing?: $global_testingState")

        //Initial
        val changeNameAlert = getStartAlert()
        val bottomDialog = getBottomDialog()
        val endTestingDialog = getEndAlert()

        //RecyclerView
        val recyclerView = binding.testingRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = TestingAdapter(global_roomList, requireContext())
        recyclerView.adapter = adapter

        model = this.run { ViewModelProvider(this)[TestingViewModel::class.java] }

        //Session Timer (minutes and seconds)
        model?.testFuncGetSec()?.observe(viewLifecycleOwner) {seconds ->
            val roomSessionNumber = bottomDialog.findViewById<TextView>(R.id.testingRoomSessionNumber)
            if (roomSessionNumber != null) roomSessionNumber.text = "$testTotalRooms"
            val time = Utility.getTimeFormat(seconds)
            binding.mainTestingTime.text = "${time.first}m ${time.second}s"
        }

        //General Stats Update For List & Testing Dialog
        model?.updateUi()?.observe(viewLifecycleOwner) {_ ->

            val roomSessionNumber = bottomDialog.findViewById<TextView>(R.id.testingRoomSessionNumber)
            if (roomSessionNumber != null) roomSessionNumber.text = "$testTotalRooms"
            binding.testingTotalRooms.text = testTotalRooms.toString()

            adapter.notifyDataSetChanged()

        }

        model?.getRoomSeconds()?.observe(viewLifecycleOwner){

            Log.d("RoomTimer", "Running")
            val roomSessionTimer = bottomDialog.findViewById<TextView>(R.id.roomSessionTimerLbl)
            val time = Utility.getTimeFormat(it)
            roomSessionTimer?.text = "${time.first}m ${time.second}s"
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


            testTotalRooms+=1
            testingRoomState = true
//            handlerRoom.postDelayed(runnableRoom, 1000)

            model?.forceUiChange()
            bottomDialog.show()
        }



        //Reload
        if (global_testingState) {
            adapter.notifyDataSetChanged()
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



    @SuppressLint("SetTextI18n")
    private fun startTesting() {

        binding.startTestingButton.visibility = View.GONE
        binding.testingSessionTimerContainer.visibility = View.VISIBLE
        binding.testingButtonContainer.visibility = View.VISIBLE
        binding.testNameLabel.text = "Session Name: $global_testName\n(tap and hold to edit)"
        binding.testNameLabel.visibility = View.VISIBLE
        binding.testingSessionTotalRoomsContainer.visibility = View.VISIBLE
        if (!global_testingState) {
            global_testingState = true
            handler.postDelayed(runnable, 1000)
        }
    }


    private fun endTesting() {

        binding.startTestingButton.visibility = View.VISIBLE
        binding.testingSessionTimerContainer.visibility = View.GONE
        binding.testingButtonContainer.visibility = View.GONE
        binding.testingSessionTotalRoomsContainer.visibility = View.GONE
        binding.testNameLabel.text = "Last Session Name: $global_testName"
        global_testingState = false
        handler.removeCallbacks(runnable)

    }

    @SuppressLint("SetTextI18n")
    private fun getBottomDialog(): BottomSheetDialog {

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
        builder.setTitle("End Testing Session?")
        builder.setPositiveButton("End Testing") {di, _ ->
            //End Testing Session
            val ed = System.currentTimeMillis()
            endDate = ed
            global_testEndTimeEpoch = ed
            endTesting()
            di.cancel()
        }
        builder.setNegativeButton("Cancel/Continue Testing") { _, _ ->
            //Do Nothing
        }

        return builder.create()

    }


}