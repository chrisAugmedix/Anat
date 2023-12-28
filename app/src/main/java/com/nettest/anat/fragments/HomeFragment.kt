package com.nettest.anat.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.nettest.anat.NetworkOperations
import com.nettest.anat.R
import com.nettest.anat.databinding.FragmentHomeBinding
import com.nettest.anat.global_testingMetricsFrequency
import kotlinx.coroutines.delay
import com.google.android.material.snackbar.Snackbar
import com.nettest.anat.Utility
import com.nettest.anat.global_snackBarDismissed
import com.nettest.anat.home_lteDialogShown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HomeFragment: Fragment(R.layout.fragment_home) {

    private fun <T> mutableListWithCapacity(capacity: Int): MutableList<T> = ArrayList(capacity)
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy { HomeViewModel() }
    private var lteMode: Boolean = false
    private var connectionCheckComplete: Boolean = false
    private var loopRunning: Boolean = false

    private val wifiManager by lazy { context?.getSystemService(Context.WIFI_SERVICE) as WifiManager }

    private val lteResultList = mutableListWithCapacity<Boolean>(3)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPause() {
        connectionCheckComplete = false
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        updateNetworkViews()
        startMetricLoop()
        activity?.runOnUiThread { binding.cellCarrierText.text = (context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.networkOperatorName  }

        viewModel.getRssi().observe(viewLifecycleOwner)         {
            binding.homeRssiLabel.text = it.first
            binding.homeRssiLabel.setTextColor(it.second)
        }
        viewModel.getLinkRate().observe(viewLifecycleOwner)     { binding.homeLinkRateLabel.text = it.toString() }
        viewModel.getNearbyTotal().observe(viewLifecycleOwner)  { binding.homeNearbyTotalLabel.text = it.toString() }
        viewModel.getBand().observe(viewLifecycleOwner)         {
            binding.homeCellBandLabel.text = it.first
            binding.homeCellBandLabel.setTextColor(it.second)
        }
        viewModel.getLteRssi().observe(viewLifecycleOwner)      { binding.homeLteRssiLabel.text = it.toString() }
        viewModel.getRsrq().observe(viewLifecycleOwner)         { binding.homeRsrqLabel.text = it.toString() }
        viewModel.getDgLast().observe(viewLifecycleOwner)       { binding.homeDgLastLabel.text = it.toString() }
        viewModel.getGoogleLast().observe(viewLifecycleOwner)   { binding.homeGoogleLastLabel.text = it.toString() }
        viewModel.getAugmedixLast().observe(viewLifecycleOwner) { binding.homeAugmedixLastLabel.text = it.toString() }

        binding.fixCellButton.setOnClickListener {
            showLteDialog(getLteDialog())
        }

        super.onViewCreated(view, savedInstanceState)

    }

    private fun showLteDialog(dialog: AlertDialog) {

        home_lteDialogShown = true
        dialog.setOnShowListener { lteDialog ->

            (lteDialog as AlertDialog)
            val dialogPositiveButton = lteDialog.getButton(AlertDialog.BUTTON_POSITIVE).apply { isEnabled = false }

            fun animateProgress() {
                activity?.runOnUiThread {
                    lteDialog.findViewById<LinearLayout>(R.id.cellStatsContainer)?.visibility = View.VISIBLE
                    lteDialog.findViewById<ProgressBar>(R.id.rssiStatusProgressBar)?.visibility = View.VISIBLE
                    lteDialog.findViewById<ProgressBar>(R.id.rsrqStatusProgressBar)?.visibility = View.VISIBLE
                }
                CoroutineScope(Dispatchers.Default).launch {
                    delay(7000)
                    val cellInfoLte = NetworkOperations().getLteStats(requireContext())
                    activity?.runOnUiThread {
                        lteDialog.findViewById<ProgressBar>(R.id.rssiStatusProgressBar)?.visibility = View.GONE
                        lteDialog.findViewById<ProgressBar>(R.id.rsrqStatusProgressBar)?.visibility = View.GONE
                        lteDialog.findViewById<ImageView>(R.id.rssiDialogResultImage)?.visibility = View.VISIBLE
                        lteDialog.findViewById<ImageView>(R.id.rsrqDialogResultImage)?.visibility = View.VISIBLE
                        dialogPositiveButton.isEnabled = true
                        if (cellInfoLte != null) {
                            lteDialog.findViewById<ImageView>(R.id.rssiDialogResultImage)?.setImageResource(R.drawable.room_grade_pass)
                            lteDialog.findViewById<ImageView>(R.id.rsrqDialogResultImage)?.setImageResource(R.drawable.room_grade_pass)
                        }
                    }
                }
            }



            var dataToggleState: Boolean = false
            var airplaneToggleState: Boolean = false

            lteDialog.findViewById<ImageButton>(R.id.homeDataUsageButton)?.apply { setOnClickListener {
                dataToggleState = true
                startActivity(Intent(Settings.ACTION_DATA_USAGE_SETTINGS))
                this.setImageResource(R.drawable.checkmark)
                this.setColorFilter(Color.WHITE)
                this.background.setTint(resources.getColor(R.color.line_green, null))
                if (airplaneToggleState) {
                    animateProgress()
                }
            } }

            lteDialog.findViewById<ImageButton>(R.id.homeAirplaneButton)?.apply { setOnClickListener {
                airplaneToggleState = true
                startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
                this.setImageResource(R.drawable.checkmark)
                this.setColorFilter(Color.WHITE)
                this.background.setTint(resources.getColor(R.color.line_green, null))
                if (dataToggleState) {
                    animateProgress()
                }
            } }

        }

        dialog.show()
    }

    private fun getLteDialog(): AlertDialog {

        val layoutInflater = LayoutInflater.from(requireContext())
        val fixLteView: View = layoutInflater.inflate(R.layout.home_fix_lte_dialog, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("No Cell Data")
        builder.setView(fixLteView)

        val dataToggleButton = fixLteView.findViewById<ImageButton>(R.id.homeDataUsageButton).apply {
            startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.shake))
        }

        val airplaneModeToggleButton = fixLteView.findViewById<ImageButton>(R.id.homeAirplaneButton).apply {
            startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.shake))
        }

        builder.setPositiveButton("Done") { di, _ ->
            airplaneModeToggleButton.background.setTint(resources.getColor(R.color.light_gray, null))
            dataToggleButton.background.setTint(resources.getColor(R.color.light_gray, null))
            di.dismiss()
        }

        return builder.create()

    }

    @SuppressLint("MissingPermission")
    private fun startMetricLoop() {

        if (loopRunning) return
        viewLifecycleOwner.lifecycleScope.launch {

            loopRunning = true
            while (true) {

                val cellInfoLte = NetworkOperations().getLteStats(requireContext())
                setLteData(cellInfoLte)

                async { pingConnectionEndpoints() }.start()
                NetworkOperations().getWifiStats(requireContext()) { wifiInfo ->
                    wifiInfo?.let {wf ->
                        val totalNearbyAps = wifiManager.scanResults?.filterNotNull()?.count { it.BSSID != wf.bssid } ?: 0
                        viewModel.setRssi(wf.rssi)
                        viewModel.setLinkRate(wf.linkSpeed)
                        viewModel.setNearbyTotal(totalNearbyAps)
                    }
                }


                delay(3000L)
            }
        }
    }

    private fun showLteFixMessage(list: MutableList<Boolean>): Boolean {
        if ( list.size < 3 ) return false
        if ( list.size == 3 ) return true //TODO: Delete this line once completed testing
        if ( list.all { !it } ) return true
        return false
    }
    private fun setLteData(cellInfoLte: CellInfoLte?) {
        lteResultList.add(cellInfoLte == null)
        val cellband = Utility.getCellBand(cellInfoLte?.cellIdentity?.earfcn ?: -1)
        activity?.runOnUiThread {
            if (!home_lteDialogShown && showLteFixMessage(lteResultList)) { binding.fixCellContainer.visibility = View.VISIBLE }
            else { binding.fixCellContainer.visibility = View.GONE }
            viewModel.setLteRssi(cellInfoLte?.cellSignalStrength?.rssi ?: -1)
            viewModel.setRsrq(cellInfoLte?.cellSignalStrength?.rsrq ?: -1)
            viewModel.setBand(cellband)
        }
    }

    private fun updateNetworkViews() {

        val cellInfo: CellInfoLte? = NetworkOperations().getLteStats(requireContext())
        val lteOperator = cellInfo?.cellIdentity?.operatorAlphaLong
        activity?.runOnUiThread { binding.cellCarrierText.text = lteOperator }

        NetworkOperations().getWifiStats(requireContext()) { wifiInfo ->
            Log.d("MainActivity", wifiInfo.toString())
            wifiInfo?.let {
                if (it.ssid == WifiManager.UNKNOWN_SSID) setConnectToWifiView()
                hideConnectToWifiView()
                activity?.runOnUiThread {
                    binding.homeConnectedSsid.text = it.ssid.replace("\"", "")
                }
                return@getWifiStats
            }
            setConnectToWifiView()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setConnectToWifiView() {

        if (connectionCheckComplete) return
        connectionCheckComplete = true

        val cellInfoLte = NetworkOperations().getLteStats(requireContext())
        val isConnectedToCell = (cellInfoLte?.cellConnectionStatus == CellInfo.CONNECTION_PRIMARY_SERVING || cellInfoLte?.cellConnectionStatus == CellInfo.CONNECTION_SECONDARY_SERVING)

        let lteService@{

            if (!isConnectedToCell) return@lteService
            val dataState = (context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.dataState
            if (dataState != TelephonyManager.DATA_CONNECTED) return@lteService

            val snackBar = Snackbar.make(requireActivity().findViewById(R.id.myCoordinatorLayout), "Running App in LTE Mode", Snackbar.LENGTH_INDEFINITE)

            snackBar.setAction("Dismiss") { snackBar.dismiss() }

            activity?.runOnUiThread {
                binding.homeNetworkType.text = "LTE/Cell"
                binding.homeConnectedSsid.text = "Tap Here To Connect To Network"
                binding.homeConnectedSsid.setOnClickListener { startActivity(Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)) }
                if (!global_snackBarDismissed) {
                    global_snackBarDismissed = true
                    snackBar.show()
                }
                lteMode = true
            }

            connectionCheckComplete = false
            return

        }

        activity?.let {
            it.runOnUiThread {
                //hide view
                binding.homeNetworkTypeContainer.visibility = View.GONE
                binding.homeServerConnectionContainer.visibility = View.GONE
                //show new view
                binding.homeConnectedSsid.text = resources.getString(R.string.connect_to_wifi)
                binding.homeConnectedSsid.setOnClickListener {
                    startActivity(Intent(WifiManager.ACTION_PICK_WIFI_NETWORK))
                }
            }
        }

        connectionCheckComplete = false
    }

    private fun hideConnectToWifiView() {
        activity?.let {
            it.runOnUiThread {
                binding.homeNetworkTypeContainer.visibility = View.VISIBLE
                binding.homeServerConnectionContainer.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun pingConnectionEndpoints() {

        val pingList = mutableListOf("www.google.com", "mcu4.augmedix.com")

        pingList.forEach {

            val res = NetworkOperations().pingRequest(it)
            if (res.destination == pingList[0]) {
                activity?.runOnUiThread { viewModel.setGoogleLast(res.duration) }
                return@forEach
            }
            activity?.runOnUiThread { viewModel.setAugmedixLast(res.duration) }

        }

        val dg: String = NetworkOperations().getDefaultGateway(requireContext()) ?: return
        val dgRes = NetworkOperations().pingRequest(dg)
        activity?.runOnUiThread { viewModel.setDefaultGatewayLast(dgRes.duration) }

    }

    override fun onResume() {
        Log.d("HomeActivity", "onResume ran")
        updateNetworkViews()
        if (!loopRunning) startMetricLoop()
        super.onResume()
    }

}

class HomeViewModel: ViewModel() {

    private val rssi = MutableLiveData<Pair<String, Int>>()
    private val linkRate = MutableLiveData<String>()
    private val nearbyTotal = MutableLiveData<String>()
    private val band = MutableLiveData<Pair<String, Int>>()
    private val lteRssi = MutableLiveData<String>()
    private val rsrq = MutableLiveData<String>()
    private val dgLast = MutableLiveData<String>()
    private val googleLast = MutableLiveData<String>()
    private val augmedixLast = MutableLiveData<String>()

    fun setRssi(strength: Int) {
        val color = if (strength > -65) Color.parseColor("#228b22") else Color.parseColor("#E4D00A")
        val res = if (strength == -1) "N/A" else "$strength dBm"
        rssi.value = Pair(res, color)
    }
    fun setLinkRate(link: Int) { linkRate.value = "$link Mbps" }
    fun setNearbyTotal(total: Int) { nearbyTotal.value = total.toString() }
    fun setBand(cellband: Int) {
        val color = if (cellband == 5 || cellband == 13) Color.RED else if (cellband == 66 || cellband == 4) Color.parseColor("#228b22") else Color.GRAY
        val res = if (cellband == -1) "N/A" else "$cellband"
        band.value = Pair(res, color)
    }
    fun setLteRssi(lteStrength: Int) {
        val res = if (lteStrength == -1) "N/A" else "$lteStrength dBm"
        lteRssi.value = res
    }
    fun setRsrq(strength: Int) {
        val res = if (strength == -1) "N/A" else "$strength dBm"
        rsrq.value = res
    }
    fun setDefaultGatewayLast(strength: Int) { dgLast.value = "$strength ms" }
    fun setGoogleLast(strength: Int) { googleLast.value = "$strength ms" }
    fun setAugmedixLast(strength: Int) { augmedixLast.value = "$strength ms" }

    fun getRssi() = rssi
    fun getLinkRate() = linkRate
    fun getNearbyTotal() = nearbyTotal
    fun getBand() = band
    fun getLteRssi() = lteRssi
    fun getRsrq() = rsrq
    fun getDgLast() = dgLast
    fun getGoogleLast() = googleLast
    fun getAugmedixLast() = augmedixLast


}