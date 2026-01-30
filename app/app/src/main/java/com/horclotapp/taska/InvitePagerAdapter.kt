package com.horclotapp.taska

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class InvitePagerAdapter(
    fragment: Fragment,
    private val inviteLink: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> InviteLinkFragment.newInstance(inviteLink)
            else -> InviteQrFragment.newInstance(inviteLink)
        }
    }
}
