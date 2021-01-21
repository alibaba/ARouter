package com.alibaba.android.arouter.demo.kotlin

import android.app.Activity
import android.os.Bundle
import com.alibaba.android.arouter.demo.kotlin.databinding.ActivityKotlinTestBinding
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter

@Route(path = "/kotlin/test")
class KotlinTestActivity : Activity() {

    @Autowired
    @JvmField
    var name: String? = null

    @Autowired
    @JvmField
    var age: Int? = 0

    private lateinit var binding: ActivityKotlinTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ARouter.getInstance().inject(this)  // Start auto inject.

        super.onCreate(savedInstanceState)
        binding = ActivityKotlinTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.content.text = "name = $name, age = $age"
    }
}
