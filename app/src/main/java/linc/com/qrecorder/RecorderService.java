package linc.com.qrecorder;

import android.app.Service;
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
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
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
        if(intent == null) {
            return Service.START_NOT_STICKY;
        } else {
            switch (intent.getAction()) {
                case ACTION_START : {
                    mediaProjection = mediaProjectionManager.getMediaProjection(
                            Activity.RESULT_OK,
                            (Intent) intent.getParcelableExtra(EXTRA_RESULT_DATA)
                    );
                    startAudioCapture();
                    return Service.START_STICKY;
                }
                case ACTION_STOP : {
                    try {
                        stopAudioCapture();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return Service.START_NOT_STICKY;
                }
                default : {
                    throw new IllegalArgumentException("Unexpected action received: ${intent.action}");
                }
            }
        }
    }

    private void startAudioCapture() {
        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA) // TODO provide UI options for inclusion/exclusion
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
                // For optimal performance, the buffer size
                // can be optionally specified to store audio samples.
                // If the value is not specified,
                // uses a single frame and lets the
                // native code figure out the minimum buffer size.
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
        File audioCapturesDirectory = new File(getExternalFilesDir(null), "/AudioCaptures");
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs();
        }
        String fileName = System.currentTimeMillis() + ".pcm";
        return new File(audioCapturesDirectory.getAbsolutePath() + "/" + fileName);
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
        if(mediaProjection == null) {
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
            bytes[i * 2] = (byte)(array[i] & 0x00FF);
            bytes[i * 2 + 1] = (byte)(((int)array[i]) >> 8);
            array[i] = 0;
        }
        return bytes;
    }

    /*
    companion object {
        private const val SAMPLING_RATE_IN_HZ = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2

        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
                SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT
        ) * BUFFER_SIZE_FACTOR
    }
     */

}

/*
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.experimental.and

class RecorderService : Service() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private lateinit var audioCaptureThread: Thread
    private var audioRecord: AudioRecord? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            SERVICE_ID,
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build()
        )

        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Audio Capture Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    mediaProjection =
                        mediaProjectionManager.getMediaProjection(
                            Activity.RESULT_OK,
                            intent.getParcelableExtra(EXTRA_RESULT_DATA)!!
                        ) as MediaProjection
                    startAudioCapture()
                    Service.START_STICKY
                }
                ACTION_STOP -> {
                    stopAudioCapture()
                    Service.START_NOT_STICKY
                }
                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        } else {
            Service.START_NOT_STICKY
        }
    }

    private fun startAudioCapture() {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA) // TODO provide UI options for inclusion/exclusion
            .build()


val audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//            .setSampleRate(8000)
        .setSampleRate(44100)
        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
        .build()

        audioRecord = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                // For optimal performance, the buffer size
                // can be optionally specified to store audio samples.
                // If the value is not specified,
                // uses a single frame and lets the
                // native code figure out the minimum buffer size.
                .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
                .setAudioPlaybackCaptureConfig(config)
                .build()

                audioRecord!!.startRecording()
                audioCaptureThread = thread(start = true) {
                val outputFile = createAudioFile()
                Log.d(LOG_TAG, "Created file for capture target: ${outputFile.absolutePath}")
                writeAudioToFile(outputFile)
                }
                }

private fun createAudioFile(): File {
        val audioCapturesDirectory = File(getExternalFilesDir(null), "/AudioCaptures")
        if (!audioCapturesDirectory.exists()) {
        audioCapturesDirectory.mkdirs()
        }
        val timestamp = SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.US).format(Date())
        val fileName = "Capture-$timestamp.pcm"
        return File(audioCapturesDirectory.absolutePath + "/" + fileName)
        }

private fun writeAudioToFile(outputFile: File) {
        val fileOutputStream = FileOutputStream(outputFile)
        val capturedAudioSamples = ShortArray(NUM_SAMPLES_PER_READ)

        while (!audioCaptureThread.isInterrupted) {
        audioRecord?.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)

        // This loop should be as fast as possible to avoid artifacts in the captured audio
        // You can uncomment the following line to see the capture samples but
        // that will incur a performance hit due to logging I/O.
        // Log.v(LOG_TAG, "Audio samples captured: ${capturedAudioSamples.toList()}")

        fileOutputStream.write(
        capturedAudioSamples.toByteArray(),
        0,
        BUFFER_SIZE_IN_BYTES
        )
        }

        fileOutputStream.close()
        Log.d(LOG_TAG, "Audio capture finished for ${outputFile.absolutePath}. File size is ${outputFile.length()} bytes.")
        }

private fun stopAudioCapture() {
        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }

        audioCaptureThread.interrupt()
        audioCaptureThread.join()

        audioRecord!!.stop()
        audioRecord!!.release()
        audioRecord = null

        mediaProjection!!.stop()
        stopSelf()
        }

        override fun onBind(p0: Intent?): IBinder? = null

private fun ShortArray.toByteArray(): ByteArray {
        // Samples get translated into bytes following little-endianness:
        // least significant byte first and the most significant byte last
        val bytes = ByteArray(size * 2)
        for (i in 0 until size) {
        bytes[i * 2] = (this[i] and 0x00FF).toByte()
        bytes[i * 2 + 1] = (this[i].toInt() shr 8).toByte()
        this[i] = 0
        }
        return bytes
        }

        companion object {
private const val LOG_TAG = "AudioCaptureService"
private const val SERVICE_ID = 123
private const val NOTIFICATION_CHANNEL_ID = "AudioCapture channel"

//        private const val NUM_SAMPLES_PER_READ = 1024
private const val NUM_SAMPLES_PER_READ = 1024
private const val BYTES_PER_SAMPLE = 2 // 2 bytes since we hardcoded the PCM 16-bit format
private const val BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE

        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"
        }
        }
 */
