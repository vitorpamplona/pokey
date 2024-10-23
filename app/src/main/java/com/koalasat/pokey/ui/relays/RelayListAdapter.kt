package com.koalasat.pokey.ui.relays

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.koalasat.pokey.R
import com.koalasat.pokey.service.NotificationsService
import com.vitorpamplona.ammolite.relays.Relay

class RelayListAdapter(private val items: List<Relay>) : RecyclerView.Adapter<RelayListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_relay_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position].url
        val color = if (items[position].isConnected())
            R.color.green
        else
            R.color.red
        holder.textView.setTextColor(ContextCompat.getColorStateList(holder.textView.context, color))
    }

    override fun getItemCount() = items.size
}
