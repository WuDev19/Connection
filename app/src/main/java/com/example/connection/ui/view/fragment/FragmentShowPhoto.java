package com.example.connection.ui.view.fragment;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.recyclerview.widget.SnapHelper;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.example.connection.R;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.ui.adapter.ShowImageAdapter;
import com.example.connection.model.Image;
import com.example.connection.repository.base.PostRepository;
import com.example.connection.repository.impl.PostRepositoryImpl;
import com.example.connection.ui.viewmodel.ShowPhotoViewModel;
import com.example.connection.data.worker.WidgetProvider;
import com.example.connection.constants.MediaPlayerUtil;
import com.example.connection.ui.state.PhotoPlaybackController;
import androidx.lifecycle.Lifecycle;
import java.util.Objects;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FragmentShowPhoto extends Fragment {

    private RecyclerView recyclerView;
    private ShowImageAdapter adapter;
    private PostRepository postRepository;
    private final String TAG = getClass().getName();
    private int position;
    private int lastPosition = RecyclerView.NO_POSITION;
    private boolean isGeneralClick = false;
    private Image lastImage;
    private ShowPhotoViewModel showPhotoViewModel;
    private EditText edt_send_message;
    private PhotoPlaybackController playback;
    private String pendingPostId;
    private String pausedPostId;
    private boolean navigating;
    private int viewGeneration;
    private boolean resolvingInitialPost;
    private Button bt_like;
    private SnapHelper snapHelper;
    private View layoutNewPostsChip;
    private TextView tvNewPostsCount;
    private boolean hasNewPostsAbove = false;
    private int newPostsCount = 0;
    private final Handler bounceHandler = new Handler(Looper.getMainLooper());
    private Runnable bounceRunnable;

    public FragmentShowPhoto() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate: ");
        if (getArguments() != null) {
            position = getArguments().getInt("position_key", -1);
            isGeneralClick = getArguments().getBoolean("isGeneralClick", false);
            Log.e(TAG, "onCreate: position=" + position + ", isGeneralClick=" + isGeneralClick);
        }
        pendingPostId = getArguments() == null ? null : getArguments().getString("post_id");
        if (savedInstanceState != null) {
            pendingPostId = savedInstanceState.getString("visible_post_id", pendingPostId);
            pausedPostId = savedInstanceState.getString("paused_post_id");
            position = savedInstanceState.getInt("visible_position", position);
        }
        showPhotoViewModel = new ViewModelProvider(this).get(ShowPhotoViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_show_photo, container, false);
        initView(view);
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            isGeneralClick = getArguments().getBoolean("isGeneralClick", isGeneralClick);
        }

        viewGeneration++;
        postRepository = new PostRepositoryImpl();
        setUpRecyclerView();

        showPhotoViewModel.getUpdateWidget().observe(getViewLifecycleOwner(), shouldUpdate -> {
            if (shouldUpdate) {
                updateUiWidget();
            }
        });

        showPhotoViewModel.getSendMessageSuccess().observe(getViewLifecycleOwner(), aBoolean -> {
            if (Boolean.TRUE.equals(aBoolean) && edt_send_message != null) edt_send_message.setText("");
            Toast.makeText(getContext(), Boolean.TRUE.equals(aBoolean) ? "Gửi tin nhắn thành công" : "Gửi tin nhắn thất bại", Toast.LENGTH_LONG).show();
        });

        showPhotoViewModel.getExpressDone().observe(getViewLifecycleOwner(), done -> {
            if (done) {
                if (bt_like != null) {
                    bt_like.setEnabled(true);
                }
            }
        });

        backPress();
    }

    private void setUpRecyclerView() {
        final int generation = viewGeneration;
        recyclerView.post(() -> {
            if (!isAdded() || getView() == null || generation != viewGeneration) return;
            playback = new PhotoPlaybackController(requireContext());
            playback.restorePausedPost(pausedPostId);
            playback.setActive(false);
            adapter = new ShowImageAdapter(requireContext(), new ShowImageAdapter.onItemClick() {
                @Override
                public void generalImageClick() {
                    if (navigating || !isResumed()) return;
                    FragmentManager manager = requireActivity().getSupportFragmentManager();
                    if (manager.isStateSaved()) return;
                    navigating = true;
                    playback.setActive(false);
                    if (isGeneralClick) {
                        manager.popBackStack();
                        return;
                    }
                    Fragment current = manager.findFragmentById(R.id.frame_layout);
                    if (current == null) { navigating = false; return; }
                    ((MainActivity) requireActivity()).setClick(true);
                    manager.beginTransaction()
                            .hide(current)
                            .setMaxLifecycle(current, Lifecycle.State.STARTED)
                            .add(R.id.frame_layout, new FragmentGeneralPhoto(), "FragmentGeneralPhoto")
                            .addToBackStack("FragmentGeneralPhoto")
                            .commit();
                }

                @Override
                public void togglePlayback(String postId) {
                    if (playback != null && lastImage != null && Objects.equals(postId, lastImage.getPostId())) {
                        playback.toggle();
                    }
                }

                @Override
                public void onMediaBound() {
                    if (generation == viewGeneration) syncCurrentPlayback();
                }

                @Override
                public void sendMessage(String message, String postUrl, String otherId, String otherUserName, String avtPoster, EditText editText) {
                    edt_send_message = editText;
                    showPhotoViewModel.sendMessage(message, postUrl, otherId, otherUserName, avtPoster);
                }

                @Override
                public void expressEmote(String emote, String postId) {
                    showPhotoViewModel.expressEmote(emote, postId);
                }

                @Override
                public void showComment(String postId) {
                    FragmentComment fragmentComment = new FragmentComment();
                    Bundle bundle = new Bundle();
                    bundle.putString("postIdComment", postId);
                    fragmentComment.setArguments(bundle);
                    fragmentComment.show(requireActivity().getSupportFragmentManager(), fragmentComment.getTag());
                }

                @Override
                public void showEmote(String postId) {
                    FragmentEmotePost fragmentEmotePost = new FragmentEmotePost();
                    Bundle bundle = new Bundle();
                    bundle.putString("postId", postId);
                    fragmentEmotePost.setArguments(bundle);
                    fragmentEmotePost.show(requireActivity().getSupportFragmentManager(), fragmentEmotePost.getTag());
                }

                @Override
                public void showOptions(String postId, String url) {
                    FragmentOptionsPost fragmentOptionsPost = new FragmentOptionsPost();
                    Bundle bundle = new Bundle();
                    bundle.putString("postIdOptions", postId);
                    bundle.putString("postUrl", url);
                    fragmentOptionsPost.setArguments(bundle);
                    fragmentOptionsPost.show(requireActivity().getSupportFragmentManager(), fragmentOptionsPost.getTag());
                }

                @Override
                public void handleButton(Button btn_like) {
                    bt_like = btn_like;
                    btn_like.setEnabled(false);
                }
            }, recyclerView);

            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);
            snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(recyclerView);

            RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            }

            String myId = FirebaseAuth.getInstance().getUid();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (manager != null) {
                        int firstVisible = manager.findFirstVisibleItemPosition();
                        // Khi người dùng tự cuộn lên tới bài mới nhất (position 0), tự động ẩn chip thông báo
                        if (firstVisible == 0 && hasNewPostsAbove) {
                            hasNewPostsAbove = false;
                            newPostsCount = 0;
                            hideNewPostsChip();
                        }

                        if (myId != null) {
                            int totalItemCount = manager.getItemCount();
                            int lastVisible = manager.findLastVisibleItemPosition();
                            // Tải thêm bài cũ khi còn 2 bài là chạm đáy (Endless Pagination)
                            if (!resolvingInitialPost && pendingPostId == null && totalItemCount > 0 && lastVisible >= totalItemCount - 2) {
                                postRepository.loadMorePosts(myId, new PostRepository.OnLoadMoreCallback() {
                                    @Override
                                    public void onLoaded(List<Image> olderPosts) {
                                        if (generation == viewGeneration && getView() != null && olderPosts != null && !olderPosts.isEmpty()) {
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
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (manager != null) {
                            int firstVisible = manager.findFirstVisibleItemPosition();
                            if (firstVisible == 0 && hasNewPostsAbove) {
                                hasNewPostsAbove = false;
                                newPostsCount = 0;
                                hideNewPostsChip();
                            }
                            View snapView = snapHelper.findSnapView(manager);
                            if (snapView != null) {
                                handleMusic(manager, snapView);
                            }
                        }
                        if (bt_like != null) {
                            bt_like.setEnabled(true);
                        }
                    } else {
                        if (playback != null) playback.setActive(false);
                    }
                }
            });

            // Bắt đầu lắng nghe Top bài đăng mới nhất theo mô hình Hybrid Realtime
            if (myId != null) {
                int initialLimit = Math.max(10, Math.min(100, position + 5));
                postRepository.listenTopPosts(myId, initialLimit, topPosts -> {
                    if (generation != viewGeneration || getView() == null || topPosts == null) return;

                    if (adapter.getItemCount() == 0) {
                        adapter.setInitialPosts(topPosts);
                        resolveInitialPost(generation);
                        hasNewPostsAbove = false;
                        newPostsCount = 0;
                        if (layoutNewPostsChip != null) {
                            layoutNewPostsChip.setVisibility(View.GONE);
                        }
                        return;
                    }

                    // 1. Cập nhật các bài đã hiển thị (likes, comments, emotes)
                    adapter.updateExistingPosts(topPosts);
                    if (pendingPostId != null) resolveInitialPost(generation);
                    recyclerView.post(this::syncCurrentPlayback);

                    // 2. Lọc các bài mới tinh chưa có trong adapter
                    List<Image> brandNewPosts = new ArrayList<>();
                    for (Image post : topPosts) {
                        if (post.getPostId() != null && !adapter.containsPostId(post.getPostId())) {
                            brandNewPosts.add(post);
                        }
                    }

                    // 3. Nếu có bài mới: Chèn vào đầu danh sách, giữ vị trí scroll hiện tại và hiển thị chip
                    if (!brandNewPosts.isEmpty()) {
                        LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        int firstPos = (manager != null) ? manager.findFirstVisibleItemPosition() : RecyclerView.NO_POSITION;
                        View firstView = (manager != null && firstPos != RecyclerView.NO_POSITION) ? manager.findViewByPosition(firstPos) : null;
                        int topOffset = (firstView != null) ? firstView.getTop() : 0;

                        int insertedCount = adapter.insertNewPostsAtTop(brandNewPosts);
                        if (insertedCount > 0) {
                            if (lastPosition != RecyclerView.NO_POSITION) {
                                lastPosition += insertedCount;
                            }
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

                            // Chỉ hiển thị nút thông báo nếu có bài mới từ người khác
                            if (othersNewCount > 0) {
                                hasNewPostsAbove = true;
                                newPostsCount += othersNewCount;
                                showNewPostsChip(newPostsCount);
                            }
                        }
                    }
                });
            }

        });
    }

    private void updateUiWidget() {
        Context context = requireContext().getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, WidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(widgetComponent);
        for (int widgetId : widgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
            views.setTextViewText(R.id.tv_widget, "");
            manager.partiallyUpdateAppWidget(widgetId, views);
        }
    }

    private void handleMusic(LinearLayoutManager manager, View snapView) {
        if (!isResumed() || playback == null || pendingPostId != null || resolvingInitialPost) return;
        int selected = manager.getPosition(snapView);
        Image image = adapter.getItem(selected);
        if (image == null || selected == RecyclerView.NO_POSITION) return;
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(selected);
        if (!(holder instanceof ShowImageAdapter.ShowImageViewHolder)) return;
        lastPosition = selected;
        lastImage = image;
        Image next = adapter.getItem(selected + 1);
        playback.select(image, ((ShowImageAdapter.ShowImageViewHolder) holder).playerViewVideo,
                next == null ? null : next.getVideoUrl());
        playback.setActive(true);
    }

    private void syncCurrentPlayback() {
        if (recyclerView == null || adapter == null || snapHelper == null || !isResumed()
                || recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) return;
        LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (manager == null) return;
        View snapped = snapHelper.findSnapView(manager);
        if (snapped != null) handleMusic(manager, snapped);
    }

    private void resolveInitialPost(int generation) {
        if (generation != viewGeneration || recyclerView == null || resolvingInitialPost) return;
        int target = -1;
        if (pendingPostId != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (pendingPostId.equals(adapter.getItem(i).getPostId())) { target = i; break; }
            }
            if (target < 0) {
                resolvingInitialPost = true;
                postRepository.loadMorePosts(FirebaseAuth.getInstance().getUid(), 30, new PostRepository.OnLoadMoreCallback() {
                    @Override public void onLoaded(List<Image> posts) {
                        if (generation != viewGeneration || getView() == null) return;
                        resolvingInitialPost = false;
                        if (posts == null || posts.isEmpty()) {
                            pendingPostId = null;
                            position = 0;
                            Toast.makeText(requireContext(), "Bài viết không còn trong danh sách", Toast.LENGTH_SHORT).show();
                        } else adapter.addOlderPosts(posts);
                        resolveInitialPost(generation);
                    }
                    @Override public void onError(Exception error) {
                        if (generation != viewGeneration || getView() == null) return;
                        resolvingInitialPost = false;
                        Toast.makeText(requireContext(), "Không tải được bài viết. Hãy mở lại ảnh để thử lại.", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
        }
        if (target < 0) target = Math.max(0, Math.min(position, adapter.getItemCount() - 1));
        pendingPostId = null;
        position = target;
        ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(target, 0);
        recyclerView.post(this::syncCurrentPlayback);
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_show_photo);
        layoutNewPostsChip = view.findViewById(R.id.layout_new_posts_chip);
        tvNewPostsCount = view.findViewById(R.id.tv_new_posts_count);

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
        // 1. Nhún nhẹ chuẩn bị nhảy (100ms)
        view.animate()
                .scaleX(1.06f)
                .scaleY(0.92f)
                .translationY(3f)
                .setDuration(100)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    // 2. Nhảy bổng lên cao với độ giãn dài cute (180ms)
                    view.animate()
                            .scaleX(0.94f)
                            .scaleY(1.08f)
                            .translationY(-14f)
                            .setDuration(180)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> {
                                // 3. Tiếp đất và nảy nhẹ (140ms)
                                view.animate()
                                        .scaleX(1.04f)
                                        .scaleY(0.96f)
                                        .translationY(0f)
                                        .setDuration(140)
                                        .setInterpolator(new AccelerateInterpolator())
                                        .withEndAction(() -> {
                                            // 4. Bật nảy phụ một nhịp nhỏ (120ms)
                                            view.animate()
                                                    .scaleX(0.98f)
                                                    .scaleY(1.03f)
                                                    .translationY(-5f)
                                                    .setDuration(120)
                                                    .setInterpolator(new DecelerateInterpolator())
                                                    .withEndAction(() -> {
                                                        // 5. Trở lại trạng thái bình thường (100ms)
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

    private void backPress() {
        if (!isGeneralClick) return;
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                FragmentManager manager = requireActivity().getSupportFragmentManager();
                if (navigating || manager.isStateSaved()) return;
                navigating = true;
                if (playback != null) playback.setActive(false);
                manager.popBackStack();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        navigating = false;
        MediaPlayerUtil.getInstance().setIsInGeneral(false);
        ((MainActivity) requireActivity()).setHideFab();
        if (isGeneralClick) ((MainActivity) requireActivity()).setClick(true);
        if (recyclerView != null) recyclerView.post(this::syncCurrentPlayback);
        showPhotoViewModel.updateSeenPost(requireContext());
    }

    @Override
    public void onPause() {
        if (playback != null) playback.setActive(false);
        stopCuteBounceAnimation();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("visible_post_id", lastImage == null ? pendingPostId : lastImage.getPostId());
        outState.putInt("visible_position", lastPosition < 0 ? position : lastPosition);
        outState.putString("paused_post_id", playback == null ? pausedPostId : playback.getPausedPostId());
    }

    @Override
    public void onDestroyView() {
        viewGeneration++;
        if (lastImage != null) pendingPostId = lastImage.getPostId();
        if (lastPosition >= 0) position = lastPosition;
        if (playback != null) {
            pausedPostId = playback.getPausedPostId();
            playback.release();
            playback = null;
        }
        postRepository.removeListener();
        stopCuteBounceAnimation();
        if (layoutNewPostsChip != null) layoutNewPostsChip.animate().cancel();
        if (snapHelper != null) snapHelper.attachToRecyclerView(null);
        if (recyclerView != null) {
            recyclerView.clearOnScrollListeners();
            recyclerView.setAdapter(null);
        }
        adapter = null;
        recyclerView = null;
        snapHelper = null;
        layoutNewPostsChip = null;
        tvNewPostsCount = null;
        edt_send_message = null;
        bt_like = null;
        resolvingInitialPost = false;
        lastPosition = RecyclerView.NO_POSITION;
        super.onDestroyView();
    }
}
