package com.horclotapp.taska

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton

class SpacesFragment : Fragment(R.layout.fragment_spaces) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addButton = view.findViewById<ImageButton>(R.id.addButton)

        addButton.setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(R.id.spacesContainer, NewSpaceCreateFragment())
                .addToBackStack(null)
                .commit()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_spaces, container, false)
        return view
    }
}