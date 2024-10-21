package com.koalasat.hiss.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.koalasat.hiss.Hiss
import com.koalasat.hiss.R
import com.koalasat.hiss.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.npubInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setNpubInput(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.startButton.setOnClickListener {
            if (Hiss.getInstance().isStarted) {
                Hiss.getInstance().isStarted = false
                val typedValue = TypedValue()
                requireContext().theme.resolveAttribute(androidx.transition.R.attr.colorPrimary, typedValue, true)
                binding.startButton.setBackgroundColor(typedValue.data)
                binding.startButton.text = getString(R.string.start)
            } else {
                if (viewModel.validationResult.value == true) {
                    Hiss.getInstance().isStarted = true
                    val typedValue = TypedValue()
                    requireContext().theme.resolveAttribute(androidx.transition.R.attr.colorButtonNormal, typedValue, true)
                    binding.startButton.setBackgroundColor(typedValue.data)
                    binding.startButton.text = getString(R.string.stop)
                } else {
                    binding.npubInput.error = getString(R.string.invalid_npub)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}