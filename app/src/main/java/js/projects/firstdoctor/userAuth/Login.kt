package js.projects.firstdoctor.userAuth

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.utils.Connection
import js.projects.firstdoctor.HomePatient
import js.projects.firstdoctor.databinding.ActivityLoginBinding

class Login : AppCompatActivity() {

    private lateinit var binding:ActivityLoginBinding
    private lateinit var auth:FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = Firebase.database.reference

        binding.apply {
            backButton.setOnClickListener {
                finish()
            }
            forgotPassword.setOnClickListener {
                openForgetPassword()
            }
            login.setOnClickListener {
                openHome()
            }
            register.setOnClickListener {
                openRegister()
            }
        }

    }

    private fun openForgetPassword() {
        val intent = Intent(this, ForgotPassword::class.java)
        startActivity(intent)
    }

    private fun openHome() {
        val conn = Connection()

        val email = binding.emailId.editText?.text.toString().trim()
        val pass = binding.pass.editText?.text.toString().trim()

        if (!conn.checkForInternet(this)) showCustomDialog()
        else if (validateEmailId() && validatePassword()){
            binding.progressBar.visibility = View.VISIBLE
            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomePatient::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Invalid Email or Password Failed!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openRegister() {
        val intent = Intent(this, Register::class.java)
        startActivity(intent)
        finish()
    }

    private fun validateEmailId(): Boolean {
        binding.apply {
            val email: String = emailId.editText?.text.toString().trim { it <= ' ' }
            return if (email.isEmpty()) {
                emailId.error = "Field can't be empty"
                false
            }else{
                emailId.error = null
                emailId.isErrorEnabled = false
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        binding.apply {
            val password: String = pass.editText?.text.toString().trim { it <= ' ' }
            return if (password.isEmpty()) {
                pass.error = "Field can't be empty"
                false
            }else{
                pass.error = null
                pass.isErrorEnabled = false
                true
            }
        }
    }


    private fun showCustomDialog() {
        val builder = AlertDialog.Builder(this@Login)
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