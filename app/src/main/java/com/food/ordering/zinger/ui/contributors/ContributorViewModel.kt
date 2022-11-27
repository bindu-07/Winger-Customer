package com.food.ordering.zinger.ui.contributors

import androidx.lifecycle.*
import com.food.ordering.zinger.data.local.Resource
import com.food.ordering.zinger.data.model.ContributorModel
import com.food.ordering.zinger.data.model.ShopConfigurationModel
import com.food.ordering.zinger.data.retrofit.ShopRepository
import kotlinx.coroutines.launch

import java.net.UnknownHostException


class ContributorViewModel() : ViewModel() {

    private var contributorList: ArrayList<ContributorModel> = arrayListOf()
    init {
        contributorList.clear()
        contributorList.add(
                ContributorModel(
                        "Bindu Hite",
                        "Android & Backend Developer",
                        "https://github.com/bindu-07",
                        "https://www.linkedin.com/in/bindu-hite-46890a193/",
                        "hitebindu2002@gmail.com",
                        "https://twitter.com/BinduHite",
                        "file:///android_asset/bindu.jpg"
                )
        )
        contributorList.add(
                ContributorModel(
                        "Harshavardhan P",
                        "Android & Backend Developer",
                        "https://github.com/harshavardhan98",
                        "https://www.linkedin.com/in/harshavardhan-p/",
                        "harshavardhan.zodiac@gmail.com",
                        "https://harshavardhan98.github.io/",
                        "file:///android_asset/harsha.jpg"
                )
        )
        contributorList.add(
                ContributorModel(
                        "Logesh Dinakaran",
                        "Backend Development",
                        "https://github.com/ddlogesh",
                        "https://www.linkedin.com/in/logesh-dinakaran",
                        "ddlogesh@gmail.com",
                        "https://logeshdina.tech/",
                        "file:///android_asset/logesh.jpg"
                )
        )
    }

    fun getContributor(contributorId: Int):ContributorModel {
        return contributorList[contributorId]
    }

}