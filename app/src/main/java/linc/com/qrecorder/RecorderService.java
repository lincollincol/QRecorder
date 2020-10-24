package linc.com.qrecorder;

import android.app.Service;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static linc.com.qrecorder.Constants.*;

public class RecorderService extends Service {

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;

    private Thread audioCaptureThread;
    private AudioRecord audioRecord;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(
                SERVICE_ID,
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build()
        );

        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager = (MediaProjectionManager) getApplicationContext()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Audio Capture Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_NOT_STICKY;
        } else {
            switch (intent.getAction()) {
                case ACTION_START: {
                    mediaProjection = mediaProjectionManager.getMediaProjection(
                            Activity.RESULT_OK,
                            (Intent) intent.getParcelableExtra(EXTRA_RESULT_DATA)
                    );
                    startAudioCapture();
                    return Service.START_STICKY;
                }
                case ACTION_STOP: {
                    try {
                        stopAudioCapture();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return Service.START_NOT_STICKY;
                }
                default: {
                    throw new IllegalArgumentException("Unexpected action received: ${intent.action}");
                }
            }
        }
    }

    private void startAudioCapture() {
//        AudioAttributes.USAGE_VOICE_COMMUNICATION
        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build();

        /**
         * Using hardcoded values for the audio format, Mono PCM samples with a sample rate of 8000Hz
         * These can be changed according to your application's needs
         */
        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//            .setSampleRate(8000)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build();

        audioRecord = new AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
                .setAudioPlaybackCaptureConfig(config)
                .build();

        audioRecord.startRecording();
        audioCaptureThread = new Thread(new Runnable() {
            @Override
            public void run() {
                File outputFile = createAudioFile();
                Log.d(LOG_TAG, "Created file for capture target: ${outputFile.absolutePath}");
                try {
                    writeAudioToFile(outputFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        audioCaptureThread.start();
    }

    private File createAudioFile() {
        String outputDirectory = getSharedPreferences(PREFERENCES_APP_NAME, MODE_PRIVATE)
                .getString(PREFERENCES_KEY_OUTPUT_DIRECTORY, "");
        if(outputDirectory == null || outputDirectory.isEmpty()) {
            outputDirectory = ContextCompat.getExternalFilesDirs(this, Environment.DIRECTORY_MUSIC)[0].toString();
        }

        File audioCapturesDirectory = new File(outputDirectory);
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs();
        }
        return new File(String.format(
                FORMAT_RECORDING_ENCODED,
                audioCapturesDirectory.getAbsolutePath(),
                RECORDING_DEFAULT_NAME
        ));
    }

    private void writeAudioToFile(File outputFile) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        short[] capturedAudioSamples = new short[NUM_SAMPLES_PER_READ];

        while (!audioCaptureThread.isInterrupted()) {
            audioRecord.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ);

            // This loop should be as fast as possible to avoid artifacts in the captured audio
            // You can uncomment the following line to see the capture samples but
            // that will incur a performance hit due to logging I/O.
            // Log.v(LOG_TAG, "Audio samples captured: ${capturedAudioSamples.toList()}")

            fileOutputStream.write(
                    shortArrayToByteArray(capturedAudioSamples),
//                    capturedAudioSamples.toByteArray(),
                    0,
                    BUFFER_SIZE_IN_BYTES
            );
        }

        fileOutputStream.close();
        Log.d(LOG_TAG, "Audio capture finished for ${outputFile.absolutePath}. File size is ${outputFile.length()} bytes.");
    }

    private void stopAudioCapture() throws InterruptedException {
//        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }
        if (mediaProjection == null) {
            return;
        }

        audioCaptureThread.interrupt();
        audioCaptureThread.join();

        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;

        mediaProjection.stop();
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private byte[] shortArrayToByteArray(short[] array) {
        // Samples get translated into bytes following little-endianness:
        // least significant byte first and the most significant byte last
        byte[] bytes = new byte[array.length * 2];
        for (int i = 0; i < array.length; i++) {
            bytes[i * 2] = (byte) (array[i] & 0x00FF);
            bytes[i * 2 + 1] = (byte) (((int) array[i]) >> 8);
            array[i] = 0;
        }
        return bytes;
    }
}