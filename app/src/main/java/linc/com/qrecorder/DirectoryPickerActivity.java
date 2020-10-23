package linc.com.qrecorder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import linc.com.getme.GetMe;
import linc.com.getme.domain.entities.GetMeFilesystemSettings;
import linc.com.getme.ui.GetMeInterfaceSettings;
import linc.com.getme.ui.callbacks.CloseFileManagerCallback;
import linc.com.getme.ui.callbacks.FileManagerCompleteCallback;
import linc.com.getme.utils.CloseParameterCallback;

public class DirectoryPickerActivity extends AppCompatActivity implements CloseFileManagerCallback, FileManagerCompleteCallback {
    private GetMe getMe;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_picker);

        getMe = new GetMe(
                getSupportFragmentManager(),
                R.id.fragmentContainer,
                new GetMeFilesystemSettings(
                        GetMeFilesystemSettings.ACTION_SELECT_DIRECTORY,
                        null,
                        null,
                        null,
                        false
                ),
                new GetMeInterfaceSettings(
                        GetMeInterfaceSettings.SELECTION_SINGLE,
                        0,
                        true,
                        GetMeInterfaceSettings.ANIMATION_ADAPTER_DISABLE,
                        false,
                        0
                ),
                this,
                this,
                null,
                findViewById(R.id.select),
                null,
                false,
                R.style.GetMeThemeQRecorder,
                -1 // Default layout
        );

        if(savedInstanceState == null) {
            getMe.show();
        }
    }

    @Override
    public void onBackPressed() {
        getMe.onBackPressed();
    }

    @Override
    public void onCloseFileManager() {
        getMe.close(this::finish);
    }

    @Override
    public void onFilesSelected(List<? extends File> list) {
        Intent data = new Intent();
        data.setData(Uri.parse(
                list.get(0).getPath() // Put selected directory path
        ));
        setResult(RESULT_OK, data);
    }
}
