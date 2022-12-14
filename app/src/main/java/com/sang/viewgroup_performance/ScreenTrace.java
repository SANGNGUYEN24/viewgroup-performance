/**
 * Copyright 2021 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sang.viewgroup_performance;

import android.app.Activity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.WindowManager;

import androidx.core.app.FrameMetricsAggregator;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.perf.util.Constants;

/**
 * Utility class to capture Screen rendering information (Slow/Frozen frames) for the
 * {@code Activity} passed to the constructor {@link ScreenTrace#ScreenTrace(Activity, String)}.
 * <p>
 * Learn more at https://firebase.google.com/docs/perf-mon/screen-traces?platform=android.
 * <p>
 * A slow screen rendering often leads to a UI Jank which creates a bad user experience. Below are
 * some tips and references to understand and fix common UI Jank issues:
 * - https://developer.android.com/topic/performance/vitals/render.html#fixing_jank
 * - https://youtu.be/CaMTIgxCSqU (Why 60fps?)
 * - https://youtu.be/HXQhu6qfTVU (Rendering Performance)
 * - https://youtu.be/1iaHxmfZGGc (Understanding VSYNC)
 * - https://www.youtube.com/playlist?list=PLOU2XLYxmsIKEOXh5TwZEv89aofHzNCiu (Android Performance Patterns)
 * <p>
 * References:
 * - Fireperf Source Code: https://bityl.co/5v2O
 */
public class ScreenTrace {

    private static final String FRAME_METRICS_AGGREGATOR_CLASSNAME =
            "androidx.core.app.FrameMetricsAggregator";

    private final Activity activity;
    private final boolean isScreenTraceSupported;
    private final String traceName;

    private FrameMetricsAggregator frameMetricsAggregator;
    private Trace perfScreenTrace;

    /**
     * Default constructor for this class.
     *
     * @param activity for which the screen traces should be recorded.
     * @param tag      used as an identifier for the name to be used to log screen rendering
     *                 information (like "ActivityName-tag").
     * @implNote It will automatically force enable hardware acceleration for the passed {@code activity}.
     * @see #enableHardwareAcceleration(Activity)
     */
    public ScreenTrace(Activity activity, String tag) {
        this.activity = activity;
        this.traceName = activity.getLocalClassName() + "-" + tag;

        enableHardwareAcceleration(activity);

        isScreenTraceSupported = isScreenTraceSupported(activity);

        if (isScreenTraceSupported) {
            frameMetricsAggregator = new FrameMetricsAggregator();
        }
    }

    // region Public APIs

    /**
     * Force enable Hardware acceleration to support screen traces as we can't observe frame
     * rates for a non hardware accelerated view.
     * <p>
     * See: https://developer.android.com/guide/topics/graphics/hardware-accel
     */
    public static void enableHardwareAcceleration(Activity activity) {
        activity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
    }

    /**
     * Returns whether recording of screen traces are supported or not.
     */
    public boolean isScreenTraceSupported() {
        return isScreenTraceSupported;
    }

    /**
     * Starts recording the frame metrics for the screen traces.
     */
    public void recordScreenTrace() {
        if (!isScreenTraceSupported) {
            throw new IllegalArgumentException("Trying to record screen trace when it's not supported!");
        }

        Log.d(MyApp.LOG_TAG, "Recording screen trace " + traceName);

        frameMetricsAggregator.add(activity);
        perfScreenTrace = FirebasePerformance.startTrace(getScreenTraceName());
    }

    /**
     * Stops recording screen traces and dispatches the trace capturing information on %age of
     * Slow/Frozen frames.
     * <p>
     * Reference: Fireperf Source Code - https://bityl.co/5v22
     */
    public void sendScreenTrace() {
        if (perfScreenTrace == null) return;

        int totalFrames = 0;
        int slowFrames = 0;
        int frozenFrames = 0;

        // Stops recording metrics for this Activity and returns the currently-collected metrics
        SparseIntArray[] arr = frameMetricsAggregator.remove(activity);

        if (arr != null) {
            SparseIntArray frameTimes = arr[FrameMetricsAggregator.TOTAL_INDEX];

            if (frameTimes != null) {
                for (int i = 0; i < frameTimes.size(); i++) {
                    int frameTime = frameTimes.keyAt(i);
                    int numFrames = frameTimes.valueAt(i);

                    totalFrames += numFrames;

                    if (frameTime > Constants.FROZEN_FRAME_TIME) {
                        // Frozen frames mean the app appear frozen. The recommended thresholds is 700ms
                        frozenFrames += numFrames;
                    }

                    if (frameTime > Constants.SLOW_FRAME_TIME) {
                        // Slow frames are anything above 16ms (i.e. 60 frames/second)
                        slowFrames += numFrames;
                    }
                }
            }
        }

        if (totalFrames == 0 && slowFrames == 0 && frozenFrames == 0) {
            // All metrics are zero, no need to send screen trace.
            // return;
        }

        // Only incrementMetric if corresponding metric is non-zero.
        if (totalFrames > 0) {
            perfScreenTrace.putMetric(Constants.CounterNames.FRAMES_TOTAL.toString(), totalFrames);
        }
        if (slowFrames > 0) {
            perfScreenTrace.putMetric(Constants.CounterNames.FRAMES_SLOW.toString(), slowFrames);
        }
        if (frozenFrames > 0) {
            perfScreenTrace.putMetric(Constants.CounterNames.FRAMES_FROZEN.toString(), frozenFrames);
        }

        Log.d(MyApp.LOG_TAG, new StringBuilder()
                .append("sendScreenTrace ").append(traceName)
                .append(", name: ").append(getScreenTraceName())
                .append(", total_frames: ").append(totalFrames)
                .append(", slow_frames: ").append(slowFrames)
                .append(", frozen_frames: ").append(frozenFrames).toString());

        // Stop and record trace
        perfScreenTrace.stop();
    }

    // endregion

    // region Helper Functions

    /**
     * Reference: Fireperf Source Code - https://bityl.co/5v0Q
     */
    private static boolean isScreenTraceSupported(Activity activity) {
        boolean hasFrameMetricsAggregatorClass = hasFrameMetricsAggregatorClass();
        boolean isActivityHardwareAccelerated = activity.getWindow() != null
                && ((activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED) != 0);

        boolean supported = hasFrameMetricsAggregatorClass && isActivityHardwareAccelerated;

        Log.d(MyApp.LOG_TAG, new StringBuilder()
                .append("isScreenTraceSupported(").append(activity).append("): ").append(supported)
                .append(" [hasFrameMetricsAggregatorClass: ").append(hasFrameMetricsAggregatorClass)
                .append(", isActivityHardwareAccelerated: ").append(isActivityHardwareAccelerated).append("]").toString());

        return supported;
    }

    /**
     * Reference: Fireperf Source Code - https://bityl.co/5v0H
     */
    private static boolean hasFrameMetricsAggregatorClass() {
        try {
            Class<?> initializerClass = Class.forName(FRAME_METRICS_AGGREGATOR_CLASSNAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Reference: Fireperf Source Code - https://bityl.co/5v0V
     */
    private String getScreenTraceName() {
        return Constants.SCREEN_TRACE_PREFIX + traceName;
    }

    // endregion
}
