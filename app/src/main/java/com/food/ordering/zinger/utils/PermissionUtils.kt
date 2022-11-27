package com.food.ordering.zinger.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.food.ordering.zinger.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PermissionUtils {
    /**
     * Function to request permission from the user
     */
    fun requestAccessFineLocationPermission(activity: AppCompatActivity, requestId: Int) {
        ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestId
        )
    }

    /**
     * Function to check if the location permissions are granted or not
     */
    fun isAccessFineLocationGranted(context: Context): Boolean {
        return ContextCompat
                .checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Function to check if location of the device is enabled or not
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager: LocationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Function to show the "enable GPS" Dialog box
     */
    fun showGPSNotEnabledDialog(context: Context) {
//        AlertDialog.Builder(context)
//                .setIcon(R.drawable.location_pin)
//                .setTitle(context.getString(R.string.enable_gps))
//                .setMessage(context.getString(R.string.required_for_this_app))
//                .setCancelable(true)
//                .setPositiveButton(context.getString(R.string.enable_now)) { _, _ ->
//                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//                }
//
//                .show()

        MaterialAlertDialogBuilder(context)
                .setIcon(R.drawable.map_marker)
                .setTitle(context.getString(R.string.enable_gps))
                .setMessage(context.getString(R.string.required_for_this_app))
                .setCancelable(true)
                .setPositiveButton(context.getString(R.string.enable_now)) { dialog, which ->
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
//                .setNegativeButton("No") { dialog, which -> dialog.dismiss() }
                .show()
    }
}