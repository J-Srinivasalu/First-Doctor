package js.projects.firstdoctor.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import js.projects.firstdoctor.R
import js.projects.firstdoctor.databinding.FragmentResearchBinding

class Research : Fragment(R.layout.fragment_research) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentResearchBinding.bind(view)
    }
}