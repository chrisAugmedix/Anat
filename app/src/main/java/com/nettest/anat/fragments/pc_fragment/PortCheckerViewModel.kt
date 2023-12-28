package com.nettest.anat.fragments.pc_fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class PortCheckerViewModel: ViewModel() {

    private var totalEndpoints = MutableLiveData(0)
    private var totalFailed = MutableLiveData(0)
    private var totalSuccess = MutableLiveData(0)
    private var lastEndpointParent = MutableLiveData<EndpointParent>()
    private val endpointParentList = MutableLiveData<MutableList<EndpointParent>>()

    fun getTotalEndpoints() = totalEndpoints
    fun getTotalFailed() = totalFailed
    fun getTotalSuccess() = totalSuccess

    fun getEndpointList() = endpointParentList
    fun getLastEndpoint() = lastEndpointParent

    fun addLastEndpointParent(ep: EndpointParent) {
        lastEndpointParent.value = ep
    }

    fun setTotal(total: Int) { totalEndpoints.value = total }
    fun setFailed(failed: Int) { totalFailed.value = failed }
    fun setSuccess(success: Int) { totalSuccess.value = success }

    fun addEndpointTotal() { totalEndpoints.value = totalEndpoints.value!! + 1 }
    fun addFailedTotal() { totalFailed.value = totalFailed.value!! + 1 }
    fun addSuccessTotal() {totalSuccess.value = totalSuccess.value!! + 1 }

}