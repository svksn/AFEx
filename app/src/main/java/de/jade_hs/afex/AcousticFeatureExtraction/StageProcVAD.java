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
                .setSilenceDurationMs(10)
                .setSpeechDurationMs(50)
                .build();

        super.start();
    }

    @Override
    protected void process(float[][] buffer) {

        // Normalise to [-1, 1], apparently this isn't done in Silero...
        for (int channel = 0; channel < buffer.length; channel++) {
            float max = 0f;
            for (float sample : buffer[channel]) {
                if (Math.abs(sample) > max) {
                    max = Math.abs(sample);
                }
            }
            if (max > 0) {
                for (int sample = 0; sample < buffer[channel].length; sample++) {
                    buffer[channel][sample] /= max;
                }
            }
        }

        float[][] dataOut = new float[buffer.length][1];
        for (int channel = 0; channel < buffer.length; channel++) {
            boolean isSpeech = vad.isSpeech(buffer[channel]);
            sendMessage("VAD", String.valueOf(isSpeech));
            dataOut[channel][0] = isSpeech ? 1 : 0;
        }

        send(dataOut);
    }

    @Override
    protected void cleanup() {
        vad.close();
    }

}
