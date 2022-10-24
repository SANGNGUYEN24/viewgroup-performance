package com.sang.viewgroup_performance.ui2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.sang.viewgroup_performance.common.Constant.DELAY_TIME
import com.sang.viewgroup_performance.FirstDrawListener
import com.sang.viewgroup_performance.R
import com.sang.viewgroup_performance.ScreenTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Ui2OtherLayoutActivity : AppCompatActivity() {
    private lateinit var viewLoadTrace: Trace

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ui2_other_layout)

        ScreenTrace.enableHardwareAcceleration(this)

        val mainView = findViewById<ViewGroup>(android.R.id.content)

        FirstDrawListener(mainView, object : FirstDrawListener.OnFirstDrawCallback {
            override fun onDrawingStart() {
                viewLoadTrace = FirebasePerformance.startTrace("Test-Ui2-OtherLayouts-DrawingTime")
            }

            override fun onDrawingFinish() {
                viewLoadTrace.stop()
                CoroutineScope(Dispatchers.Main.immediate).launch {
                    delay(DELAY_TIME)
                    finish()
                }
            }
        })
    }
}