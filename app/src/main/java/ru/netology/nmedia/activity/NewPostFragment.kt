package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel

private const val MAX_SIZE = 2048
class NewPostFragment : Fragment() {

    companion object {
        var Bundle.textArg: String? by StringArg
    }

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )

        val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireContext(),
                    R.string.image_picker_error,
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            val uri = it.data?.data ?: return@registerForActivityResult

            viewModel.savePhoto(uri, uri.toFile())
        }

        arguments?.textArg
            ?.let(binding.edit::setText)

        requireActivity()
            .addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(
                        menu: Menu,
                        menuInflater: MenuInflater
                    ) {
                        menuInflater.inflate(R.menu.new_post_menu, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                        if (menuItem.itemId == R.id.save) {
                            viewModel.changeContent(binding.edit.text.toString())
                            viewModel.save()
                            AndroidUtils.hideKeyboard(requireView())
                            true
                        } else {
                            false
                        }

                },
                viewLifecycleOwner,

        )

        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            if (photo == null) {
                binding.photoContainer.isGone = true
                return@observe
            }

            binding.photoContainer.isVisible = true
            binding.photo.setImageURI(photo.uri)
        }

        binding.remove.setOnClickListener {
            viewModel.removePhoto()
        }

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .maxResultSize(MAX_SIZE, MAX_SIZE)
                .cameraOnly()
                .createIntent(imagePickerLauncher::launch)
        }

        binding.openGallery.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .maxResultSize(MAX_SIZE, MAX_SIZE)
                .galleryOnly()
                .createIntent(imagePickerLauncher::launch)
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }
        return binding.root
    }
}