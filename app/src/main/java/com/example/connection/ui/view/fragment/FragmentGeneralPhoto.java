package com.example.connection.ui.view.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.ui.adapter.GeneralPhotoAdapter;
import com.example.connection.model.Image;
import com.example.connection.repository.PostRepository;
import com.example.connection.constants.MediaPlayerUtil;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FragmentGeneralPhoto extends Fragment {

    private RecyclerView recyclerView;
    private GeneralPhotoAdapter adapter;
    private PostRepository postRepository;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private View layoutNewPostsChip;
    private TextView tvNewPostsCount;
    private boolean hasNewPostsAbove = false;
    private int newPostsCount = 0;
    private final Handler bounceHandler = new Handler(Looper.getMainLooper());
    private Runnable bounceRunnable;
    private final String TAG = "FragmentGeneralPhoto";
    private FragmentHome fragmentHome;
    private OnBackPressedCallback generalPhotoBackCallback;

    public FragmentGeneralPhoto() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postRepository = new PostRepository();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_general_photo, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpRecyclerView();
        setupBackPress();
    }

    private void setUpRecyclerView() {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new GeneralPhotoAdapter(getContext(), position -> {
            FragmentShowPhoto fragmentShowPhoto = new FragmentShowPhoto();
            Bundle bundle = new Bundle();
            bundle.putInt("position_key", position);
            bundle.putBoolean("isGeneralClick", true);
            Log.e("Position", position + "general");
            fragmentShowPhoto.setArguments(bundle);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, fragmentShowPhoto, "FragmentShowPhoto")
                    .addToBackStack("FragmentShowPhoto")
                    .commit();
        });
        recyclerView.setAdapter(adapter);

        // Lắng nghe sự kiện cuộn: Tự động ẩn chip khi về đỉnh & Phân trang vô tận khi chạm đáy
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (manager != null) {
                    int firstVisible = manager.findFirstVisibleItemPosition();
                    // Khi người dùng cuộn lên tới ảnh đầu tiên (position = 0), tự động ẩn chip
                    if (firstVisible == 0 && hasNewPostsAbove) {
                        hasNewPostsAbove = false;
                        newPostsCount = 0;
                        hideNewPostsChip();
                    }

                    int totalItemCount = manager.getItemCount();
                    int lastVisible = manager.findLastVisibleItemPosition();
                    // Phân trang: Khi còn khoảng 2 hàng (6 ảnh) là chạm đáy -> tải thêm 21 ảnh cũ
                    if (totalItemCount > 0 && lastVisible >= totalItemCount - 6) {
                        postRepository.loadMorePosts(myId, 21, new PostRepository.OnLoadMoreCallback() {
                            @Override
                            public void onLoaded(List<Image> olderPosts) {
                                if (getActivity() != null && isAdded() && olderPosts != null && !olderPosts.isEmpty()) {
                                    adapter.addOlderPosts(olderPosts);
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "loadMore error: " + e.getMessage());
                            }
                        });
                    }
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
                    if (manager != null) {
                        int firstVisible = manager.findFirstVisibleItemPosition();
                        if (firstVisible == 0 && hasNewPostsAbove) {
                            hasNewPostsAbove = false;
                            newPostsCount = 0;
                            hideNewPostsChip();
                        }
                    }
                }
            }
        });

        // Bắt đầu lắng nghe Realtime Top 30 ảnh mới nhất
        postRepository.listenTopPosts(myId, 30, topPosts -> {
            if (getActivity() == null || !isAdded() || topPosts == null) return;
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            // Lần nạp dữ liệu đầu tiên
            if (adapter.getItemCount() == 0) {
                adapter.setInitialPosts(topPosts);
                hasNewPostsAbove = false;
                newPostsCount = 0;
                if (layoutEmpty != null) {
                    layoutEmpty.setVisibility(topPosts.isEmpty() ? View.VISIBLE : View.GONE);
                }
                hideNewPostsChip();
                return;
            }

            if (layoutEmpty != null) {
                layoutEmpty.setVisibility(View.GONE);
            }

            // 1. Cập nhật các bài đã hiển thị
            adapter.updateExistingPosts(topPosts);

            // 2. Lọc các bài mới tinh chưa có trong adapter
            List<Image> brandNewPosts = new ArrayList<>();
            for (Image post : topPosts) {
                if (post.getPostId() != null && !adapter.containsPostId(post.getPostId())) {
                    brandNewPosts.add(post);
                }
            }

            // 3. Nếu có bài mới: Chèn vào đầu danh sách, giữ vị trí scroll hiện tại và hiển thị chip
            if (!brandNewPosts.isEmpty()) {
                GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
                int firstPos = (manager != null) ? manager.findFirstVisibleItemPosition() : RecyclerView.NO_POSITION;
                View firstView = (manager != null && firstPos != RecyclerView.NO_POSITION) ? manager.findViewByPosition(firstPos) : null;
                int topOffset = (firstView != null) ? firstView.getTop() : 0;

                int insertedCount = adapter.insertNewPostsAtTop(brandNewPosts);
                if (insertedCount > 0) {
                    if (manager != null && firstPos != RecyclerView.NO_POSITION) {
                        manager.scrollToPositionWithOffset(firstPos + insertedCount, topOffset);
                    }

                    // Đếm số bài mới KHÔNG PHẢI của chính mình
                    int othersNewCount = 0;
                    for (Image post : brandNewPosts) {
                        if (post.getPosterId() != null && !post.getPosterId().equals(myId)) {
                            othersNewCount++;
                        }
                    }

                    // Chỉ hiện thông báo nếu có bài mới từ người khác
                    if (othersNewCount > 0) {
                        hasNewPostsAbove = true;
                        newPostsCount += othersNewCount;
                        showNewPostsChip(newPostsCount);
                    }
                }
            }
        });
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_general_photo);
        progressBar = view.findViewById(R.id.progress_general_photo);
        layoutEmpty = view.findViewById(R.id.layout_empty_general);
        layoutNewPostsChip = view.findViewById(R.id.layout_new_posts_chip_general);
        tvNewPostsCount = view.findViewById(R.id.tv_new_posts_count_general);

        if (layoutNewPostsChip != null) {
            layoutNewPostsChip.setOnClickListener(v -> {
                hasNewPostsAbove = false;
                newPostsCount = 0;
                hideNewPostsChip();
                recyclerView.smoothScrollToPosition(0);
            });
        }
    }

    private void showNewPostsChip(int count) {
        if (tvNewPostsCount != null) {
            String message = count > 9 ? "Có 9+ bài mới nè! ✨" : (count > 1 ? "Có " + count + " bài mới nè! ✨" : "Có bài mới nè! ✨");
            tvNewPostsCount.setText(message);
        }
        if (layoutNewPostsChip != null && layoutNewPostsChip.getVisibility() != View.VISIBLE) {
            layoutNewPostsChip.setAlpha(0f);
            layoutNewPostsChip.setTranslationY(-30f);
            layoutNewPostsChip.setScaleX(0.85f);
            layoutNewPostsChip.setScaleY(0.85f);
            layoutNewPostsChip.setVisibility(View.VISIBLE);
            layoutNewPostsChip.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .setDuration(350)
                    .withEndAction(this::startCuteBounceAnimation)
                    .start();
        } else if (layoutNewPostsChip != null && layoutNewPostsChip.getVisibility() == View.VISIBLE) {
            performCuteHop(layoutNewPostsChip);
        }
    }

    private void hideNewPostsChip() {
        stopCuteBounceAnimation();
        if (layoutNewPostsChip != null && layoutNewPostsChip.getVisibility() == View.VISIBLE) {
            layoutNewPostsChip.animate().cancel();
            layoutNewPostsChip.animate()
                    .alpha(0f)
                    .translationY(-30f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (layoutNewPostsChip != null) {
                            layoutNewPostsChip.setVisibility(View.GONE);
                            layoutNewPostsChip.setScaleX(1f);
                            layoutNewPostsChip.setScaleY(1f);
                            layoutNewPostsChip.setTranslationY(0f);
                        }
                    })
                    .start();
        }
    }

    private void startCuteBounceAnimation() {
        stopCuteBounceAnimation();
        bounceRunnable = new Runnable() {
            @Override
            public void run() {
                if (layoutNewPostsChip != null && layoutNewPostsChip.getVisibility() == View.VISIBLE) {
                    performCuteHop(layoutNewPostsChip);
                    bounceHandler.postDelayed(this, 3800);
                }
            }
        };
        bounceHandler.postDelayed(bounceRunnable, 1500);
    }

    private void stopCuteBounceAnimation() {
        if (bounceRunnable != null) {
            bounceHandler.removeCallbacks(bounceRunnable);
            bounceRunnable = null;
        }
    }

    private void performCuteHop(View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) return;
        view.animate()
                .scaleX(1.06f)
                .scaleY(0.92f)
                .translationY(3f)
                .setDuration(100)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(0.94f)
                            .scaleY(1.08f)
                            .translationY(-14f)
                            .setDuration(180)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> {
                                view.animate()
                                        .scaleX(1.04f)
                                        .scaleY(0.96f)
                                        .translationY(0f)
                                        .setDuration(140)
                                        .setInterpolator(new AccelerateInterpolator())
                                        .withEndAction(() -> {
                                            view.animate()
                                                    .scaleX(0.98f)
                                                    .scaleY(1.03f)
                                                    .translationY(-5f)
                                                    .setDuration(120)
                                                    .setInterpolator(new DecelerateInterpolator())
                                                    .withEndAction(() -> {
                                                        view.animate()
                                                                .scaleX(1.0f)
                                                                .scaleY(1.0f)
                                                                .translationY(0f)
                                                                .setDuration(100)
                                                                .setInterpolator(new OvershootInterpolator())
                                                                .start();
                                                    })
                                                    .start();
                                        })
                                        .start();
                            })
                            .start();
                })
                .start();
    }

    private void setupBackPress() {
        generalPhotoBackCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity == null) return;
                mainActivity.setClick(false);
                MediaPlayerUtil.getInstance().setIsInGeneral(false);
                MediaPlayerUtil.getInstance().setWhenBackPressFromGeneral(true);
                FragmentManager manager = mainActivity.getSupportFragmentManager();
                fragmentHome = (FragmentHome) manager.findFragmentByTag("FragmentHome");
                if (fragmentHome == null) fragmentHome = new FragmentHome();
                manager.beginTransaction()
                        .replace(R.id.frame_layout, fragmentHome, "FragmentHome")
                        .commit();
                Log.e("backpress", "handleOnBackPressed: returned to FragmentHome");
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), generalPhotoBackCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.setClick(true);
        }
        if (generalPhotoBackCallback != null) {
            generalPhotoBackCallback.setEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCuteBounceAnimation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "onDestroyView: ");
        stopCuteBounceAnimation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        stopCuteBounceAnimation();
        if (postRepository != null) {
            postRepository.removeListener();
        }
    }
}
