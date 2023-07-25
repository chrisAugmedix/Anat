package com.nettest.anat.fragments.pc_fragment

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nettest.anat.R

class PortCheckerRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }


    class ViewHolderParent(ItemView: View): RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = ItemView.findViewById<ImageView>(R.id.imageView)
        val roomName: TextView = ItemView.findViewById(R.id.cardRoomName)
        val roomTime: TextView = ItemView.findViewById(R.id.roomTestingTime)
        val layout: CardView = ItemView.findViewById(R.id.testingCardView)
    }

    class ViewHolderChild(ItemView: View): RecyclerView.ViewHolder(ItemView) {

    }




}




