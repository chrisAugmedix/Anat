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
        return if (result.isEmpty()) 0 else result[0]
    }

    fun getWiFiAPChannel(channel: Int?): Int {

        when (channel) {

            2412 -> return 1
            2417 -> return 2
            2422 -> return 3
            2427 -> return 4
            2432 -> return 5
            2437 -> return 6
            2442 -> return 7
            2447 -> return 8
            2452 -> return 9
            2457 -> return 10
            2462 -> return 11
            5180 -> return 36
            5190 -> return 38
            5200 -> return 40
            5210 -> return 42
            5220 -> return 44
            5230 -> return 46
            5240 -> return 48
            5250 -> return 50
            5260 -> return 52
            5270 -> return 54
            5280 -> return 56
            5290 -> return 58
            5300 -> return 60
            5310 -> return 62
            5320 -> return 64
            5500 -> return 100
            5510 -> return 102
            5520 -> return 104
            5530 -> return 106
            5540 -> return 108
            5550 -> return 110
            5560 -> return 112
            5570 -> return 114
            5580 -> return 116
            5590 -> return 118
            5600 -> return 120
            5610 -> return 122
            5620 -> return 124
            5630 -> return 126
            5640 -> return 128
            5650 -> return 130
            5660 -> return 132
            5670 -> return 134
            5680 -> return 136
            5690 -> return 138
            5700 -> return 140
            5710 -> return 142
            5720 -> return 144
            5745 -> return 149
            5755 -> return 151
            5765 -> return 153
            5775 -> return 155
            5785 -> return 157
            5795 -> return 159
            5805 -> return 161
            5825 -> return 165
            else -> return 0

        }

    }
}