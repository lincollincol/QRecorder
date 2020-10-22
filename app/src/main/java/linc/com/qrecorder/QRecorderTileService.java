package linc.com.qrecorder;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static android.service.quicksettings.Tile.STATE_UNAVAILABLE;
import static linc.com.qrecorder.Constants.ACTION_START;
import static linc.com.qrecorder.Constants.ACTION_STOP;
import static linc.com.qrecorder.Constants.EXTRA_RESULT_DATA;
import static linc.com.qrecorder.Constants.MEDIA_PROJECTION_REQUEST_CODE;

public class QRecorderTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        System.out.println("START LISTEN");
        Tile qrecorder = getQsTile();
        switch (getQsTile().getState()) {
            case STATE_ACTIVE : {
                qrecorder.setState(STATE_INACTIVE);
                // Stop recording
//                Intent serviceIntent = new Intent(getApplicationContext(), RecorderService.class);
//                serviceIntent.setAction(ACTION_STOP);
//                startService(serviceIntent);
                break;
            }
            case STATE_UNAVAILABLE :
            case STATE_INACTIVE : {
                qrecorder.setState(STATE_ACTIVE);
                //FLAG_ACTIVITY_NEW_TASK
//                Intent i = new Intent(getApplicationContext(), AudioCaptureRequestActivity.class);
//                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivityAndCollapse(i);
                break;
            }
        }
        qrecorder.updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        System.out.println("STOP LISTEN");
    }

    @Override
    public void onClick() {
        super.onClick();
        System.out.println("CLICK");
    }
}
