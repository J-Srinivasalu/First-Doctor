package js.projects.firstdoctor.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.ConsultNow
import js.projects.firstdoctor.R
import js.projects.firstdoctor.databinding.FragmentHomeBinding
import js.projects.firstdoctor.utils.Connection

class Home : Fragment(R.layout.fragment_home) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database:DatabaseReference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)
        binding.apply {
            layout.visibility = View.GONE
            progressBar.visibility = View.VISIBLE

            if(!Connection().checkForInternet(requireContext())){
                showCustomDialog()
            }
            else{
                auth = Firebase.auth
                database = Firebase.database.reference

                database.child("Users").child(auth.currentUser!!.uid).child("userName").get().addOnSuccessListener {
                    binding.username.text = it.value.toString()
                    progressBar.visibility = View.GONE
                    layout.visibility = View.VISIBLE
                }
            }

            consultNow.setOnClickListener {
                startActivity(Intent(requireContext(), ConsultNow::class.java))
            }

        }

    }
    private fun showCustomDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setMessage("Please connect to the internet to proceed further")
            .setCancelable(false)
            .setPositiveButton(
                "Connect"
            ) { _: DialogInterface?, _: Int ->
                startActivity(
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                )
            }
            .setNegativeButton(
                "cancel"
            ) { _: DialogInterface, _: Int -> }
        builder.show()
    }
}