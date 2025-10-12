package com.example.connection.view.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.connection.R;
import com.example.connection.model.Image;
import com.example.connection.utils.MediaPlayerUtil;
import com.example.connection.viewmodel.TakePhotoViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentTakePhoto extends Fragment {

    private ImageButton btn_gallery, btn_success, btn_cancel, btn_rotate, btn_download, btn_take_photo, btn_start_video, btn_red_dot;
    private TextInputEditText edt_status;
    private ImageView imgFromGallery, img_music;
    private TextView tv_number_post, tv_music_name, tv_singer;
    private ProgressBar progressBar;
    private PreviewView previewView;
    private PlayerView playerViewVideo;
    private Preview preview;
    private View view1, view2;
    private FrameLayout frame_photo_video;
    private ImageSlider imageSlider;
    private CardView cardViewSlider;
    private ActivityResultLauncher<Intent> resultLauncher;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> listenableFuture;
    private CameraSelector.Builder cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private boolean checkLancing = true; //cam sau
    private final String TAG = "FragmentTakePhoto";
    private Bitmap photo;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private String urlMusic, musicName, singerName;
    private ActivityResultLauncher<String> permissionLauncher;
    private TakePhotoViewModel takePhotoViewModel;
    private final Handler handlerVideo = new Handler();
    private Runnable runnableVideo, runnableProcess;
    private ExoPlayer exoPlayer;
    private File file = null;
    private boolean isVideo = false;
    private boolean isStop = false;
    private long firstTouch, lastTouch;
    private final List<SlideModel> slideModels = new ArrayList<>();
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("click_when_take_playing".equals(intent.getAction())) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }
            if ("back_press_when_playing".equals(intent.getAction())) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying() && MediaPlayerUtil.getInstance().getPosition() == 0) {
                    mediaPlayer.start();
                }
            }
        }
    };

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == 1) {
                previewView.setVisibility(View.GONE);
                photo = (Bitmap) message.obj;
                if (!checkLancing) {
                    photo = flipBitmap(photo);
                }
                imgFromGallery.setVisibility(View.VISIBLE);
                imgFromGallery.setImageBitmap(photo);
                btn_success.setVisibility(View.VISIBLE);
                btn_cancel.setVisibility(View.VISIBLE);
                edt_status.setVisibility(View.VISIBLE);
                btn_download.setVisibility(View.VISIBLE);
                img_music.setVisibility(View.VISIBLE);
                frame_photo_video.setVisibility(View.GONE);
                btn_gallery.setVisibility(View.GONE);
                btn_rotate.setVisibility(View.GONE);
            }
            return false;
        }
    });

    public FragmentTakePhoto() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        takePhotoViewModel = new ViewModelProvider(this).get(TakePhotoViewModel.class);
        registerResult();
        registerPermission();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_take_photo, container, false);
        initView(view);
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        startCamera(previewView);

        showGallery();

        rotateCamera();

        downloadPost();

        cancelSendPhoto();

        sendPhotoToFirebaseFireStore();

        GestureDetector gestureDetectorPhoto = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                takePhoto();
                return true;
            }

            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                btn_take_photo.animate()
                        .rotationYBy(180f)
                        .setDuration(500)
                        .withEndAction(() -> {
                            btn_start_video.setVisibility(View.VISIBLE);
                            btn_take_photo.setVisibility(View.GONE);
                            view1.setBackgroundResource(R.drawable.button_corner);
                            view2.setBackgroundResource(R.drawable.button_corner_black);
                        })
                        .start();
                return true;
            }
        });

        btn_take_photo.setOnTouchListener((view3, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                view3.getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                view3.getParent().requestDisallowInterceptTouchEvent(false);
            }
            gestureDetectorPhoto.onTouchEvent(motionEvent);
            return true;
        });

        GestureDetector gestureDetectorVideo = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                Toast.makeText(requireContext(), "Giữ lâu để quay video", Toast.LENGTH_LONG).show();
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                firstTouch = System.currentTimeMillis();
                btn_start_video.animate()
                        .alpha(0f)
                        .withEndAction(() -> {
                            btn_start_video.setVisibility(View.GONE);
                            btn_red_dot.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setMax(100);
                        })
                        .start();
                startVideo();
            }

            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                btn_start_video.animate()
                        .rotationYBy(180f)
                        .setDuration(500)
                        .withEndAction(() -> {
                            btn_take_photo.setVisibility(View.VISIBLE);
                            btn_start_video.setVisibility(View.GONE);
                            view1.setBackgroundResource(R.drawable.button_corner_black);
                            view2.setBackgroundResource(R.drawable.button_corner);
                        })
                        .start();
                return true;
            }
        });

        btn_start_video.setOnTouchListener((view33, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                view33.getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                lastTouch = System.currentTimeMillis();
                Log.e("thoi gian giu", "onCreateView: " + (lastTouch - firstTouch));
                view33.getParent().requestDisallowInterceptTouchEvent(false);
                isStop = true;
                stopVideo();
            }
            gestureDetectorVideo.onTouchEvent(motionEvent);
            return true;
        });

        takePhotoViewModel.getSuccessLiveData().observe(getViewLifecycleOwner(), success -> {
            Toast.makeText(FragmentTakePhoto.this.requireContext(), success, Toast.LENGTH_SHORT).show();
            if (isVideo) {
                if (file != null && file.delete()) Log.e(TAG, "onChanged: xoa cache");
            }
        });

        takePhotoViewModel.getFailureLiveData().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            if (isVideo) {
                if (file != null && file.delete()) Log.e(TAG, "onChanged: xoa cache");
            }
        });

        takePhotoViewModel.getNumberOfNewPost().observe(getViewLifecycleOwner(), unseenImage -> {
            FragmentTakePhoto.this.updatePostCountUI(unseenImage.size());
            slideModels.clear();
            for (Image image : unseenImage) {
                if (image.getImageUrl() != null) {
                    SlideModel slideModel = new SlideModel(image.getImageUrl(), ScaleTypes.CENTER_CROP);
                    slideModels.add(slideModel);
                }
            }
            imageSlider.setImageList(slideModels);
        });
    }

    private void cancelSendPhoto() {
        btn_cancel.setOnClickListener(view -> {
            if (isVideo) {
                if (file != null && file.delete()) Log.e(TAG, "cancelSendPhoto: xoa cache thanh cong");
                playerViewVideo.setVisibility(View.GONE);
                btn_start_video.setAlpha(1f);
                file = null;
                isVideo = false;
            } else {
                imgFromGallery.setVisibility(View.GONE);
                isVideo = true;
            }
            btn_cancel.setVisibility(View.GONE);
            btn_success.setVisibility(View.GONE);
            btn_download.setVisibility(View.GONE);
            edt_status.setVisibility(View.GONE);
            edt_status.setText("");
            img_music.setVisibility(View.GONE);
            tv_music_name.setVisibility(View.GONE);
            tv_singer.setVisibility(View.GONE);
            previewView.setVisibility(View.VISIBLE);
            frame_photo_video.setVisibility(View.VISIBLE);
            btn_gallery.setVisibility(View.VISIBLE);
            btn_rotate.setVisibility(View.VISIBLE);
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            }
        });
    }

    private void sendPhotoToFirebaseFireStore() {
        img_music.setOnClickListener(view -> {
            FragmentChooseMusic fragmentChooseMusic = new FragmentChooseMusic(new FragmentChooseMusic.onSelectedMusic() {
                @Override
                public void onSelected(String name, String singer, String url, int position) {
                    urlMusic = url;
                    musicName = name;
                    singerName = singer;
                    tv_music_name.setVisibility(View.VISIBLE);
                    name = addPaddingForMarquee(name, tv_music_name);
                    tv_music_name.setText(name);
                    tv_music_name.setSelected(true);
                    tv_singer.setVisibility(View.VISIBLE);
                    tv_singer.setText(singer);
                    try {
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(requireContext(), Uri.parse(url));
                        mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                        mediaPlayer.prepareAsync();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onShow() {
                    if (mediaPlayer != null) {
                        mediaPlayer.pause();
                    }
                }

                @Override
                public void onHide() {
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }
                }
            });
            fragmentChooseMusic.show(requireActivity().getSupportFragmentManager(), fragmentChooseMusic.getTag());
        });
        btn_success.setOnClickListener(view -> {
            String status = Objects.requireNonNull(edt_status.getText()).toString().trim();
            takePhotoViewModel.uploadPost(photo, file, urlMusic, musicName, singerName, status, requireContext().getApplicationContext(), isVideo);
            if (isVideo) {
                playerViewVideo.setVisibility(View.GONE);
                btn_start_video.setAlpha(1f);
                isVideo = false;
            } else {
                imgFromGallery.setVisibility(View.GONE);
                isVideo = true;
            }
            btn_success.setVisibility(View.GONE);
            btn_cancel.setVisibility(View.GONE);
            btn_download.setVisibility(View.GONE);
            img_music.setVisibility(View.GONE);
            edt_status.setVisibility(View.GONE);
            edt_status.setText("");
            tv_music_name.setVisibility(View.GONE);
            tv_singer.setVisibility(View.GONE);
            previewView.setVisibility(View.VISIBLE);
            frame_photo_video.setVisibility(View.VISIBLE);
            btn_gallery.setVisibility(View.VISIBLE);
            btn_rotate.setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(), "Vui lòng chờ xử lý", Toast.LENGTH_LONG).show();
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            }
        });
    }

    private String addPaddingForMarquee(String text, TextView textView) {
        Paint paint = textView.getPaint();
        float textWidth = paint.measureText(text);
        int textViewWidthInDp = 150;
        float density = getResources().getDisplayMetrics().density;
        int textViewWidthInPx = (int) (textViewWidthInDp * density);
        if (textWidth < textViewWidthInPx) {
            StringBuilder paddedText = new StringBuilder(text);
            float spaceWidth = paint.measureText(" ");
            int spacesToAdd = (int) ((textViewWidthInPx - textWidth) / spaceWidth) + 5;
            for (int i = 0; i < spacesToAdd; i++) {
                paddedText.append(" ");
            }
            return paddedText.toString();
        } else {
            return text;
        }
    }

    private void updatePostCountUI(int unseenCount) {
        if (unseenCount == 0) {
            tv_number_post.setVisibility(View.GONE);
            cardViewSlider.setVisibility(View.GONE);
        } else {
            tv_number_post.setVisibility(View.VISIBLE);
            cardViewSlider.setVisibility(View.VISIBLE);
            if (unseenCount > 9) {
                tv_number_post.setText("9+");
            } else {
                tv_number_post.setText(String.valueOf(unseenCount));
            }
        }
    }

    private void downloadPost() {
        btn_download.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                requestFilePermission();
            } else {
                if (isVideo) {
                    takePhotoViewModel.downloadVideo(file, requireContext().getApplicationContext());
                } else {
                    takePhotoViewModel.downloadPhoto(photo, requireContext().getApplicationContext());
                }
            }
        });
    }

    private void rotateCamera() {
        btn_rotate.setOnClickListener(view -> {
            if (checkLancing) {
                startCamera(previewView);
                checkLancing = false;
                btn_rotate.animate().rotationYBy(180f).setDuration(500).start();
            } else {
                startCamera(previewView);
                checkLancing = true;
                btn_rotate.animate().rotationYBy(-180f).setDuration(500).start();
            }
        });
    }

    private void registerPermission() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (isVideo) {
                            Log.e("download", "registerPermission: ");
                            takePhotoViewModel.downloadVideo(file, requireContext().getApplicationContext());
                        } else
                            takePhotoViewModel.createFolderConnectionExternalStorage(photo, requireContext().getApplicationContext());
                    } else {
                        if (isVideo)
                            Toast.makeText(requireContext(), "Lưu video không thành công", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(requireContext(), "Lưu ảnh không thành công", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void requestFilePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            if (isVideo) {
                Log.e("download", "registerPermission: ");
                takePhotoViewModel.downloadVideo(file, requireContext().getApplicationContext());
            } else
                takePhotoViewModel.createFolderConnectionExternalStorage(photo, requireContext().getApplicationContext());
        }
    }

    private void registerResult() {
        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent == null) {
                    return;
                }
                Uri uri = intent.getData();
                previewView.setVisibility(View.GONE);
                frame_photo_video.setVisibility(View.GONE);
                btn_gallery.setVisibility(View.GONE);
                btn_rotate.setVisibility(View.GONE);
                imgFromGallery.setVisibility(View.VISIBLE);
                imgFromGallery.setImageURI(uri);
                btn_success.setVisibility(View.VISIBLE);
                btn_cancel.setVisibility(View.VISIBLE);
                btn_download.setVisibility(View.VISIBLE);
                edt_status.setVisibility(View.VISIBLE);
                img_music.setVisibility(View.VISIBLE);
                try {
                    photo = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void showGallery() {
        btn_gallery.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            resultLauncher.launch(Intent.createChooser(intent, "Select Image"));
        });
    }

    private void initView(View view) {
        previewView = view.findViewById(R.id.img_take_photo);
        btn_gallery = view.findViewById(R.id.btn_gallery);
        imgFromGallery = view.findViewById(R.id.img_picture_from_gallery);
        cameraExecutor = Executors.newSingleThreadExecutor();
        btn_success = view.findViewById(R.id.button_success);
        btn_cancel = view.findViewById(R.id.button_cancel);
        btn_rotate = view.findViewById(R.id.button_rotate);
        edt_status = view.findViewById(R.id.edt_status);
        btn_download = view.findViewById(R.id.button_download);
        tv_number_post = view.findViewById(R.id.tv_number_post);
        img_music = view.findViewById(R.id.img_music);
        tv_music_name = view.findViewById(R.id.tv_music_name);
        tv_singer = view.findViewById(R.id.tv_singer_take_photo);
        playerViewVideo = view.findViewById(R.id.player_view_video);
        view1 = view.findViewById(R.id.view_swipe1);
        view2 = view.findViewById(R.id.view_swipe2);
        frame_photo_video = view.findViewById(R.id.frame_photo_video);
        btn_take_photo = view.findViewById(R.id.button_take_photo);
        btn_start_video = view.findViewById(R.id.button_start_video);
        btn_red_dot = view.findViewById(R.id.button_red_dot);
        progressBar = view.findViewById(R.id.progress_bar9);
        imageSlider = view.findViewById(R.id.unseen_image_slider);
        cardViewSlider = view.findViewById(R.id.card_view_slider);
        exoPlayer = new ExoPlayer.Builder(requireContext()).build();
        playerViewVideo.setPlayer(exoPlayer);
        view1.setBackgroundResource(R.drawable.button_corner_black);
    }

    private void startCamera(View view) {
        listenableFuture = ProcessCameraProvider.getInstance(requireContext());
        Log.e(TAG, "startCamera: " + listenableFuture + " " + requireActivity());
        listenableFuture.addListener(() -> {
            try {
                Log.e(TAG, "startCamera: ");
                cameraProvider = listenableFuture.get();
                preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(view.getDisplay().getRotation())
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                        .build();
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.SD))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);
                cameraSelector = new CameraSelector.Builder();
                if (checkLancing) {
                    cameraSelector.requireLensFacing(CameraSelector.LENS_FACING_BACK);
                } else {
                    cameraSelector.requireLensFacing(CameraSelector.LENS_FACING_FRONT);
                }
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(FragmentTakePhoto.this, cameraSelector.build(), preview, imageCapture, videoCapture);
                Log.e(TAG, "startCamera: 2");
            } catch (Exception e) {
                Log.e(TAG, "startCamera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        isVideo = false;
        btn_take_photo.animate()
                .rotation(180f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(260)
                .withEndAction(() -> btn_take_photo.animate()
                        .rotation(-180f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(260)
                        .start())
                .start();
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                photo = image.toBitmap();
                Message message = new Message();
                message.obj = photo;
                message.what = 1;
                message.setTarget(handler);
                message.sendToTarget();
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
            }
        });
    }

    private void startVideo() {
        isVideo = true;
        isStop = false;
        file = new File(requireContext().getCacheDir(), UUID.randomUUID() + "_tmp_video.mp4");
        FileOutputOptions options = new FileOutputOptions.Builder(file).build();
        recording = videoCapture
                .getOutput()
                .prepareRecording(requireContext(), options)
                .start(cameraExecutor, videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        requireActivity().runOnUiThread(() -> {
                            showVideo();
                            playVideo();
                        });
                    }
                });
        if (runnableVideo != null) {
            handlerVideo.removeCallbacks(runnableVideo);
        }
        if (runnableProcess != null) {
            handlerVideo.removeCallbacks(runnableProcess);
        }
        runnableProcess = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - firstTouch;
                long progress = (elapsed * 100 / 5000);
                progressBar.setProgress((int) progress);
                if (elapsed < 5000) {
                    handlerVideo.postDelayed(this, 16);
                }
            }
        };
        handlerVideo.post(runnableProcess);
        runnableVideo = () -> {
            if (!isStop) {
                lastTouch = System.currentTimeMillis();
                FragmentTakePhoto.this.stopVideo();
            }
        };
        handlerVideo.postDelayed(runnableVideo, 5000);
    }

    private void stopVideo() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
        if (runnableVideo != null) handlerVideo.removeCallbacks(runnableVideo);
        if (runnableProcess != null) handlerVideo.removeCallbacks(runnableProcess);
        btn_red_dot.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        btn_start_video.setVisibility(View.VISIBLE);
    }

    private void playVideo() {
        Uri uriFile = Uri.fromFile(file);
        MediaItem mediaItem = MediaItem.fromUri(uriFile);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void showVideo() {
        playerViewVideo.setVisibility(View.VISIBLE);
        btn_success.setVisibility(View.VISIBLE);
        btn_cancel.setVisibility(View.VISIBLE);
        edt_status.setVisibility(View.VISIBLE);
        btn_download.setVisibility(View.VISIBLE);
        img_music.setVisibility(View.VISIBLE);
        frame_photo_video.setVisibility(View.GONE);
        btn_gallery.setVisibility(View.GONE);
        btn_rotate.setVisibility(View.GONE);
        previewView.setVisibility(View.GONE);
    }

    private Bitmap flipBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            Log.e(TAG, "onDestroy: ");
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if(runnableProcess != null){
            handlerVideo.removeCallbacks(runnableProcess);
        }
        if(runnableVideo != null){
            handlerVideo.removeCallbacks(runnableVideo);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finish();
            }
        });
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) mediaPlayer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: ");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, new IntentFilter("click_when_take_playing"));
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, new IntentFilter("back_press_when_playing"));
        takePhotoViewModel.listenForNewPosts();
        takePhotoViewModel.getUserInfoFromFirebase();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: ");
        takePhotoViewModel.unregisterListening();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver);
    }

}