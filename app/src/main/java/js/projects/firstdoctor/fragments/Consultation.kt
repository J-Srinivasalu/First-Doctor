package js.projects.firstdoctor.fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import js.projects.firstdoctor.R
import js.projects.firstdoctor.databinding.FragmentConsultationBinding

class Consultation : Fragment(R.layout.fragment_consultation) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentConsultationBinding.bind(view)

        binding.apply {
            gender.editText?.setOnClickListener {
                var selectedIndex = 0
                val genders = arrayOf("Male", "Female", "Others")
                with(AlertDialog.Builder(requireContext())) {
                    setTitle("Select Gender")
                    setSingleChoiceItems(
                        genders,
                        0
                    ) { _, i -> selectedIndex = i}
                        .setPositiveButton("Done"){_,_ -> gender.editText?.setText(genders[selectedIndex])}
                    create()
                }.show()
            }
            domain.editText?.setOnClickListener {
                var selectedIndex = 0
                val domains = arrayOf("Oral Cancer", "General Health")
                with(AlertDialog.Builder(requireContext())) {
                    setTitle("Select Domain")
                    setSingleChoiceItems(
                        domains,
                        0
                    ) { _, i -> selectedIndex = i}
                        .setPositiveButton("Done"){_,_ -> domain.editText?.setText(domains[selectedIndex])}
                    create()
                }.show()
            }

        }
    }
}