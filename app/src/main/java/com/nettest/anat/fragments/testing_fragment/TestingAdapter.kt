package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nettest.anat.R
import com.nettest.anat.Utility

@SuppressLint("SetTextI18n")
class TestingAdapter(private val itemList: List<RoomInfo>, private val c: Context): RecyclerView.Adapter<TestingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestingAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.testing_recycler_view_parent, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: TestingAdapter.ViewHolder, position: Int) {

        val item = itemList[position]
        Log.d("Result", item.toString())
        val wifiImageView = holder.wifiStatusView
        val lteImageView = holder.lteStatusView
        wifiImageView.setImageResource(item.wifiImage)
        wifiImageView.setColorFilter(Color.parseColor(item.wifiImageColor))
        lteImageView.setImageResource(item.lteImage)
        lteImageView.setColorFilter(Color.parseColor(item.lteImageColor))
        if (item.roomName.length > 10) { holder.roomName.textSize = 13F }
        else { holder.roomName.textSize = 18F }
        holder.roomName.text = item.roomName
        val time = Utility.getTimeFormat(item.totalTimeSeconds)
        holder.roomTime.text = "${time.first}m ${time.second}s"

        //Card View for when user taps a card
//        val dialog = BottomSheetDialog(c)
//        val view = LayoutInflater.from(c).inflate(R.layout.view_bottom_testing_dialog, null)
//        val roomNameTitle = view.findViewById<TextView>(R.id.roomNameTitle)
//        roomNameTitle.text = item.roomName
//        val duration = view.findViewById<TextView>(R.id.roomSessionTimerLbl)
//        duration.text = "${time.first}m ${time.second}s"
//        dialog.setContentView(view)
//
//        holder.layout.setOnLongClickListener {
//            dialog.show()
//            true
//        }

    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    class ViewHolder(ItemView: View): RecyclerView.ViewHolder(ItemView) {
        val wifiStatusView: ImageView = ItemView.findViewById(R.id.wifiStatusImageView)
        val lteStatusView: ImageView = ItemView.findViewById(R.id.lteStatusImageView)
        val roomName: TextView = ItemView.findViewById(R.id.cardRoomName)
        val roomTime: TextView = ItemView.findViewById(R.id.roomTestingTime)
        val layout: CardView = ItemView.findViewById(R.id.testingCardView)
    }

}
