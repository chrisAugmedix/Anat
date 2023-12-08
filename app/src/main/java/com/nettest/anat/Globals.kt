package com.nettest.anat


import android.util.Log
import com.nettest.anat.fragments.pc_fragment.EndpointParent
import com.nettest.anat.fragments.pc_fragment.PingResult
import java.math.BigDecimal
import kotlin.random.Random

enum class RoomResult(val color: String) {
    GOOD("#228b22"),
    ALERT("#f0e130")
}

//Application Globals
var global_completedPortChecker: Boolean = false
var global_isPortCheckerRunning: Boolean = false

//TESTING ACTIVITY GLOBALS
var global_testingState = false //For BroadcastReceiver Activity -- required to ensure the back button is disabled & screen auto-turns on when lock button is pressed.

//Changeable in app settings
var global_testingRoomTimeLimit: Int = 45
var global_testingMetricsFrequency = 5
var global_portCheckRequired: Boolean = false

fun generateRandomId(): Int = Random.nextInt(1000, 100000)

//PORT CHECKER GLOBALS
var global_resultList = mutableListOf<EndpointParent>()
val global_sessionDataList = mutableListOf<SessionData>()


val poorWifiChannels = arrayOf(1, 6, 11, 36, 40, 44, 48, 149, 153, 157, 161, 165)

var pingHostnameList = mutableListOf( "www.google.com", "mcu6.augmedix.com" )

data class SessionData  (var sessionName: String = "", var roomDataList: MutableList<RoomData> = mutableListOf(),
                         var metricDataList: MutableList<MetricData> = mutableListOf(), var ssid: String? = null,
                         var sessionId: Int = generateRandomId(),
                         var startEpoch: Long = System.currentTimeMillis(), var endEpoch: Long = System.currentTimeMillis())
data class MetricData   ( val wifiMetrics: WifiMetrics, val cellMetrics: CellMetrics, val connectivityMetrics: ConnectivityMetrics, val timestamp: Long )
data class WifiMetrics  (val ip: String, val rssi: Int, val linkRateGeneral: Int, val bssid: String, val channel: Int, val neighborList: MutableList<NeighborData>?,
                         val band: Double, val linkRateTx: Int, val linkRateRx: Int )
data class NeighborData ( val rssi: Int, val bssid: String, val channel: Int )
data class CellMetrics  (var rssi: Int? = null, var rsrp: Int? = null, var rsrq: Int? = null, var band: Int? = null, var earfcn: Int? = null, var pci: Int? = null )
data class ConnectivityMetrics ( val pingResultList: MutableList<PingResult> )



class RoomGrade {

    private var image: Int = R.drawable.room_grade_pass
    private var gradeValue = 3

    fun getGrade() = image
    fun failGrade() {
        image = R.drawable.room_grade_fail
        gradeValue = 1
    }
    fun raiseGrade() {

        if (gradeValue == 3) return
        if (gradeValue == 2) R.drawable.room_grade_pass
        else R.drawable.room_grade_alert
        gradeValue++

    }

    fun lowerGrade() {
        if (gradeValue == 1) return
        if (gradeValue == 3) R.drawable.room_grade_alert
        else R.drawable.room_grade_fail
        gradeValue--
    }

    fun setUnknownGrade() { image = R.drawable.room_grade_unknown }

}
class RoomData {

    private val roomId: Int = generateRandomId()
    private var roomName: String? = null
    private val metricDataList: MutableList<MetricData> = mutableListOf()
    private var cellGrade: RoomGrade = RoomGrade()
    private var wifiGrade: RoomGrade = RoomGrade()
    private var dlSpeedTestResult: BigDecimal? = null
    private var totalSeconds: Int = 0
    private var start: Long = 0
    private var end: Long = 0

    override fun equals(other: Any?): Boolean {
        return if (other is RoomData) other.roomId == this.roomId
        else false
    }

    fun addMetrics(metrics: MetricData) { metricDataList.add(metrics) }
    fun updateRoomName(name: String) { roomName = name }
    fun finalizeRoom() { updateGrade() }
    fun setSpeedTestResult(result: BigDecimal?) { dlSpeedTestResult = result }

    fun updateStartTime() { start = System.currentTimeMillis() }
    fun updateEndTime() { end = System.currentTimeMillis() }
    fun addSecond() { totalSeconds++ }
    fun getRoomName() = roomName
    fun getCellGrade() = cellGrade
    fun getWifiGrade() = wifiGrade
    fun getMetricData() = metricDataList
    fun getRoomSeconds() = totalSeconds

    fun getSpeedTestResult() = dlSpeedTestResult ?: (0).toBigDecimal()
    fun getStart() = start
    fun getEnd() = end

    private fun updateGrade() {


        val cellbandList    = metricDataList.map { it.cellMetrics }.mapNotNull { it.band }.toMutableList()
        val wifiBandList    = metricDataList.map { it.wifiMetrics }.map { it.band }.toMutableList()

        //LTE
        let {

            if ( cellbandList.size < 5 ) {
                cellGrade.setUnknownGrade()
                return@let
            }

            val rsrpList        = metricDataList.map { it.cellMetrics }.mapNotNull { it.rsrp }.toMutableList()
            val cellRssiList    = metricDataList.map { it.cellMetrics }.mapNotNull { it.rssi }.toMutableList()
            val rsrqList        = metricDataList.map { it.cellMetrics }.mapNotNull { it.rsrq }.toMutableList()

            when ( Utility.mostCommonInList(cellbandList) ) {
                66, 4 -> { cellGrade.raiseGrade() }
                5, 13 -> { cellGrade.lowerGrade() }
            }

            when ( cellRssiList.average().toInt() ) {
                in 0 downTo -73 -> { cellGrade.raiseGrade() }
                in -73 downTo -78 -> { }
                else -> { cellGrade.lowerGrade() }
            }

            when ( rsrqList.average().toInt() ) {
                in 30 downTo -7 -> { cellGrade.raiseGrade() }
                in -7 downTo -12 -> { }
                else -> { cellGrade.lowerGrade() }
            }

            when ( rsrpList.average().toInt() ) {
                in 0 downTo -73 -> { cellGrade.raiseGrade() }
                in -73 downTo -78 -> { }
                else -> { cellGrade.lowerGrade() }
            }

        }

        //Wifi
        let {

            if ( wifiBandList.size < 5 ) {
                wifiGrade.setUnknownGrade()
                return@let
            }

            val rssiList     = metricDataList.map { it.wifiMetrics }.mapNotNull { it.rssi }.toMutableList()
            val linkRateList = metricDataList.map { it.wifiMetrics }.mapNotNull { it.linkRateGeneral }.toMutableList()

            var bandHopped: Boolean = false
            wifiBandList.forEach { if (it != wifiBandList.first()) bandHopped = true }
            val channelList = metricDataList.map { it.wifiMetrics }.mapNotNull { it.channel }.toMutableList()
            if ( ( channelList.filter { it in poorWifiChannels } ).isNotEmpty() ) wifiGrade.lowerGrade()

            if (!bandHopped) {

                if ( wifiBandList.first() == 5.0 ) wifiGrade.raiseGrade()
                val goodRssiRange = if ( wifiBandList.first() == 5.0 ) 0 downTo -65 else 0 downTo -59
                val neutralRssiRange = if ( wifiBandList.first() == 5.0 ) -65 downTo -70 else -60 downTo -67
                when (rssiList.average().toInt()) {
                    in goodRssiRange -> { wifiGrade.raiseGrade() }
                    in neutralRssiRange -> {}
                    else -> { wifiGrade.lowerGrade() }
                }

                val goodLinkRateRange = if ( wifiBandList.first() == 5.0 ) 5000 downTo 100 else 5000 downTo 30
                val neutralLinkRateRange = if ( wifiBandList.first() == 5.0 ) 99 downTo 50 else 29 downTo 15
                when (linkRateList.average().toInt()) {
                    in goodLinkRateRange -> { wifiGrade.raiseGrade() }
                    in neutralLinkRateRange -> {}
                    else -> { wifiGrade.lowerGrade() }
                }


            } else {

                val goodRange = 0 downTo -59
                val neutralRange = -60 downTo -67
                when (rssiList.average().toInt()) {
                    in goodRange -> { wifiGrade.raiseGrade() }
                    in neutralRange -> {}
                    else -> { wifiGrade.lowerGrade() }
                }

                val goodLinkRateRange = 5000 downTo 100
                val neutralLinkRateRange = 99 downTo 50
                when (linkRateList.average().toInt()) {
                    in goodLinkRateRange -> { wifiGrade.raiseGrade() }
                    in neutralLinkRateRange -> {}
                    else -> { wifiGrade.lowerGrade() }
                }
            }





        }

    }

    override fun hashCode(): Int {
        var result = roomId
        result = 31 * result + (roomName?.hashCode() ?: 0)
        result = 31 * result + metricDataList.hashCode()
        result = 31 * result + cellGrade.hashCode()
        result = 31 * result + wifiGrade.hashCode()
        result = 31 * result + (dlSpeedTestResult?.hashCode() ?: 0)
        return result
    }

}

fun testSessionData() {
    val sessionData = SessionData()
    sessionData.sessionName = "TestSessionName"
    sessionData.metricDataList = getJunkMetricData()
    global_sessionDataList.add(sessionData)
    val sessionDataTwo = SessionData()
    sessionDataTwo.sessionName = "Really Long Testing Session Name For Testing Purposes"
    sessionDataTwo.metricDataList = getJunkMetricData()
    global_sessionDataList.add(sessionDataTwo)
}

private fun getJunkWifiMetrics(): WifiMetrics {
    return WifiMetrics("junk", Random.nextInt(1, 100), Random.nextInt(1, 100), "junk", Random.nextInt(1, 100), null, Random.nextDouble(1.0, 100.0), Random.nextInt(1, 100), Random.nextInt(1, 100))
}

private fun getJunkConnectivityMetrics() :ConnectivityMetrics {
    return ConnectivityMetrics(mutableListOf())
}

private fun getRandomCellMetrics(): CellMetrics {
    return CellMetrics(Random.nextInt(1, 100), Random.nextInt(1, 100), Random.nextInt(1, 100), Random.nextInt(1, 100), Random.nextInt(1, 100), Random.nextInt(1, 100))
}

private fun getJunkMetricData(): MutableList<MetricData> {
    val currentTime = System.currentTimeMillis()
//    val currentTime = 0L
    val dataList = mutableListOf<MetricData>()
    for (x in 1..30) {
        val timestamp: Long = currentTime + x*60*1000//(1000*x)
        val cm = getRandomCellMetrics()
        Log.d("TestingXAxis", "value: ${cm.rssi}\tts: $timestamp")
        dataList.add(MetricData(getJunkWifiMetrics(), cm, getJunkConnectivityMetrics(), timestamp))
    }
    return dataList
}