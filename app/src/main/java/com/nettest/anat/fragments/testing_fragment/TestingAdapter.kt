package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nettest.anat.R
import com.nettest.anat.RoomResult
import com.nettest.anat.Utility

@SuppressLint("SetTextI18n")
class TestingAdapter(private val itemList: List<RoomInfo>, private val c: Context): RecyclerView.Adapter<TestingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestingAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: TestingAdapter.ViewHolder, position: Int) {

        val item = itemList[position]
        val imageView = holder.imageView
        imageView.setImageResource(item.image)
        imageView.setColorFilter(Color.parseColor(item.roomState.color))
        holder.roomName.text = item.roomName
        val time = Utility.getTimeFormat(item.totalTimeSeconds)
        holder.roomTime.text = "${time.first}m ${time.second}s"

        //Card View for when user taps a card
        val dialog = BottomSheetDialog(c)
        val view = LayoutInflater.from(c).inflate(R.layout.view_bottom_testing_dialog, null)
        val roomNameTitle = view.findViewById<TextView>(R.id.roomNameTitle)
        roomNameTitle.text = item.roomName
        val duration = view.findViewById<TextView>(R.id.roomSessionTimerLbl)
        duration.text = "${time.first}m ${time.second}s"
        dialog.setContentView(view)

        holder.layout.setOnLongClickListener {
            dialog.show()
            true
        }

    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    class ViewHolder(ItemView: View): RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = ItemView.findViewById<ImageView>(R.id.imageView)
        val roomName: TextView = ItemView.findViewById(R.id.cardRoomName)
        val roomTime: TextView = ItemView.findViewById(R.id.roomTestingTime)
        val layout: CardView = ItemView.findViewById(R.id.testingCardView)
    }

}
