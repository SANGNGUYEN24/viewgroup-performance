package com.sang.viewgroup_performance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.sang.viewgroup_performance.common.Constant.REPEAT_TIME
import com.sang.viewgroup_performance.databinding.ActivityMainBinding
import com.sang.viewgroup_performance.ui1.Ui1ConstraintActivity
import com.sang.viewgroup_performance.ui1.Ui1LinearActivity
import com.sang.viewgroup_performance.ui2.Ui2ConstraintActivity
import com.sang.viewgroup_performance.ui2.Ui2OtherLayoutActivity
import com.sang.viewgroup_performance.ui3.Ui3ConstraintActivity
import com.sang.viewgroup_performance.ui3.Ui3OtherLayoutActivity


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val viewLoadTrace: Trace = FirebasePerformance.startTrace("MainActivity-LoadTime")

    private lateinit var mainActivityBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(mainActivityBinding.root)

        ScreenTrace.enableHardwareAcceleration(this)

        val mainView = findViewById<ViewGroup>(android.R.id.content)

        FirstDrawListener(mainView, object : FirstDrawListener.OnFirstDrawCallback {
            override fun onDrawingStart() {

            }

            override fun onDrawingFinish() {
                viewLoadTrace.stop()
            }
        })

        mainActivityBinding.ui1ConstraintBtn.setOnClickListener(this)
        mainActivityBinding.ui1OtherBtn.setOnClickListener(this)

        mainActivityBinding.ui2ConstraintBtn.setOnClickListener(this)
        mainActivityBinding.ui2OtherBtn.setOnClickListener(this)

        mainActivityBinding.ui3ConstraintBtn.setOnClickListener(this)
        mainActivityBinding.ui3OtherBtn.setOnClickListener(this)

    }

    override fun onClick(view: View?) {
        when (view) {
            mainActivityBinding.ui1OtherBtn -> {
                repeatActivity(Ui1LinearActivity::class.java)
            }

            mainActivityBinding.ui1ConstraintBtn -> {
                repeatActivity(Ui1ConstraintActivity::class.java)
            }

            mainActivityBinding.ui2OtherBtn -> {
                repeatActivity(Ui2OtherLayoutActivity::class.java)
            }

            mainActivityBinding.ui2ConstraintBtn -> {
                repeatActivity(Ui2ConstraintActivity::class.java)
            }

            mainActivityBinding.ui3ConstraintBtn -> {
                repeatActivity(Ui3ConstraintActivity::class.java)
            }

            mainActivityBinding.ui3OtherBtn -> {
                repeatActivity(Ui3OtherLayoutActivity::class.java)
            }
        }
    }

    private fun repeatActivity(cls: Class<*>) {
        var count = 0
        while (count++ < REPEAT_TIME) {
            startActivity(Intent(this, cls))
        }
    }
}