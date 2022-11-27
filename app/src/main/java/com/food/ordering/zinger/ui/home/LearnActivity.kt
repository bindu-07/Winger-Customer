package com.food.ordering.zinger.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.food.ordering.zinger.R
import com.food.ordering.zinger.databinding.ActivityLearnBinding
import com.food.ordering.zinger.databinding.ActivityProfileBinding

class LearnActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLearnBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_learn)

        binding.imageClose.setOnClickListener { onBackPressed() }


    }
}