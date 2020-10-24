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
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static linc.com.qrecorder.Constants.CAPTURE_MEDIA_PROJECTION_REQUEST_CODE;
import static linc.com.qrecorder.Constants.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView outputPath;
    private FloatingActionButton record;
    private FloatingActionButton play;

    private MediaProjectionManager mediaProjectionManager;
    private PlayerManager playerManager;
    private boolean recording = false;
    private boolean playing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerManager = new PlayerManager();
        outputPath = findViewById(R.id.outputDirectory);
        record = findViewById(R.id.record);
        play = findViewById(R.id.play);
        record.setOnClickListener(this);
        play.setOnClickListener(this);
        outputPath.setOnClickListener(this);

        updateOutputDirectory();
    }

    private void updateOutputDirectory() {
        String outputDirectory = getSharedPreferences(PREFERENCES_APP_NAME, MODE_PRIVATE)
                .getString(PREFERENCES_KEY_OUTPUT_DIRECTORY, "");
        if(outputDirectory == null || outputDirectory.isEmpty())
            outputPath.setText(String.format(
                    FORMAT_OUTPUT_DIRECTORY,
                    getString(R.string.app_name),
                    TITLE_DEFAULT_DIRECTORY
            ));
        else {
            String[] directories = outputDirectory.split("/");
            outputPath.setText(String.format(
                    FORMAT_OUTPUT_DIRECTORY,
                    directories[1],
                    directories[directories.length - 1]
            ));
        }
    }

    private void decodeCapture() {
        String outputDirectory = getSharedPreferences(PREFERENCES_APP_NAME, MODE_PRIVATE)
                .getString(PREFERENCES_KEY_OUTPUT_DIRECTORY, "");
        if(outputDirectory == null || outputDirectory.isEmpty()) {
            outputDirectory = ContextCompat.getExternalFilesDirs(this, Environment.DIRECTORY_MUSIC)[0].toString();
        }
        SimpleDateFormat df = new SimpleDateFormat(FORMAT_DATE_FULL, Locale.US);
        String decodedRecording = String.format(
                FORMAT_RECORDING_DECODED,
                outputDirectory,
                df.format(new Date(System.currentTimeMillis()))
        );
        PCMToMp3Encoder.init(
                String.format(FORMAT_RECORDING_ENCODED, outputDirectory, RECORDING_DEFAULT_NAME),
                DECODE_CHANNELS_COUNT,
                DECODE_BIT_RATE,
                DECODE_SAMPLE_RATE,
                decodedRecording
        );
        PCMToMp3Encoder.encode();
        PCMToMp3Encoder.destroy();
        playerManager.setAudio(decodedRecording);
    }

    private void startCapturing() {
        if (!isRecordAudioPermissionGranted()) {
            requestRecordAudioPermission();
        } else {
            startMediaProjectionRequest();
        }
    }

    private void stopCapturing() {
        Intent serviceIntent = new Intent(getApplicationContext(), RecorderService.class);
        serviceIntent.setAction(ACTION_STOP);
        startService(serviceIntent);
        decodeCapture();
    }

    private boolean isRecordAudioPermissionGranted() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS
                },
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Permissions to capture audio granted. Click the button once again.",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(this,
                        "Permissions to capture audio denied.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void startMediaProjectionRequest() {
        mediaProjectionManager = (MediaProjectionManager) getApplication()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                CAPTURE_MEDIA_PROJECTION_REQUEST_CODE
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this,
                        "MediaProjection permission obtained. Foreground service will be started to capture audio.",
                        Toast.LENGTH_SHORT
                ).show();

                Intent audioCaptureIntent = new Intent(this, RecorderService.class);
                audioCaptureIntent.setAction(ACTION_START);
                audioCaptureIntent.putExtra(EXTRA_RESULT_DATA, data);
                startForegroundService(audioCaptureIntent);
            } else {
                Toast.makeText(this,
                        "Request to obtain MediaProjection denied.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else if(requestCode == SELECT_OUTPUT_DIRECTORY_REQUEST_CODE) {
            getSharedPreferences(PREFERENCES_APP_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(PREFERENCES_KEY_OUTPUT_DIRECTORY, data.getData().toString())
                    .apply();
            updateOutputDirectory();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.outputDirectory: {
                Intent i = new Intent(this, DirectoryPickerActivity.class);
                startActivityForResult(i, SELECT_OUTPUT_DIRECTORY_REQUEST_CODE);
                break;
            }
            case R.id.record: {
                if(recording) {
                    stopCapturing();
                    record.setImageResource(R.drawable.ic_start_recording);
                } else {
                    startCapturing();
                    record.setImageResource(R.drawable.ic_stop_recording);
                }
                recording = !recording;
                break;
            }
            case R.id.play: {
                if(playing) {
                    playerManager.pausePlayer();
                    play.setImageResource(R.drawable.ic_play);
                    playing = !playing;
                } else {
                    try {
                        playerManager.resumePlaying(this, () -> {
                            play.setImageResource(R.drawable.ic_play);
                            playing = !playing;
                        });
                        play.setImageResource(R.drawable.ic_pause);
                        playing = !playing;
                    } catch (NullPointerException npe) {
                        Toast.makeText(this,
                                "Record something and then - start player!",
                                Toast.LENGTH_SHORT
                        ).show();
                        npe.printStackTrace();
                    }
                }
                break;
            }
        }
    }
}
