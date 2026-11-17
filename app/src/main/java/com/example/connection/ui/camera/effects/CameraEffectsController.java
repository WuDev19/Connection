package com.example.connection.ui.camera.effects;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connection.R;
import com.example.connection.ui.camera.CameraManager;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;

/** Selection UI has two independent groups backed by one immutable camera snapshot. */
public final class CameraEffectsController {
    private final View root;
    private final CameraManager camera;
    private final RecyclerView choices;
    private final Slider intensity;
    private final View intensityLabel;
    private final ChoiceAdapter adapter = new ChoiceAdapter();
    private boolean faceTab;

    public CameraEffectsController(View root, CameraManager camera) {
        this.root = root;
        this.camera = camera;
        choices = root.findViewById(R.id.effect_choices);
        intensity = root.findViewById(R.id.effect_intensity);
        intensityLabel = root.findViewById(R.id.effect_intensity_label);
        choices.setLayoutManager(new LinearLayoutManager(root.getContext(), RecyclerView.HORIZONTAL, false));
        choices.setAdapter(adapter);
        choices.setNestedScrollingEnabled(false);
        choices.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView view, @NonNull android.view.MotionEvent event) {
                boolean finished = event.getActionMasked() == android.view.MotionEvent.ACTION_UP
                        || event.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL;
                view.getParent().requestDisallowInterceptTouchEvent(!finished);
                return false;
            }
        });
        intensity.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                slider.getParent().requestDisallowInterceptTouchEvent(true);
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                slider.getParent().requestDisallowInterceptTouchEvent(false);
            }
        });
        intensity.setValue(camera.getEffectState().intensity);
        intensity.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) camera.setEffectState(camera.getEffectState().withIntensity(value));
        });

        View closeBtn = root.findViewById(R.id.btn_close_effects);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> hide());
        }

        TabLayout tabs = root.findViewById(R.id.effect_tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                faceTab = tab.getPosition() == 1;
                refresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        refresh();
    }

    public void show() {
        if (root.getVisibility() != View.VISIBLE) {
            root.setVisibility(View.VISIBLE);
            root.setAlpha(0f);
            root.setTranslationY(20f);
            root.animate().alpha(1f).translationY(0f).setDuration(200).start();
        }
    }

    public void hide() {
        if (root.getVisibility() == View.VISIBLE) {
            root.animate().alpha(0f).translationY(20f).setDuration(160).withEndAction(() -> {
                root.setVisibility(View.GONE);
                root.setAlpha(1f);
                root.setTranslationY(0f);
            }).start();
        }
    }

    public void toggleVisibility() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public boolean isVisible() {
        return root.getVisibility() == View.VISIBLE;
    }

    @android.annotation.SuppressLint("NotifyDataSetChanged") // Switching tabs replaces the complete small catalogue.
    private void refresh() {
        adapter.notifyDataSetChanged();
        intensity.setVisibility(faceTab ? View.GONE : View.VISIBLE);
        intensityLabel.setVisibility(faceTab ? View.GONE : View.VISIBLE);
        intensity.setEnabled(camera.getEffectState().color != CameraEffectState.Color.ORIGINAL);
    }

    public static void restore(CameraManager camera, Bundle saved) {
        if (saved == null) return;
        try {
            camera.setEffectState(new CameraEffectState(
                    CameraEffectState.Color.valueOf(saved.getString("camera.color", "ORIGINAL")),
                    CameraEffectState.Face.valueOf(saved.getString("camera.face", "NONE")),
                    saved.getFloat("camera.intensity", .7f)));
        } catch (IllegalArgumentException ignored) {
            camera.setEffectState(new CameraEffectState());
        }
    }

    public static void save(CameraManager camera, Bundle out) {
        CameraEffectState state = camera.getEffectState();
        out.putString("camera.color", state.color.name());
        out.putString("camera.face", state.face.name());
        out.putFloat("camera.intensity", state.intensity);
    }

    private final class ChoiceAdapter extends RecyclerView.Adapter<ChoiceHolder> {
        private final String[] icons = {"∅", "🐱", "🐰", "👑", "😈", "🕶", "💗", "🐈", "✦", "🌸", "💕"};
        private final int[] colors = {0xffe0e1e5, 0xfff4bb8b, 0xff9acde4, 0xffc7ad7e, 0xff777783};

        @NonNull
        @Override
        public ChoiceHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            LinearLayout item = new LinearLayout(parent.getContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(2), dp(4), dp(2));
            item.setLayoutParams(new RecyclerView.LayoutParams(dp(76), ViewGroup.LayoutParams.MATCH_PARENT));

            TextView icon = new TextView(parent.getContext());
            icon.setGravity(Gravity.CENTER);
            icon.setTextSize(22);
            item.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

            TextView label = new TextView(parent.getContext());
            label.setGravity(Gravity.CENTER);
            label.setTextSize(11);
            label.setTextColor(Color.BLACK);
            label.setMaxLines(2);
            label.setPadding(0, dp(2), 0, 0);
            item.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ChoiceHolder(item, icon, label);
        }

        @Override
        public void onBindViewHolder(@NonNull ChoiceHolder holder, int position) {
            String[] names = holder.itemView.getResources().getStringArray(faceTab ? R.array.camera_face_names : R.array.camera_color_names);
            CameraEffectState state = camera.getEffectState();
            boolean selected = position == (faceTab ? state.face.ordinal() : state.color.ordinal());
            holder.label.setText(names[position]);
            holder.label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            holder.label.setTextColor(selected ? 0xFF1B5E20 : Color.BLACK);
            holder.icon.setText(faceTab ? icons[position] : (position == 0 ? "○" : ""));

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(faceTab ? 0xfffff5f8 : colors[position]);
            bg.setStroke(dp(selected ? 3 : 1), selected ? 0xff2e7d32 : 0x22774455);
            holder.icon.setBackground(bg);
            holder.itemView.setSelected(selected);
            holder.itemView.setScaleX(selected ? 1.05f : 1f);
            holder.itemView.setScaleY(selected ? 1.05f : 1f);
            holder.itemView.setContentDescription(names[position] + (selected ? " ✓" : ""));

            holder.itemView.setOnClickListener(v -> {
                int index = holder.getBindingAdapterPosition();
                if (index == RecyclerView.NO_POSITION) return;
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
                CameraEffectState now = camera.getEffectState();
                camera.setEffectState(faceTab ? now.withFace(CameraEffectState.Face.values()[index]) : now.withColor(CameraEffectState.Color.values()[index]));
                refresh();
            });
        }

        @Override
        public int getItemCount() {
            return faceTab ? CameraEffectState.Face.values().length : CameraEffectState.Color.values().length;
        }

        private int dp(int value) {
            return Math.round(value * choices.getResources().getDisplayMetrics().density);
        }
    }

    private static final class ChoiceHolder extends RecyclerView.ViewHolder {
        final TextView icon, label;

        ChoiceHolder(View item, TextView icon, TextView label) {
            super(item);
            this.icon = icon;
            this.label = label;
        }
    }
}
