package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.AttachmentImageBinding

class AttachmentImageFragment : Fragment() {

    private var _binding: AttachmentImageBinding? = null
    private val binding: AttachmentImageBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AttachmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUrl = arguments?.getString("imageUrl")
        val fullImageUrl = "http://10.0.2.2:9999/media/$imageUrl"

        Glide.with(this)
            .load(fullImageUrl)
            .placeholder(R.drawable.ic_downloading_24)
            .error(R.drawable.ic_baseline_error_24)
            .into(binding.viewAttachmentImage)

        binding.exit.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
