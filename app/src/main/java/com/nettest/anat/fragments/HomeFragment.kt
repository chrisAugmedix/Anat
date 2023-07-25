package com.nettest.anat.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentHomeBinding
import com.nettest.anat.databinding.FragmentTestingBinding
import java.lang.Exception

class HomeFragment: Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    private var isConnectedToSsid: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Update Textview to reflect network state and whatnot
        updateNetworkTextViews()

        super.onViewCreated(view, savedInstanceState)

    }

    private fun updateNetworkTextViews() {

        val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        isConnectedToSsid = isConnectedToSSID(wifiManager)
        if (!isConnectedToSsid) { binding.homeConnectedSsidDynamic.setTextColor(Color.parseColor("#ff8080")) }
        else {
            binding.homeConnectedSsidDynamic.text = getSSIDName(wifiManager)
            binding.homeConnectedSsidDynamic.setTextColor(resources.getColor(R.color.gray))
        }

        binding.homeConnectedSsidDynamic.setOnClickListener {
            if (!isConnectedToSsid) startActivity(Intent(WifiManager.ACTION_PICK_WIFI_NETWORK))
        }
    }

    private fun isConnectedToSSID(wm: WifiManager?): Boolean {

        if (wm == null) return false
        return try {
            val ssid = wm.connectionInfo.ssid
            if (ssid == "<unknown ssid>") return false
            true
        } catch (e: Exception) {
            false
        }

    }

    private fun getSSIDName(wm: WifiManager?): String {

        if (wm == null) return "N/A"
        return wm.connectionInfo.ssid.replace("\"", "")

    }

    override fun onResume() {
        Log.d("HomeActivity", "onResume ran")
        updateNetworkTextViews()
        super.onResume()
    }


}