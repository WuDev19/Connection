package com.example.connection;

import android.content.Context;
import android.os.SystemClock;
import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.connection.ui.camera.CameraPinchZoomHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Synthetic preview touches only: these tests never open a camera or capture media. */
@RunWith(AndroidJUnit4.class)
public class CameraPinchZoomHandlerTest {
    private static final class Preview {
        final View view;
        final CameraPinchZoomHandler handler;
        final AtomicBoolean blocked = new AtomicBoolean();
        final AtomicInteger clicks = new AtomicInteger();
        float ratio = 1;
        final float spanStep;
        final float center;
        final long down = SystemClock.uptimeMillis();
        long time = down;

        Preview() {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            ViewConfiguration configuration = ViewConfiguration.get(context);
            // Gesture thresholds are physical-device dependent, not a fixed number of pixels.
            spanStep = Math.max(80, Build.VERSION.SDK_INT >= 29
                    ? configuration.getScaledMinimumScalingSpan() : configuration.getScaledTouchSlop() * 8);
            int side = (int) spanStep * 8;
            center = side / 2f;
            view = new View(context);
            FrameLayout host = new FrameLayout(context);
            host.addView(view, new FrameLayout.LayoutParams(side, side));
            int size = View.MeasureSpec.makeMeasureSpec(side, View.MeasureSpec.EXACTLY);
            host.measure(size, size);
            host.layout(0, 0, side, side);
            view.setOnClickListener(v -> clicks.incrementAndGet());
            handler = new CameraPinchZoomHandler(view, factor -> ratio *= factor, blocked::set);
            view.setOnTouchListener(handler);
        }

        void touch(int action, float... points) {
            int count = points.length / 2;
            MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[count];
            MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[count];
            for (int i = 0; i < count; i++) {
                properties[i] = new MotionEvent.PointerProperties();
                properties[i].id = i;
                properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
                coordinates[i] = new MotionEvent.PointerCoords();
                coordinates[i].x = points[i * 2];
                coordinates[i].y = points[i * 2 + 1];
                coordinates[i].pressure = coordinates[i].size = 1;
            }
            time += 20;
            MotionEvent event = MotionEvent.obtain(down, time, action, count, properties, coordinates,
                    0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            view.dispatchTouchEvent(event);
            event.recycle();
        }
    }

    @Test public void pinchControlsCameraZoomWithoutScalingPreviewOrClicking() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Preview preview = new Preview();
            float center = preview.center;
            float step = preview.spanStep;
            preview.touch(MotionEvent.ACTION_DOWN, center - step / 4, center);
            preview.touch(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    center - step / 4, center, center + step / 4, center);
            assertTrue(preview.blocked.get());
            for (int i = 1; i <= 4; i++) {
                preview.touch(MotionEvent.ACTION_MOVE, center - step * i / 2, center, center + step * i / 2, center);
            }
            assertTrue("Zoom callback did not run; span threshold=" + step, preview.ratio > 1);
            assertEquals(1f, preview.view.getScaleX(), 0.001f);
            preview.touch(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    center - step * 2, center, center + step * 2, center);
            assertTrue(preview.blocked.get());
            preview.touch(MotionEvent.ACTION_UP, center - step * 2, center);
            assertFalse(preview.blocked.get());
            assertEquals(0, preview.clicks.get());
            preview.handler.detach();
        });
    }

    @Test public void singleTapStillClicksAndCancellationReleasesTheParent() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Preview preview = new Preview();
            preview.touch(MotionEvent.ACTION_DOWN, 200, 200);
            preview.touch(MotionEvent.ACTION_UP, 200, 200);
            assertEquals(1, preview.clicks.get());
            preview.touch(MotionEvent.ACTION_DOWN, 160, 200);
            preview.touch(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 160, 200, 240, 200);
            assertTrue(preview.blocked.get());
            preview.handler.cancel();
            assertFalse(preview.blocked.get());
            assertEquals(1, preview.clicks.get());
            preview.handler.detach();
        });
    }
}
