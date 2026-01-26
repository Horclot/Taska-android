package com.horclotapp.taska

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID

class CreateRoomBottomSheet : com.google.android.material.bottomsheet.BottomSheetDialogFragment() {

    private lateinit var qrBtn: View
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var roomId = ""
    private var inviteCode = ""

    private var membersListener: ListenerRegistration? = null

    private lateinit var titleInput: EditText
    private lateinit var descInput: EditText
    private lateinit var inviteText: TextView
    private lateinit var membersContainer: LinearLayout
    private lateinit var copyBtn: View
    private lateinit var createBtn: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_create_room, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        titleInput = view.findViewById(R.id.roomTitleInput)
        descInput = view.findViewById(R.id.roomDescInput)
        inviteText = view.findViewById(R.id.inviteCodeText)
        membersContainer = view.findViewById(R.id.membersContainer)
        copyBtn = view.findViewById(R.id.copyInviteBtn)
        createBtn = view.findViewById(R.id.createRoomBtn)

        createDraftRoom()
        setupActions()
        qrBtn = view.findViewById(R.id.showQrBtn)

        qrBtn.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_qr, null)

            val img = dialogView.findViewById<ImageView>(R.id.qrImage)
            img.setImageBitmap(generateQrBitmap(buildInviteLink()))

            MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Закрыть", null)
                .show()
        }

        copyBtn.setOnClickListener {
            val link = buildInviteLink()
            val cm = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("invite", link))
            Toast.makeText(requireContext(), "Ссылка скопирована", Toast.LENGTH_SHORT).show()
        }

    }
    private fun generateQrBitmap(text: String): Bitmap {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val matrix = writer.encode(
            text,
            com.google.zxing.BarcodeFormat.QR_CODE,
            600,
            600
        )

        val bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565)

        for (x in 0 until 600) {
            for (y in 0 until 600) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.WHITE else Color.BLACK)
            }
        }

        return bmp
    }
    private fun buildInviteLink(): String {
        return "https://taska.app/invite?code=$inviteCode"
    }
    private fun createDraftRoom() {
        val userId = auth.currentUser?.uid ?: return

        roomId = db.collection("rooms").document().id
        inviteCode = UUID.randomUUID().toString().take(8)

        val room = mapOf(
            "title" to "",
            "description" to "",
            "ownerId" to userId,
            "inviteCode" to inviteCode,
            "draft" to true,
            "createdAt" to System.currentTimeMillis()
        )

        val member = mapOf(
            "roomId" to roomId,
            "userId" to userId,
            "systemRoles" to listOf("creator"),
            "status" to "joined",
            "joinedAt" to System.currentTimeMillis()
        )

        db.collection("rooms").document(roomId).set(room)
        db.collection("room_members").document("${roomId}_$userId").set(member)
        db.collection("room_invites").document(inviteCode)
            .set(mapOf("roomId" to roomId, "createdAt" to System.currentTimeMillis()))

        val link = buildInviteLink()
        inviteText.text = link

        listenMembers()
    }

    private fun listenMembers() {
        membersListener = db.collection("room_members")
            .whereEqualTo("roomId", roomId)
            .addSnapshotListener { snap, _ ->
                membersContainer.removeAllViews()

                snap?.documents?.forEach { doc ->
                    val status = doc.getString("status") ?: "pending"

                    val chip = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_member_chip, membersContainer, false) as TextView

                    chip.text = if (status == "joined") "Подключился" else "Ожидает…"
                    membersContainer.addView(chip)

                    chip.alpha = 0f
                    chip.animate().alpha(1f).setDuration(250).start()
                }
            }
    }

    private fun setupActions() {
        copyBtn.setOnClickListener {
            val cm = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("invite", inviteCode))
            Toast.makeText(requireContext(), "Код скопирован", Toast.LENGTH_SHORT).show()
        }

        createBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val desc = descInput.text.toString().trim()

            if (title.isEmpty()) {
                titleInput.error = "Введите название"
                return@setOnClickListener
            }

            db.collection("rooms").document(roomId)
                .update(
                    mapOf(
                        "title" to title,
                        "description" to desc,
                        "draft" to false
                    )
                )

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        membersListener?.remove()
    }
}
