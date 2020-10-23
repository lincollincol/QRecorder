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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static linc.com.qrecorder.Constants.CAPTURE_MEDIA_PROJECTION_REQUEST_CODE;
import static linc.com.qrecorder.Constants.*;

public class MainActivity extends AppCompatActivity {

    private TextView outputPath;
    private FloatingActionButton record;
    private FloatingActionButton play;

    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputPath = findViewById(R.id.outputDirectory);
        record = findViewById(R.id.record);
        play = findViewById(R.id.play);
        record.setOnClickListener(view -> startCapturing());
        play.setOnClickListener(view -> stopCapturing());
        outputPath.setOnClickListener(view -> {
            Intent i = new Intent(this, DirectoryPickerActivity.class);
            startActivityForResult(i, SELECT_OUTPUT_DIRECTORY_REQUEST_CODE);
        });
        setButtonsEnabled(false);
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
        PCMToMp3Encoder.init(
                String.format(FORMAT_RECORDING_ENCODED, outputDirectory, RECORDING_DEFAULT_NAME),
                DECODE_CHANNELS_COUNT,
                DECODE_BIT_RATE,
                DECODE_SAMPLE_RATE,
                String.format(FORMAT_RECORDING_DECODED, outputDirectory, df.format(new Date(System.currentTimeMillis())))
        );
        PCMToMp3Encoder.encode();
        PCMToMp3Encoder.destroy();
    }


    private void setButtonsEnabled(boolean isCapturingAudio) {
        play.setEnabled(!isCapturingAudio);
        record.setEnabled(isCapturingAudio);
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
                setButtonsEnabled(true);
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

}
