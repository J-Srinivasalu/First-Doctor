package js.projects.firstdoctor.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.R
import js.projects.firstdoctor.databinding.FragmentProfileBinding
import js.projects.firstdoctor.model.User
import js.projects.firstdoctor.userAuth.ForgotPassword
import js.projects.firstdoctor.userAuth.WelcomeScreen
import js.projects.firstdoctor.utils.Connection

class Profile : Fragment(R.layout.fragment_profile) {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var binding: FragmentProfileBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)
        mAuth = Firebase.auth
        database = Firebase.database.reference
        binding.progressBar.visibility = View.VISIBLE
        val user = mAuth.currentUser
        database.child("Users").child(user!!.uid).get().addOnSuccessListener {
            val mName = it.child("userName").value.toString()
            val mAge = it.child("userAge").value.toString()
            val mGender = it.child("userSex").value.toString()
            val mMobileNo = it.child("userMobile").value.toString()
            val mEmail = it.child("userEmail").value.toString()

            binding.apply {
                name.editText?.setText(mName)
                age.editText?.setText(mAge)
                gender.editText?.setText(mGender)
                email.editText?.setText(mEmail)

                countryCodePicker.setCountryForPhoneCode(mMobileNo.substring(1,3).toInt())
                mobileNo.editText?.setText(mMobileNo.substring(3))

                progressBar.visibility = View.GONE
                update.visibility = View.GONE
            }

        }
       binding.apply {
           name.editText?.doOnTextChanged { _, _, _, _ ->
               update.visibility = View.VISIBLE
           }
           age.editText?.doOnTextChanged { _, _, _, _ ->
               update.visibility = View.VISIBLE
           }
           gender.editText?.doOnTextChanged { _, _, _, _ ->
               update.visibility = View.VISIBLE
           }
           mobileNo.editText?.doOnTextChanged { _, _, _, _ ->
               update.visibility = View.VISIBLE
           }
           countryCodePicker.setOnCountryChangeListener {
               update.visibility = View.VISIBLE
           }
       }
        binding.resetPassword.setOnClickListener {
            startActivity(Intent(activity, ForgotPassword::class.java))
        }
        binding.update.setOnClickListener {

            binding.progressBar.visibility = View.VISIBLE
            val conn = Connection()
            if (!conn.checkForInternet(requireContext())) showCustomDialog()
            else if(validate() && validatePhoneNo()){
                val email = binding.email.editText?.text.toString().trim()
                val name = binding.name.editText?.text.toString().trim()
                val age = binding.age.editText?.text.toString().trim()
                val countryCode = binding.countryCodePicker.defaultCountryCodeAsInt
                val mobileNo = binding.mobileNo.editText?.text.toString().trim()
                val gender = binding.gender.editText?.text.toString().trim()
                val userId = mAuth.currentUser
                val userIdString = userId!!.uid
                writeNewUser(userIdString,name, email, gender, "+$countryCode$mobileNo", age)
            }

            binding.progressBar.visibility = View.GONE
        }

        binding.logout.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setMessage("Are you sure?")
                .setCancelable(false)
                .setPositiveButton(
                    "Logout"
                ) { _: DialogInterface?, _: Int ->
                    val mUser: FirebaseUser? = mAuth.currentUser
                    if (mUser != null) {
                        mAuth.signOut()
                        val intent = Intent(activity, WelcomeScreen::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                    } else {
                        Toast.makeText(activity, "Some error occurred. Please contact us", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(
                    "cancel"
                ) { _: DialogInterface, _: Int -> }
            builder.show()
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
        database.child("Users").child(userId).setValue(user).addOnSuccessListener {

            Toast.makeText(activity, "Updated Successfully",Toast.LENGTH_SHORT).show()
        }
    }

    private fun validate(): Boolean{
        binding.apply {
            val fullName = name.editText?.text.toString().trim()
            val mAge = age.editText?.text.toString().trim()
            val mGender = gender.editText?.text.toString().trim()
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