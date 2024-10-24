package com.koalasat.pokey.ui.relays

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.koalasat.pokey.Pokey
import com.koalasat.pokey.R
import com.koalasat.pokey.databinding.FragmentRelaysBinding
import com.vitorpamplona.ammolite.relays.Relay
import com.vitorpamplona.ammolite.relays.RelayPool

class RelaysFragment : Fragment() {
    private var _binding: FragmentRelaysBinding? = null
    private val binding get() = _binding!!
    private lateinit var relaysView: RecyclerView
    private lateinit var adapter: RelayListAdapter
    private lateinit var itemList: List<Relay>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val relaysViewModel =
            ViewModelProvider(this).get(RelaysViewModel::class.java)

        _binding = FragmentRelaysBinding.inflate(inflater, container, false)

        val root: View = binding.root

        val textView: TextView = binding.titleRelays
        Pokey.isEnabled.observe(viewLifecycleOwner) {
            if (it) {
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE
            }
        }

        relaysView = root.findViewById(R.id.relays)
        relaysView.layoutManager = LinearLayoutManager(context)

        itemList = RelayPool.getAll()
        adapter = RelayListAdapter(itemList)
        relaysView.adapter = adapter

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
