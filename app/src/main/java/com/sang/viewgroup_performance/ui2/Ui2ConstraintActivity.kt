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
import com.sang.viewgroup_performance.databinding.ActivityUi1ConstraintBinding
import com.sang.viewgroup_performance.databinding.ActivityUi2ConstraintBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Ui2ConstraintActivity : AppCompatActivity() {
    private lateinit var viewLoadTrace: Trace

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ui2_constraint)

        ScreenTrace.enableHardwareAcceleration(this)

        FirstDrawListener(
            findViewById<ViewGroup>(android.R.id.content),
            object : FirstDrawListener.OnFirstDrawCallback {
                override fun onDrawingStart() {
                    viewLoadTrace = FirebasePerformance.startTrace("Test-Ui2-ConstraintLayout-DrawingTime")
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