package com.alibaba.android.arouter.demo.kotlin

import android.content.Context
import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.service.PretreatmentService

/** Test provider that records the effective context delivered before navigation. */
@Route(path = "/kotlin/service/pretreatment")
class RecordingPretreatmentService : PretreatmentService {
    override fun onPretreatment(context: Context, postcard: Postcard): Boolean {
        if (recordingEnabled) {
            lastContext = context
            lastPostcard = postcard
            invocationCount++
        }
        return true
    }

    override fun init(context: Context) = Unit

    companion object {
        @JvmField
        @Volatile
        var lastContext: Context? = null

        @JvmField
        @Volatile
        var lastPostcard: Postcard? = null

        @JvmField
        @Volatile
        var invocationCount: Int = 0

        @Volatile
        private var recordingEnabled: Boolean = false

        @JvmStatic
        fun startRecording() {
            clear()
            recordingEnabled = true
        }

        @JvmStatic
        fun stopRecording() {
            recordingEnabled = false
            clear()
        }

        private fun clear() {
            lastContext = null
            lastPostcard = null
            invocationCount = 0
        }
    }
}
