package com.horclotapp.taska

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

class InviteLinkFragment : Fragment(R.layout.item_invite_link) {

    companion object {
        fun newInstance(link: String) = InviteLinkFragment().apply {
            arguments = bundleOf("link" to link)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val link = requireArguments().getString("link")!!
        val tvLink = view.findViewById<TextView>(R.id.tvLink)
        val btnCopy = view.findViewById<ImageButton>(R.id.btnCopyLink)

        tvLink.text = link

        btnCopy.setOnClickListener {
            val cm = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("link", link))
            Toast.makeText(requireContext(), "Ссылка скопирована", Toast.LENGTH_SHORT).show()
        }
    }
}
