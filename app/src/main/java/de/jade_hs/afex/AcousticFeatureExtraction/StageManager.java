package de.jade_hs.afex.AcousticFeatureExtraction;

import android.content.Context;
import android.os.Environment;

import org.threeten.bp.Instant;

import java.io.File;

import de.jade_hs.afex.Tools.AudioFileIO;

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
        File features = new File(Environment.getExternalStoragePublicDirectory(AudioFileIO.MAIN_FOLDER) + File.separator + AudioFileIO.STAGE_CONFIG);

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
