package com.licoresluna2.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.licoresluna2.databinding.ActivityContainersBinding

class ContainersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContainersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContainersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }
}
