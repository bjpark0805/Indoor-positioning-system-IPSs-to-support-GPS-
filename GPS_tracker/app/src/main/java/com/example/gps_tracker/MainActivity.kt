package com.example.gps_tracker

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timer


const val EXTRA_MESSAGE = "com.example.gps_tracker.MESSAGE"
const val GPS_ENABLE_REQUEST_CODE = 2001
const val PERMISSIONS_REQUEST_CODE = 100


class MainActivity : AppCompatActivity() {
    var REQUIRED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private var time = 0
    private var isRunning = false
    private var timerTask: Timer? = null
    private var lap = 1
    private var lastTimeBackPressed: Long = -1500

    public lateinit var adapter: MyAdapter

    private val coordinationList: ArrayList<String> = ArrayList<String>()

//    val path = "/data/data/GPS_tracker/files/experiment/${current_time}.txt"


    private fun start(file: File) {
        play_fab.setImageResource(R.drawable.ic_pause)
        val textView = TextView(this)
        val gpsTracker = GpsTracker(this@MainActivity)
        timerTask = timer(period = 1000) {
            time++

            val latitude = gpsTracker.getLatitude()
            val longitude = gpsTracker.getLongitude()

            val coordinate = "Location: latitude $latitude, longitude $longitude"
            textView.text = coordinate
            val now = System.currentTimeMillis()
            val date = Date(now)
            val sdfNow = SimpleDateFormat("yy_MM_dd_HH_mm_ss")
            val currentTime = sdfNow.format(date) // filename String
            try {

                val buf = BufferedWriter(FileWriter(file, true))
                buf.append("$currentTime : $coordinate")
                buf.newLine()
                buf.close()


            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            lap++

            runOnUiThread {
                Toast.makeText(this@MainActivity, coordinate, Toast.LENGTH_LONG).show()
                // file write??
                adapter.addData("$currentTime\n$coordinate")

            }

        }
    }

    private fun pause() {
        play_fab.setImageResource(R.drawable.ic_stat_name)
        // Timer객체의 cancel메소드로 타이머 중지 가능
        timerTask?.cancel()
    }

    private fun reset() {
        timerTask?.cancel()

        if (isRunning)
            isRunning = false

        time = 0
        play_fab.setImageResource(R.drawable.ic_stat_name) // ic_stat_name == play (I misnamed the icon)
//        lapLayout.removeAllViews()
        lap = 1

        adapter.clearData()
        // File write all the things in lapLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val now1 = System.currentTimeMillis()
        val date1 = Date(now1)
        val sdfNow1 = SimpleDateFormat("yy_MM_dd")
        val today = sdfNow1.format(date1) // filename String
        val filename = "/${today}.txt"
        val FileDirectory = getExternalFilesDir(null)
        val file = File(FileDirectory, filename)
        if (file.exists()) {
            Toast.makeText(this@MainActivity, filename, Toast.LENGTH_LONG).show()

        }

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting()
        } else {
            checkRunTimePermission();
        }

//        val textviewaddress = findViewById<TextView>(R.id.textView)

        // ShowLocationButton is for the test.
        val showLocationButton = findViewById<Button>(R.id.button)

        val clickListener =
            View.OnClickListener { view -> //view를 딱히 사용은 안해서 그냥 아무것도 안해놨는데 나중에 view가 필요하면 update
                val gpsTracker = GpsTracker(this@MainActivity)

                val latitude = gpsTracker.getLatitude()
                val longitude = gpsTracker.getLongitude()

//            val address = getCurrentAddress(latitude, longitude)
//            textviewaddress.text = address
                val string = "Location: \nlatitude $latitude\nlongitude $longitude"
                Toast.makeText(this@MainActivity, string, Toast.LENGTH_LONG).show()
            }

        showLocationButton.setOnClickListener(clickListener)

        play_fab.setOnClickListener {
            when (isRunning) {
                false -> start(file)
                true -> pause()
            }
            isRunning = !isRunning
        }

        clear.setOnClickListener {
            reset()
        }

        resultList.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.VERTICAL, false
        );
        adapter = MyAdapter(coordinationList)
        resultList.adapter = adapter;
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size) {
            // If the requested code equals PERMISSIONS_REQUEST_CODE, and the number of received equals required_permission
            var check = true
            // Check all the permissions 모든 퍼미션을 허용했는지 체크합니다.

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check = false
                    break;
                }
            }
            if (!check) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS[0]
                    )
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS[1]
                    )
                ) {

                    Toast.makeText(
                        this@MainActivity,
                        "Permission denied. Restart the Application.",
                        Toast.LENGTH_LONG
                    ).show();
                    finish();


                } else {

                    Toast.makeText(
                        this@MainActivity,
                        "Permission denied. You need to change your setting to get permission.",
                        Toast.LENGTH_LONG
                    ).show();

                }
            }
        }
    }

    fun checkRunTimePermission() {
        //Runtime permission check
        // 1. Check location permission available

        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // This case, already has permission, nothing to do.
        } else {
            // Request permission to the user to run the app.
            Toast.makeText(
                this@MainActivity,
                "Need a permission to GPS to run this application.",
                Toast.LENGTH_LONG
            ).show();
            // Request result is received at onRequestPermissionResult.
            ActivityCompat.requestPermissions(
                this@MainActivity,
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            );
        }
    }

    fun getCurrentAddress(latitude: Double, longitude: Double): String? {

        //Change GPS to address
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address>?
        addresses = try {
            geocoder.getFromLocation(latitude, longitude, 7)
        } catch (ioException: IOException) {
            //네트워크 문제
            Toast.makeText(this, "Geocoder service not available", Toast.LENGTH_LONG).show()
            return "Geocoder service not available"
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(this, "Wrong GPS coordinates", Toast.LENGTH_LONG).show()
            return "Wrong GPS coordinates"
        }
        if (addresses == null || addresses.isEmpty()) {
            Toast.makeText(this, "Address not found", Toast.LENGTH_LONG).show()
            return "Address not found"
        }
        val address: Address = addresses[0]
        return "${address.getAddressLine(0).toString()}\n"
    }

    private fun showDialogForLocationServiceSetting() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Address service deactivated")
        builder.setMessage(
            """
                You need address setting to use this application.
                Do you want to modify?
                """.trimIndent()
        )
        builder.setCancelable(true)
        builder.setPositiveButton("Setting", DialogInterface.OnClickListener { dialog, id ->
            val callGPSSettingIntent =
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE)
        })
        builder.setNegativeButton("취소",
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
        builder.create().show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GPS_ENABLE_REQUEST_CODE ->
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS activated")
                        checkRunTimePermission()
                        return
                    }
                }
        }
    }

    fun checkLocationServicesStatus(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    fun saveFile(inputStream: InputStream, filePath: String) {
        val saveFile = File(filePath)
        saveFile.outputStream().use { fileOutput ->
            inputStream.copyTo(fileOutput)
        }
    }
}
