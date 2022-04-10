package js.projects.firstdoctor.userAuth

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.HomePatient
import js.projects.firstdoctor.databinding.ActivityWelcomeScreenBinding
import js.projects.firstdoctor.utils.Connection

class WelcomeScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityWelcomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = Firebase.database.reference

        binding.apply {
            login.setOnClickListener {
                startActivity(Intent(this@WelcomeScreen, Login::class.java))
            }
            register.setOnClickListener {
                startActivity(Intent(this@WelcomeScreen, Register::class.java))
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val conn = Connection()
        if (!conn.checkForInternet(this@WelcomeScreen)) showCustomDialog()

        val userId = auth.currentUser
        if(userId != null){
            val intent = Intent(this, HomePatient::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun showCustomDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this@WelcomeScreen)
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
            ) { _: DialogInterface, _: Int -> finish()}
        builder.show()
    }
}