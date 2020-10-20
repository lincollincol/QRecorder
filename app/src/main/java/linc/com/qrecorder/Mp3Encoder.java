package linc.com.qrecorder;

/**
 * Created by rangaofei on 2018/4/24.
 */

public class Mp3Encoder {
    static {
        System.loadLibrary("Mp3Codec");
    }

    public static native int init(String pcmFilePath, int audioChannels, int bitRate,
                                  int sampleRate, String mp3FilePath);

    public static native void encode();

    public static native void destroy();
}
