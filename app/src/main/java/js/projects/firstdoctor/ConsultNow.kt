package js.projects.firstdoctor

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import js.projects.firstdoctor.databinding.ActivityConsultNowBinding
import js.projects.firstdoctor.model.Consult
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ConsultNow : AppCompatActivity() {

    companion object {
        private const val LOG_TAG = "AudioRecordTest"
    }

    private lateinit var storage: StorageReference
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissionToCameraAccepted = false
    private var permissionToReadStorage = false
    private var permissionToWriteStorage = false
    private var permissionToManageStorage = false
    private lateinit var binding: ActivityConsultNowBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: DatabaseReference


    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private fun requestPermission() {
        permissionToRecordAccepted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        permissionToCameraAccepted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        permissionToReadStorage = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionToManageStorage = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            permissionToWriteStorage = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        val permissionRequest: MutableList<String> = ArrayList()
        if (!permissionToRecordAccepted) {
            permissionRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!permissionToCameraAccepted) {
            permissionRequest.add(Manifest.permission.CAMERA)
        }
        if (!permissionToReadStorage) {
            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!permissionToManageStorage)
                permissionRequest.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            if (!permissionToWriteStorage) {
                permissionRequest.add(Manifest.permission.CAMERA)
            }
        }

        if (permissionRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }

    }


    private fun startPlaying(fileName: String) {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }

    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private fun startRecording(fileName: String) {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
                start()

            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed $e")
            }
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }


    private lateinit var fileComp: String
    private lateinit var fileHistory: String
    private var pic1Uri: Uri? = null
    private var pic2Uri: Uri? = null
    private var pic3Uri: Uri? = null
    private var pdfUri: Uri? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultNowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fileComp = externalCacheDir?.absolutePath + "/recComp.mp3"
        fileHistory = externalCacheDir?.absolutePath + "/recHistory.mp3"

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

                permissionToRecordAccepted =
                    permissions[Manifest.permission.RECORD_AUDIO] ?: permissionToRecordAccepted
                permissionToCameraAccepted =
                    permissions[Manifest.permission.CAMERA] ?: permissionToCameraAccepted
                permissionToReadStorage = permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                    ?: permissionToReadStorage

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    permissionToManageStorage =
                        permissions[Manifest.permission.MANAGE_EXTERNAL_STORAGE]
                            ?: permissionToManageStorage
                } else {
                    permissionToWriteStorage =
                        permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                            ?: permissionToWriteStorage
                }

            }

        requestPermission()

        var mRecComplaint = false
        var mPlayComplaint = false
        var mPlayHistory = false
        var mRecordedComp = false
        var mRecHistory = false
        var mRecordedHistory = false

        mAuth = Firebase.auth
        database = Firebase.database.reference
        storage = FirebaseStorage.getInstance().getReference("Users")

        binding.apply {

            playComplaint.visibility = View.GONE
            playHistory.visibility = View.GONE
//            deletePic.visibility = View.GONE

            timerComp.visibility = View.GONE
            timerHistory.visibility = View.GONE
            progressBar.visibility = View.GONE

            deletePic1.visibility = View.GONE
            deletePic2.visibility = View.GONE
            deletePic3.visibility = View.GONE
            deleteFile.visibility = View.GONE

            problemLocation.editText?.setOnClickListener {
                var selectedIndex = 0
                val problem = arrayOf("Oral", "Slein", "Secondary Oninion")
                with(AlertDialog.Builder(this@ConsultNow)) {
                    setTitle("where is the problem")
                    setSingleChoiceItems(
                        problem,
                        0
                    ) { _, i -> selectedIndex = i }
                        .setPositiveButton("Done") { _, _ ->
                            problemLocation.editText?.setText(
                                problem[selectedIndex]
                            )
                        }
                    create()
                }.show()
            }

            recordComplaint.setOnClickListener {
                if (mRecordedComp) {
                    recordComplaint.setImageResource(R.drawable.mic_rec)
                    playComplaint.visibility = View.GONE
                    timerComp.visibility = View.GONE
                    chiefComplaint.visibility = View.VISIBLE
                    mRecordedComp = !mRecordedComp
                    mRecComplaint = !mRecComplaint
                } else if (!mRecComplaint && !mRecordedComp) {
                    startRecording(fileComp)
                    chiefComplaint.visibility = View.GONE
                    timerComp.visibility = View.VISIBLE
                    timerComp.base = SystemClock.elapsedRealtime()
                    timerComp.start()
                    recordComplaint.setImageResource(R.drawable.mic_start)
                } else {
                    stopRecording()
                    timerComp.stop()
                    recordComplaint.setImageResource(R.drawable.delete)
                    playComplaint.visibility = View.VISIBLE
                    mRecordedComp = true
                }
                mRecComplaint = !mRecComplaint
            }
            recordHistory.setOnClickListener {
                if (mRecordedHistory) {
                    recordHistory.setImageResource(R.drawable.mic_rec)
                    playHistory.visibility = View.GONE
                    timerHistory.visibility = View.GONE
                    medicalHistory.visibility = View.VISIBLE
                    mRecordedHistory = !mRecordedHistory
                    mRecHistory = !mRecHistory
                } else if (!mRecHistory && !mRecordedHistory) {
                    startRecording(fileHistory)
                    medicalHistory.visibility = View.GONE
                    timerHistory.visibility = View.VISIBLE
                    timerHistory.base = SystemClock.elapsedRealtime()
                    timerHistory.start()
                    recordHistory.setImageResource(R.drawable.mic_start)
                } else {
                    stopRecording()
                    timerHistory.stop()
                    recordHistory.setImageResource(R.drawable.delete)
                    playHistory.visibility = View.VISIBLE
                    mRecordedHistory = true
                }
                mRecHistory = !mRecHistory
            }

            playComplaint.setOnClickListener {
                if (!mPlayComplaint) {
                    playComplaint.setImageResource(R.drawable.stop)
                    startPlaying(fileComp)
                    player?.setOnCompletionListener {
                        playComplaint.setImageResource(R.drawable.play)
                    }
                } else {
                    stopPlaying()
                    playComplaint.setImageResource(R.drawable.play)
                }
                mPlayComplaint = !mPlayComplaint
            }

            playHistory.setOnClickListener {
                if (!mPlayHistory) {
                    playHistory.setImageResource(R.drawable.stop)
                    startPlaying(fileHistory)
                    player?.setOnCompletionListener {
                        playHistory.setImageResource(R.drawable.play)
                    }
                } else {
                    stopPlaying()
                    playHistory.setImageResource(R.drawable.play)
                }
                mPlayHistory = !mPlayHistory
            }

            pic1.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                pic1ResultLauncher.launch(intent)
            }
            pic2.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                pic2ResultLauncher.launch(intent)
            }
            pic3.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                pic3ResultLauncher.launch(intent)
            }
            pdfFile.editText?.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "application/pdf"
                pdfFileResultLauncher.launch(intent)
            }
            deletePic1.setOnClickListener {
                pic1Uri = null
                pic1.setImageResource(R.drawable.add_image)
                deletePic1.visibility = View.GONE
            }
            deletePic2.setOnClickListener {
                pic2Uri = null
                pic2.setImageResource(R.drawable.add_image)
                deletePic2.visibility = View.GONE
            }
            deletePic3.setOnClickListener {
                pic3Uri = null
                pic3.setImageResource(R.drawable.add_image)
                deletePic3.visibility = View.GONE
            }
            deleteFile.setOnClickListener {
                pdfUri = null
                pdfFile.editText?.text!!.clear()
                deleteFile.visibility = View.GONE
            }
            upload.setOnClickListener {
                if (checkFields()) {
                    upload()
                    }
                }
            }
        
    }

    private var pic1ResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            if(it.data != null){
                pic1Uri = it.data!!.data
                binding.pic1.setImageURI(pic1Uri)
                binding.deletePic1.visibility = View.VISIBLE
            }
        }
    }
    private var pic2ResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            if(it.data != null){
                pic2Uri = it.data!!.data
                binding.pic2.setImageURI(pic2Uri)
                binding.deletePic2.visibility = View.VISIBLE
            }
        }
    }
    private var pic3ResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            if(it.data != null){
                pic3Uri = it.data!!.data
                binding.pic3.setImageURI(pic3Uri)
                binding.deletePic3.visibility = View.VISIBLE
            }
        }
    }
    private var pdfFileResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            if(it.data != null){
                pdfUri = it.data!!.data
                binding.pdfFile.editText?.setText(pdfUri.toString())
                binding.deleteFile.visibility = View.VISIBLE
            }
        }
    }

    private fun checkFields(): Boolean {
        binding.apply {
            if (chiefComplaint.isVisible) {
                val complaint = chiefComplaint.editText?.text.toString().trim()
                if (complaint.isEmpty()) {
                    chiefComplaint.error = "Field can't be empty"
                    return false
                } else {
                    chiefComplaint.error = null
                    chiefComplaint.isErrorEnabled = false
                }
            }
            if (medicalHistory.isVisible) {
                val history = medicalHistory.editText?.text.toString().trim()
                if (history.isEmpty()) {
                    medicalHistory.error = "Field can't be empty"
                    return false
                } else {
                    medicalHistory.error = null
                    medicalHistory.isErrorEnabled = false
                }
            }
/*val image = pic.editText?.text.toString().trim()
if(image.isEmpty()){
   problemPic.error = "Please select images"
   return false
}else{
   problemPic.error = null
   problemPic.isErrorEnabled = false
}*/
            if (problemLocation.isEmpty()) {
                problemLocation.error = "Field can't be empty"
                return false
            } else {
                problemLocation.error = null
                problemLocation.isErrorEnabled = false
            }
        }
        return true
    }

    private fun upload(){
        val userid = mAuth.currentUser
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss", Locale.US)
        val dateString = simpleDateFormat.format(System.currentTimeMillis())
        var chiefConsult:String? = null
        var medicalHist: String? = null

        binding.apply {
            if(pdfFile.editText?.text!!.isNotEmpty()){
                progressBar.visibility = View.VISIBLE
                val ref = storage.child("${userid!!.uid}/${dateString}/${pdfUri?.lastPathSegment}")
                val uploadTask = pdfUri?.let { ref.putFile(it) }
                uploadTask?.addOnFailureListener{
                    Toast.makeText(this@ConsultNow,"Something went wrong!", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }?.addOnSuccessListener {
                    Toast.makeText(this@ConsultNow,"${pdfUri?.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE

                    val intent = Intent(this@ConsultNow, HomePatient::class.java)
                    startActivity(intent)
                    finish()

                }
            }
            if(pic1Uri != null){
                val ref = storage.child("${userid!!.uid}/${dateString}/${pic1Uri?.lastPathSegment}")
                val uploadTask = pic1Uri?.let { ref.putFile(it)}
                uploadTask?.addOnFailureListener{
                    Toast.makeText(this@ConsultNow, "Something went wrong!", Toast.LENGTH_SHORT).show()
                }?.addOnSuccessListener {
                    Toast.makeText(this@ConsultNow,"${pic1Uri?.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
            if(pic2Uri != null){
                val ref = storage.child("${userid!!.uid}/${dateString}/${pic2Uri?.lastPathSegment}")
                val uploadTask = pic2Uri?.let { ref.putFile(it)}
                uploadTask?.addOnFailureListener{
                    Toast.makeText(this@ConsultNow, "Something went wrong!", Toast.LENGTH_SHORT).show()
                }?.addOnSuccessListener {
                    Toast.makeText(this@ConsultNow,"${pic2Uri?.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
            if(pic3Uri != null){
                val ref = storage.child("${userid!!.uid}/${dateString}/${pic3Uri?.lastPathSegment}")
                val uploadTask = pic3Uri?.let { ref.putFile(it)}
                uploadTask?.addOnFailureListener{
                    Toast.makeText(this@ConsultNow, "Something went wrong!", Toast.LENGTH_SHORT).show()
                }?.addOnSuccessListener {
                    Toast.makeText(this@ConsultNow,"${pic3Uri?.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
            if(timerComp.isVisible){
                val file1 = Uri.fromFile(File(fileComp))
                val ref = storage.child("${userid!!.uid}/${dateString}/${file1.lastPathSegment}")
                val uploadTask = ref.putFile(file1)
                uploadTask.addOnFailureListener{
                    Toast.makeText(this@ConsultNow,"Something went wrong!", Toast.LENGTH_SHORT).show()
                }.addOnSuccessListener {
                    Toast.makeText(this@ConsultNow,"${file1.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                chiefConsult = chiefComplaint.editText?.text.toString()
            }
            if(timerHistory.isVisible){
                val file2 = Uri.fromFile(File(fileHistory))
                val ref = storage.child("${userid!!.uid}/${dateString}/${file2.lastPathSegment}")
                val uploadTask = ref.putFile(file2)
                uploadTask.addOnFailureListener{
                    Toast.makeText(this@ConsultNow,"Something went wrong!", Toast.LENGTH_SHORT).show()
                }.addOnSuccessListener {
                    Toast.makeText(this@ConsultNow,"${file2.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            }else{
                medicalHist = medicalHistory.editText?.text.toString()
            }

            val consult: Consult = if(chiefConsult != null && medicalHist != null){
                Consult(chiefConsult, medicalHist, problemLocation.editText?.text.toString())
            }else if (chiefConsult != null){
                Consult(chiefComplaint = chiefConsult, problemPlace = problemLocation.editText?.text.toString())
            } else if (medicalHist != null){
                Consult(medicalHistory = medicalHist, problemPlace = problemLocation.editText?.text.toString())
            } else{
                Consult(problemPlace = problemLocation.editText?.text.toString())
            }

            database.child("Users").child(userid!!.uid).child(dateString).setValue(consult).addOnSuccessListener {
                Toast.makeText(this@ConsultNow, "Uploaded Successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
}