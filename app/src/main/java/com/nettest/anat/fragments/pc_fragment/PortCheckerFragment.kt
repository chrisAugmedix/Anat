package com.nettest.anat.fragments.pc_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentPortCheckerBinding
import com.nettest.anat.databinding.FragmentStatsChildLteBinding

class PortCheckerFragment: Fragment(R.layout.fragment_port_checker) {

    private var _binding: FragmentPortCheckerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentPortCheckerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {

        //TODO(Add in a check when user returns back)
        super.onResume()

    }


}