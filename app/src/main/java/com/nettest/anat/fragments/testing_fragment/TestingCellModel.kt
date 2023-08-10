package com.nettest.anat.fragments.testing_fragment

import com.nettest.anat.R
import com.nettest.anat.Utility

class TestingCellModel {

    val rsrpValueList: MutableList<Int> = mutableListOf()
    val rsrqValueList: MutableList<Int> = mutableListOf()
    private val connectedBandList: MutableList<Int> = mutableListOf()

    var grade: MetricGrade = MetricGrade.GOOD
    var gradeColor: String = ""
    var gradeImage: Int = 0

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

        if ( (rsrpValueList.average() < -106 || rsrqValueList.average() < -8) && subBand ) reduceGrade()

    }

    private fun reduceGrade(autoFail: Boolean = false) {
        if (autoFail) {

            grade = MetricGrade.FAILED
            gradeColor = "#228b22"
            gradeImage = R.drawable.status_failed
            return
        }

        if (grade == MetricGrade.GOOD) {
            MetricGrade.ALERT
            gradeColor = "#f0e130"
            gradeImage = R.drawable.status_alert
        }
        else {
            MetricGrade.FAILED
            gradeColor = "#FE0000"
            gradeImage = R.drawable.status_failed
        }
    }

    private fun raiseGrade() {
        if (grade == MetricGrade.FAILED) {
            grade = MetricGrade.ALERT
            gradeColor = "#f0e130"
            gradeImage = R.drawable.status_alert
        }
        else {
            grade = MetricGrade.GOOD
            gradeColor = "#228b22"
            gradeImage = R.drawable.status_good
        }
    }






}

enum class MetricGrade() {
    GOOD,
    ALERT,
    FAILED
}