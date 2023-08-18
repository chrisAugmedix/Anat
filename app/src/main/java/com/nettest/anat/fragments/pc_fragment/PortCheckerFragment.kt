package com.nettest.anat.fragments.pc_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentPortCheckerBinding
import com.nettest.anat.global_completedPortChecker

class PortCheckerFragment: Fragment(R.layout.fragment_port_checker) {

    private var _binding: FragmentPortCheckerBinding? = null
    private val binding get() = _binding!!
    private val resultList: MutableList<EndpointParent> = mutableListOf()
    private var portCheckerViewModel: PortCheckerViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentPortCheckerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        val pep = EndpointParent("TestEndpointNameWithChild")
        pep.endpoints.add(EndpointChild("ChildHostname1"))
        pep.endpoints.add(EndpointChild("ChildHostname2"))
        pep.endpoints.add(EndpointChild("ReallyLongEndpointForSomeReasonWecantshortenIt"))

        resultList.add(EndpointParent("TestEndpointName1"))
        resultList.add(EndpointParent("TestEndpointName2"))
        resultList.add(EndpointParent("TestEndpointName3"))
        resultList.add(pep)

        val rv: RecyclerView = binding.pcRecyclerView
        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = PortCheckerRecyclerViewAdapter(resultList, requireContext())
        rv.adapter = adapter

        portCheckerViewModel = this.run {ViewModelProvider(this)[PortCheckerViewModel::class.java]}
        portCheckerViewModel?.getList()?.observe(viewLifecycleOwner) { results ->
            adapter.notifyDataSetChanged()
        }

        binding.pcButtonRun.setOnClickListener {

            binding.pcButtonRun.showLoading()
            global_completedPortChecker = true

        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun updateStats() {

    }

    override fun onResume() {

        //TODO(Add in a check when user returns back)
        super.onResume()

    }


}