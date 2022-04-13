package js.projects.firstdoctor

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import js.projects.firstdoctor.databinding.ActivityHomePatientBinding
import js.projects.firstdoctor.fragments.*
import js.projects.firstdoctor.utils.Connection

class HomePatient : AppCompatActivity() {

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var binding:ActivityHomePatientBinding
    private lateinit var mAuth:FirebaseAuth
    private var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePatientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mAuth = Firebase.auth

        setSupportActionBar(binding.toolbar)
        drawerToggle = ActionBarDrawerToggle(this, binding.drawerLayout,binding.toolbar,
            R.string.open,
            R.string.close
        )

        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setUpDrawerContent(binding.navView)

        val fragmentClass: Class<*> = Home::class.java

        try{
            fragment = fragmentClass.newInstance() as Fragment?
        }catch (e: Exception) {
            e.printStackTrace()
        }

        supportFragmentManager.beginTransaction().replace(R.id.frameLayout, fragment!!).commit()
        title = getString(R.string.home)
        binding.navView.setCheckedItem(R.id.home)

    }
    private fun setUpDrawerContent(navView: NavigationView) {
        navView.setNavigationItemSelectedListener {menuItem ->
            selectDrawerItem(menuItem)
            true
        }
    }

    private fun selectDrawerItem(menuItem: MenuItem) {

        val fragmentClass: Class<*>
        when(menuItem.itemId){
            R.id.home ->
                fragmentClass = Home::class.java
            R.id.consultNow ->
                fragmentClass = Consultation::class.java
            R.id.notification ->
                fragmentClass = Notification::class.java
            R.id.research ->
                fragmentClass = Research::class.java
            R.id.myComplaints ->
                fragmentClass = Complaints::class.java
            R.id.profile -> {
                val conn = Connection()
                if (!conn.checkForInternet(this)) {
                    fragmentClass = Home::class.java
                    showCustomDialog()
                }
                else{
                    fragmentClass = Profile::class.java
                }
            }
            else ->
                fragmentClass = Home::class.java
        }
        try{
            fragment = fragmentClass.newInstance() as Fragment?
        }catch (e: Exception) {
            e.printStackTrace()
        }
        val fragmentManager: FragmentManager = supportFragmentManager
        if (fragment != null) {
            fragmentManager.beginTransaction().replace(R.id.frameLayout, fragment!!).commit()
        }
        menuItem.isChecked = true
        title = menuItem.title
        binding.drawerLayout.closeDrawers()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            android.R.id.home ->{
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)) binding.drawerLayout.closeDrawer(
            GravityCompat.START)
        else super.onBackPressed()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
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