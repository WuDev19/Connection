package com.example.connection.ui.view.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import com.example.connection.R;
import com.example.connection.ui.adapter.GuideWidgetAdapter;
import com.example.connection.model.Guide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.Arrays;
import java.util.List;

public class GuideWidget extends BottomSheetDialogFragment {

    private View view1, view2, view3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_guide_widget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_guide_widget);
        view1 = view.findViewById(R.id.view_vuot1);
        view2 = view.findViewById(R.id.view_vuot2);
        view3 = view.findViewById(R.id.view_vuot3);
        Guide guide1 = new Guide(R.drawable.anhminhhoa1, "Giữ vào khoảng trắng bất kỳ trên màn hình");
        Guide guide2 = new Guide(R.drawable.anhminhhoa2, "Chọn vào biểu tượng có chữ Tiện ích");
        Guide guide3 = new Guide(R.drawable.anhminhhoa3, "Tìm và chọn tiện ích Connection");
        List<Guide> list = Arrays.asList(guide1, guide2, guide3);
        GuideWidgetAdapter adapter = new GuideWidgetAdapter(requireContext(), list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if(manager != null){
                    View snapView = snapHelper.findSnapView(manager);
                    if(snapView != null){
                        int position = manager.getPosition(snapView);
                        if(position == 0){
                            view1.setBackgroundResource(R.drawable.button_corner_black);
                            view2.setBackgroundResource(R.drawable.button_corner);
                            view3.setBackgroundResource(R.drawable.button_corner);
                        }
                        else if (position == 1){
                            view1.setBackgroundResource(R.drawable.button_corner);
                            view2.setBackgroundResource(R.drawable.button_corner_black);
                            view3.setBackgroundResource(R.drawable.button_corner);
                        }
                        else if (position == 2){
                            view1.setBackgroundResource(R.drawable.button_corner);
                            view2.setBackgroundResource(R.drawable.button_corner);
                            view3.setBackgroundResource(R.drawable.button_corner_black);
                        }
                    }
                }
            }
        });

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        bottomSheetDialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog dialog = (BottomSheetDialog) dialogInterface;
            FrameLayout frameLayout = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if(frameLayout != null){
                frameLayout.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                frameLayout.requestLayout();
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(frameLayout);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return bottomSheetDialog;
    }

}
