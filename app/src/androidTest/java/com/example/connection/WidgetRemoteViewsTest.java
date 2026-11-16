package com.example.connection;

import android.content.Context;
import android.graphics.Outline;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WidgetRemoteViewsTest {
    @Test public void widgetInflatesWithClippedCornersAndRefreshButton() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
            views.setTextViewText(R.id.tv_widget, "9+");
            views.setViewVisibility(R.id.tv_widget, View.VISIBLE);
            View root = views.apply(context, new FrameLayout(context));
            int width = (int) (240 * context.getResources().getDisplayMetrics().density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, width);
            assertTrue(root.getClipToOutline());
            Outline outline = new Outline();
            // This detached host has not drawn a frame yet, so View has not set background bounds.
            root.getBackground().setBounds(0, 0, width, width);
            root.getBackground().getOutline(outline);
            assertEquals(context.getResources().getDimension(R.dimen.widget_corner_radius), outline.getRadius(), 0.1f);
            assertNotNull(root.findViewById(R.id.widget_refresh));
            assertEquals("9+", ((TextView) root.findViewById(R.id.tv_widget)).getText().toString());
        });
    }
}
