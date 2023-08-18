package com.nettest.anat.fragments.testing_fragment

import android.util.Log
import com.nettest.anat.R
import com.nettest.anat.Utility

class TestingCellModel {

    val rsrpValueList: MutableList<Int> = mutableListOf()
    val rsrqValueList: MutableList<Int> = mutableListOf()
    private val connectedBandList: MutableList<Int> = mutableListOf()

    var grade: MetricGrade = MetricGrade.GOOD
    var gradeImage: Int = R.drawable.status_good

    fun addMetrics(rsrp: Int, rsrq: Int, band: Int) {
        rsrpValueList.add(rsrp)
        rsrqValueList.add(rsrq)
        connectedBandList.add(band)
        updateGrade()
    }

    private fun updateGrade() {

        //Grade based on average values
        var subBand: Boolean = false

        when(Utility.mostCommonInList(connectedBandList)) {
            5, 13 -> { reduceGrade(autoFail = true) }
            2 -> { subBand = true }
            else -> { raiseGrade() }
        }

        if ( (rsrpValueList.average() <= -106 || rsrqValueList.average() <= -8) && subBand ) reduceGrade()

    }

    private fun reduceGrade(autoFail: Boolean = false) {

        Log.d("Result", "reduceGrade: ")
        if (autoFail) {
            grade = MetricGrade.FAILED
            gradeImage = R.drawable.status_failed
            return
        }

        when(grade) {
            MetricGrade.GOOD -> {
                grade = MetricGrade.ALERT
                gradeImage = R.drawable.status_alert
            }
            MetricGrade.ALERT -> {
                grade = MetricGrade.FAILED
                gradeImage = R.drawable.status_alert
            }
            MetricGrade.FAILED -> {}
        }

    }

    private fun raiseGrade() {

        Log.d("Result", "raiseGrade: ")
        when (grade) {
            MetricGrade.GOOD -> {}
            MetricGrade.ALERT -> {
                grade = MetricGrade.GOOD
                gradeImage = R.drawable.status_good
            }
            MetricGrade.FAILED -> {
                grade = MetricGrade.ALERT
                gradeImage = R.drawable.status_alert
            }
        }

    }

}

enum class MetricGrade {
    GOOD,
    ALERT,
    FAILED
}