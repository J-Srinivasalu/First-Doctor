package js.projects.firstdoctor.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import js.projects.firstdoctor.R
import js.projects.firstdoctor.databinding.FragmentConsultationBinding
import js.projects.firstdoctor.model.Consult
import js.projects.firstdoctor.utils.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


class Consultation : Fragment(R.layout.fragment_consultation) {

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
    private lateinit var binding: FragmentConsultationBinding
    private lateinit var pdfFile: File
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: DatabaseReference


    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private fun requestPermission(){
        permissionToRecordAccepted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        permissionToCameraAccepted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        permissionToReadStorage = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionToManageStorage = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }else{
            permissionToWriteStorage = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        val permissionRequest: MutableList<String> = ArrayList()
        if(!permissionToRecordAccepted){
            permissionRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if(!permissionToCameraAccepted){
            permissionRequest.add(Manifest.permission.CAMERA)
        }
        if(!permissionToReadStorage){
            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!permissionToManageStorage)
            permissionRequest.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        }else{
            if(!permissionToWriteStorage){
                permissionRequest.add(Manifest.permission.CAMERA)
            }
        }

        if(permissionRequest.isNotEmpty()){
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }

    }



    private fun startPlaying(fileName:String) {
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentConsultationBinding.bind(view)


        fileComp = requireContext().externalCacheDir?.absolutePath + "/recComp.mp3"
        fileHistory = requireContext().externalCacheDir?.absolutePath + "/recHistory.mp3"

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->

            permissionToRecordAccepted = permissions[Manifest.permission.RECORD_AUDIO] ?: permissionToRecordAccepted
            permissionToCameraAccepted = permissions[Manifest.permission.CAMERA] ?: permissionToCameraAccepted
            permissionToReadStorage = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: permissionToReadStorage

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                permissionToManageStorage = permissions[Manifest.permission.MANAGE_EXTERNAL_STORAGE] ?: permissionToManageStorage
            }else{
                permissionToWriteStorage = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: permissionToWriteStorage
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
            deletePic.visibility = View.GONE

            timerComp.visibility = View.GONE
            timerHistory.visibility = View.GONE
            progressBar.visibility = View.GONE

            problemLocation.editText?.setOnClickListener {
                var selectedIndex = 0
                val problem = arrayOf("Oral", "Slein", "Secondary Oninion")
                with(AlertDialog.Builder(requireContext())) {
                    setTitle("Select Gender")
                    setSingleChoiceItems(
                        problem,
                        0
                    ) { _, i -> selectedIndex = i}
                        .setPositiveButton("Done"){_,_ -> problemLocation.editText?.setText(problem[selectedIndex])}
                    create()
                }.show()
            }

            recordComplaint.setOnClickListener {
                if(mRecordedComp){
                    recordComplaint.setImageResource(R.drawable.mic_rec)
                    playComplaint.visibility = View.GONE
                    timerComp.visibility = View.GONE
                    chiefComplaint.visibility = View.VISIBLE
                    mRecordedComp = !mRecordedComp
                    mRecComplaint = !mRecComplaint
                }
                else if (!mRecComplaint && !mRecordedComp) {
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
                if(mRecordedHistory){
                    recordHistory.setImageResource(R.drawable.mic_rec)
                    playHistory.visibility = View.GONE
                    timerHistory.visibility = View.GONE
                    medicalHistory.visibility = View.VISIBLE
                    mRecordedHistory = !mRecordedHistory
                    mRecHistory = !mRecHistory
                }
                else if (!mRecHistory && !mRecordedHistory) {
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
                if(!mPlayComplaint){
                    playComplaint.setImageResource(R.drawable.stop)
                    startPlaying(fileComp)
                    player?.setOnCompletionListener {
                        playComplaint.setImageResource(R.drawable.play)
                    }
                }else{
                    stopPlaying()
                    playComplaint.setImageResource(R.drawable.play)
                }
                mPlayComplaint = !mPlayComplaint
            }

            playHistory.setOnClickListener {
                if(!mPlayHistory){
                    playHistory.setImageResource(R.drawable.stop)
                    startPlaying(fileHistory)
                    player?.setOnCompletionListener {
                        playHistory.setImageResource(R.drawable.play)
                    }
                }else{
                    stopPlaying()
                    playHistory.setImageResource(R.drawable.play)
                }
                mPlayHistory = !mPlayHistory
            }

            problemPic.editText?.setOnClickListener {


                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.type = "image/*"
                resultLauncher.launch(intent)

            }

            deletePic.setOnClickListener {
                problemPic.editText?.setText("")
                deletePic.visibility = View.GONE
            }

            upload.setOnClickListener {
                if(checkFields()){
                    upload()
                }
            }


        }
    }

    private var imageArray = ArrayList<File>()

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == Activity.RESULT_OK){
            if(it.data?.clipData != null){
                val count = it.data!!.clipData!!.itemCount
                for (i in 0 until count){
                    imageArray.add(getFileFromUri(it.data!!.clipData!!.getItemAt(i).uri)!!)
                }
                createPdf(imageArray)
            }
        }
    }


    private fun getFileFromUri(uri: Uri): File? {
        if (uri.path == null) {
            return null
        }
        var realPath = String()
        val databaseUri: Uri
        val selection: String?
        val selectionArgs: Array<String>?
        if (uri.path!!.contains("/document/image:")) {
            databaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            selection = "_id=?"
            selectionArgs = arrayOf(DocumentsContract.getDocumentId(uri).split(":")[1])
        } else {
            databaseUri = uri
            selection = null
            selectionArgs = null
        }
        try {
            val column = "_data"
            val projection = arrayOf(column)
            val cursor = requireContext().contentResolver.query(
                databaseUri,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.let {
                if (it.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    realPath = cursor.getString(columnIndex)
                }
                cursor.close()
            }
        } catch (e: Exception) {
            Log.i("GetFileUri Exception:", e.message ?: "")
        }
        val path = realPath.ifEmpty {
            when {
                uri.path!!.contains("/document/raw:") -> uri.path!!.replace(
                    "/document/raw:",
                    ""
                )
                uri.path!!.contains("/document/primary:") -> uri.path!!.replace(
                    "/document/primary:",
                    "/storage/emulated/0/"
                )
                else -> return null
            }
        }
        return File(path)
    }

    private fun checkFields(): Boolean{
        binding.apply{
            if(chiefComplaint.isVisible){
                val complaint = chiefComplaint.editText?.text.toString().trim()
                if(complaint.isEmpty()){
                    chiefComplaint.error = "Field can't be empty"
                    return false
                }else{
                    chiefComplaint.error = null
                    chiefComplaint.isErrorEnabled = false
                }
            }
            if(medicalHistory.isVisible){
                val history = medicalHistory.editText?.text.toString().trim()
                if(history.isEmpty()){
                    medicalHistory.error = "Field can't be empty"
                    return false
                }else{
                    medicalHistory.error = null
                    medicalHistory.isErrorEnabled = false
                }
            }
            val image = problemPic.editText?.text.toString().trim()
            if(image.isEmpty()){
                problemPic.error = "Please select images"
                return false
            }else{
                problemPic.error = null
                problemPic.isErrorEnabled = false
            }
            if(problemLocation.isEmpty()){
                problemLocation.error = "Field can't be empty"
                return false
            }else{
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
            if(problemPic.isNotEmpty()){
                progressBar.visibility = View.VISIBLE
                Log.i(LOG_TAG, "upload: ${pdfFile.path}")
                val file3 = Uri.fromFile(File(pdfFile.path))
                val ref = storage.child("${userid!!.uid}/${file3.lastPathSegment}")
                val uploadTask = ref.putFile(file3)
                uploadTask.addOnFailureListener{
                    Toast.makeText(requireContext(),"Something went wrong!", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }.addOnSuccessListener {
                    Toast.makeText(requireContext(),"${file3.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
            if(timerComp.isVisible){
                val file1 = Uri.fromFile(File(fileComp))
                val ref = storage.child("${userid!!.uid}/${file1.lastPathSegment}")
                val uploadTask = ref.putFile(file1)
                uploadTask.addOnFailureListener{
                    Toast.makeText(requireContext(),"Something went wrong!", Toast.LENGTH_SHORT).show()
                }.addOnSuccessListener {
                    Toast.makeText(requireContext(),"${file1.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                chiefConsult = chiefComplaint.editText?.text.toString()
            }
            if(timerHistory.isVisible){
                val file2 = Uri.fromFile(File(fileHistory))
                val ref = storage.child("${userid!!.uid}/${file2.lastPathSegment}")
                val uploadTask = ref.putFile(file2)
                uploadTask.addOnFailureListener{
                    Toast.makeText(requireContext(),"Something went wrong!", Toast.LENGTH_SHORT).show()
                }.addOnSuccessListener {
                    Toast.makeText(requireContext(),"${file2.lastPathSegment} uploaded successfully", Toast.LENGTH_SHORT).show()
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

                Toast.makeText(activity, "Uploaded Successfully",Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun createPdf(data:ArrayList<File>){
        binding.progressBar.visibility = View.VISIBLE
        pdfFile = File(requireContext().externalCacheDir
            !!.absolutePath + File.separator + "ProblemPics"
        + System.currentTimeMillis() + ".pdf"
        )
        Toast.makeText(requireContext(), "Creating PDF, Please wait...", Toast.LENGTH_SHORT).show()
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            //Background work here
            val document = PdfDocument()
            try {
                for (item in data) {
                    Log.e(LOG_TAG, "createPdf: ${item.absolutePath}")
                    val bitmap = BitmapFactory.decodeFile(item.absolutePath)
                    val pageInfo = PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas
                    val paint = Paint()
                    paint.color = Color.parseColor("#ffffff")
                    canvas.drawPaint(paint)
                    canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)
                    document.finishPage(page)
                }
                document.writeTo(FileOutputStream(pdfFile))
            } catch (e: IOException){
                e.printStackTrace()
            }finally {
                document.close()
            }

            handler.post {
                if(pdfFile.exists() && pdfFile.length()>0) {
                    FileUtil().openFile(requireContext(), pdfFile.absolutePath) // See: https://gist.github.com/omkar-tenkale/34d3aa1966653e6949d1ddaee1ba3355
                    binding.problemPic.editText?.setText(pdfFile.toString())
                    binding.progressBar.visibility = View.GONE
                }else {
                    Toast.makeText(requireContext(), "Something went wrong creating the PDF :(", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


}