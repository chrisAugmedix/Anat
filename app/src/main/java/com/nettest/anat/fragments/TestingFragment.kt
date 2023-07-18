package com.nettest.anat.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentTestingBinding

class TestingFragment: Fragment(R.layout.fragment_testing)  {

    private var _binding: FragmentTestingBinding? = null
    private val binding get() = _binding!!
    private var testName = ""
    private var testingState = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentTestingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Initial
        val startAlert = getStartAlert()

        binding.testNameLabel.setOnLongClickListener {
            startAlert.show()
            true
        }

        binding.startTestingButton.setOnClickListener {
            startAlert.show()
        }


        //Reload
        if (testName.length > 3) { changeTestName() }
        if (testingState) { changeButtonLayout() }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun changeButtonLayout() {

    }

    @SuppressLint("SetTextI18n")
    private fun changeTestName() {
        binding.testNameLabel.text = "Test Name: $testName\n(tap and hold to edit)"
        binding.testNameLabel.visibility = View.VISIBLE
    }


    private fun getStartAlert(): AlertDialog {

        val layoutInflater = LayoutInflater.from(requireContext())
        val startTestingView: View  = layoutInflater.inflate(R.layout.dialog_testing_start_testing, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Physician or Clinic Name")
        builder.setView(startTestingView)

        val input = startTestingView.findViewById<EditText>(R.id.editTextText)

        builder.setPositiveButton("SUBMIT") { di, _ ->
            testName = input.text.toString()
            if ( testName.length > 4 ) {
                testingState = true
                changeTestName()
            }
            di.cancel()
        }

        return builder.create()
    }


}