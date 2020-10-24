package linc.com.qrecorder;

public final class Constants {
    public static final String LOG_TAG = "AudioCaptureService";
    public static final int SERVICE_ID = 123;
    public static final String NOTIFICATION_CHANNEL_ID = "AudioCapture channel";

    public static final String RECORDING_DEFAULT_NAME = "q_recorder_def";
    public static final int NUM_SAMPLES_PER_READ = 1024;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE;

    public static final int DECODE_SAMPLE_RATE = 22000;
    public static final int DECODE_BIT_RATE = 128000;
    public static final int DECODE_CHANNELS_COUNT = 2;

    public static final String ACTION_START = "AudioCaptureService:Start";
    public static final String ACTION_STOP = "AudioCaptureService:Stop";
    public static final String EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData";

    public static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 42;
    public static final int CAPTURE_MEDIA_PROJECTION_REQUEST_CODE = 13;
    public static final int SELECT_OUTPUT_DIRECTORY_REQUEST_CODE = 14;

    public static final String PREFERENCES_APP_NAME = "q_recorder";
    public static final String PREFERENCES_KEY_OUTPUT_DIRECTORY = "q_recorder";

    public static final String FORMAT_DATE_FULL = "hh_mm_ss";
    public static final String FORMAT_RECORDING_ENCODED = "%s/%s.pcm";
    public static final String FORMAT_RECORDING_DECODED = "%s/%s.mp3";
    public static final String FORMAT_OUTPUT_DIRECTORY = "%s/.../%s";

    public static final String TITLE_DEFAULT_DIRECTORY = "Music/";

}
