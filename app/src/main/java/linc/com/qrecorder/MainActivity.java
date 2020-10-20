package linc.com.qrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static linc.com.qrecorder.Constants.MEDIA_PROJECTION_REQUEST_CODE;
import static linc.com.qrecorder.Constants.*;

public class MainActivity extends AppCompatActivity {

    private Button start;
    private Button stop;
    private MediaProjectionManager mediaProjectionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Mp3Encoder.init("/storage/emulated/0/Music/rec.pcm", 1, 64000, 22000, "/storage/emulated/0/Music/out.mp3");
        Mp3Encoder.init("/storage/emulated/0/Android/data/linc.com.qrecorder/files/AudioCaptures/1603204084515.pcm", 1, 64000, 22000, "/storage/emulated/0/Music/out.mp3");
        Mp3Encoder.encode();
        Mp3Encoder.destroy();



        start = findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCapturing();
            }
        });

        stop = findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopCapturing();
            }
        });
    }

    private void setButtonsEnabled(boolean isCapturingAudio) {
        start.setEnabled(!isCapturingAudio);
        stop.setEnabled(isCapturingAudio);
    }

    private void startCapturing() {
        if (!isRecordAudioPermissionGranted()) {
            requestRecordAudioPermission();
        } else {
            startMediaProjectionRequest();
        }
    }

    private void stopCapturing() {
        setButtonsEnabled(false);
        Intent serviceIntent = new Intent(this, RecorderService.class);
        serviceIntent.setAction(ACTION_STOP);
        startService(serviceIntent);
    }

    private boolean isRecordAudioPermissionGranted() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[] {
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        this,
                        "Permissions to capture audio granted. Click the button once again.",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        this, "Permissions to capture audio denied.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    /**
     * Before a capture session can be started, the capturing app must
     * call MediaProjectionManager.createScreenCaptureIntent().
     * This will display a dialog to the user, who must tap "Start now" in order for a
     * capturing session to be started. This will allow both video and audio to be captured.
     */
    private void startMediaProjectionRequest() {
        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager = (MediaProjectionManager) getApplication()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                MEDIA_PROJECTION_REQUEST_CODE
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(
                        this,
                        "MediaProjection permission obtained. Foreground service will be started to capture audio.",
                        Toast.LENGTH_SHORT
                ).show();

                Intent audioCaptureIntent = new Intent(this, RecorderService.class);
                audioCaptureIntent.setAction(ACTION_START);
                audioCaptureIntent.putExtra(EXTRA_RESULT_DATA, data);
                startForegroundService(audioCaptureIntent);
                setButtonsEnabled(true);
            } else {
                Toast.makeText(
                        this, "Request to obtain MediaProjection denied.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

}
