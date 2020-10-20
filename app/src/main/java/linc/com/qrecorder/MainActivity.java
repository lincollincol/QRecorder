package linc.com.qrecorder;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Mp3Encoder.init("/storage/emulated/0/Music/rec.pcm", 1, 64000, 22000, "/storage/emulated/0/Music/out.mp3");
        Mp3Encoder.encode();
        Mp3Encoder.destroy();
    }

}
