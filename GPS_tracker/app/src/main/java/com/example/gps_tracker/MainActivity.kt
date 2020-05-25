package com.example.gps_tracker

import android.Manifest
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
import java.io.IOException
import java.util.*


const val EXTRA_MESSAGE = "com.example.gps_tracker.MESSAGE"
const val GPS_ENABLE_REQUEST_CODE = 2001
const val PERMISSIONS_REQUEST_CODE = 100

class MainActivity : AppCompatActivity() {
    var REQUIRED_PERMISSIONS = arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting()
        }else {
            checkRunTimePermission();
        }

        val textviewaddress = findViewById<TextView>(R.id.textView)
        val showLocationButton = findViewById<Button>(R.id.button)
        val clickListener = View.OnClickListener { view-> //view를 딱히 사용은 안해서 그냥 아무것도 안해놨는데 나중에 view가 필요하면 update
            val gpsTracker = GpsTracker(this@MainActivity)

            val latitude = gpsTracker.getLatitude()
            val longitude = gpsTracker.getLongitude()

            val address = getCurrentAddress(latitude, longitude)
            textviewaddress.text = address

            Toast.makeText(this@MainActivity, "Location: \nlatitude $latitude\nlongitude $longitude", Toast.LENGTH_LONG).show()
        }
        showLocationButton.setOnClickListener(clickListener)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size){
            // If the requested code equals PERMISSIONS_REQUEST_CODE, and the number of received equals required_permission
            var check = true
            // Check all the permissions 모든 퍼미션을 허용했는지 체크합니다.

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check = false
                    break;
                }
            }
            if(!check){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(this@MainActivity, "Permission denied. Restart the Application.", Toast.LENGTH_LONG).show();
                    finish();


                }else {

                    Toast.makeText(this@MainActivity,"Permission denied. You need to change your setting to get permission.", Toast.LENGTH_LONG).show();

                }
            }
        }
    }

    fun checkRunTimePermission(){
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
        }
        else{
            // Request permission to the user to run the app.
            Toast.makeText(this@MainActivity, "Need a permission to GPS to run this application.", Toast.LENGTH_LONG).show();
            // Request result is received at onRequestPermissionResult.
            ActivityCompat.requestPermissions(this@MainActivity, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
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
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this@MainActivity)
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
}
