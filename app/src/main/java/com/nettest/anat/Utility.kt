package com.nettest.anat

object Utility {

    fun getTimeFormat(seconds: Int): Pair<Int, Int> {

        val minutes = seconds / 60
        val sec = seconds % 60
        return Pair(minutes, sec)

    }

}