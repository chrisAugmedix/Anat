package com.nettest.anat.fragments

import okhttp3.OkHttp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NetworkOperations {

    private val port = 1201
    private val protocol = "https"
    private val hostname = "grafana.augmedix.com"
    private val fullHostName = "${protocol}://${hostname}:${port}"

    private val httpsClient by lazy {
        OkHttpClient.Builder().connectTimeout(10000, TimeUnit.MILLISECONDS).writeTimeout(10000, TimeUnit.MILLISECONDS).build()
    }





}