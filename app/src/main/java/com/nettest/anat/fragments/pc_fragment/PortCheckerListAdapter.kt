package com.nettest.anat.fragments.pc_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.nettest.anat.R

class PortCheckerListAdapter(private val context: Context, private val childList: MutableList<EndpointChild>): BaseAdapter() {


    override fun getCount(): Int {
        return childList.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.pc_recycler_view_list_view, null)

        val endpointName = view.findViewById<TextView>(R.id.pc_list_textView)
        endpointName.text = childList[position].endpoint
        endpointName.setTextColor(Color.parseColor(childList[position].textColor))

        return view
    }
}