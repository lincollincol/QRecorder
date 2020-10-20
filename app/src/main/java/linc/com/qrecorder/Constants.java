package linc.com.qrecorder;

public final class Constants {
    public static final String LOG_TAG = "AudioCaptureService";
    public static final int SERVICE_ID = 123;
    public static final String NOTIFICATION_CHANNEL_ID = "AudioCapture channel";

    public static final int NUM_SAMPLES_PER_READ = 1024;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE;

    public static final String ACTION_START = "AudioCaptureService:Start";
    public static final String ACTION_STOP = "AudioCaptureService:Stop";
    public static final String EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData";

    public static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 42;
    public static final int MEDIA_PROJECTION_REQUEST_CODE = 13;
}
