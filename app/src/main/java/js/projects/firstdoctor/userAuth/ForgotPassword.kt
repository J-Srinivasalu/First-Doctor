package js.projects.firstdoctor.userAuth

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.R
import js.projects.firstdoctor.databinding.ActivityForgotPasswordBinding

class ForgotPassword : AppCompatActivity() {

    private lateinit var binding:ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth



        binding.backButton.setOnClickListener {
            finish()
        }


        binding.reset.setOnClickListener{
            val email = binding.emailId.editText?.text.toString().trim()
            if(email.isEmpty()){
                Toast.makeText(this, "Email can't be empty", Toast.LENGTH_LONG).show()
            }else{
                binding.progressBar.visibility = View.VISIBLE
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showCustomDialog()
                        }
                        else{
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this, "Invalid Email Failed!", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
    private fun showCustomDialog() {
        val builder = AlertDialog.Builder(this@ForgotPassword)
        builder.setMessage("Email sent successfully, please follow the instructions in the email to reset your password")
            .setCancelable(false)
            .setIcon(R.drawable.icon_success)
            .setPositiveButton(
                "OK"
            ) { _: DialogInterface?, _: Int ->
                startActivity(
                    Intent(this@ForgotPassword, Login::class.java)
                )
                finish()
            }
        builder.show()
    }
}