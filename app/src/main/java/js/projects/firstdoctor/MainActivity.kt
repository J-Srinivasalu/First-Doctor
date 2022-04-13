package js.projects.firstdoctor

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.databinding.ActivityMainBinding
import js.projects.firstdoctor.userAuth.WelcomeScreen
import js.projects.firstdoctor.utils.Connection

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = Firebase.auth
        database = Firebase.database.reference



        binding.imageView.alpha = 0f
        binding.imageView.animate().setDuration(1500).alpha(1f).withEndAction {
            startActivity(Intent(this, WelcomeScreen::class.java))
            finish()
        }



    }
}