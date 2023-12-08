package com.nettest.anat.fragments.pc_fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentPortCheckerBinding
import com.nettest.anat.fragments.NetworkOperations
import com.nettest.anat.global_completedPortChecker
import com.nettest.anat.global_isPortCheckerRunning
import com.nettest.anat.global_resultList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PortCheckerFragment: Fragment(R.layout.fragment_port_checker) {

    private var _binding: FragmentPortCheckerBinding? = null
    private val binding get() = _binding!!
    private val recyclerViewResultItems: MutableList<EndpointParent> = mutableListOf()

    private var portCheckerViewModel: PortCheckerViewModel = PortCheckerViewModel()
    private val portCheckRecyclerView by lazy { binding.pcRecyclerView }
    private val portCheckRecyclerViewAdapter by lazy {PortCheckerRecyclerViewAdapter(recyclerViewResultItems, context)}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentPortCheckerBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

//        if (global_isPortCheckerRunning) {
//            deselectButton(binding.pcButtonFailed)
//            deselectButton(binding.pcButtonSuccess)
//            binding.pcButtonRun.showLoading()
//            binding.pcButtonRun.isClickable = false
//            highlightButton(binding.pcButtonAll)
//        }
        setButtonStatus(false)
        portCheckerViewModel = this.run { ViewModelProvider(this)[PortCheckerViewModel::class.java] }
        portCheckerViewModel.getTotalEndpoints().observe(viewLifecycleOwner) { binding.pcTotalEndpoints.text = it.toString() }
        portCheckerViewModel.getTotalFailed().observe(viewLifecycleOwner) { binding.pcTotalFailed.text = it.toString() }
        portCheckerViewModel.getTotalSuccess().observe(viewLifecycleOwner) { binding.pcTotalSuccess.text = it.toString() }
        portCheckerViewModel.getLastEndpoint().observe(viewLifecycleOwner) { addItemToRvList(it) }
        portCheckRecyclerView.layoutManager = LinearLayoutManager(context)
        portCheckRecyclerView.adapter = portCheckRecyclerViewAdapter

        binding.titleInfoButton.setOnClickListener {
            getInfoDialog().show()
        }

        binding.pcButtonAll.setOnClickListener {
            buttonSelection(ButtonOption.ALL)
        }

        binding.pcButtonFailed.setOnClickListener {
            buttonSelection(ButtonOption.FAILED)
        }

        binding.pcButtonSuccess.setOnClickListener {
            buttonSelection(ButtonOption.PASSED)
        }

        binding.pcButtonRun.setOnClickListener {

            clearRvList()
//            hideBottomNavBar()
            setButtonStatus(false)
            clearAllLists()
            deselectButton(binding.pcButtonFailed)
            deselectButton(binding.pcButtonSuccess)

            binding.pcButtonRun.showLoading()
            resetEndpointStats()
            binding.pcButtonRun.isClickable = false
            highlightButton(binding.pcButtonAll)
            global_isPortCheckerRunning = true
            CoroutineScope(Dispatchers.Default).launch {

                endpointList.forEach {
                    Log.d("EndpointLoop", "onViewCreated: $it")
                    val endpointParent = when(it.requestType) {
                        RequestType.PING -> { processPing(it) }
                        RequestType.GET -> { processGet(it) }
                    }
                    global_resultList.add(endpointParent)
                    activity?.runOnUiThread {
                        portCheckerViewModel.addLastEndpointParent(endpointParent)
                        portCheckerViewModel.addEndpointTotal()
                    }

                }

                CoroutineScope(Dispatchers.Main).launch {
                    setButtonStatus(true)
                    highlightButton(binding.pcButtonAll)
                    binding.pcButtonRun.hideLoading()
                    binding.pcButtonRun.isClickable = true
                    global_isPortCheckerRunning = false
                    showNavBar()
                }
            }
            global_completedPortChecker = true

        }

        loadList()

        super.onViewCreated(view, savedInstanceState)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun hideBottomNavBar() {
        activity?.let {
            it.findViewById<BottomNavigationView>(R.id.nav_view).apply {
                clearAnimation()
                animate().translationY(this.height.toFloat()).duration = 500
            }
        }
    }

    private fun showNavBar() {
        val navBar: BottomNavigationView = activity?.findViewById(R.id.nav_view) ?: return
        navBar.clearAnimation()
        navBar.animate().translationY(0F).duration = 500
    }

    private fun clearAllLists() {
        global_resultList.clear()
    }

    private fun getInfoDialog(): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("More Information")
        builder.setMessage("For more information on each endpoint, please refer to our \"Best Practices\" documentation")

        builder.setPositiveButton("Got it") { di, _ ->
            di.cancel()
        }

        return builder.create()
    }
    private fun highlightButton(button: Button) {
        activity?.let {
            button.setBackgroundColor(it.resources.getColor(R.color.light_gray, null))
            button.setTextColor(it.resources.getColor(R.color.white, null))
        }

    }

    private fun deselectButton(button: Button) {
        button.setBackgroundColor(resources.getColor(R.color.white, null))
        button.setTextColor(resources.getColor(R.color.gray, null))
    }
    private fun setButtonStatus(status: Boolean) {

        binding.pcButtonSuccess.isEnabled = status
        binding.pcButtonFailed.isEnabled = status
        binding.pcButtonAll.isEnabled = status
        activity?.let {
            if (!status) {
                binding.pcButtonSuccess.setTextColor(it.resources.getColor(R.color.light_gray, null))
                binding.pcButtonFailed.setTextColor(it.resources.getColor(R.color.light_gray, null))
                binding.pcButtonAll.setTextColor(it.resources.getColor(R.color.light_gray, null))
            } else {
                binding.pcButtonSuccess.setTextColor(it.resources.getColor(R.color.gray, null))
                binding.pcButtonFailed.setTextColor(it.resources.getColor(R.color.gray, null))
                binding.pcButtonAll.setTextColor(it.resources.getColor(R.color.gray, null))
            }
        }

    }

    private fun buttonSelection(buttonOption: ButtonOption) {

        val listResult = when(buttonOption) {

            ButtonOption.ALL ->     { global_resultList }
            ButtonOption.PASSED ->  { global_resultList.filter { it.result } }
            ButtonOption.FAILED ->  { global_resultList.filter { !it.result } }

        }
        listResult.forEach { it.isExpanded = false }

        val button = when(buttonOption) {

            ButtonOption.ALL ->     { binding.pcButtonAll }
            ButtonOption.PASSED ->  { binding.pcButtonSuccess }
            ButtonOption.FAILED ->  { binding.pcButtonFailed }

        }

        when(buttonOption) {

            ButtonOption.ALL ->     {
                deselectButton(binding.pcButtonSuccess)
                deselectButton(binding.pcButtonFailed)
            }
            ButtonOption.PASSED ->  {
                deselectButton(binding.pcButtonAll)
                deselectButton(binding.pcButtonFailed)
            }
            ButtonOption.FAILED ->  {
                deselectButton(binding.pcButtonAll)
                deselectButton(binding.pcButtonSuccess)
            }

        }

        highlightButton(button)
        clearRvList()
        addListToRvList(listResult)

    }

    private fun resetEndpointStats() {
        portCheckerViewModel.setTotal(0)
        portCheckerViewModel.setFailed(0)
        portCheckerViewModel.setSuccess(0)
    }

    private fun clearRvList() {
        if (recyclerViewResultItems.isEmpty() ) return
        val size = recyclerViewResultItems.size
        recyclerViewResultItems.clear()
        portCheckRecyclerViewAdapter.notifyItemRangeRemoved(0, size)

    }

    private fun addListToRvList(list: List<EndpointParent>) {
        recyclerViewResultItems.addAll(list)
        portCheckRecyclerViewAdapter.notifyItemRangeInserted(0, list.size)
    }
    private fun addItemToRvList(item: EndpointParent) {
        recyclerViewResultItems.add(item)
        portCheckRecyclerViewAdapter.notifyItemInserted(recyclerViewResultItems.size)
    }

    private suspend fun processPing(endpointParent: EndpointParent): EndpointParent {

        val endpoint = EndpointParent(endpointParent.endpointName, endpoints = endpointParent.endpoints)

        endpoint.endpoints.forEach {
            val result = NetworkOperations().pingRequest(it.targetHostName)
            if(!result.result) endpoint.result = false
            it.result = result.result
            it.targetHostName = result.destination
            it.statusColor = if (result.result) R.color.status_good else R.color.button_red
        }

        endpoint.endpointResult = if (!endpoint.result) R.drawable.room_grade_alert else R.drawable.room_grade_pass
        if (endpoint.result) Log.d("PortCheckerFragment", "True: ${endpoint.endpointName}")
        activity?.runOnUiThread {
            if (endpoint.result) portCheckerViewModel.addSuccessTotal()
            else portCheckerViewModel.addFailedTotal()
        }
        return endpoint
    }
    private suspend fun processGet(endpointParent: EndpointParent): EndpointParent {

        val endpoint = EndpointParent(endpointParent.endpointName, endpoints = endpointParent.endpoints)

        endpoint.endpoints.forEach {
            val result = NetworkOperations().httpRequest(it.targetHostName)
            if (!result.result) endpoint.result = false
            it.result = result.result
            it.targetHostName = result.destination
            it.statusColor = if (result.result) R.color.status_good else R.color.button_red
        }

        endpoint.endpointResult = if (!endpoint.result) R.drawable.room_grade_alert else R.drawable.room_grade_pass
        if (endpoint.result) Log.d("PortCheckerFragment", "True: ${endpoint.endpointName}")
        activity?.runOnUiThread {
            if (endpoint.result) portCheckerViewModel.addSuccessTotal()
            else portCheckerViewModel.addFailedTotal()
        }
        return endpoint



    }

    private fun loadList() {
        if ( global_resultList.isEmpty() ) return
        clearRvList()
        addListToRvList(global_resultList)
        setButtonStatus(true)
        highlightButton(binding.pcButtonAll)
        portCheckerViewModel.setSuccess(global_resultList.filter { it.result }.size)
        portCheckerViewModel.setFailed(global_resultList.filter { !it.result }.size)
        portCheckerViewModel.setTotal(global_resultList.size)
    }

    enum class ButtonOption {
        ALL,
        PASSED,
        FAILED
    }


}

