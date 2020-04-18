package com.food.ordering.zinger.ui.cart

import androidx.lifecycle.*
import com.food.ordering.zinger.data.local.Resource
import com.food.ordering.zinger.data.model.MenuItem
import com.food.ordering.zinger.data.model.PlaceOrderRequest
import com.food.ordering.zinger.data.model.PlaceOrderResponse
import com.food.ordering.zinger.data.model.Shop
import com.food.ordering.zinger.data.retrofit.ItemRepository
import com.food.ordering.zinger.data.retrofit.OrderRepository
import com.food.ordering.zinger.data.retrofit.UserRepository
import kotlinx.coroutines.launch

import java.net.UnknownHostException


class CartViewModel(private val orderRepository: OrderRepository) : ViewModel() {

    //Fetch total stats
    private val insertOrder = MutableLiveData<Resource<PlaceOrderResponse>>()
    val insertOrderStatus: LiveData<Resource<PlaceOrderResponse>>
        get() = insertOrder

    fun insertOrder(placeOrderRequest: PlaceOrderRequest) {
        viewModelScope.launch {
            try {
                insertOrder.value = Resource.loading()
                val response = orderRepository.insertOrder(placeOrderRequest)
                if (response != null) {
                    if (response.code == 1) {
                        if (!response.data.isNullOrEmpty()) {
                            insertOrder.value = Resource.success(response)
                        } else {
                            insertOrder.value = Resource.error(null, message = response.data)
                        }

                    } else {
                        insertOrder.value = Resource.error(null, response.message)
                    }
                }
            } catch (e: Exception) {
                println("insert order failed ${e.message}")
                if (e is UnknownHostException) {
                    insertOrder.value = Resource.offlineError()
                } else {
                    insertOrder.value = Resource.error(e)
                }
            }
        }
    }

}