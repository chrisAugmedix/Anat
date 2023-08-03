package com.nettest.anat.fragments.pc_fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nettest.anat.global_resultList

class PortCheckerViewModel: ViewModel() {

    private var resultList = MutableLiveData<MutableList<EndpointParent>>()
    private var portCheckerResult = MutableLiveData<Boolean>()

    fun addResultToList(result: EndpointParent) {
        global_resultList.add(result)
        resultList.value?.add(result)
    }

    fun reportCheckerDone() {
        portCheckerResult.value = true
    }

    fun getList(): MutableLiveData<MutableList<EndpointParent>> {
        return resultList
    }

    fun getResult(): MutableLiveData<Boolean> {
        return portCheckerResult
    }



}