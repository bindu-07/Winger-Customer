package com.food.ordering.zinger.mapPicker.permission


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker

object PermissionManagerUtil {
    private const val REQUEST_ACCESS_LOCATION = 1
    private val TAG = PermissionManagerUtil::class.java.simpleName

    var onPermissionInterfaces: HashMap<Int, ArrayList<OnPermissionInterface>?> = HashMap()
    var hasAlreadyAskedPermission: HashMap<Int, Boolean?> = HashMap()
    private var context: Activity? = null
    private var dialog: AlertDialog? = null

    fun callPermissionManagerCallBack(requestCode: Int, grantResults: IntArray) {
        if (onPermissionInterfaces.containsKey(requestCode) && onPermissionInterfaces[requestCode] != null
        ) {
            for (onPermissionInterface in onPermissionInterfaces[requestCode]!!) {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionInterface.onPermissionGranted()
                } else {
                    onPermissionInterface.onPermissionNotGranted()
                }
            }
            hasAlreadyAskedPermission.remove(requestCode)
            onPermissionInterfaces[requestCode]!!.clear()
        }
    }

    fun init(context: Activity?) {
        PermissionManagerUtil.context = context
    }

    fun requestLocationPermission(
            showAlertForSettingsIfNeeded: Boolean,
            onPermissionInterface: OnPermissionInterface?
    ) {
        if (checkPermissionForLocation()) {
            onPermissionInterface!!.onPermissionGranted()
            dialog?.cancel()
            return
        }


        if (showAlertForSettingsIfNeeded && ActivityCompat.shouldShowRequestPermissionRationale(
                        context!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
        ) {
            // if user clicked on "never ask again" then popup will not show by android
            //https://stackoverflow.com/questions/33224432/android-m-anyway-to-know-if-a-user-has-chosen-never-to-show-the-grant-permissi
            onPermissionInterface!!.onPermissionNotGranted()
            showAlertDialogWithAppSettings(
                    Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            if (!hasAlreadyAskedPermission.containsKey(
                            REQUEST_ACCESS_LOCATION
                    )) {
                hasAlreadyAskedPermission[REQUEST_ACCESS_LOCATION] = true
                ActivityCompat.requestPermissions(
                        context!!,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_ACCESS_LOCATION
                )
            }
            if (onPermissionInterfaces[REQUEST_ACCESS_LOCATION] == null) {
                onPermissionInterfaces[REQUEST_ACCESS_LOCATION] = ArrayList()
            }
            if (onPermissionInterface != null) {
                onPermissionInterfaces[REQUEST_ACCESS_LOCATION]
                        ?.add(onPermissionInterface)
            }
        }
    }

    private fun showAlertDialogWithAppSettings(permission: String) {
        showAlertDialogWithAppSettings(
                context,
                permission
        )
    }

    @SuppressLint("RestrictedApi")
    private fun showAlertDialogWithAppSettings(
            context: Activity?,
            permission: String
    ) {
        var title = "Allow permissions"
        var message = "Please allow this permission to proceed."
        when (permission) {

            Manifest.permission.RECORD_AUDIO -> {
                title = "Allow Microphone Permission"
                message = title
            }
            Manifest.permission.READ_CONTACTS -> {
                title = "Allow Contacts Permission"
                message = title
            }
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                title = "Allow Storage Permission"
                message = title
            }
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> {
                title = "Allow Location Permission"
                message = title
            }
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(context!!)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Go to settings") { dialog, which ->
            val intent = Intent()
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri: Uri = Uri.fromParts("package", context.getPackageName(), null)
            intent.setData(uri)
            dialog.cancel()
            context.startActivity(intent)
        }
        builder.setNegativeButton("Cancel", { dialog, which -> dialog.dismiss() })
        dialog = builder.create()
        dialog!!.show()
    }


    private fun checkPermissionForLocation(): Boolean {
        val result: Int =
                PermissionChecker.checkSelfPermission(
                        context!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsEnabled(): Boolean {
        var enabled = false
        try {
            val locationManager =
                    context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
        }
        try {
            if (!enabled) {
                val provider = Settings.Secure.getString(
                        context!!.contentResolver,
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED
                )
                enabled = provider.contains("gps")
            }
        } catch (e: Exception) {
        }
        try {
            if (!enabled) {
                enabled =
                        isGpsEnabled(
                                context!!
                        )
            }
        } catch (e: Exception) {
        }
        try {
            if (!enabled) {
                val provider = Settings.Secure.getString(
                        context!!.contentResolver,
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED
                )
                enabled = provider != ""
            }
        } catch (e: Exception) {
        }
        return enabled
    }

    private fun isGpsEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val providers = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED
            )
            if (TextUtils.isEmpty(providers)) {
                false
            } else providers.contains(LocationManager.GPS_PROVIDER)
        } else {
            val locationMode: Int
            locationMode = try {
                Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.LOCATION_MODE
                )
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
                return false
            }
            when (locationMode) {
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY, Settings.Secure.LOCATION_MODE_SENSORS_ONLY -> true
                Settings.Secure.LOCATION_MODE_BATTERY_SAVING, Settings.Secure.LOCATION_MODE_OFF -> false
                else -> false
            }
        }
    }


}