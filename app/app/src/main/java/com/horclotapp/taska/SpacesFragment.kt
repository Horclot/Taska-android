package com.horclotapp.taska

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class SpacesFragment : Fragment() {
    private val qrLauncher = registerForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        if (result.contents != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.contents))
            startActivity(intent)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_spaces, container, false)

        val addButton = view.findViewById<View>(R.id.addButton)
        addButton.setOnClickListener {
            CreateRoomBottomSheet().show(parentFragmentManager, "create_room")
        }

        addButton.setOnLongClickListener {
            qrLauncher.launch(
                com.journeyapps.barcodescanner.ScanOptions()
                    .setPrompt("Отсканируйте QR пространства")
                    .setBeepEnabled(true)
                    .setOrientationLocked(true)
            )
            true
        }




        return view
    }
}