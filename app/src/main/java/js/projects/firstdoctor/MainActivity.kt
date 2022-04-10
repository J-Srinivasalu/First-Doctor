package js.projects.firstdoctor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import js.projects.firstdoctor.databinding.ActivityMainBinding
import js.projects.firstdoctor.userAuth.WelcomeScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imageView.alpha = 0f
        binding.imageView.animate().setDuration(1500).alpha(1f).withEndAction {
            val intent = Intent(this, WelcomeScreen::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}