package js.projects.firstdoctor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.databinding.ActivityHomeDoctorBinding
import js.projects.firstdoctor.userAuth.Login

class HomeDoctor : AppCompatActivity() {

    private lateinit var binding: ActivityHomeDoctorBinding
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = Firebase.auth

        binding.logout.setOnClickListener {
            val user: FirebaseUser? = mAuth.currentUser
            if (user != null) {
                mAuth.signOut()
                val intent = Intent(this, Login::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Some error occurred. Please contact us", Toast.LENGTH_SHORT).show()
            }
        }

    }
}