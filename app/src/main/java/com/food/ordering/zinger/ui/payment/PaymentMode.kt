package com.food.ordering.zinger.ui.payment

class PaymentMode {

    private var employeeName: String? = null


    private var employeeContactNumber: String? = null

    private var employeeAddress: String? = null


    fun PaymentMode() {}


    fun getEmployeeName(): String? {
        return employeeName
    }

    fun setEmployeeName(employeeName: String?) {
        this.employeeName = employeeName
    }

    fun getEmployeeContactNumber(): String? {
        return employeeContactNumber
    }

    fun setEmployeeContactNumber(employeeContactNumber: String?) {
        this.employeeContactNumber = employeeContactNumber
    }

    fun getEmployeeAddress(): String? {
        return employeeAddress
    }

    fun setEmployeeAddress(employeeAddress: String?) {
        this.employeeAddress = employeeAddress
    }
}