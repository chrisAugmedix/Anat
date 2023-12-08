package com.nettest.anat.fragments

import com.nettest.anat.fragments.pc_fragment.GetResult
import com.nettest.anat.fragments.pc_fragment.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class NetworkOperations {

    private val port = 1201
    private val protocol = "https"
    private val hostname = "grafana.augmedix.com"
    private val fullHostName = "${protocol}://${hostname}:${port}"

    private val httpsClient by lazy {
        OkHttpClient.Builder().connectTimeout(10000, TimeUnit.MILLISECONDS).writeTimeout(10000, TimeUnit.MILLISECONDS).build()
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



}