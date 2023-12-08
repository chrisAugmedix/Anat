package com.nettest.anat.fragments.pc_fragment

data class EndpointParent(val endpointName: String, var type: EndpointType = EndpointType.PARENT, var requestType: RequestType = RequestType.PING,
                          var endpointResult: Int = 0, var result: Boolean = true, var endpoints: List<EndpointChild> = mutableListOf(),
                          var isExpanded: Boolean = false, var httpResponse: Int = 200
)

data class EndpointChild(var targetHostName: String, var result: Boolean = false, var statusColor: Int? = null, val endpointType: EndpointType = EndpointType.CHILD, var httpResponse: Int = 200)
data class PingResult   ( val destination: String, val duration: Int, val result: Boolean )
data class GetResult    (val destination: String, var httpResponse: Int = 200, val result: Boolean)
enum class EndpointType(val res: Int) {
    PARENT(0),
    CHILD(1)
}

enum class RequestType {
    PING,
    GET
}



val endpointList = listOf(
    EndpointParent(endpointName = "Backup API GA", endpoints = listOf(EndpointChild("52.223.52.124"), EndpointChild("35.71.178.142"))),
    EndpointParent(endpointName = "Backup Websocket GA", endpoints = listOf(EndpointChild("52.223.48.71"), EndpointChild("35.71.150.142"))),
    EndpointParent(endpointName = "Cloud Service Wrapper", endpoints = listOf(EndpointChild("44.232.59.183"))),
    EndpointParent(endpointName = "Content Delivery Networks", endpoints = listOf(EndpointChild("13.226.235.24"), EndpointChild("13.226.251.193"))),
    EndpointParent(endpointName = "Customer Portal", endpoints = listOf(EndpointChild("15.197.218.73"), EndpointChild("3.33.251.133"))),
    EndpointParent(endpointName = "File Server", endpoints = listOf(EndpointChild("44.230.35.166"), EndpointChild("44.232.67.224"))),
    EndpointParent(endpointName = "Live Service Web Sockets & APIs", endpoints = listOf(EndpointChild("34.160.50.26"), EndpointChild("34.120.193.39"))),
    EndpointParent(endpointName = "GO Service Web Sockets & APIs", endpoints = listOf(EndpointChild("34.36.209.222"), EndpointChild("34.95.91.54"))),
    EndpointParent(endpointName = "GCP MCU Set 1", endpoints = listOf(EndpointChild("35.211.48.92"), EndpointChild("35.211.103.195"), EndpointChild("35.211.35.10"))),
    EndpointParent(endpointName = "GCP MCU Set 2", endpoints = listOf(EndpointChild("35.215.122.184"), EndpointChild("35.215.125.35"), EndpointChild("35.215.67.231"))),
    EndpointParent(endpointName = "Logentries", requestType = RequestType.GET, endpoints = listOf(EndpointChild("https://api.logentries.com"), EndpointChild("https://eu.ops.insight.rapid7.com"))),
    EndpointParent(endpointName = "MCUs (Region 2)", endpoints = listOf(EndpointChild("44.228.233.113"), EndpointChild("54.201.10.158"), EndpointChild("52.38.141.16"))),
    EndpointParent(endpointName = "HITRUST MCUs", endpoints = listOf(EndpointChild("54.148.155.161"), EndpointChild("52.38.93.28"), EndpointChild("54.71.127.202"))),
    EndpointParent(endpointName = "Messaging Platform", endpoints = listOf(EndpointChild("54.214.60.53"), EndpointChild("54.245.66.233"))),
    EndpointParent(endpointName = "Microservice API", endpoints = listOf(EndpointChild("52.12.75.209"), EndpointChild("44.233.181.152"))),
    EndpointParent(endpointName = "Microservice in Global Accelerator", endpoints = listOf(EndpointChild("52.223.52.124"), EndpointChild("35.71.178.142"))),
    EndpointParent(endpointName = "Airwatch MDM Suite", requestType = RequestType.GET, endpoints = listOf( EndpointChild("https://cn705.awmdm.com"), EndpointChild("https://ds705.awmdm.com"), EndpointChild("https://awcm705.awmdm.com"),
                                    EndpointChild("https://na1.data.vmwservices.com"), EndpointChild("https://play.google.com"), EndpointChild("https://www.android.com"),
                                    EndpointChild("https://www.google-analytics.com"), EndpointChild("https://dl.google.com"), EndpointChild("https://www.google.com"),
                                    EndpointChild("https://dl-ssl.google.com"), EndpointChild("https://www.google.com/generate_204"))),
    EndpointParent(endpointName = "JAMF MDM Suite", requestType = RequestType.GET, endpoints = listOf(EndpointChild("https://augmedix.jamfcloud.com/"))),
    EndpointParent(endpointName = "Websocket in Global Accelerator", endpoints = listOf(EndpointChild("15.197.218.73"), EndpointChild("3.33.251.133")))
)