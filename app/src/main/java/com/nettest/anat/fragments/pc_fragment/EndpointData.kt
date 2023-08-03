package com.nettest.anat.fragments.pc_fragment

data class EndpointParent(val hostName: String, val type: EndpointType = EndpointType.PARENT,
                          val endpoints: MutableList<EndpointChild> = mutableListOf(), var expandedState: Boolean = false,
                          val description: String = "This is a basic description"
)

data class EndpointChild(val endpoint: String, val result: Boolean = false, val textColor: String = "#e50000")

enum class EndpointType(val res: Int) {
    PARENT(0),
    CHILD(1)
}