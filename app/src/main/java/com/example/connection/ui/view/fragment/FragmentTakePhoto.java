package com.example.connection.ui.view.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.connection.R;
import com.example.connection.constants.MediaPlayerUtil;
import com.example.connection.databinding.FragmentTakePhotoBinding;
import com.example.connection.model.Image;
import com.example.connection.ui.camera.CameraManager;
import com.example.connection.ui.camera.MediaPreviewHandler;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.ui.viewmodel.MainViewModel;
import com.example.connection.ui.viewmodel.TakePhotoViewModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FragmentTakePhoto extends Fragment {

    private static final String TAG = "FragmentTakePhoto";

    private FragmentTakePhotoBinding binding;
    private CameraManager cameraManager;
    private MediaPreviewHandler mediaPreviewHandler;
    private TakePhotoViewModel takePhotoViewModel;
    private MainViewModel mainViewModel;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> storagePermissionLauncher;

    private final Handler handlerVideo = new Handler();
    private Runnable runnableVideo;
    private Runnable runnableProcess;
    private long firstTouch;
    private boolean isRecordingVideo = false;

    private final List<SlideModel> slideModels = new ArrayList<>();
    private List<String> listFriendId = new ArrayList<>();

    public FragmentTakePhoto() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        takePhotoViewModel = new ViewModelProvider(this).get(TakePhotoViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        cameraManager = new CameraManager();

        registerLaunchers();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTakePhotoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initMediaPreviewHandler();
        setupBackPress();
        setupGestures();
        setupSliderZoom();
        setupRotateCamera();
        setupGalleryButton();
        setupObservers();

        cameraManager.startCamera(
                requireContext(),
                getViewLifecycleOwner(),
                binding.imgTakePhoto,
                requireActivity().getWindowManager().getDefaultDisplay().getRotation(),
                null
        );
    }

    private void registerLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                                mediaPreviewHandler.showGalleryPhotoPreview(uri, bitmap);
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading image from gallery: " + e.getMessage(), e);
                            }
                        }
                    }
                }
        );

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        mediaPreviewHandler.performDownload();
                    } else {
                        Toast.makeText(requireContext(), "Lưu không thành công: Cần cấp quyền bộ nhớ", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void initMediaPreviewHandler() {
        mediaPreviewHandler = new MediaPreviewHandler(
                binding,
                requireContext(),
                requireActivity().getSupportFragmentManager(),
                takePhotoViewModel
        );

        mediaPreviewHandler.setupListeners(
                (photo, videoFile, urlMusic, musicName, singerName, status, isVideo, friendIds) -> {
                    List<String> targetFriends = friendIds.isEmpty() ? listFriendId : friendIds;
                    takePhotoViewModel.uploadPost(
                            photo,
                            videoFile,
                            urlMusic,
                            musicName,
                            singerName,
                            status,
                            requireContext().getApplicationContext(),
                            isVideo,
                            targetFriends
                    );
                },
                () -> {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    } else {
                        mediaPreviewHandler.performDownload();
                    }
                }
        );

        binding.frameSlider.setScaleX(0.13f);
        binding.frameSlider.setScaleY(0.35f);
        binding.sliderZoom.setEnabled(false);
        binding.viewSwipe1.setBackgroundResource(R.drawable.button_corner_black);
    }

    private void setupBackPress() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null && mainActivity.isClick()) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                    return;
                }

                ViewPager2 viewPager2 = mainActivity != null ? mainActivity.findViewById(R.id.view_pager) : null;
                if (viewPager2 != null && viewPager2.getCurrentItem() != 0) {
                    viewPager2.setCurrentItem(0, true);
                    return;
                }

                if (mediaPreviewHandler != null && mediaPreviewHandler.isEditingOrPreviewing()) {
                    mediaPreviewHandler.cancel();
                    return;
                }

                requireActivity().finish();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestures() {
        // Chụp ảnh: Chạm 1 lần để chụp, vuốt ngang để đổi sang chế độ quay video
        GestureDetector gestureDetectorPhoto = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                collapseZoomSlider();
                takePhoto();
                return true;
            }

            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                binding.buttonTakePhoto.animate()
                        .rotationYBy(180f)
                        .setDuration(500)
                        .withEndAction(() -> {
                            binding.buttonStartVideo.setVisibility(View.VISIBLE);
                            binding.buttonTakePhoto.setVisibility(View.GONE);
                            binding.viewSwipe1.setBackgroundResource(R.drawable.button_corner);
                            binding.viewSwipe2.setBackgroundResource(R.drawable.button_corner_black);
                        })
                        .start();
                return true;
            }
        });

        binding.buttonTakePhoto.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            gestureDetectorPhoto.onTouchEvent(event);
            return true;
        });

        // Quay video: Chạm để nhắc, giữ lâu để quay, vuốt ngang để đổi về chụp ảnh
        GestureDetector gestureDetectorVideo = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                Toast.makeText(requireContext(), "Giữ lâu để quay video", Toast.LENGTH_LONG).show();
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                startVideo();
            }

            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                binding.buttonStartVideo.animate()
                        .rotationYBy(180f)
                        .setDuration(500)
                        .withEndAction(() -> {
                            binding.buttonTakePhoto.setVisibility(View.VISIBLE);
                            binding.buttonStartVideo.setVisibility(View.GONE);
                            binding.viewSwipe1.setBackgroundResource(R.drawable.button_corner_black);
                            binding.viewSwipe2.setBackgroundResource(R.drawable.button_corner);
                        })
                        .start();
                return true;
            }
        });

        binding.buttonStartVideo.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
                if (isRecordingVideo) {
                    stopVideo();
                    return true;
                }
            }
            gestureDetectorVideo.onTouchEvent(event);
            return true;
        });
    }

    private void takePhoto() {
        binding.buttonTakePhoto.animate()
                .rotation(180f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(260)
                .withEndAction(() -> binding.buttonTakePhoto.animate()
                        .rotation(-180f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(260)
                        .start())
                .start();

        cameraManager.takePhoto(new CameraManager.OnPhotoCapturedListener() {
            @Override
            public void onSuccess(Bitmap photo) {
                requireActivity().runOnUiThread(() -> mediaPreviewHandler.showPhotoPreview(photo));
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "takePhoto error: " + exception.getMessage(), exception);
            }
        });
    }

    private void startVideo() {
        isRecordingVideo = true;
        firstTouch = System.currentTimeMillis();

        binding.buttonStartVideo.animate()
                .alpha(0f)
                .withEndAction(() -> {
                    binding.buttonRedDot.setVisibility(View.VISIBLE);
                    binding.progressBar9.setVisibility(View.VISIBLE);
                    binding.progressBar9.setMax(100);
                })
                .start();

        File tempVideoFile = new File(requireContext().getCacheDir(), UUID.randomUUID() + "_tmp_video.mp4");
        cameraManager.startVideoRecording(requireContext(), tempVideoFile, new CameraManager.OnVideoFinalizedListener() {
            @Override
            public void onFinalized(File videoFile) {
                requireActivity().runOnUiThread(() -> mediaPreviewHandler.showVideoPreview(videoFile));
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "video recording error: " + exception.getMessage(), exception);
            }
        });

        // Thanh tiến trình quay 5 giây
        runnableProcess = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - firstTouch;
                long progress = (elapsed * 100 / 5000);
                binding.progressBar9.setProgress((int) progress);
                if (elapsed < 5000) {
                    handlerVideo.postDelayed(this, 16);
                }
            }
        };
        handlerVideo.post(runnableProcess);

        runnableVideo = this::stopVideo;
        handlerVideo.postDelayed(runnableVideo, 5000);
    }

    private void stopVideo() {
        if (!isRecordingVideo) return;
        isRecordingVideo = false;

        cameraManager.stopVideoRecording();

        if (runnableVideo != null) handlerVideo.removeCallbacks(runnableVideo);
        if (runnableProcess != null) handlerVideo.removeCallbacks(runnableProcess);

        binding.buttonRedDot.setVisibility(View.GONE);
        binding.progressBar9.setVisibility(View.GONE);
        binding.buttonStartVideo.setVisibility(View.VISIBLE);
        binding.buttonStartVideo.animate().alpha(1f).setDuration(200).start();
    }

    private void setupSliderZoom() {
        binding.frameSlider.setOnClickListener(v -> {
            binding.frameSlider.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            binding.frameSlider.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .withEndAction(() -> binding.sliderZoom.setEnabled(true))
                    .start();
        });

        binding.imgTakePhoto.setOnClickListener(v -> collapseZoomSlider());
        binding.sliderZoom.addOnChangeListener((slider, value, fromUser) -> cameraManager.setLinearZoom(value));
    }

    private void collapseZoomSlider() {
        binding.frameSlider.animate()
                .scaleX(0.13f)
                .scaleY(0.35f)
                .withEndAction(() -> {
                    binding.frameSlider.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    binding.sliderZoom.setEnabled(false);
                })
                .start();
    }

    private void setupRotateCamera() {
        binding.buttonRotate.setOnClickListener(v -> {
            cameraManager.flipCamera(
                    requireContext(),
                    getViewLifecycleOwner(),
                    binding.imgTakePhoto,
                    requireActivity().getWindowManager().getDefaultDisplay().getRotation()
            );
            float rotationDeg = cameraManager.isBackFacing() ? -180f : 180f;
            binding.buttonRotate.animate().rotationYBy(rotationDeg).setDuration(500).start();
        });
    }

    private void setupGalleryButton() {
        binding.btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            galleryLauncher.launch(Intent.createChooser(intent, "Select Image"));
        });
    }

    private void setupObservers() {
        takePhotoViewModel.getSuccessLiveData().observe(getViewLifecycleOwner(), success ->
                Toast.makeText(requireContext(), success, Toast.LENGTH_SHORT).show());

        takePhotoViewModel.getFailureLiveData().observe(getViewLifecycleOwner(), error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());

        takePhotoViewModel.getNumberOfNewPost().observe(getViewLifecycleOwner(), unseenImages -> {
            updatePostCountUI(unseenImages.size());
            slideModels.clear();
            for (Image image : unseenImages) {
                if (image.getImageUrl() != null) {
                    slideModels.add(new SlideModel(image.getImageUrl(), ScaleTypes.CENTER_CROP));
                }
            }
            binding.unseenImageSlider.setImageList(slideModels);
        });

        takePhotoViewModel.getListFriendLiveData().observe(getViewLifecycleOwner(), list ->
                listFriendId = list != null ? list : new ArrayList<>());

        takePhotoViewModel.getSelectionFriendLiveData().observe(getViewLifecycleOwner(), friends -> {
            if (mediaPreviewHandler != null) {
                mediaPreviewHandler.updateFriendsList(friends);
            }
        });

        // Thay thế LocalBroadcastManager bằng MainViewModel LiveData
        mainViewModel.getCameraMusicPlaying().observe(getViewLifecycleOwner(), playing -> {
            if (Boolean.TRUE.equals(playing)) {
                if (isResumed() && MediaPlayerUtil.getInstance().getPosition() == 0) {
                    mediaPreviewHandler.onResume();
                }
            } else if (Boolean.FALSE.equals(playing)) {
                mediaPreviewHandler.onPause();
            }
        });
    }

    private void updatePostCountUI(int unseenCount) {
        if (unseenCount == 0) {
            binding.tvNumberPost.setVisibility(View.GONE);
            binding.cardViewSlider.setVisibility(View.GONE);
        } else {
            binding.tvNumberPost.setVisibility(View.VISIBLE);
            binding.cardViewSlider.setVisibility(View.VISIBLE);
            binding.tvNumberPost.setText(unseenCount > 9 ? "9+" : String.valueOf(unseenCount));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mediaPreviewHandler != null) {
            mediaPreviewHandler.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaPreviewHandler != null) {
            mediaPreviewHandler.onPause();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        takePhotoViewModel.listenForNewPosts();
        takePhotoViewModel.getUserInfoFromFirebase();
    }

    @Override
    public void onStop() {
        super.onStop();
        takePhotoViewModel.unregisterListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (runnableProcess != null) handlerVideo.removeCallbacks(runnableProcess);
        if (runnableVideo != null) handlerVideo.removeCallbacks(runnableVideo);
        if (mediaPreviewHandler != null) mediaPreviewHandler.onDestroy();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraManager != null) {
            cameraManager.shutdown();
        }
    }
}
