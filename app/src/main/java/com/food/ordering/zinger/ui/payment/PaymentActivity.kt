package com.food.ordering.zinger.ui.payment

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.food.ordering.zinger.R
import com.food.ordering.zinger.data.local.PreferencesHelper
import com.food.ordering.zinger.data.model.*
import com.food.ordering.zinger.databinding.ActivityPaymentBinding
import com.food.ordering.zinger.ui.order.OrderViewModel
import com.food.ordering.zinger.ui.placeorder.PlaceOrderActivity
import com.food.ordering.zinger.utils.AppConstants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class PaymentActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityPaymentBinding
    private val viewModel: PaymentViewModel by viewModel()
    private val viewModel1: OrderViewModel by viewModel()
    private val preferencesHelper: PreferencesHelper by inject()
    private var orderId: String? = null
    private var token: String? = null
    private var totalPrice: String? = null
    val UPI_PAYMENT = 0
    var firebaseDatabase: FirebaseDatabase? = null

    var databaseReference: DatabaseReference? = null

    var paymentmode: PaymentMode? = null
    private lateinit var order: OrderItemListModel
    var pay_on_delivery: String? = null
    var pay_via_upi: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getArgs()
        initView()
        setListener()
        setObservers()
        setupPaymentModes()
    }

    private fun getArgs() {
        orderId = intent.getStringExtra(AppConstants.ORDER_ID)
        token = intent.getStringExtra(AppConstants.TRANSACTION_TOKEN)
        pay_on_delivery = intent.getStringExtra(AppConstants.PAYMENT_MODE)
        totalPrice = intent.getStringExtra("Data")
        firebaseDatabase = FirebaseDatabase.getInstance()
        val id: String = orderId.toString()

        // below line is used to get reference for our database.

        // below line is used to get reference for our database.
        databaseReference = firebaseDatabase!!.getReference("PaymentMode").child(id)

        // initializing our object
        // class variable.

        // initializing our object
        // class variable.
        paymentmode = PaymentMode()
//        if (orderId.isNullOrEmpty()) {
//            order = Gson().fromJson(intent.getStringExtra(AppConstants.ORDER_DETAIL), OrderItemListModel::class.java)
//        } else {
//            viewModel1.getOrderById(orderId!!.toInt())
//        }

    }

    private fun initView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_payment)
        binding.layoutContent.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
//        order.transactionModel.paymentMode = pay_on_delivery.toString()

        binding.textTotalPrice.text = "Bill total: ₹" + totalPrice.toString()
    }

    private fun setListener() {
        binding.imageClose.setOnClickListener { onBackPressed() }
        binding.buttonCreditPay.setOnClickListener(this)
        binding.buttonDebitPay.setOnClickListener(this)
        binding.buttonBhimPay.setOnClickListener(this)
        binding.buttonPaytmPay.setOnClickListener(this)
        binding.radioCredit.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.cardCredit.visibility = View.VISIBLE
                binding.radioDebit.isChecked = false
                binding.radioBhim.isChecked = false
                binding.radioPaytm.isChecked = false
            } else {
                binding.cardCredit.visibility = View.GONE
            }
        }
        binding.radioDebit.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.cardDebitDetails.visibility = View.VISIBLE
                binding.radioCredit.isChecked = false
                binding.radioBhim.isChecked = false
                binding.radioPaytm.isChecked = false
            } else {
                binding.cardDebitDetails.visibility = View.GONE
            }
        }
        binding.radioBhim.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.cardBhim.visibility = View.VISIBLE
                binding.radioCredit.isChecked = false
                binding.radioDebit.isChecked = false
                binding.radioPaytm.isChecked = false
            } else {
                binding.cardBhim.visibility = View.GONE
            }
        }
        binding.radioPaytm.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.cardPaytm.visibility = View.VISIBLE
                binding.radioCredit.isChecked = false
                binding.radioDebit.isChecked = false
                binding.radioBhim.isChecked = false
            } else {
                binding.cardPaytm.visibility = View.GONE
            }
        }
    }

    private fun setObservers() {

    }

    private fun setupPaymentModes() {

    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this@PaymentActivity)
                .setTitle("Cancel process?")
                .setMessage("Are you sure want to cancel the order")
                .setPositiveButton("Yes") { dialog, which ->
                    dialog.dismiss()
                    super.onBackPressed()
                }
                .setNegativeButton("No") { dialog, which -> dialog.dismiss() }
                .show()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button_bhim_pay,
            R.id.button_credit_pay,
            R.id.button_debit_pay -> {
//                val intent = Intent(applicationContext,PlaceOrderActivity::class.java)
//                intent.putExtra(AppConstants.ORDER_ID, orderId)
//                startActivity(intent)
//                finish()


                val amountMoney: String = totalPrice.toString()

                payUsingUpi(amountMoney, "imdeep2357@okaxis", "Bindu Hite")

            }
        }
        when (v?.id) {
            R.id.button_paytm_pay -> {

                preferencesHelper.paymentMode = "Pay on Delivery(POD)"
//                val transactionModel = TransactionModel(
//                    "NULL",
//                    "BT"+orderId,
//                    ""+totalPrice.toString(),
//                    "INR",
//                    "POD",
//
//                    "Pay on Delivery(POD)",
//                    "01",
//                    "Success",
//                    "T"+orderId
//
//                )
//                val payTransactionModel = OrderItemListModel(,transactionModel)
//                viewModel.placeOrder(payTransactionModel)

//                addDatatoFirebase(paymode, price);

                val intent = Intent(applicationContext,PlaceOrderActivity::class.java)
                intent.putExtra(AppConstants.ORDER_ID, orderId)
//                intent.putExtra(AppConstants.PAYMENT_MODE, pay_on_delivery)
                startActivity(intent)
                finish()

            }
        }
    }

    private fun addDatatoFirebase(paymode: String, price: String) {

        paymentmode?.setEmployeeName(paymode)
        paymentmode?.setEmployeeContactNumber(price)
//        employeeInfo.setEmployeeAddress(address)

        // we are use add value event listener method
        // which is called with database reference.

        // we are use add value event listener method
        // which is called with database reference.
        databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // inside the method of on Data change we are setting
                // our object class to our database reference.
                // data base reference will sends data to firebase.
                databaseReference!!.setValue(paymentmode)

                // after adding this data we are showing toast message.
//                Toast.makeText(this@MainActivity, "data added", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                // if the data is not added or it is cancelled then
                // we are displaying a failure toast message.
//                Toast.makeText(this@MainActivity, "Fail to add data $error", Toast.LENGTH_SHORT)
//                    .show()
            }
        })

    }

    private fun payUsingUpi(amount: String, upiId: String, name: String) {

        val uri: Uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .build()

        val upiPayIntent = Intent(Intent.ACTION_VIEW)
        upiPayIntent.data = uri

        // will always show a dialog to user to choose an app
        // will always show a dialog to user to choose an app
        val chooser = Intent.createChooser(upiPayIntent, "Pay with")

        // check if intent resolves
        if (null != chooser.resolveActivity(getPackageManager())) {
            startActivityForResult(chooser, UPI_PAYMENT);
        } else {
            Toast.makeText(applicationContext, "No UPI app found, please install one to continue", Toast.LENGTH_SHORT).show();
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            UPI_PAYMENT -> if (RESULT_OK == resultCode || resultCode == 11) {
                if (data != null) {
                    val trxt = data.getStringExtra("response")
                    Log.d("UPI", "onActivityResult: $trxt")
                    val dataList: ArrayList<String> = ArrayList()
                    if (trxt != null) {
                        dataList.add(trxt)
                    }
                    upiPaymentDataOperation(dataList)
                } else {
                    Log.d("UPI", "onActivityResult: " + "Return data is null")
                    val dataList: ArrayList<String> = ArrayList()
                    dataList.add("nothing")
                    upiPaymentDataOperation(dataList)
                }
            } else {
                Log.d("UPI", "onActivityResult: " + "Return data is null") //when user simply back without payment
                val dataList: ArrayList<String> = ArrayList()
                dataList.add("nothing")
                upiPaymentDataOperation(dataList)
            }
        }
    }

    private fun upiPaymentDataOperation(data: ArrayList<String>) {
        if (isConnectionAvailable(this@PaymentActivity)) {
            var str: String = data.get(0)
            Log.d("UPIPAY", "upiPaymentDataOperation: $str")
            var paymentCancel = ""
            if (str == null) str = "discard"
            var status = ""
            var approvalRefNo = ""
            val response = str.split("&".toRegex()).toTypedArray()
            for (i in response.indices) {
                val equalStr = response[i].split("=".toRegex()).toTypedArray()
                if (equalStr.size >= 2) {
                    if (equalStr[0].toLowerCase() == "Status".toLowerCase()) {
                        status = equalStr[1].toLowerCase()
                    } else if (equalStr[0].toLowerCase() == "ApprovalRefNo".toLowerCase() || equalStr[0].toLowerCase() == "txnRef".toLowerCase()) {
                        approvalRefNo = equalStr[1]
                    }
                } else {
                    paymentCancel = "Payment cancelled by user."
                }
            }
            if (status == "success") {
                //Code to handle successful transaction here.
                Toast.makeText(this@PaymentActivity, "Transaction successful.", Toast.LENGTH_SHORT).show()

                val intent = Intent(applicationContext, PlaceOrderActivity::class.java)
                intent.putExtra(AppConstants.ORDER_ID, orderId)
                startActivity(intent)
                finish()

                Log.d("UPI", "responseStr: $approvalRefNo")
            } else if ("Payment cancelled by user." == paymentCancel) {
                Toast.makeText(this@PaymentActivity, "Payment cancelled by user.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@PaymentActivity, "Transaction failed.Please try again", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this@PaymentActivity, "Internet connection is not available. Please check and try again", Toast.LENGTH_SHORT).show()
        }
    }

    fun isConnectionAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val netInfo = connectivityManager.activeNetworkInfo
            if (netInfo != null && netInfo.isConnected
                    && netInfo.isConnectedOrConnecting
                    && netInfo.isAvailable) {
                return true
            }
        }
        return false
    }

}
