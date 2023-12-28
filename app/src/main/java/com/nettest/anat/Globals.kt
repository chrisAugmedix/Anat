package com.nettest.anat

import com.nettest.anat.fragments.pc_fragment.EndpointParent
import com.nettest.anat.fragments.pc_fragment.PingResult
import java.math.BigDecimal
import kotlin.random.Random

enum class ConnectionInterface {
    CELLULAR, WIFI, OFFLINE, ETHERNET
}
//Application Globals
var global_completedPortChecker: Boolean = false
var global_isPortCheckerRunning: Boolean = false

//Home Fragment Global
var global_snackBarDismissed: Boolean = false
var home_lteDialogShown: Boolean = false

//TESTING ACTIVITY GLOBALS
var global_testingState = false //For BroadcastReceiver Activity -- required to ensure the back button is disabled & screen auto-turns on when lock button is pressed.

//Changeable in app settings
var global_testingRoomTimeLimit: Int = 15
var global_testingMetricsFrequency = 5
var global_portCheckRequired: Boolean = false

fun generateRandomId(): Int = Random.nextInt(1000, 100000)

//PORT CHECKER GLOBALS
var global_resultList = mutableListOf<EndpointParent>()
val global_sessionDataList = mutableListOf<SessionData>()
var global_continuePortCheckFlag: Boolean = false

val poorWifiChannels = arrayOf(1, 6, 11, 36, 40, 44, 48, 149, 153, 157, 161, 165)
var pingHostnameList = mutableListOf( "www.google.com", "mcu6.augmedix.com" )

data class SessionData  (val sessionName: String, val sessionId: Int = generateRandomId(),
                         val roomList: MutableList<RoomData> = mutableListOf(), var metricDataList: MutableList<MetricData> = mutableListOf())
data class MetricData   ( var wifiMetrics: WifiMetrics? = null, var cellMetrics: CellMetrics? = null,
                          var connectivityMetrics: ConnectivityMetrics? = null, val timestamp: Long )
data class WifiMetrics  (val ip: String, val ssid: String, val bssid: String,
                         val rssi: Int, val linkRateGeneral: Int, val channel: Int,
                         val band: Double, val linkRateTx: Int, val linkRateRx: Int )
data class CellMetrics  (var rssi: Int, var rsrp: Int, var rsrq: Int, var band: Int, var earfcn: Int, var pci: Int )
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
class RoomData( private var sessionName: String, private val sessionId: Int ) {

    private val roomId: Int = generateRandomId()
    private var roomName: String? = null
    private val metricDataList: MutableList<MetricData> = mutableListOf()
    private var cellGrade: RoomGrade = RoomGrade()
    private var wifiGrade: RoomGrade = RoomGrade()
    private var uploadStatus: Boolean = false
    private var dlSpeedTestResult: BigDecimal? = null
    private var totalSeconds: Int = 0
    private var start: Long = 0
    private var end: Long = 0

    override fun equals(other: Any?): Boolean {
        return if (other is RoomData) other.roomId == this.roomId
        else false
    }
    fun getSessionName() = sessionName
    fun getSessionId() = sessionId

    fun addMetrics(metrics: MetricData) { metricDataList.add(metrics) }
    fun updateRoomName(name: String) { roomName = name }
    fun finalizeRoom() { updateGrade() }
    fun setSpeedTestResult(result: BigDecimal?) { dlSpeedTestResult = result }
    fun setUploadStatus(status: Boolean) { uploadStatus = status }
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


        //LTE
        let {

            val cellData = metricDataList.mapNotNull { it.cellMetrics }

            if ( cellData.size < 5 ) {
                cellGrade.setUnknownGrade()
                return@let
            }

            val rsrpList        = cellData.map { it.rsrp }.toMutableList()
            val cellRssiList    = cellData.map { it.rssi }.toMutableList()
            val rsrqList        = cellData.map { it.rsrq }.toMutableList()

            when ( Utility.mostCommonInList( cellData.map { it.band }.toMutableList() ) ) {
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

            val wifiMetrics    = metricDataList.mapNotNull { it.wifiMetrics }

            if ( wifiMetrics.size < 5 ) {
                wifiGrade.setUnknownGrade()
                return@let
            }

            val rssiList     = wifiMetrics.map { it.rssi }.toMutableList()
            val linkRateList = wifiMetrics.map { it.linkRateGeneral }.toMutableList()


            val bandList = wifiMetrics.map { it.band }
            val hopped = ( !bandList.all { it == bandList.first() } )
            if ( !bandList.all { it == bandList.first() } ) wifiGrade.lowerGrade()
            if ( wifiMetrics.map { it.channel }.any { it in poorWifiChannels } ) wifiGrade.lowerGrade()

            var goodRssiRange = 0 downTo -59
            var neutralRssiRange = -60 downTo -65
            var goodRateRange = 5000 downTo 30
            var neutralLinkRateRange = 30 downTo 15

            let noHop@{
                if ( hopped ) return@noHop
                if ( bandList.first() == 2.4 ) return@noHop
                goodRssiRange = 0 downTo -65
                neutralRssiRange = -65 downTo -70
                goodRateRange = 5000 downTo 100
                neutralLinkRateRange = 99 downTo 50
            }

            if ( rssiList.average().toInt() in goodRssiRange ) wifiGrade.raiseGrade()
            else if ( ! neutralRssiRange.contains(rssiList.average().toInt()) ) wifiGrade.lowerGrade()

            if ( linkRateList.average().toInt() in goodRateRange ) wifiGrade.raiseGrade()
            else if ( ! neutralLinkRateRange.contains(linkRateList.average().toInt()) ) wifiGrade.lowerGrade()

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