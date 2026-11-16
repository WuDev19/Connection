package com.example.connection;

import android.content.Context;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.connection.ui.view.widget.ZoomableMediaLayout;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ZoomableMediaLayoutTest {
    private static final class Host extends FrameLayout {
        boolean blocked;
        int intercepted;
        Host(Context context) { super(context); }
        @Override public void requestDisallowInterceptTouchEvent(boolean value) {
            blocked = value;
            super.requestDisallowInterceptTouchEvent(value);
        }
        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            if (!blocked && event.getActionMasked() == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1) {
                intercepted++;
                return true;
            }
            return false;
        }
        @Override public boolean onTouchEvent(MotionEvent event) { return true; }
    }

    private static final class Fixture {
        final Host host;
        final ZoomableMediaLayout zoom;
        final FrameLayout content;
        final AtomicInteger clicks = new AtomicInteger();
        final long downTime = SystemClock.uptimeMillis();
        long time = downTime;

        Fixture(boolean video) {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            host = new Host(context);
            zoom = new ZoomableMediaLayout(context);
            content = new FrameLayout(context);
            content.addView(video ? new TextureView(context) : new ImageView(context), new FrameLayout.LayoutParams(-1, -1));
            zoom.addView(content, new FrameLayout.LayoutParams(-1, -1));
            host.addView(zoom, new FrameLayout.LayoutParams(-1, -1));
            int size = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY);
            host.measure(size, size);
            host.layout(0, 0, 400, 400);
            zoom.setOnClickListener(view -> clicks.incrementAndGet());
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
            MotionEvent event = MotionEvent.obtain(downTime, time, action, count, properties, coordinates,
                    0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            host.dispatchTouchEvent(event);
            event.recycle();
        }

        void pinch() {
            touch(MotionEvent.ACTION_DOWN, 160, 200);
            touch(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 160, 200, 240, 200);
            touch(MotionEvent.ACTION_MOVE, 120, 200, 280, 200);
            touch(MotionEvent.ACTION_MOVE, 80, 200, 320, 200);
            touch(MotionEvent.ACTION_MOVE, 40, 200, 360, 200);
        }
    }

    @Test public void imageAndVideoPinchDoNotTriggerPauseOrParentScroll() {
        java.util.List<Fixture> fixtures = new java.util.ArrayList<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (boolean video : new boolean[]{false, true}) {
                Fixture fixture = new Fixture(video);
                fixture.pinch();
                assertTrue(fixture.content.getScaleX() > 1);
                assertTrue(fixture.host.blocked);
                assertEquals(0, fixture.host.intercepted);
                fixture.touch(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 40, 200, 360, 200);
                fixture.touch(MotionEvent.ACTION_MOVE, 100, 220);
                fixture.touch(MotionEvent.ACTION_UP, 100, 220);
                assertFalse(fixture.host.blocked);
                assertEquals(0, fixture.host.intercepted);
                fixtures.add(fixture);
            }
        });
        SystemClock.sleep(ViewConfiguration.getDoubleTapTimeout() + 300);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Fixture fixture : fixtures) {
                assertEquals(0, fixture.clicks.get());
                fixture.zoom.resetZoom();
                assertEquals(1, fixture.content.getScaleX(), 0.001f);
            }
        });
    }

    @Test public void normalOneFingerSwipeStillReachesParentList() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = new Fixture(false);
            fixture.touch(MotionEvent.ACTION_DOWN, 200, 200);
            fixture.touch(MotionEvent.ACTION_MOVE, 200, 300);
            fixture.touch(MotionEvent.ACTION_UP, 200, 300);
            assertTrue(fixture.host.intercepted > 0);
            assertEquals(1, fixture.content.getScaleX(), 0.001f);
            fixture.zoom.resetZoom();
        });
    }
}
