package com.licoresluna2.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.licoresluna2.databinding.ActivitySaleBinding

class SaleActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySaleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }
}
