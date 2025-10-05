package com.example.connection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.Image;
import com.example.connection.utils.MediaPlayerUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ShowImageAdapter extends FirestoreRecyclerAdapter<Image, ShowImageAdapter.ShowImageViewHolder> {

    private final Context context;
    private final onItemClick click;
    private final RecyclerView recyclerView;
    private int positionClick;
    private final String TAG = getClass().getName();
    private boolean isGeneralClick;
    private final ExoPlayer exoPlayer;
    private final String myId = FirebaseAuth.getInstance().getUid();
    private final List<String> emoteList = Arrays.asList(
            "\uD83D\uDC4D",
            "\uD83D\uDE02",
            "\u2764\uFE0F",
            "\uD83D\uDE2E",
            "\uD83D\uDE22",
            "\uD83D\uDE21");

    public ShowImageAdapter(@NonNull FirestoreRecyclerOptions<Image> options,
                            Context context,
                            onItemClick onItemClick,
                            RecyclerView recyclerView,
                            int positionClick,
                            boolean isGeneralClick,
                            ExoPlayer exoPlayer) {
        super(options);
        this.context = context;
        this.click = onItemClick;
        this.recyclerView = recyclerView;
        this.positionClick = positionClick;
        this.isGeneralClick = isGeneralClick;
        this.exoPlayer = exoPlayer;
        Log.e("kiemtranhac", "ShowImageAdapter: " + positionClick);
    }

    public interface onItemClick {
        void generalImageClick();

        void sendMessage(String message, String postUrl, String otherId, String otherUserName, String avtPoster, EditText editText);

        void expressEmote(String emote, String postId);

        void showComment(String postId);

        void showEmote(String postId);

        void showOptions(String postId, String url);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onBindViewHolder(@NonNull ShowImageViewHolder holder, int position, @NonNull Image model) {
        Log.e("kiemtra", "onBindViewHolder: ");
        if (model.getImageUrl() != null) {
            Log.e(TAG, "onBindViewHolder: " + model.getImageUrl());
            holder.playerViewVideo.setVisibility(View.GONE);
            holder.img_post.setVisibility(View.VISIBLE);
            Glide.with(context).load(model.getImageUrl()).error(R.drawable.icon_avatar).into(holder.img_post);
        } else if (model.getVideoUrl() != null) {
            Log.e(TAG, "onBindViewHolder: " + model.getVideoUrl());
            holder.img_post.setVisibility(View.GONE);
            holder.playerViewVideo.setVisibility(View.VISIBLE);

            if (position == 0 || position == positionClick) {
                Log.e(TAG, "onBindViewHolder: click position");
                holder.playerViewVideo.setPlayer(null);
                holder.playerViewVideo.setPlayer(exoPlayer);
            }
        }

        if (model.getStatus().isEmpty()) holder.tv_status.setVisibility(View.GONE);

        else {
            holder.tv_status.setVisibility(View.VISIBLE);
            holder.tv_status.setText(model.getStatus());
        }

        if (model.getNameMusic() != null && model.getSingerName() != null && model.getUrlMusic() != null) {
            holder.tv_music_post.setVisibility(View.VISIBLE);
            holder.tv_music_post.setText(addPaddingForMarquee(model.getNameMusic(), holder.tv_music_post));
            holder.tv_music_post.setSelected(true);
            holder.tv_singer_post.setVisibility(View.VISIBLE);
            holder.tv_singer_post.setText(model.getSingerName());
        } else {
            holder.tv_music_post.setVisibility(View.GONE);
            holder.tv_singer_post.setVisibility(View.GONE);
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDay = new SimpleDateFormat(
                "dd-MM HH:mm:ss",
                new Locale("vi", "VN")
        );
        String fullTime = formatDay.format(model.getTimestamp().toDate());
        holder.tv_time_post.setText(getTimeAgo(fullTime, model.getTimestamp()));
        holder.img_general_image.setOnClickListener(view -> click.generalImageClick());
        Glide.with(context)
                .load(model.getAvatarPoster())
                .error(R.drawable.icon_avatar)
                .into(holder.img_user_post);
        holder.tv_username_post.setText(model.getUserNamePoster());

        holder.btn_send.setOnClickListener(view -> {
            String message = holder.edt_send_message.getText().toString().trim();
            if (model.getImageUrl() != null) {
                click.sendMessage(message,
                        model.getImageUrl(),
                        model.getPosterId(),
                        model.getUserNamePoster(),
                        model.getAvatarPoster(),
                        holder.edt_send_message);
            } else if (model.getVideoUrl() != null) {
                click.sendMessage(message,
                        model.getVideoUrl(),
                        model.getPosterId(),
                        model.getUserNamePoster(),
                        model.getAvatarPoster(),
                        holder.edt_send_message);
            }
        });

        holder.img_post.setOnLongClickListener(view -> {
            click.showOptions(model.getPostId(), model.getImageUrl());
            return true;
        });

        holder.playerViewVideo.setOnLongClickListener(view -> {
            click.showOptions(model.getPostId(), model.getVideoUrl());
            return true;
        });

        holder.playerViewVideo.setOnClickListener(view -> {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
            } else exoPlayer.play();
            checkIsPlayingMusic();
        });

        holder.img_post.setOnClickListener(view -> checkIsPlayingMusic());

        //nhan nut like
        //van bi doan gang o day, fix them sau
        // TODO: 10/3/2025
        holder.btn_like.setOnClickListener(view -> {
            if (!model.getEmoteMap().containsKey(myId)) {
                holder.btn_like.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A8D38D")));
                holder.btn_like.setText("\uD83D\uDC4D" + " " + "Thích");
                click.expressEmote("\uD83D\uDC4D",
                        model.getPostId());
            } else {
                holder.btn_like.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00000000")));
                holder.btn_like.setText("Thích");
                String emoteAndId = model.getEmoteMap().get(myId);
                if (emoteAndId != null) {
                    String emote = emoteAndId.split("_")[1];
                    click.expressEmote(emote,
                            model.getPostId());
                }
            }
        });

        //show cac emote
        holder.btn_like.setOnLongClickListener(view -> {
            holder.recyclerEmote.setVisibility(View.VISIBLE);
            EmoteAdapter emoteAdapter = new EmoteAdapter(context, emoteList, emote -> {
                click.expressEmote(emote,
                        model.getPostId());
                holder.recyclerEmote.setAdapter(null);
                holder.recyclerEmote.setVisibility(View.GONE);
                holder.btn_like.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A8D38D")));
                checkReaction(emote, holder.btn_like);
            });
            holder.recyclerEmote.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            holder.recyclerEmote.setAdapter(emoteAdapter);
            return true;
        });

        //show frag comment
        holder.btn_comment.setOnClickListener(view -> click.showComment(model.getPostId()));

        //show danh sach ai da emote
        holder.tv_like.setOnClickListener(view -> click.showEmote(model.getPostId()));

        if (model.getNumberEmote() > 0) {
            holder.tv_like.setVisibility(View.VISIBLE);
            if (model.getEmoteMap().containsKey(myId)) {
                int count = model.getNumberEmote() - 1;
                if (count > 0) {
                    holder.tv_like.setText("Bạn và " + count + " " + "người khác");
                } else {
                    holder.tv_like.setText("Bạn");
                }
            } else {
                holder.tv_like.setText(model.getNumberEmote() + " người khác");
            }
        } else {
            holder.tv_like.setVisibility(View.INVISIBLE);
        }

        if (model.getEmoteMap().containsKey(myId)) {
            String emo = model.getEmoteMap().get(myId);
            if (emo != null) {
                checkReaction(emo.split("_")[1], holder.btn_like);
            }
            holder.btn_like.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A8D38D")));
        } else {
            holder.btn_like.setText("Thích");
            holder.btn_like.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00000000")));
        }

        if (model.getNumberComment() > 0) {
            holder.tv_comment.setVisibility(View.VISIBLE);
            holder.tv_comment.setText(model.getNumberComment() + " bình luận");
        } else {
            holder.tv_comment.setVisibility(View.INVISIBLE);
        }

    }

    @NonNull
    @Override
    public ShowImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_image_post, parent, false);
        return new ShowImageViewHolder(view);
    }

    @Override
    public void onViewRecycled(@NonNull ShowImageViewHolder holder) {
        super.onViewRecycled(holder);
        holder.playerViewVideo.setPlayer(null);
    }

    public static class ShowImageViewHolder extends RecyclerView.ViewHolder {

        private final ImageView img_post, img_general_image;
        public PlayerView playerViewVideo;
        private final TextView tv_status, tv_username_post, tv_time_post, tv_music_post, tv_singer_post, tv_like, tv_comment;
        private final CircleImageView img_user_post;
        private final EditText edt_send_message;
        private final ImageButton btn_send;
        private final Button btn_like, btn_comment;
        private final RecyclerView recyclerEmote;

        public ShowImageViewHolder(@NonNull View itemView) {
            super(itemView);
            img_post = itemView.findViewById(R.id.img_post);
            tv_status = itemView.findViewById(R.id.tv_status);
            tv_username_post = itemView.findViewById(R.id.tv_username_post);
            tv_time_post = itemView.findViewById(R.id.tv_time_post);
            img_user_post = itemView.findViewById(R.id.img_user_post);
            edt_send_message = itemView.findViewById(R.id.edt_send_message);
            btn_send = itemView.findViewById(R.id.btn_send);
            img_general_image = itemView.findViewById(R.id.img_general_image);
            tv_music_post = itemView.findViewById(R.id.tv_music_post);
            tv_singer_post = itemView.findViewById(R.id.tv_singer_post);
            playerViewVideo = itemView.findViewById(R.id.player_view_video_post);
            btn_like = itemView.findViewById(R.id.btn_like);
            btn_comment = itemView.findViewById(R.id.btn_comment);
            tv_like = itemView.findViewById(R.id.tv_like);
            tv_comment = itemView.findViewById(R.id.tv_comment);
            recyclerEmote = itemView.findViewById(R.id.recycler_view_emote_in_post);
        }
    }

    // TODO: 10/4/2025 xu ly logic cho phat nhac cho item dau tien 
    @Override
    public void onDataChanged() {
        super.onDataChanged();
        int itemCount = getItemCount();
        if (itemCount == 0) {
            MediaPlayerUtil.getInstance().setFirstTimeScrollToShow(false);
            return;
        }
        Log.e("kiemtranhac", "onDataChanged: " + itemCount);
        if (positionClick >= 0 && positionClick < getItemCount() && isGeneralClick) {
            Log.e("kiemtranhac", "onDataChanged: general click");
            recyclerView.scrollToPosition(positionClick);
            Image image = getItem(positionClick);
            playVideo(image);
            if (image.getUrlMusic() == null) return;
            MediaPlayerUtil.getInstance().playMusic(context, image.getUrlMusic());
            isGeneralClick = false;
        } else if (positionClick == 0 && MediaPlayerUtil.getInstance().isFirstTimeScrollToShow()) {
            Log.e("kiemtranhac", "onDataChanged: lan dau sang show" + positionClick);
            Image image = getItem(positionClick);
            playVideo(image);
            MediaPlayerUtil.getInstance().setFirstTimeScrollToShow(false);
            positionClick = - 1;
            if (image.getUrlMusic() == null) return;
            MediaPlayerUtil.getInstance().setPosition(1);
            MediaPlayerUtil.getInstance().playMusic(context, image.getUrlMusic());
        }
    }

    public String getTimeAgo(String time, Timestamp timestamp) {

        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("dd-MM HH:mm:ss", new Locale("vi", "VN"));
        String currentTime = format.format(new Date().getTime());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat fullTime = new SimpleDateFormat("dd-MM-yyyy", new Locale("vi", "VN"));
        String full = fullTime.format(timestamp.toDate());

        Log.e(TAG, currentTime);
        Log.e(TAG, time);

        long now = (new Date().getTime()) / 1000;
        Log.e(TAG, now + " s - now");
        long before = (timestamp.toDate().getTime()) / 1000;
        Log.e(TAG, before + " s - before");

        long signal = now - before;
        Log.e(TAG, signal + " s - signal");

        if (signal > 0 && signal < 60) return signal + "s";
        else {
            double signalMinute = (double) signal / 60;
            if (signalMinute <= 60 && signalMinute >= 1) return (int) signalMinute + "m";
            else {
                double signalHour = (double) signal / 3600;
                if (signalHour <= 24 && signalHour >= 1) return (int) signalHour + "h";
                else {
                    double signalDay = (double) signal / 86400;
                    if (signalDay <= 7 && signalDay >= 1) return (int) signalDay + "d";
                }
            }
        }
        return full;
    }

    public void playVideo(Image image) {
        if (image.getVideoUrl() != null) {
            MediaItem mediaItem = MediaItem.fromUri(image.getVideoUrl());
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }

    private String addPaddingForMarquee(String text, TextView textView) {
        // 1. Lấy đối tượng Paint từ TextView
        Paint paint = textView.getPaint();

        // 2. Đo chiều rộng của văn bản gốc
        float textWidth = paint.measureText(text);

        // 3. Lấy chiều rộng của TextView (đã được chuyển đổi sang px)
        int textViewWidthInDp = 150;
        float density = context.getResources().getDisplayMetrics().density;
        int textViewWidthInPx = (int) (textViewWidthInDp * density);

        // 4. Nếu văn bản ngắn hơn TextView, thêm khoảng trắng
        if (textWidth < textViewWidthInPx) {
            StringBuilder paddedText = new StringBuilder(text);

            // Thêm khoảng trắng cho đến khi đủ dài để cuộn
            float spaceWidth = paint.measureText(" ");
            int spacesToAdd = (int) ((textViewWidthInPx - textWidth) / spaceWidth) + 5; // Thêm thêm 5 khoảng trắng để đảm bảo

            for (int i = 0; i < spacesToAdd; i++) {
                paddedText.append(" ");
            }
            return paddedText.toString();
        } else {
            // Nếu văn bản đã đủ dài, trả về văn bản gốc
            return text;
        }
    }

    public void checkIsPlayingMusic() {
        if (MediaPlayerUtil.getInstance().getMediaPlayer() == null) return;
        if (MediaPlayerUtil.getInstance().getMediaPlayer().isPlaying()) {
            MediaPlayerUtil.getInstance().pauseMusic();
            MediaPlayerUtil.getInstance().setCLickToPause(true);
        } else {
            MediaPlayerUtil.getInstance().resumeMusic();
            MediaPlayerUtil.getInstance().setCLickToPause(false);
        }
    }

    @SuppressLint("SetTextI18n")
    public void checkReaction(String emote, Button btn_like) {
        switch (emote) {
            case "\uD83D\uDC4D":
                btn_like.setText("\uD83D\uDC4D" + " " + "Thích");
                break;
            case "\uD83D\uDE02":
                btn_like.setText("\uD83D\uDE02" + " " + "Haha");
                break;
            case "\u2764\uFE0F":
                btn_like.setText("\u2764\uFE0F" + " " + "Yêu thích");
                break;
            case "\uD83D\uDE2E":
                btn_like.setText("\uD83D\uDE2E" + " " + "Wow");
                break;
            case "\uD83D\uDE22":
                btn_like.setText("\uD83D\uDE22" + " " + "Buồn bã");
                break;
            case "\uD83D\uDE21":
                btn_like.setText("\uD83D\uDE21" + " " + "Phẫn nộ");
                break;
        }
    }
}
