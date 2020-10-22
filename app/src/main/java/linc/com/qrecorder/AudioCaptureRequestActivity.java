package linc.com.qrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import static linc.com.qrecorder.Constants.ACTION_START;
import static linc.com.qrecorder.Constants.EXTRA_RESULT_DATA;
import static linc.com.qrecorder.Constants.MEDIA_PROJECTION_REQUEST_CODE;

public class AudioCaptureRequestActivity extends AppCompatActivity {

    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaProjectionManager = (MediaProjectionManager) getApplication()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                MEDIA_PROJECTION_REQUEST_CODE
        );
        System.out.println("REQUEST ACTIVITY");
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

                Intent audioCaptureIntent = new Intent(getApplicationContext(), RecorderService.class);
                audioCaptureIntent.setAction(ACTION_START);
                audioCaptureIntent.putExtra(EXTRA_RESULT_DATA, data);
                startForegroundService(audioCaptureIntent);
            } else {
                Toast.makeText(
                        this, "Request to obtain MediaProjection denied.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}
