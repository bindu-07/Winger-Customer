package com.food.ordering.zinger.ui.cart


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager

import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.food.ordering.zinger.R
import com.food.ordering.zinger.data.local.PreferencesHelper
import com.food.ordering.zinger.data.local.Resource
import com.food.ordering.zinger.data.model.*
import com.food.ordering.zinger.databinding.*
import com.food.ordering.zinger.ui.payment.PaymentActivity
import com.food.ordering.zinger.utils.AppConstants
import com.food.ordering.zinger.utils.PermissionUtils
import com.google.android.gms.location.*
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import com.sucho.placepicker.AddressData
import com.sucho.placepicker.Constants
import com.sucho.placepicker.MapType
import com.sucho.placepicker.PlacePicker
import kotlinx.android.synthetic.main.activity_cart.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


class CartActivity : AppCompatActivity() {
    private val REQUEST_LOCATION = 1

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        private val REQUEST_PERMISSION_REQUEST_CODE = 2020
        const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private lateinit var binding: ActivityCartBinding

    private val viewModel: CartViewModel by viewModel()
    private val preferencesHelper: PreferencesHelper by inject()

    private lateinit var cartAdapter: CartAdapter
    private lateinit var progressDialog: ProgressDialog
    private var cartList: MutableList<MenuItemModel> = ArrayList()
    private var shop: ShopConfigurationModel? = null
    private lateinit var snackBar: Snackbar
    private lateinit var errorSnackBar: Snackbar
    private var isPickup = true
    private lateinit var placeOrderRequest: PlaceOrderRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        getArgs()
        initView()
        setListeners()
        setObservers()
        getCurrentLocation()

    }

    private fun getArgs() {
        shop = preferencesHelper.getCartShop()
        if (shop?.configurationModel?.deliveryPrice !== null) {
            deliveryPrice = shop?.configurationModel?.deliveryPrice!!
        }
    }

    private fun initView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cart)
        snackBar = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
        snackBar.setBackgroundTint(ContextCompat.getColor(applicationContext, R.color.green))
        errorSnackBar = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
        val snackButton: Button = errorSnackBar.view.findViewById(R.id.snackbar_action)
        snackButton.setCompoundDrawables(null, null, null, null)
        snackButton.background = null
        snackButton.setTextColor(ContextCompat.getColor(applicationContext, R.color.accent))
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)
        binding.toolbarLayout.setExpandedTitleColor(ContextCompat.getColor(applicationContext, android.R.color.white))
        binding.toolbarLayout.setCollapsedTitleTextColor(ContextCompat.getColor(applicationContext, android.R.color.black))
        binding.appBar.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (abs(verticalOffset) - appBarLayout.totalScrollRange == 0) { //Collapsed
                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp)
            } else { //Expanded
                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)
            }
        })
        setupMenuRecyclerView()
        updateShopUI()
    }

    @SuppressLint("SetTextI18n")
    private fun setListeners() {
        snackBar.setAction("Place Order") {
            if (!isPickup) {
                if (preferencesHelper.cartDeliveryLocation.isNullOrEmpty()) {
                    Handler().postDelayed({
                        snackBar.show()
                    }, 500)
                    Toast.makeText(applicationContext, "Please choose a delivery location", Toast.LENGTH_SHORT).show()
                } else {
                    if (cart.isEmpty()) {
                        Toast.makeText(applicationContext, "Cart is empty", Toast.LENGTH_SHORT).show()
                    } else {
                        showOrderConfirmation()
                    }
                }
            } else {
                if (cart.isEmpty()) {
                    Toast.makeText(applicationContext, "Cart is empty", Toast.LENGTH_SHORT).show()
                } else {
                    showOrderConfirmation()
                }
            }
        }
        errorSnackBar.setAction("Try again") {
            viewModel.verifyOrder(placeOrderRequest)
        }
        binding.radioPickup.setOnClickListener {
            binding.radioPickup.isChecked = true
            binding.radioDelivery.isChecked = false
            binding.textDeliveryPrice.text = "₹0"
            binding.deliveryTotal.text = "FREE"
            binding.taxes.text = "Pickup charge"
            isPickup = true
            binding.textDeliveryLocation.visibility = View.GONE
            binding.setupDelivery.visibility = View.GONE
            preferencesHelper.cartDeliveryPref = ""
            updateCartUI()
        }
        binding.radioDelivery.setOnClickListener {
            binding.radioDelivery.isChecked = true
            binding.radioPickup.isChecked = false
            binding.taxes.text = "Delivery charge"
            binding.textDeliveryPrice.text = "₹" + deliveryPrice.toInt().toString()
            binding.deliveryTotal.text = "₹" + deliveryPrice.toInt().toString()
            isPickup = false
            binding.textDeliveryLocation.visibility = View.VISIBLE
            binding.setupDelivery.visibility = View.VISIBLE
            updateCartUI()
        }
        binding.textInfo.setOnClickListener {
            val dialogBinding: BottomSheetShopInfoBinding =
                    DataBindingUtil.inflate(layoutInflater, R.layout.bottom_sheet_shop_info, null, false)
            val dialog = BottomSheetDialog(this)
            dialog.setContentView(dialogBinding.root)
            dialog.show()
            dialogBinding.editTextInfo.setText(preferencesHelper.cartShopInfo)
            dialogBinding.buttonSaveTextInfo.setOnClickListener {
                preferencesHelper.cartShopInfo = dialogBinding.editTextInfo.text.toString()
                if (!preferencesHelper.cartShopInfo.isNullOrEmpty()) {
                    binding.textInfo.text = preferencesHelper.cartShopInfo
                } else {
                    binding.textInfo.text = "Any information to convey to " + shop?.shopModel?.name + "?"
                }
                dialog.dismiss()
            }
        }
//        binding.textDeliveryLocation.setOnClickListener {
//            val dialogBinding: BottomSheetDeliveryLocationBinding =
//                    DataBindingUtil.inflate(layoutInflater, R.layout.bottom_sheet_delivery_location, null, false)
//            val dialog = BottomSheetDialog(this)
//            dialog.setContentView(dialogBinding.root)
//            dialog.show()
//            dialogBinding.editLocation.setText(preferencesHelper.cartDeliveryLocation)
//            dialogBinding.buttonSaveLocation.setOnClickListener {
//                preferencesHelper.cartDeliveryLocation = dialogBinding.editLocation.text.toString()
//                if(!preferencesHelper.cartDeliveryLocation.isNullOrEmpty()){
//                    binding.textDeliveryLocation.text = preferencesHelper.cartDeliveryLocation
//                }else{
//                    binding.textDeliveryLocation.text = "Enter delivery location"
//                }
//                dialog.dismiss()
//            }
//        }

//        val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
//        binding.setupDelivery.setOnClickListener {
//            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//                OnGPS();
//            }
//            if (ActivityCompat.checkSelfPermission(
//                            this@CartActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                            this@CartActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
//            } else {
//                val locationGPS: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//                if (locationGPS != null) {
//                    val lat: Double = locationGPS.getLatitude()
//                    val longi: Double = locationGPS.getLongitude()
//                    latitude = lat.toString()
//                    longitude = longi.toString()
////                    showLocation.setText("Your Location: \nLatitude: $latitude\nLongitude: $longitude")
//                } else {
//                    Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show()
//                }
//            }

//            val intent = PlacePicker.IntentBuilder()
//                    .setLatLong(40.748672, -73.985628)
//                    .showLatLong(true)
//                    .setMapZoom(12.0f)
//                    .setMarkerImageImageColor(R.color.red)
//                    .setMapType(MapType.NORMAL)
//                    .setPrimaryTextColor(R.color.md_black_1000) // Change text color of Shortened Address
//                    .setSecondaryTextColor(R.color.md_black_1000) // Change text color of full Address
//                    .setBottomViewColor(R.color.white)
//                    .setFabColor(R.color.red)
//                    .setPlaceSearchBar(true, applicationInfo.metaData.getString("com.google.android.geo.API_KEY"))
//                    .build(this)
//            startActivityForResult(intent, Constants.PLACE_PICKER_REQUEST)

//            val dialogBinding: Step1DeliveryLocationBinding =
//                    DataBindingUtil.inflate(layoutInflater, R.layout.step1_delivery_location, null, false)
//            val dialog = BottomSheetDialog(this)
//            dialog.setContentView(AppConstants.root)
//            dialog.show()
//            dialogBinding.setup1.setOnClickListener {
//                val dialogBinding: Step2DeliveryLocationBinding =
//                DataBindingUtil.inflate(layoutInflater, R.layout.step2_delivery_location, null, false)
//                val dialog1 = BottomSheetDialog(this)
//                dialog1.setContentView(dialogBinding.root)
//                dialog1.show()
//                dialog.dismiss()
//                dialogBinding.setup2.setOnClickListener {
//                    val dialogBinding: Step3DeliveryLocationBinding =
//                    DataBindingUtil.inflate(layoutInflater, R.layout.step3_delivery_location, null, false)
//                    val dialog2 = BottomSheetDialog(this)
//                    dialog2.setContentView(dialogBinding.root)
//                    dialog2.show()
//                    dialog1.dismiss()
//                    dialogBinding.getLocation.setOnClickListener {
////                        Toast.makeText(applicationContext,"Get your location",Toast.LENGTH_SHORT).show()
//
//                        //check permission
//                        if (ContextCompat.checkSelfPermission(
//                                        applicationContext,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
//                            ActivityCompat.requestPermissions(this@CartActivity,
//                                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
//                                    ,REQUEST_PERMISSION_REQUEST_CODE)
//
//                        }else {
//                            binding.textDeliveryLocation.text = ""
//                            loader.visibility = View.VISIBLE
//                            getCurrentLocation()
//                        }
//
//                        dialog2.dismiss()
//                    }
//
//                }
//            }

//
//        }
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?
    ) {
        if (requestCode == Constants.PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    val addressData = data?.getParcelableExtra<AddressData>(Constants.ADDRESS_INTENT)
                    if (addressData != null) {
                        preferencesHelper.cartDeliveryLocation = addressData.latitude.toString()+" "+addressData.longitude.toString()
                    }

                    binding.textDeliveryLocation.text = preferencesHelper.cartDeliveryLocation
                } catch (e: Exception) {

                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }


    //    private fun getCurrentLocation() {
//
//        var locationRequest = LocationRequest()
//        locationRequest.interval = 10000
//        locationRequest.fastestInterval = 5000
//        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//
//        //now getting address from latitude and longitude
//
//        val geocoder = Geocoder(this, Locale.getDefault())
//        var addresses: List<Address>
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            ActivityCompat.requestPermissions(this@CartActivity,
//                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_REQUEST_CODE)
//            Toast.makeText(this@CartActivity, "Turn On yor mobile location!", Toast.LENGTH_SHORT).show()
//
//        }
//        LocationServices.getFusedLocationProviderClient(this@CartActivity)
//                .requestLocationUpdates(locationRequest, object : LocationCallback() {
//                    override fun onLocationResult(locationResult: LocationResult?) {
//                        super.onLocationResult(locationResult)
//                        LocationServices.getFusedLocationProviderClient(this@CartActivity)
//                                .removeLocationUpdates(this)
//                        if (locationResult != null && locationResult.locations.size > 0) {
//                            var locIndex = locationResult.locations.size - 1
//
//                            var latitude = locationResult.locations.get(locIndex).latitude
//                            var longitude = locationResult.locations.get(locIndex).longitude
//                            addresses = geocoder.getFromLocation(latitude, longitude, 1)
//
//                            var address: String = addresses[0].getAddressLine(0)
//                            preferencesHelper.cartDeliveryLocation = "Latitude: " + latitude + ", Longitude: " + longitude + " \nAddress: " + address
//
//                            binding.textDeliveryLocation.text = preferencesHelper.cartDeliveryLocation
//
////                            tvAddress.text = address
//                            if (binding.textDeliveryLocation != null) {
//                                loader.visibility = View.GONE
//                            }
//                        }
//                    }
//                }, Looper.getMainLooper())
//
//    }
//
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_ACCESS_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            getCurrentLocation()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                } else {
                    Toast.makeText(
                            this,
                            getString(R.string.location_permission_not_granted),
                            Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        when {
            PermissionUtils.isAccessFineLocationGranted(this) -> {
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
                        getCurrentLocation()
                    }
                    else -> {
                        PermissionUtils.showGPSNotEnabledDialog(this)
                    }
                }
            }
            else -> {
                PermissionUtils.requestAccessFineLocationPermission(
                        this,
                        PERMISSION_REQUEST_ACCESS_LOCATION
                )
            }
        }
    }

    private fun getCurrentLocation() {
        if (checkPermission()) {


            binding.setupDelivery.setOnClickListener {


                    //final result
                    fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                        val location: Location? = task.result
                        val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

                        if (location == null) {

                            val intent = PlacePicker.IntentBuilder()
                                    .setLatLong(23.421095168485305, 86.20656855404377)
                                    .showLatLong(false)
                                    .setMapZoom(12.0f)
                                    .setMarkerImageImageColor(R.color.red)
                                    .setMapType(MapType.NORMAL)
                                    .setPrimaryTextColor(R.color.md_black_1000) // Change text color of Shortened Address
                                    .setSecondaryTextColor(R.color.md_black_1000) // Change text color of full Address
                                    .setBottomViewColor(R.color.white)
                                    .setFabColor(R.color.red)
                                    .setAddressRequired(false)
                                    .setPlaceSearchBar(true, applicationInfo.metaData.getString("com.google.android.geo.API_KEY"))
                                    .build(this)
                            startActivityForResult(intent, Constants.PLACE_PICKER_REQUEST)


                        } else {
                            val intent = PlacePicker.IntentBuilder()
                                    .setLatLong(location.latitude, location.longitude)
                                    .showLatLong(false)
                                    .setMapZoom(12.0f)
                                    .setMarkerImageImageColor(R.color.red)
                                    .setMapType(MapType.NORMAL)
                                    .setPrimaryTextColor(R.color.md_black_1000) // Change text color of Shortened Address
                                    .setSecondaryTextColor(R.color.md_black_1000) // Change text color of full Address
                                    .setBottomViewColor(R.color.white)
                                    .setFabColor(R.color.red)
                                    .setAddressRequired(false)
                                    .setPlaceSearchBar(true, applicationInfo.metaData.getString("com.google.android.geo.API_KEY"))
                                    .build(this)
                            startActivityForResult(intent, Constants.PLACE_PICKER_REQUEST)

                        }

                    }
//                } else {
//                    Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
////                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
////                    startActivity(intent)
//                }


            }

        } else {
            //requestPermission here
            requestPermission()
        }
    }

    private fun isLocationEnable(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER)

    }

    private fun setObservers() {
        viewModel.insertOrderStatus.observe(this, androidx.lifecycle.Observer {
            when (it.status) {
                Resource.Status.LOADING -> {
                    errorSnackBar.dismiss()
                    progressDialog.setMessage("Verifying cart items...")
                    progressDialog.show()
                }
                Resource.Status.SUCCESS -> {
                    progressDialog.dismiss()
                    errorSnackBar.dismiss()
                    initiatePayment(it.data?.data?.transactionToken, it.data?.data?.orderId.toString())
                }
                Resource.Status.OFFLINE_ERROR -> {
                    progressDialog.dismiss()
                    errorSnackBar.setText("No Internet Connection")
                    errorSnackBar.show()
                }
                Resource.Status.ERROR -> {
                    progressDialog.dismiss()
                    if (!it.message.isNullOrEmpty()) {
                        errorSnackBar.setText(it.message.toString())
                    } else {
                        errorSnackBar.setText("Cart verify failed")
                    }
                    errorSnackBar.show()
                }
                else -> {
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateShopUI() {
        Picasso.get().load(shop?.shopModel?.photoUrl).placeholder(R.drawable.ic_shop).into(binding.layoutShop.imageShop)
        Picasso.get().load(shop?.shopModel?.coverUrls?.get(0)).placeholder(R.drawable.shop_placeholder).into(binding.imageExpanded)
        binding.layoutShop.textShopName.text = shop?.shopModel?.name
        if (shop?.configurationModel?.isOrderTaken == 1) {
            if (shop?.configurationModel?.isDeliveryAvailable == 1) {
                binding.layoutShop.textShopDesc.text = "Closes at " + shop?.shopModel?.closingTime?.substring(0, 5)
            } else {
                binding.layoutShop.textShopDesc.text = "Closes at " + shop?.shopModel?.closingTime?.substring(0, 5) + " (Delivery not available)"
            }
        } else {
            binding.layoutShop.textShopDesc.text = "Closed Now"
        }
        binding.layoutShop.textShopRating.text = shop?.ratingModel?.rating.toString()
        if (!preferencesHelper.cartShopInfo.isNullOrEmpty()) {
            binding.textInfo.text = preferencesHelper.cartShopInfo
        } else {
            binding.textInfo.text = "Any information to convey to " + shop?.shopModel?.name + "?"
        }
        if (!preferencesHelper.cartDeliveryLocation.isNullOrEmpty()) {
            binding.textDeliveryLocation.text = preferencesHelper.cartDeliveryLocation
        } else {
            binding.textDeliveryLocation.text = "Setup delivery location"
        }
        if (!preferencesHelper.cartDeliveryPref.isNullOrEmpty()) {
            if (preferencesHelper.cartDeliveryPref == "delivery") {
                binding.radioDelivery.isChecked = true
                binding.radioPickup.isChecked = false
                binding.textDeliveryPrice.text = "₹" + deliveryPrice.toInt().toString()
                binding.taxes.text = "Delivery charge"
                binding.deliveryTotal.text = "₹" + deliveryPrice.toInt().toString()
                isPickup = false
                updateCartUI()
                binding.textDeliveryLocation.visibility = View.VISIBLE
                binding.setupDelivery.visibility = View.VISIBLE
            } else {
                binding.deliveryTotal.text = "FREE"
                binding.taxes.text = "Pickup charge"
                binding.textDeliveryLocation.visibility = View.GONE
                binding.setupDelivery.visibility = View.GONE
            }
        } else {
            binding.textDeliveryLocation.visibility = View.GONE
            binding.setupDelivery.visibility = View.GONE
            binding.deliveryTotal.text = "FREE"
            binding.taxes.text = "Pickup charge"
        }
    }

    private fun setupMenuRecyclerView() {
        cartList.clear()
        cartList.addAll(cart)
        updateCartUI()
        cartAdapter = CartAdapter(applicationContext, cartList, object : CartAdapter.OnItemClickListener {

            override fun onItemClick(item: MenuItemModel?, position: Int) {
                //TODO navigate to restaurant activity
            }

            override fun onQuantityAdd(position: Int) {
                println("quantity add clicked $position")
                cartList[position].quantity = cartList[position].quantity + 1
                cartAdapter.notifyItemChanged(position)
                updateCartUI()
                saveCart(cartList)
            }

            override fun onQuantitySub(position: Int) {
                println("quantity sub clicked $position")
                if (cartList[position].quantity - 1 >= 0) {
                    cartList[position].quantity = cartList[position].quantity - 1
                    if (cartList[position].quantity == 0) {
                        cartList.removeAt(position)
                        cartAdapter.notifyDataSetChanged()
                    } else {
                        cartAdapter.notifyDataSetChanged()
                    }
                    updateCartUI()
                    saveCart(cartList)
                }
            }
        })
        binding.recyclerFoodItems.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerFoodItems.adapter = cartAdapter
    }

    private var deliveryPrice = 0.0
    private var cartTotalPrice = 0

    @SuppressLint("SetTextI18n")
    private fun updateCartUI() {
        var total = 0
        var item_total = 0
        var totalItems = 0
        if (cartList.size > 0) {

            binding.layoutContent.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            for (i in cartList.indices) {
                total += cartList[i].price * cartList[i].quantity
                item_total += cartList[i].price * cartList[i].quantity
                totalItems += 1
            }
            binding.totalItem.text = "Item Total (" + totalItems + ")"
            if (!isPickup) {
                total += deliveryPrice.toInt()
                preferencesHelper.cartDeliveryPref = "delivery"
            }
            binding.itemTotal.text = "₹$item_total"
            binding.textTotal.text = "₹$total"
            if (totalItems == 1) {
                snackBar.setText("₹$total | $totalItems item")
            } else {
                snackBar.setText("₹$total | $totalItems items")
            }
            snackBar.show()
            cartTotalPrice = total
        } else {
            preferencesHelper.clearCartPreferences()
            snackBar.dismiss()
            binding.layoutContent.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        }
    }

    fun saveCart(foodItems: List<MenuItemModel>?) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val cartString = gson.toJson(foodItems)
        preferencesHelper.cart = cartString
    }

    val cart: List<MenuItemModel>
        get() {
            val items: MutableList<MenuItemModel> = ArrayList()
            val temp = preferencesHelper.getCart()
            if (temp != null) {
                items.addAll(temp)
            }
            return items
        }

    private fun initiatePayment(token: String?, orderId: String) {
        val intent = Intent(applicationContext, PaymentActivity::class.java)
        intent.putExtra(AppConstants.TRANSACTION_TOKEN, token)
        intent.putExtra(AppConstants.ORDER_ID, orderId)
        intent.putExtra("Data", cartTotalPrice.toString())
        startActivity(intent)
        finish()
    }

    private fun verifyOrder() {
        var cookingInfo: String? = null
        var deliveryLocation = ""
        if (!preferencesHelper.cartShopInfo.isNullOrEmpty()) {
            cookingInfo = preferencesHelper.cartShopInfo
        }
        if (!preferencesHelper.cartDeliveryLocation.isNullOrEmpty()) {
            deliveryLocation = preferencesHelper.cartDeliveryLocation!!
        }
        val cartOrderModel = CartOrderModel(
                cookingInfo,
                if (isPickup) null else deliveryLocation,
                if (isPickup) null else deliveryPrice.toInt(),
                cartTotalPrice,
                CartShopModel(shop?.shopModel?.id),
                CartUserModel(preferencesHelper.userId)
        )
        val cartTransactionModel = CartTransactionModel(cartOrderModel)
        val listCartOrderItems: ArrayList<CartOrderItems> = ArrayList()
        cart.forEach {
            listCartOrderItems.add(CartOrderItems(FoodItem(it.id), it.price, it.quantity))
        }
        placeOrderRequest = PlaceOrderRequest(listCartOrderItems, cartTransactionModel)
        viewModel.verifyOrder(placeOrderRequest)
    }

    private fun showOrderConfirmation() {
        MaterialAlertDialogBuilder(this@CartActivity)
                .setTitle("Place order")
                .setCancelable(false)
                .setMessage("Are you sure want to place this order?")
                .setPositiveButton("Yes") { _, _ ->
                    verifyOrder()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                    Handler().postDelayed({
                        snackBar.show()
                    }, 500)
                }
                .show()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}

