package com.example.connection.ui.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.connection.databinding.FragmentTakePhotoBinding;
import com.example.connection.model.SelectionFriendModel;
import com.example.connection.ui.adapter.ChooseFriendToPostAdapter;
import com.example.connection.ui.view.fragment.FragmentChooseMusic;
import com.example.connection.ui.viewmodel.TakePhotoViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MediaPreviewHandler {

    private static final String TAG = "MediaPreviewHandler";

    private final FragmentTakePhotoBinding binding;
    private final Context context;
    private final FragmentManager fragmentManager;
    private final TakePhotoViewModel viewModel;

    private Bitmap photo;
    private File videoFile;
    private boolean isVideo = false;
    private String urlMusic, musicName, singerName;

    private final List<String> tmpFriendList = new ArrayList<>();
    private List<SelectionFriendModel> selectionFriendList = new ArrayList<>();
    private ChooseFriendToPostAdapter friendAdapter;

    private ExoPlayer videoPlayer;
    private ExoPlayer musicPlayer;

    public interface OnUploadRequestedListener {
        void onUploadRequested(Bitmap photo, File videoFile, String urlMusic, String musicName,
                               String singerName, String status, boolean isVideo, List<String> friendIds);
    }

    public interface OnPermissionRequestListener {
        void onRequestStoragePermission();
    }

    public MediaPreviewHandler(@NonNull FragmentTakePhotoBinding binding,
                               @NonNull Context context,
                               @NonNull FragmentManager fragmentManager,
                               @NonNull TakePhotoViewModel viewModel) {
        this.binding = binding;
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.viewModel = viewModel;

        initPlayers();
        initFriendAdapter();
    }

    private void initPlayers() {
        videoPlayer = new ExoPlayer.Builder(context).build();
        binding.playerViewVideo.setPlayer(videoPlayer);
    }

    private void ensureMusicPlayer() {
        if (musicPlayer == null) {
            musicPlayer = new ExoPlayer.Builder(context).build();
            musicPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        }
    }

    private void initFriendAdapter() {
        friendAdapter = new ChooseFriendToPostAdapter(context, selectionFriendList, model -> {
            if (model.isSelected()) {
                tmpFriendList.add(model.getId());
            } else {
                tmpFriendList.remove(model.getId());
            }
        });
        binding.recyclerViewChooseToPost.setAdapter(friendAdapter);
        binding.recyclerViewChooseToPost.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewChooseToPost.setItemAnimator(null);
    }

    public void updateFriendsList(List<SelectionFriendModel> friends) {
        if (friends != null) {
            this.selectionFriendList = friends;
            friendAdapter.setData(friends);
        }
    }

    public void showPhotoPreview(@NonNull Bitmap photo) {
        this.photo = photo;
        this.isVideo = false;

        binding.imgTakePhoto.setVisibility(View.GONE);
        binding.framePhotoVideo.setVisibility(View.GONE);
        binding.btnGallery.setVisibility(View.GONE);
        binding.buttonRotate.setVisibility(View.GONE);

        binding.imgPictureFromGallery.setVisibility(View.VISIBLE);
        binding.imgPictureFromGallery.setImageBitmap(photo);

        showActionControls();
    }

    public void showGalleryPhotoPreview(@NonNull Uri uri, @NonNull Bitmap photo) {
        this.photo = photo;
        this.isVideo = false;

        binding.imgTakePhoto.setVisibility(View.GONE);
        binding.framePhotoVideo.setVisibility(View.GONE);
        binding.btnGallery.setVisibility(View.GONE);
        binding.buttonRotate.setVisibility(View.GONE);

        binding.imgPictureFromGallery.setVisibility(View.VISIBLE);
        binding.imgPictureFromGallery.setImageURI(uri);

        showActionControls();
    }

    public void showVideoPreview(@NonNull File videoFile) {
        this.videoFile = videoFile;
        this.isVideo = true;

        binding.imgTakePhoto.setVisibility(View.GONE);
        binding.framePhotoVideo.setVisibility(View.GONE);
        binding.btnGallery.setVisibility(View.GONE);
        binding.buttonRotate.setVisibility(View.GONE);

        binding.playerViewVideo.setVisibility(View.VISIBLE);

        playVideo(videoFile);
        showActionControls();
    }

    private void playVideo(@NonNull File file) {
        if (videoPlayer != null) {
            MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(file));
            videoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            videoPlayer.setMediaItem(mediaItem);
            videoPlayer.prepare();
            videoPlayer.play();
        }
    }

    private void showActionControls() {
        binding.buttonSuccess.setVisibility(View.VISIBLE);
        binding.buttonSuccess.playAnimation();
        binding.buttonCancel.setVisibility(View.VISIBLE);
        binding.buttonDownload.setVisibility(View.VISIBLE);
        binding.edtStatus.setVisibility(View.VISIBLE);
        binding.imgMusic.setVisibility(View.VISIBLE);

        binding.recyclerViewChooseToPost.setVisibility(View.VISIBLE);
        friendAdapter.setData(selectionFriendList);
        binding.recyclerViewChooseToPost.setAdapter(friendAdapter);

        // Thu nhỏ slider zoom nếu đang mở
        binding.frameSlider.animate()
                .scaleX(0.13f)
                .scaleY(0.35f)
                .withEndAction(() -> {
                    binding.sliderZoom.setEnabled(false);
                    binding.frameSlider.setVisibility(View.GONE);
                })
                .start();
    }

    public boolean isEditingOrPreviewing() {
        return binding.buttonCancel.getVisibility() == View.VISIBLE;
    }

    public void cancel() {
        if (isVideo) {
            if (videoFile != null && videoFile.delete()) {
                Log.e(TAG, "cancel: deleted video cache file");
            }
            if (videoPlayer != null) {
                videoPlayer.stop();
            }
            binding.playerViewVideo.setVisibility(View.GONE);
            binding.buttonStartVideo.setAlpha(1f);
            videoFile = null;
            isVideo = false;
        } else {
            binding.imgPictureFromGallery.setVisibility(View.GONE);
            photo = null;
        }

        binding.buttonCancel.setVisibility(View.GONE);
        binding.buttonSuccess.setVisibility(View.GONE);
        binding.buttonDownload.setVisibility(View.GONE);
        binding.edtStatus.setVisibility(View.GONE);
        binding.edtStatus.setText("");
        binding.imgMusic.setVisibility(View.GONE);
        binding.tvMusicName.setVisibility(View.GONE);
        binding.tvSingerTakePhoto.setVisibility(View.GONE);

        binding.imgTakePhoto.setVisibility(View.VISIBLE);
        binding.framePhotoVideo.setVisibility(View.VISIBLE);
        binding.btnGallery.setVisibility(View.VISIBLE);
        binding.buttonRotate.setVisibility(View.VISIBLE);
        binding.frameSlider.setVisibility(View.VISIBLE);

        binding.recyclerViewChooseToPost.setAdapter(null);
        binding.recyclerViewChooseToPost.setVisibility(View.GONE);
        tmpFriendList.clear();

        urlMusic = null;
        musicName = null;
        singerName = null;

        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.clearMediaItems();
        }
    }

    public void setupListeners(@NonNull OnUploadRequestedListener uploadListener,
                               @NonNull OnPermissionRequestListener permissionListener) {
        binding.buttonCancel.setOnClickListener(v -> cancel());

        binding.imgMusic.setOnClickListener(v -> {
            FragmentChooseMusic fragmentChooseMusic = new FragmentChooseMusic(new FragmentChooseMusic.onSelectedMusic() {
                @Override
                public void onSelected(String name, String singer, String url, int position) {
                    urlMusic = url;
                    musicName = name;
                    singerName = singer;
                    binding.tvMusicName.setVisibility(View.VISIBLE);
                    String padded = addPaddingForMarquee(name, binding.tvMusicName);
                    binding.tvMusicName.setText(padded);
                    binding.tvMusicName.setSelected(true);
                    binding.tvSingerTakePhoto.setVisibility(View.VISIBLE);
                    binding.tvSingerTakePhoto.setText(singer);

                    ensureMusicPlayer();
                    if (musicPlayer != null) {
                        musicPlayer.setMediaItem(MediaItem.fromUri(url));
                        musicPlayer.prepare();
                        musicPlayer.play();
                    }
                }

                @Override
                public void onShow() {
                    if (musicPlayer != null) musicPlayer.pause();
                }

                @Override
                public void onHide() {
                    if (musicPlayer != null) musicPlayer.play();
                }
            });
            fragmentChooseMusic.show(fragmentManager, fragmentChooseMusic.getTag());
        });

        binding.buttonDownload.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissionListener.onRequestStoragePermission();
            } else {
                performDownload();
            }
        });

        binding.buttonSuccess.setOnClickListener(v -> {
            String status = Objects.requireNonNull(binding.edtStatus.getText()).toString().trim();
            List<String> targetFriendList = new ArrayList<>(tmpFriendList);
            if (!targetFriendList.isEmpty()) {
                String currentId = viewModel.getCurrentUserId();
                if (!targetFriendList.contains(currentId)) {
                    targetFriendList.add(currentId);
                }
            }

            uploadListener.onUploadRequested(photo, videoFile, urlMusic, musicName, singerName, status, isVideo, targetFriendList);
            Toast.makeText(context, "Vui lòng chờ xử lý", Toast.LENGTH_LONG).show();
            cancel();
        });
    }

    public void performDownload() {
        if (isVideo) {
            viewModel.downloadVideo(videoFile, context.getApplicationContext());
        } else {
            viewModel.downloadPhoto(photo, context.getApplicationContext());
        }
    }

    private String addPaddingForMarquee(String text, TextView textView) {
        Paint paint = textView.getPaint();
        float textWidth = paint.measureText(text);
        int textViewWidthInDp = 150;
        float density = context.getResources().getDisplayMetrics().density;
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

    public void onResume() {
        if (musicPlayer != null && !musicPlayer.isPlaying()) {
            musicPlayer.play();
        }
        if (videoPlayer != null && isVideo && !videoPlayer.isPlaying()) {
            videoPlayer.play();
        }
    }

    public void onPause() {
        if (musicPlayer != null) {
            musicPlayer.pause();
        }
        if (videoPlayer != null && videoPlayer.isPlaying()) {
            videoPlayer.pause();
        }
    }

    public void onDestroy() {
        if (musicPlayer != null) {
            musicPlayer.release();
            musicPlayer = null;
        }
        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }
    }

    public boolean isVideo() {
        return isVideo;
    }

    public File getVideoFile() {
        return videoFile;
    }

    public Bitmap getPhoto() {
        return photo;
    }
}
