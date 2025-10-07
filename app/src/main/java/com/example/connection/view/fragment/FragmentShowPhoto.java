package com.example.connection.view.fragment;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
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
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.example.connection.R;
import com.example.connection.view.activity.MainActivity;
import com.example.connection.adapter.ShowImageAdapter;
import com.example.connection.model.Image;
import com.example.connection.viewmodel.ShowPhotoViewModel;
import com.example.connection.work.WidgetProvider;
import com.example.connection.utils.MediaPlayerUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class FragmentShowPhoto extends Fragment {

    private RecyclerView recyclerView;
    private ShowImageAdapter adapter;
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

    public FragmentShowPhoto() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate: ");
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

    }

    private void setUpRecyclerView() {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (getArguments() != null) {
                    position = getArguments().getInt("position_key", -1);
                    isGeneralClick = getArguments().getBoolean("isGeneralClick", false);
                    Log.e("TAGG", position + "show");
                }
                showPhotoViewModel.getQueryFireStore();
                showPhotoViewModel.getQuery().observe(getViewLifecycleOwner(), new Observer<Query>() {
                    @Override
                    public void onChanged(Query query) {
                        if (query != null) {
                            exoPlayer = new ExoPlayer.Builder(requireContext()).build();
                            FirestoreRecyclerOptions<Image> options = new FirestoreRecyclerOptions.Builder<Image>()
                                    .setQuery(query, Image.class)
                                    .build();
                            adapter = new ShowImageAdapter(options, requireContext(), new ShowImageAdapter.onItemClick() {
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
                                        activity.setClick(true); // Cập nhật trạng thái
                                        fragmentGeneralPhoto = (FragmentGeneralPhoto) requireActivity().getSupportFragmentManager().findFragmentByTag("FragmentGeneralPhoto");
                                        if (fragmentGeneralPhoto == null)
                                            fragmentGeneralPhoto = new FragmentGeneralPhoto();
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

                            }, recyclerView, position, isGeneralClick, exoPlayer);

                            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                            recyclerView.setAdapter(adapter);
                            SnapHelper snapHelper = new PagerSnapHelper();
                            snapHelper.attachToRecyclerView(recyclerView);

                            RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
                            if (animator instanceof SimpleItemAnimator) {
                                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
                            }

                            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                @Override
                                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                                    super.onScrollStateChanged(recyclerView, newState);
                                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                        LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                                        if (manager != null) {
                                            View snapView = snapHelper.findSnapView(manager);
                                            if (snapView != null) {
                                                handleMusic(manager, snapView);
                                            }
                                        }
                                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                                        MediaPlayerUtil.getInstance().pauseMusic();
                                        exoPlayer.pause();
                                    }
                                }

                            });
                            adapter.startListening();
                            scrollWhenClickGeneralFrag();
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
        int position = manager.getPosition(snapView);
        if (position != lastPosition) {
            lastPosition = position;
            lastImage = adapter.getItem(lastPosition);
            MediaPlayerUtil.getInstance().playMusic(requireContext(), lastImage.getUrlMusic());
            playVideo(lastImage);
        } else {
            Image image1 = adapter.getItem(position);
            if (image1.equals(lastImage)) {
                MediaPlayerUtil.getInstance().resumeMusic();
            } else {
                MediaPlayerUtil.getInstance().playMusic(requireContext(), image1.getUrlMusic());
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
                return false; // để recyclerview vẫn có thể scroll
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
    }

    private void backPress() {
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainActivity mainActivity = (MainActivity) requireActivity();
                FragmentManager manager = mainActivity.getSupportFragmentManager();
                fragmentGeneralPhoto = (FragmentGeneralPhoto) manager.findFragmentByTag("FragmentGeneralPhoto");
                if (fragmentGeneralPhoto == null) fragmentGeneralPhoto = new FragmentGeneralPhoto();
                if (mainActivity.isClick()) {
                    MediaPlayerUtil.getInstance().pauseMusic();
                    MediaPlayerUtil.getInstance().setIsInGeneral(true);
                    manager.beginTransaction()
                            .replace(R.id.frame_layout, fragmentGeneralPhoto)
                            .commit();
                } else {
                    ViewPager2 viewPager2 = mainActivity.findViewById(R.id.view_pager);
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
            MediaPlayerUtil.getInstance().resumeMusic(); //khi back ve tu recent chat activity
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: ");
        MediaPlayerUtil.getInstance().pauseMusic(); //khi out app hoac bam vao recent chat activity
    }

    @Override
    public void onResume() {
        super.onResume();
        playMusicWhenBackPress();
        backPress();
        Log.e("testrealtime", "onResume: ");
        showPhotoViewModel.updateSeenPost(requireContext());
    }

    public void playMusicWhenBackPress() {
        if (adapter != null) {
            LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (manager != null && manager.findFirstVisibleItemPosition() != -1) {
                Image image = adapter.getItem(manager.findFirstVisibleItemPosition());
                if (MediaPlayerUtil.getInstance().isBackPress()) {
                    if (MediaPlayerUtil.getInstance().isClickGeneral()) {
                        lastPosition = 0; //khi click vao item mot xong back
                        lastImage = image;
                        playVideo(image);
                    }
                    MediaPlayerUtil.getInstance().setClickGeneral(false);
                    Log.e(TAG, manager.findFirstVisibleItemPosition() + "");
                    MediaPlayerUtil.getInstance().resumeMusicWhenBackPress(requireContext(), image.getUrlMusic());//khi back press tu general photo
                    MediaPlayerUtil.getInstance().setWhenBackPressFromGeneral(false);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        if (adapter != null) {
            adapter.stopListening();
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

}