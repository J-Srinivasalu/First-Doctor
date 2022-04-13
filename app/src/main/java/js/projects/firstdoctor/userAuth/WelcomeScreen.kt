package js.projects.firstdoctor.userAuth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.HomePatient
import js.projects.firstdoctor.databinding.ActivityWelcomeScreenBinding

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

    override fun onStart() {
        super.onStart()

        val currUser = auth.currentUser
        if(currUser != null){
            startActivity(Intent(this,HomePatient::class.java))
            finish()
        }
    }

}