package com.horclotapp.taska

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
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
        val btnSave = view.findViewById<View>(R.id.btnSaveQr)

        val qrBitmap = QrUtils.generateQr(link)
        ivQr.setImageBitmap(qrBitmap)

        btnSave.setOnClickListener {
            saveQrToGallery(qrBitmap)
        }
    }

    private fun saveQrToGallery(bitmap: Bitmap) {
        val resolver = requireContext().contentResolver
        val filename = "qr_${System.currentTimeMillis()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Taska")
        }

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (uri == null) {
            Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show()
            return
        }

        val outputStream = resolver.openOutputStream(uri)

        if (outputStream == null) {
            Toast.makeText(requireContext(), "Не удалось открыть поток", Toast.LENGTH_SHORT).show()
            return
        }

        outputStream.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        Toast.makeText(requireContext(), "QR-код сохранён в галерее", Toast.LENGTH_SHORT).show()
    }

}
