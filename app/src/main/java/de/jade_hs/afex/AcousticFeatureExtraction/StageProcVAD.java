package de.jade_hs.afex.AcousticFeatureExtraction;

import com.konovalov.vad.silero.Vad;
import com.konovalov.vad.silero.VadSilero;
import com.konovalov.vad.silero.config.FrameSize;
import com.konovalov.vad.silero.config.Mode;
import com.konovalov.vad.silero.config.SampleRate;

import java.util.HashMap;

/**
 * Feature: Voice Activity Detection using Silero
 * https://github.com/gkonovalov/android-vad/
 */

public class StageProcVAD extends Stage {

    final static String LOG = "StageProcVAD";
    private VadSilero vad;

    public StageProcVAD(HashMap parameter) {
        super(parameter);
    }

    @Override
    void start(){
        vad = Vad.builder()
                .setContext(context)
                .setSampleRate(SampleRate.SAMPLE_RATE_16K)
                .setFrameSize(FrameSize.FRAME_SIZE_512)
                .setMode(Mode.NORMAL)
                .setSilenceDurationMs(300)
                .setSpeechDurationMs(50)
                .build();

        super.start();
    }

    @Override
    protected void process(float[][] buffer) {

        //float[][] dataOut = new float[buffer.length][1];
        //for (int i = 0; i < buffer.length; i++) {
        boolean isSpeech = vad.isSpeech(buffer[0]);
        System.out.println("----------------> VAD: " + isSpeech);
        //}

        //send(dataOut);
    }

    @Override
    protected void cleanup() {
        vad.close();
    }

}
