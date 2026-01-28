package com.horclotapp.taska

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class NewSpaceInviteFragment : Fragment(R.layout.fragment_new_space_invite) {

    companion object {
        fun newInstance(name: String, desc: String, code: String): NewSpaceInviteFragment {
            return NewSpaceInviteFragment().apply {
                arguments = bundleOf(
                    "name" to name,
                    "desc" to desc,
                    "code" to code
                )
            }
        }
    }

    private lateinit var linkContainer: View
    private lateinit var qrContainer: View
    private lateinit var tvLink: TextView
    private lateinit var ivQr: ImageView

    private lateinit var inviteLink: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val code = requireArguments().getString("code")!!
        inviteLink = "https://example.com/join/$code"

        linkContainer = view.findViewById(R.id.linkContainer)
        qrContainer = view.findViewById(R.id.qrContainer)
        tvLink = view.findViewById(R.id.tvLink)
        ivQr = view.findViewById(R.id.ivQr)



        val btnLinkMode = view.findViewById<Button>(R.id.btnLinkMode)
        val btnQrMode = view.findViewById<Button>(R.id.btnQrMode)
        val btnCopy = view.findViewById<Button>(R.id.btnCopyLink)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)

        setMode(true, btnLinkMode, btnQrMode)

        tvLink.text = inviteLink
        ivQr.setImageBitmap(generateQr(inviteLink))

        btnLinkMode.setOnClickListener {
            setMode(true, btnLinkMode, btnQrMode)
            linkContainer.visibility = View.VISIBLE
            qrContainer.visibility = View.GONE
        }

        btnQrMode.setOnClickListener {
            setMode(false, btnLinkMode, btnQrMode)
            linkContainer.visibility = View.GONE
            qrContainer.visibility = View.VISIBLE
        }

        btnCopy.setOnClickListener {
            val cm = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("link", inviteLink))
            Toast.makeText(requireContext(), "Ссылка скопирована", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


    }

    private fun setMode(isLink: Boolean, btnLink: Button, btnQr: Button) {
        if (isLink) {
            linkContainer.visibility = View.VISIBLE
            qrContainer.visibility = View.GONE

            btnLink.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#FF6B6B"))
            )
            btnQr.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#313642"))
            )
        } else {
            linkContainer.visibility = View.GONE
            qrContainer.visibility = View.VISIBLE

            btnLink.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#313642"))
            )
            btnQr.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#FF6B6B"))
            )
        }
    }

    private fun generateQr(text: String): Bitmap {
        val size = 600
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
