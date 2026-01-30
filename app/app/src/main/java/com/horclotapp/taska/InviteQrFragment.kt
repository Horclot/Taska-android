package com.horclotapp.taska

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

class InviteQrFragment : Fragment(R.layout.item_invite_qr) {

    companion object {
        fun newInstance(link: String) = InviteQrFragment().apply {
            arguments = bundleOf("link" to link)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val link = requireArguments().getString("link")!!
        val ivQr = view.findViewById<ImageView>(R.id.ivQr)

        ivQr.setImageBitmap(QrUtils.generateQr(link))
    }
}

