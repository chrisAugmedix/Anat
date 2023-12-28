package com.nettest.anat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.nettest.anat.fragments.pc_fragment.GetResult
import com.nettest.anat.fragments.pc_fragment.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

class NetworkOperations {

    private val port = 1201
    private val protocol = "https"
    private val hostname = "grafana.augmedix.com"
    private val fullHostName = "${protocol}://${hostname}:${port}"

    private val httpsClient by lazy {
        OkHttpClient.Builder().connectTimeout(10000, TimeUnit.MILLISECONDS).writeTimeout(10000, TimeUnit.MILLISECONDS).build()
    }

    suspend fun uploadRoomData(roomData: RoomData) {

        val roomName = roomData.getRoomName()





        roomData.setUploadStatus(true)
    }

    suspend fun pingRequest(address: String): PingResult = withContext(Dispatchers.IO) {

        val runtime = Runtime.getRuntime()
        var duration = "0"

        try {
            val command = runtime.exec("/system/bin/ping -W 1 -w 1 -c 1  $address")
            val response = command.waitFor()
            val output = BufferedReader(InputStreamReader(command.inputStream))

            val result = (response == 0)
            var count = 0

            output.forEachLine {
                if (count == 1) { duration = getPingTime(it) }
                count++
            }
            return@withContext PingResult(address, duration.toInt(), result)
        } catch (E: Exception) {
            return@withContext PingResult("$address (-1)", -1, false)
        }

    }

    suspend fun httpRequest(address: String): GetResult = withContext(Dispatchers.IO) {

        val request = Request.Builder().url(address).build()
        try {
            httpsClient.newCall(request).execute().use { response ->
                if (address == "https://augmedix.jamfcloud.com/" && response.networkResponse?.code == 401) return@withContext GetResult(address, 200, true)
                else return@withContext GetResult(address, response.networkResponse?.code ?: -1, response.isSuccessful)
            }
        } catch(e: Exception) {
            return@withContext GetResult(address, -1, false)
        }

    }

    private fun getPingTime(line: String): String {

        val timePatternRx = "(?<=time=)[0-9]?[0-9]?[0-9]?[0-9]?[0-9]".toRegex()
        val timeMatch = timePatternRx.find(line) ?: return "-1"
        return timeMatch.value

    }

    fun getWifiStats(context: Context?, callback: (WifiInfo?) -> Unit) {

        Log.d("HomeFragmentCheck", "Running ${context == null}")
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? ?: return
        val networkRequest = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val callbackExecutor = Handler(Looper.getMainLooper())
        val networkCallback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.d("HomeFragmentCheck", networkCapabilities.toString())
                val wifiInfo = networkCapabilities.transportInfo as WifiInfo? ?: return
                callback(wifiInfo)
            }

            override fun onUnavailable() {
                Log.d("HomeFragmentCheck", "Unavailable Network")
                callback(null)
                super.onUnavailable()
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback, callbackExecutor, 5)
    }

    fun getLteStats(context: Context?): CellInfoLte? {
        if (context == null) return null
        if ( ActivityCompat.checkSelfPermission( context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) { return null }
        return (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.allCellInfo?.firstOrNull { info -> (info is CellInfoLte) && (info.isRegistered)} as CellInfoLte?
    }

    fun getDefaultGateway(context: Context?): String? {
        if (context == null) return null
        val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        return linkProperties?.dhcpServerAddress.toString().replace("/", "")
    }

    fun getIpAddress(context: Context?): String? {
        if (context == null) return null
        val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        val ipAddress = linkProperties?.linkAddresses?.find { it.address is Inet4Address }
        return ipAddress?.address?.hostAddress
    }
    fun getActiveConnectionInterface(context: Context?): ConnectionInterface {

        val cm: ConnectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? ?: return ConnectionInterface.OFFLINE
        val network = cm.activeNetwork
        val activeNetwork = cm.getNetworkCapabilities(network) ?: return ConnectionInterface.OFFLINE

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionInterface.WIFI
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionInterface.CELLULAR
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionInterface.ETHERNET
            else -> ConnectionInterface.OFFLINE
        }
    }

}