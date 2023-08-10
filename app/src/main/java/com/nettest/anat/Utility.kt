package com.nettest.anat

object Utility {

    fun getTimeFormat(seconds: Int): Pair<Int, Int> {

        val minutes = seconds / 60
        val sec = seconds % 60
        return Pair(minutes, sec)

    }

    fun getCellBand(earfcn: Int): Int {
        return when (earfcn) {
            in 0..599       -> 1
            in 600..1199    -> 2
            in 1200..1949   -> 3
            in 1950..2399   -> 4
            in 2400..2649   -> 5
            in 3450..3799   -> 8
            in 4150..4749   -> 10
            in 5010..5179   -> 12
            in 5180..5279   -> 13
            in 5280..5379   -> 14
            in 5730..5849   -> 17
            in 7700..8039   -> 24
            in 8040..8689   -> 25
            in 8690..9039   -> 26
            in 9040..9209   -> 27
            in 9660..9769   -> 29
            in 9770..9869   -> 30
            in 9870..9919   -> 31
            in 36350..36949 -> 35
            in 36950..37549 -> 36
            in 37550..37749 -> 37
            in 39650..41589 -> 41
            in 54540..55239 -> 47
            in 65536..66435 -> 65
            in 66436..67335 -> 66
            in 68336..68585 -> 70
            in 68586..68935 -> 71
            in 69036..69465 -> 74
            in 70366..70545 -> 85
            else -> 0
        }
    }

    fun mostCommonInList(list: MutableList<Int>): Int {
        val result = list.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(1).map { it.first }
        return result[0]
    }


}