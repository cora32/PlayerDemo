package io.iskopasi.player_test.fragments

import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectorFragment : Fragment() {
//    private val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        Manifest.permission.READ_MEDIA_AUDIO
//    } else {
//        Manifest.permission.READ_EXTERNAL_STORAGE
//    }
//    private lateinit var binding: FragmentSelectorBinding
//
//    private val requesterXml = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        permissionCallback(isGranted, SelectorFragmentDirections.actionToXml())
//    }
//
//    private val requesterCompose = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        permissionCallback(isGranted, SelectorFragmentDirections.actionToCompose())
//    }
//
//    private val requesterRx = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        permissionCallback(isGranted, SelectorFragmentDirections.actionToRx())
//    }
//
//    private fun permissionCallback(isGranted: Boolean, action: NavDirections) = when {
//        isGranted -> onGranted(action)
//        !ActivityCompat.shouldShowRequestPermissionRationale(
//            requireActivity(),
//            permission
//        ) -> onPermaDenied()
//
//        else -> onDenied(action)
//    }
//
//    private fun getRequester(action: NavDirections) = when (action) {
//        SelectorFragmentDirections.actionToXml() -> requesterXml
//        SelectorFragmentDirections.actionToCompose() -> requesterCompose
//        SelectorFragmentDirections.actionToRx() -> requesterRx
//        else -> requesterXml
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentSelectorBinding.inflate(inflater, container, false)
//
//        binding.buttonXml.setOnClickListener {
//            requestPermissionContract(SelectorFragmentDirections.actionToXml())
//        }
//        binding.buttonJetpack.setOnClickListener {
//            Snackbar.make(binding.root, "Not implemented", Snackbar.LENGTH_LONG).show()
//        }
//
//        return binding.root
//    }
//
//    private fun onGranted(action: NavDirections) {
//        findNavController().navigate(action)
//    }
//
//    private fun onDenied(action: NavDirections) {
//        requestPermissionContract(action)
//    }
//
//    private fun onPermaDenied() {
//        findNavController().navigate(SelectorFragmentDirections.actionToPermissionDenied())
//    }
//
//    private fun requestPermissionContract(action: NavDirections) {
//        val requester = getRequester(action)
//
//        if (ActivityCompat.shouldShowRequestPermissionRationale(
//                requireActivity(),
//                permission
//            )
//        ) {
//            AlertDialog.Builder(requireActivity())
//                .setTitle("We need file permission")
//                .setMessage("Plz!")
//                .setPositiveButton(
//                    "OK"
//                ) { dialog, which ->
//                    requester.launch(permission)
//                }
//                .setNegativeButton("Cancel") { dialog, which ->
//                    onPermaDenied()
//                }
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .show()
//        } else {
//            requester.launch(permission)
//        }
//    }
}