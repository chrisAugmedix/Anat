package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nettest.anat.R
import com.nettest.anat.RoomData
import com.nettest.anat.Utility
import com.patrykandpatrick.vico.core.axis.AxisPosition

@SuppressLint("SetTextI18n")
class TestingAdapter( private val itemList: MutableList<RoomData>, private val context: Context ): RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.testing_recycler_view_parent, parent, false)
        return ViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        Log.d("RecyclerViewTapped", "Tapped $position")
        return super.getItemId(position)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val roomData = itemList[position]
        val cellGrade = roomData.getCellGrade().getGrade()
        holder.lteStatusView.setImageResource(cellGrade)
        holder.roomName.textSize = if ( roomData.getRoomName()!!.length > 10 ) 13F else 18F
        holder.roomName.text = roomData.getRoomName()

        //Card View for when user taps a card
        val dialog = getRoomDialog(context, roomData)

        holder.layout.setOnClickListener {
            dialog.show()
        }

    }




    override fun getItemCount(): Int {
        return itemList.size
    }

    private fun getRoomDialog(context: Context, data: RoomData):BottomSheetDialog {

        //Data portion
        val rssiAvg = data.getMetricData().map { it.wifiMetrics }.map { it.rssi }.average().toInt()
        val linkRateRxAvg = data.getMetricData().map { it.wifiMetrics }.map { it.linkRateRx }.average().toInt()
        val linkRateTxAvg = data.getMetricData().map { it.wifiMetrics }.map { it.linkRateTx }.average().toInt()
        val commonChannel = Utility.mostCommonInList( data.getMetricData().map { it.wifiMetrics }.map { it.channel }.toMutableList() )

        val rsrpAvg = data.getMetricData().map { it.cellMetrics }.mapNotNull { it.rsrp }.average().toInt()
        val rsrqAvg = data.getMetricData().map { it.cellMetrics }.mapNotNull { it.rsrq }.average().toInt()
        val lteRssiAvg = data.getMetricData().map { it.cellMetrics }.mapNotNull { it.rssi }.average().toInt()
        val commonBand = Utility.mostCommonInList( data.getMetricData().map { it.cellMetrics }.mapNotNull { it.band }.toMutableList() )

        val time = Utility.getTimeFormat(data.getRoomSeconds())

        val dialog = BottomSheetDialog(context)

        val view = View.inflate(context, R.layout.view_bottom_testing_dialog, null)
        view.findViewById<TextView>(R.id.roomNameTitle).apply { text = data.getRoomName() }
        view.findViewById<TextView>(R.id.roomSessionTimerLbl).apply { text = "${time.first}m ${time.second}s" }
        view.findViewById<TextView>(R.id.rssiAvgLabel).apply { text = "$rssiAvg dBm" }
        view.findViewById<TextView>(R.id.linkRateTxAvgLabel).apply { text = "$linkRateTxAvg Mbps" }
        view.findViewById<TextView>(R.id.linkRateRxAvgLabel).apply { text = "$linkRateRxAvg Mbps" }
        view.findViewById<TextView>(R.id.commonApChannelLabel).apply { text = commonChannel.toString() }
        view.findViewById<TextView>(R.id.rsrpAvgLabel).apply { text = "$rsrpAvg dBm" }
        view.findViewById<TextView>(R.id.rsrqAvgLabel).apply { text = "$rsrqAvg dBm" }
        view.findViewById<TextView>(R.id.lteRssiAvgLabel).apply { text = "$lteRssiAvg dBm" }
        view.findViewById<TextView>(R.id.commonCellBandLabel).apply { text = "$commonBand" }

        view.findViewById<Button>(R.id.closeButton).setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)

        return dialog

    }



}

class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {


    val lteStatusView: ImageView = itemView.findViewById(R.id.lteStatusImageView)
    val roomName: TextView = itemView.findViewById(R.id.cardRoomName)
    val layout: CardView = itemView.findViewById(R.id.testingCardView)

}
