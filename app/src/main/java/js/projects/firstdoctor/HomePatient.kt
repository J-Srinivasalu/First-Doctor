package js.projects.firstdoctor

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.databinding.ActivityHomePatientBinding
import js.projects.firstdoctor.fragments.*
import js.projects.firstdoctor.utils.Connection

class HomePatient : AppCompatActivity(){

    private lateinit var binding:ActivityHomePatientBinding
    private lateinit var mAuth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePatientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mAuth = Firebase.auth

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.toolBar.setTitleTextColor(resources.getColor( R.color.white,this.theme))
        }else{
            binding.toolBar.setTitleTextColor(resources.getColor( R.color.white))
        }

        setSupportActionBar(binding.toolBar)
        binding.toolBar.visibility = View.GONE
        setCurrentFragment(Home())
        title = getString(R.string.home)

        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.home -> setCurrentFragment(Home())
                R.id.profile -> setCurrentFragment(Profile())
                R.id.consultNow -> setCurrentFragment(Consultation())
                R.id.notification -> setCurrentFragment(Notification())
                R.id.research -> setCurrentFragment(Research())

            }
            title = it.title
            true
        }

    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }
    }

    override fun onBackPressed() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setMessage("do you want to quit the app?")
            .setCancelable(false)
            .setPositiveButton(
                "Yes"
            ) { _: DialogInterface?, _: Int ->
                super.onBackPressed()
            }
            .setNegativeButton(
                "No"
            ) { _: DialogInterface, _: Int -> }
        builder.show()
    }

    override fun onStart() {
        super.onStart()
        if(!Connection().checkForInternet(this)){
            showCustomDialog()
        }
    }

    private fun showCustomDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
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