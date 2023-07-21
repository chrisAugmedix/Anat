package com.nettest.anat.fragments.testing_fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TestingAdapter: RecyclerView.Adapter<TestingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestingAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate()
    }

    override fun onBindViewHolder(holder: TestingAdapter.ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    class ViewHolder(ItemView: View): RecyclerView.ViewHolder(ItemView) {


    }

}