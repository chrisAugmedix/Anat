package com.nettest.anat.fragments.pc_fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nettest.anat.R
import com.nettest.anat.global_isPortCheckerRunning

@SuppressLint("NotifyDataSetChanged")
class PortCheckerRecyclerViewAdapter(private var resultList: MutableList<EndpointParent>, private var context: Context?): RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == EndpointType.CHILD.res) ViewHolderChild(LayoutInflater.from(parent.context).inflate(R.layout.pc_recycler_view_child, parent, false))
        else ViewHolderParent(LayoutInflater.from(parent.context).inflate(R.layout.pc_recycler_view_parent, parent, false))
    }


    override fun getItemCount(): Int { return resultList.size }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (holder is ViewHolderParent) {
            val endpointParent = resultList[position]
            holder.roomName.text = endpointParent.endpointName
            holder.roomName.setCompoundDrawablesWithIntrinsicBounds(0, 0, endpointParent.endpointResult, 0)

            holder.layout.setOnClickListener {
                if (global_isPortCheckerRunning) return@setOnClickListener
                if (!endpointParent.isExpanded) expandList(position)
                else minimizeList(position)
            }
        }

        if (holder is ViewHolderChild) {

            val endpointChild = resultList[position]
            val name = if ( endpointChild.requestType == RequestType.PING ) endpointChild.endpointName else "${endpointChild.endpointName} (${endpointChild.httpResponse})"
            holder.targetEndpoint.text = name
            context?.let { holder.targetEndpoint.setTextColor(ContextCompat.getColor(it, ( if (!endpointChild.result) R.color.status_failed else R.color.status_good ))) }

        }

        //Parent Line
//        val endpointParent = resultList[position]
//        holder.roomName.text = endpointParent.endpointName
//        holder.roomName.setCompoundDrawablesWithIntrinsicBounds(0, 0, endpointParent.endpointResult, 0)

//        val dialog = getDetailsDialog(context, endpointParent.endpointName, endpointParent.endpointDescription, endpointParent.endpoints)
//        holder.layout.setOnClickListener { dialog.show() }

    }

    private fun minimizeList(position: Int) {
        val selectedEndpoint = resultList[position]
        if (selectedEndpoint.type == EndpointType.CHILD) return
        selectedEndpoint.isExpanded = false
        selectedEndpoint.endpoints.forEach { _ ->
            resultList.removeAt(position+1)
        }
        notifyDataSetChanged()
    }


    private fun expandList(position: Int) {
        if (resultList[position].type == EndpointType.CHILD) return
        val selectedEndpoint = resultList[position].apply { isExpanded = true }
        var nextPosition = position+1
        selectedEndpoint.endpoints.forEach {
            val endpoint = EndpointParent(it.targetHostName).apply {
                type = EndpointType.CHILD
                result = it.result
                httpResponse = it.httpResponse
                requestType = selectedEndpoint.requestType
            }
            resultList.add(nextPosition, endpoint)
            nextPosition++
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int { return resultList[position].type.res }

    private fun getDetailsDialog(context: Context, name: String, description: String, endpoints: List<EndpointChild>): Dialog {
        val dialog = Dialog(context)

        val view = LayoutInflater.from(context).inflate(R.layout.pc_recycler_view_dialog, null)
        val list = view.findViewById<ListView>(R.id.pc_listView)
        val listAdapter = PortCheckerListAdapter(context, endpoints)
        list.adapter = listAdapter
        view.findViewById<TextView>(R.id.pc_endpointDescription).apply {
            text = description
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        view.findViewById<TextView>(R.id.pc_endpointHostName).text = name
        dialog.setContentView(view)

        return dialog
    }

    class ViewHolderParent(itemView: View): ViewHolder(itemView) {
        val roomName: TextView = itemView.findViewById(R.id.endpointHostName)
        val layout: CardView = itemView.findViewById(R.id.pc_cardView)
    }

    class ViewHolderChild(itemView: View): ViewHolder(itemView) {
        val targetEndpoint: TextView = itemView.findViewById(R.id.pc_childAddress)
    }

}

class PortCheckerListAdapter(private val context: Context, private val childList: List<EndpointChild>): BaseAdapter() {


    override fun getCount(): Int {
        return childList.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view = convertView ?: View.inflate(context, R.layout.pc_recycler_view_list_view, null)
        val endpointName = view.findViewById<TextView>(R.id.pc_list_textView)
        endpointName.text = childList[position].targetHostName
        endpointName.setTextColor( childList[position].statusColor ?: R.color.gray )
        return view
    }
}



