package de.jade_hs.afex.AcousticFeatureExtraction;

import android.util.Log;

import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier;
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult;
import com.google.mediapipe.tasks.audio.core.RunningMode;
import com.google.mediapipe.tasks.components.containers.AudioData;
import com.google.mediapipe.tasks.core.BaseOptions;


import java.util.HashMap;


/**
 * Feature: Audio Classification using yamnet
 */

public class StageProcClassify extends Stage {

    final static String LOG = "StageProcClassify";
    private final AudioClassifier audioClassifier;
    final private AudioData audioData;

    public StageProcClassify(HashMap parameter) {
        super(parameter);

        Log.d(LOG, id + ": load yamet Model");

        BaseOptions baseOptions =
                BaseOptions.builder().setModelAssetPath("yamnet.tflite").build();

        Log.d(LOG, id + ": setup classifier");

        AudioClassifier.AudioClassifierOptions options =
            AudioClassifier.AudioClassifierOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.AUDIO_CLIPS)
                    .setMaxResults(5)
                    .build();
        ;

        Log.d(LOG, id + ": create classifier");
        Log.d(LOG, id + ": context: " + context.toString());
        Log.d(LOG, id + ": options: " + options.toString());

        audioClassifier = AudioClassifier.createFromOptions(context, options);

        Log.d(LOG, id + ": setup audioData");

        // input length for yamnet is 0.96 * 16000 = 15360 samples = blockSize
        audioData = AudioData.create(AudioData.AudioDataFormat.builder()
                                .setNumOfChannels(1)
                                .setSampleRate(samplingrate)
                                .build(),
                            blockSize);
    }


    @Override
    protected void process(float[][] buffer) {

        //float[][] dataOut = new float[buffer.length][1];

        //for (int i = 0; i < buffer.length; i++) {
            audioData.load(buffer[0]);
            AudioClassifierResult result = audioClassifier.classify(audioData);
            System.out.println("----------------> CLASSIFY: " + result);
        //}

        //send(dataOut);
    }


}
