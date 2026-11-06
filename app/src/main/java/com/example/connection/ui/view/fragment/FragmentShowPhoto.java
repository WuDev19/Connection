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
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.recyclerview.widget.SnapHelper;
import androidx.viewpager2.widget.ViewPager2;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.example.connection.repository.PostRepository;
import com.example.connection.ui.viewmodel.ShowPhotoViewModel;
import com.example.connection.data.worker.WidgetProvider;
import com.example.connection.constants.MediaPlayerUtil;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FragmentShowPhoto extends Fragment {

    private RecyclerView recyclerView;
    private ShowImageAdapter adapter;
    private PostRepository postRepository;
    private final String TAG = getClass().getName();
    private int position;
    private FragmentGeneralPhoto fragmentGeneralPhoto;
    private int lastPosition = RecyclerView.NO_POSITION;
    private boolean isGeneralClick = false;
    private Image lastImage;
    private ShowPhotoViewModel showPhotoViewModel;
    private EditText edt_send_message;
    private ExoPlayer exoPlayer;
    private boolean onceScroll;
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
        showPhotoViewModel = new ViewModelProvider(this).get(ShowPhotoViewModel.class);
        postRepository = new PostRepository();
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

        setUpRecyclerView();

        showPhotoViewModel.updateSeenPost(requireContext());

        showPhotoViewModel.getUpdateWidget().observe(getViewLifecycleOwner(), shouldUpdate -> {
            if (shouldUpdate) {
                updateUiWidget();
            }
        });

        showPhotoViewModel.getSendMessageSuccess().observe(getViewLifecycleOwner(), aBoolean -> {
            edt_send_message.setText("");
            Toast.makeText(getContext(), "Gửi tin nhắn thành công", Toast.LENGTH_LONG).show();
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
        recyclerView.post(() -> {
            if (getArguments() != null) {
                position = getArguments().getInt("position_key", -1);
                isGeneralClick = getArguments().getBoolean("isGeneralClick", false);
                Log.e("TAGG", position + "show");
            }

            exoPlayer = new ExoPlayer.Builder(requireContext()).build();
            adapter = new ShowImageAdapter(requireContext(), new ShowImageAdapter.onItemClick() {
                @Override
                public void generalImageClick() {
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(0);
                    }
                    MediaPlayerUtil.getInstance().setClickGeneral(true);
                    MediaPlayerUtil.getInstance().pauseMusic();
                    MediaPlayerUtil.getInstance().setIsInGeneral(true);
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.setClick(true);
                        fragmentGeneralPhoto = (FragmentGeneralPhoto) requireActivity().getSupportFragmentManager().findFragmentByTag("FragmentGeneralPhoto");
                        if (fragmentGeneralPhoto == null) {
                            fragmentGeneralPhoto = new FragmentGeneralPhoto();
                        }
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_layout, fragmentGeneralPhoto, "FragmentGeneralPhoto")
                                .addToBackStack("FragmentGeneralPhoto")
                                .commit();
                    }
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
            }, recyclerView, position, isGeneralClick, exoPlayer);

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
                            if (totalItemCount > 0 && lastVisible >= totalItemCount - 2) {
                                postRepository.loadMorePosts(myId, new PostRepository.OnLoadMoreCallback() {
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
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        MediaPlayerUtil.getInstance().pauseMusic();
                        exoPlayer.pause();
                    }
                }
            });

            // Bắt đầu lắng nghe Top bài đăng mới nhất theo mô hình Hybrid Realtime
            if (myId != null) {
                int initialLimit = (isGeneralClick && position >= 10) ? (position + 5) : 10;
                postRepository.listenTopPosts(myId, initialLimit, topPosts -> {
                    if (getActivity() == null || !isAdded() || topPosts == null) return;

                    if (adapter.getItemCount() == 0) {
                        adapter.setInitialPosts(topPosts);
                        hasNewPostsAbove = false;
                        newPostsCount = 0;
                        if (layoutNewPostsChip != null) {
                            layoutNewPostsChip.setVisibility(View.GONE);
                        }
                        return;
                    }

                    // 1. Cập nhật các bài đã hiển thị (likes, comments, emotes)
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

            scrollWhenClickGeneralFrag();
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
        int position = manager.getPosition(snapView);
        if (position != lastPosition) {
            lastPosition = position;
            lastImage = adapter.getItem(lastPosition);
            if (lastImage != null) {
                MediaPlayerUtil.getInstance().playMusic(requireContext(), lastImage.getUrlMusic());
                playVideo(lastImage);
            }
        } else {
            Image image1 = adapter.getItem(position);
            if (image1 != null) {
                if (image1.equals(lastImage)) {
                    MediaPlayerUtil.getInstance().resumeMusic();
                } else {
                    MediaPlayerUtil.getInstance().playMusic(requireContext(), image1.getUrlMusic());
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void scrollWhenClickGeneralFrag() {
        GestureDetector gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if (!onceScroll && e1 != null) {
                    float dx = e2.getRawX() - e1.getRawX();
                    float dy = Math.abs(e2.getRawY() - e1.getRawY());
                    if (dx > 50 && dy < 15) {
                        onceScroll = true;
                        Toast.makeText(requireContext(), "Ấn back để quay lại màn hình camera", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                return true;
            }

            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                onceScroll = false;
                return true;
            }
        });

        if (MediaPlayerUtil.getInstance().isClickGeneral()) {
            recyclerView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return false;
            });
        }
    }

    private void playVideo(Image model) {
        if (model != null && model.getVideoUrl() != null) {
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(lastPosition);
            if (vh instanceof ShowImageAdapter.ShowImageViewHolder) {
                ShowImageAdapter.ShowImageViewHolder videoHolder = (ShowImageAdapter.ShowImageViewHolder) vh;
                videoHolder.playerViewVideo.setPlayer(null);
                videoHolder.playerViewVideo.setPlayer(exoPlayer);
                MediaItem mediaItem = MediaItem.fromUri(model.getVideoUrl());
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.play();
            }
        }
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
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity == null) return;

                // Nếu đây là FragmentShowPhoto được mở từ GeneralPhoto (xem chi tiết 1 ảnh)
                if (isGeneralClick) {
                    MediaPlayerUtil.getInstance().pauseMusic();
                    MediaPlayerUtil.getInstance().setIsInGeneral(true);
                    FragmentManager manager = mainActivity.getSupportFragmentManager();
                    fragmentGeneralPhoto = (FragmentGeneralPhoto) manager.findFragmentByTag("FragmentGeneralPhoto");
                    if (fragmentGeneralPhoto == null) fragmentGeneralPhoto = new FragmentGeneralPhoto();
                    manager.beginTransaction()
                            .replace(R.id.frame_layout, fragmentGeneralPhoto, "FragmentGeneralPhoto")
                            .commit();
                    return;
                }

                // Nếu đang ở màn hình GeneralPhoto che lên màn hình (isClick == true):
                // ShowPhoto trong ViewPager KHÔNG can thiệp, nhường quyền cho GeneralPhoto xử lý
                if (mainActivity.isClick()) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                    return;
                }

                // Bình thường: Vuốt về tab camera (0)
                ViewPager2 viewPager2 = mainActivity.findViewById(R.id.view_pager);
                if (viewPager2 != null && viewPager2.getCurrentItem() != 0) {
                    viewPager2.setCurrentItem(0);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: ");
        if (!MediaPlayerUtil.getInstance().isCLickToPause()) {
            MediaPlayerUtil.getInstance().resumeMusic();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: ");
        MediaPlayerUtil.getInstance().pauseMusic();
    }

    @Override
    public void onResume() {
        super.onResume();
        playMusicWhenBackPress();
        Log.e("testrealtime", "onResume: ");
        showPhotoViewModel.updateSeenPost(requireContext());
    }

    public void playMusicWhenBackPress() {
        if (adapter != null) {
            LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (manager != null && manager.findFirstVisibleItemPosition() != -1) {
                Image image = adapter.getItem(manager.findFirstVisibleItemPosition());
                if (image != null && MediaPlayerUtil.getInstance().isBackPress()) {
                    if (MediaPlayerUtil.getInstance().isClickGeneral()) {
                        lastPosition = 0;
                        lastImage = image;
                        playVideo(image);
                    }
                    MediaPlayerUtil.getInstance().setClickGeneral(false);
                    Log.e(TAG, manager.findFirstVisibleItemPosition() + "");
                    MediaPlayerUtil.getInstance().resumeMusicWhenBackPress(requireContext(), image.getUrlMusic());
                    MediaPlayerUtil.getInstance().setWhenBackPressFromGeneral(false);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCuteBounceAnimation();
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        stopCuteBounceAnimation();
        if (postRepository != null) {
            postRepository.removeListener();
        }
        if (adapter != null) {
            adapter.stopListening();
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

}
