package js.projects.firstdoctor.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import js.projects.firstdoctor.R
import js.projects.firstdoctor.databinding.FragmentNotificationBinding

class Notification : Fragment(R.layout.fragment_notification) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNotificationBinding.bind(view)
    }
}