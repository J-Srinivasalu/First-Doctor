package js.projects.firstdoctor.userAuth

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.utils.Connection
import js.projects.firstdoctor.HomePatient
import js.projects.firstdoctor.databinding.ActivityRegisterBinding
import js.projects.firstdoctor.model.User

class Register : AppCompatActivity() {

    private lateinit var binding:ActivityRegisterBinding
    private lateinit var auth:FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = Firebase.database.reference

        binding.apply {
            backButton.setOnClickListener {
                finish()
            }
            login.setOnClickListener {
                val intent = Intent(this@Register, Login::class.java)
                startActivity(intent)
                finish()
            }
            register.setOnClickListener {
                val conn = Connection()
                if (!conn.checkForInternet(this@Register)) showCustomDialog()
                else if(validate() && validatePhoneNo() && validateEmailId() && validatePassword()){
                    openHome()
                }
            }
            gender.editText?.setOnClickListener {
                var selectedIndex = 0
                val genders = arrayOf("Male", "Female", "Others")
                with(AlertDialog.Builder(this@Register)) {
                    setTitle("Select Gender")
                    setSingleChoiceItems(
                        genders,
                        0
                    ) { _, i -> selectedIndex = i}
                        .setPositiveButton("Done"){_,_ -> gender.editText?.setText(genders[selectedIndex])}
                    create()
                }.show()
            }
        }
    }

    private fun openHome(){
        binding.progressBar.visibility = View.VISIBLE
        val email = binding.email.editText?.text.toString().trim()
        val pass = binding.pass.editText?.text.toString().trim()
        val name = binding.name.editText?.text.toString().trim()
        val age = binding.age.editText?.text.toString().trim()
        val countryCode = binding.countryCodePicker.defaultCountryCodeAsInt
        val mobileNo = binding.mobileNo.editText?.text.toString().trim()
        val gender = binding.gender.editText?.text.toString().trim()
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Toast.makeText(this, "Registration Successfully", Toast.LENGTH_SHORT).show()
                val userId = auth.currentUser
                val userIdString = userId!!.uid
                writeNewUser(userIdString,name, email, gender, "+$countryCode$mobileNo", age)
                val intent = Intent(this, HomePatient::class.java)
                startActivity(intent)
                finish()
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Registration Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeNewUser(userId: String, name: String, email: String, gender: String, mobileNo:String, age:String) {
        val user = User(
            userName = name,
            userEmail = email,
            userSex = gender,
            userMobile = mobileNo,
            userAge = age
        )
        database.child("Users").child(userId).setValue(user)
    }

    private fun validate(): Boolean{
        binding.apply {
            val fullName = name.editText?.text.toString().trim { it <= ' ' }
            val mAge = age.editText?.text.toString().trim { it <= ' ' }
            val mGender = gender.editText?.text.toString().trim { it <= ' ' }
            if(fullName.isEmpty()){
                name.error = "Field can't be empty"
                return false
            }else{
                name.error = null
                name.isErrorEnabled = false
            }
            if(mAge.isEmpty()){
                age.error = "Field can't be empty"
                return false
            }else{
                age.error = null
                age.isErrorEnabled = false
            }
            if(mGender.isEmpty()){
                gender.error = "Field can't be empty"
                return false
            }else{
                gender.error = null
                gender.isErrorEnabled = false
            }
        }
        return true
    }

    private fun validateEmailId(): Boolean {
        binding.apply {
            val emailId: String = email.editText?.text.toString().trim { it <= ' ' }
            val checkEmailId = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
            return if (emailId.isEmpty()) {
                email.error = "Field can't be empty"
                false
            } else if (!emailId.matches(checkEmailId.toRegex())) {
                email.error = "Invalid EmailId"
                false
            } else {
                email.error = null
                email.isErrorEnabled = false
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        binding.apply {
            val password: String = pass.editText?.text.toString().trim { it <= ' ' }
            val checkPass = "^" +// "(?=.*[0-9])"+                  //at least 1 digit
                    "(?=.*[a-zA-Z])" +                            //any letter
                    //"(?=.*[@#$%^&+=])"+                           //at least 1 special character
                    "(?=\\S+$)" +                                 //no white spaces
                    ".{6,}" +                                     //at least 6 characters
                    "$"
            return if (password.isEmpty()) {
                pass.error = "Field can't be empty"
                false
            } else if (!password.matches(checkPass.toRegex())) {
                pass.error = "Password should contain at least 6 character and should not contain white spaces"
                false
            } else {
                pass.error = null
                pass.isErrorEnabled = false
                true
            }
        }
    }

    private fun validatePhoneNo(): Boolean {
        binding.apply {
            val phoneNo: String = mobileNo.editText?.text.toString().trim { it <= ' ' }
            return when {
                phoneNo.isEmpty() -> {
                    mobileNo.error = "Field can't be empty"
                    false
                }
                phoneNo.length != 10 -> {
                    mobileNo.error = "Mobile no. must be 10 digits long"
                    false
                }
                else -> {
                    mobileNo.error = null
                    mobileNo.isErrorEnabled = false
                    true
                }
            }
        }
    }
    private fun showCustomDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this@Register)
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