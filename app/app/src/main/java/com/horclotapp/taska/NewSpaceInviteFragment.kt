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
import androidx.viewpager2.widget.ViewPager2
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class NewSpaceInviteFragment : Fragment(R.layout.fragment_new_space_invite) {

    companion object {
        fun newInstance(
            name: String,
            desc: String,
            code: String
        ): NewSpaceInviteFragment {
            return NewSpaceInviteFragment().apply {
                arguments = bundleOf(
                    "name" to name,
                    "desc" to desc,
                    "code" to code
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val code = requireArguments().getString("code")!!
        val inviteLink = "https://example.com/join/$code"

        val btnLink = view.findViewById<Button>(R.id.btnLinkMode)
        val btnQr = view.findViewById<Button>(R.id.btnQrMode)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter = InvitePagerAdapter(this, inviteLink)

        btnLink.setOnClickListener {
            viewPager.currentItem = 0
        }

        btnQr.setOnClickListener {
            viewPager.currentItem = 1
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val active = Color.parseColor("#FF6B6B")
                val inactive = Color.parseColor("#313642")

                btnLink.backgroundTintList =
                    ColorStateList.valueOf(if (position == 0) active else inactive)
                btnQr.backgroundTintList =
                    ColorStateList.valueOf(if (position == 1) active else inactive)
            }
        })

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}

