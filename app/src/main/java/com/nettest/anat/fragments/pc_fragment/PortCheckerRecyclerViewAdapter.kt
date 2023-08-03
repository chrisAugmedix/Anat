package com.nettest.anat.fragments.pc_fragment

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nettest.anat.R

class PortCheckerRecyclerViewAdapter(private var resultList: MutableList<EndpointParent>, private var context: Context): RecyclerView.Adapter<PortCheckerRecyclerViewAdapter.ViewHolderParent>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderParent {
        return ViewHolderParent(LayoutInflater.from(parent.context).inflate(R.layout.pc_recycler_view_parent, parent, false))
    }

    override fun getItemCount(): Int { return resultList.size }

    override fun onBindViewHolder(holder: ViewHolderParent, position: Int) {

        val row = resultList[position]

        //Build Dialog to display info
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.pc_recycler_view_dialog, null)

        val title = view.findViewById<TextView>(R.id.pc_endpointHostName)
        val description = view.findViewById<TextView>(R.id.pc_endpointDescription)
        val list = view.findViewById<ListView>(R.id.pc_listView)

        title.text = row.hostName
        description.text = row.description


        val listAdapter = PortCheckerListAdapter(context, row.endpoints)
        list.adapter = listAdapter

        dialog.setContentView(view)

        holder.roomName.text = row.hostName

        holder.layout.setOnClickListener {
            dialog.show()
        }


    }

    override fun getItemViewType(position: Int): Int { return resultList[position].type.res }


    class ViewHolderParent(ItemView: View): RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = ItemView.findViewById(R.id.imageView)
        val roomName: TextView = ItemView.findViewById(R.id.endpointHostName)
        val layout: CardView = ItemView.findViewById(R.id.pc_cardView)
    }

}




