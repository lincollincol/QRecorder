package linc.com.qrecorder;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.io.IOException;

public class PlayerManager {

    private MediaPlayer player;
    private String audioPath;

    public void resumePlaying(Context context, PlayerOnComplete playerOnComplete) {
        if(player == null) {
            player = MediaPlayer.create(context, Uri.fromFile(new File(audioPath)));
        }
        player.start();
        player.setOnCompletionListener(mp -> {
            player.pause();
            player.seekTo(0);
            playerOnComplete.onComplete();
        });
    }

    public void pausePlayer() {
        player.pause();
    }

    public void setAudio(String audioPath) {
        this.audioPath = audioPath;
    }

    interface PlayerOnComplete {
        void onComplete();
    }
}
