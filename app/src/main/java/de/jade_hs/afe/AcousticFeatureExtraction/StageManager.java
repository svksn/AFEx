package de.jade_hs.afe.AcousticFeatureExtraction;

import android.content.Context;
import android.os.Environment;

import org.threeten.bp.Instant;

import java.io.File;
import java.util.Date;

import de.jade_hs.afe.R;
import de.jade_hs.afe.Tools.Timestamp;

/**
 * Sets up and starts stages, i.e. producers, conducers and consumers.
 */

public class StageManager {

    Stage mainStage;
    public boolean isRunning = false;

    public StageManager(Context context) {

        //android.os.Debug.waitForDebugger();

        Stage.samplingrate = 16000;
        Stage.channels = 2;
        Stage.context = context;

        // build processing tree
        // TODO: properly get path, see FileIO and AudioFileIO.
        File features = new File(Environment.getExternalStoragePublicDirectory("/AFE")
                + "/features.xml");

        mainStage = new StageFactory().parseConfig(features);

    }

    public void start() {

        // Start time is set here, will get overwritten in 1st Stage, e.g. StageAudioCapture.
        Stage.startTime = Instant.now();

        mainStage.start();
        isRunning = true;

    }

    public void stop() {

        mainStage.stop();
        // Following stages will stop automagically when queue.poll() times out.
        isRunning = false;

    }

}
