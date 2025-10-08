package de.jade_hs.afex.AcousticFeatureExtraction;

import java.util.HashMap;

import com.konovalov.vad.silero.Vad;
import com.konovalov.vad.silero.VadSilero;
import com.konovalov.vad.silero.config.FrameSize;
import com.konovalov.vad.silero.config.Mode;
import com.konovalov.vad.silero.config.SampleRate;

import de.jade_hs.afex.Processing.PersonalVAD;
import de.jade_hs.afex.Processing.Utilities;

public class StageProcCombinedVAD extends Stage {

    final static String LOG = "StageProcCombinedVAD";

    private VadSilero vad;
    private PersonalVAD pvad;
    int outchannels = 1;
    float[] vadBuffer;

    public StageProcCombinedVAD(HashMap parameter) {
        super(parameter);
    }

    @Override
    void start(){

        // general VAD (Silero)
        vad = Vad.builder()
                .setContext(context)
                .setSampleRate(SampleRate.SAMPLE_RATE_16K)
                .setFrameSize(FrameSize.FRAME_SIZE_512)
                .setMode(Mode.NORMAL)
                .setSilenceDurationMs(0)
                .setSpeechDurationMs(25)
                .build();

        // pVAD
        pvad = new PersonalVAD(context);

        super.start();
    }


    @Override
    protected void process(float[][] buffer) {

        // vad needs 512 samples and pvad 256, use 50% overlap for vad and init with zeroes for the
        // 1st block.
        if (vadBuffer == null) {
            vadBuffer = new float[512];
            for (int i = 0; i < blockSize; i++) {
                vadBuffer[i] = 0.0f;
            }
        }
        System.arraycopy(buffer[0], 0, vadBuffer, 256, blockSize);

        // Normalise, apparently this isn't done in Silero...
        Utilities.normaliseToDbFS(buffer, -12.0f);

        if (passthrough) {
            outchannels += buffer.length;
        }

        // feature data layout: [vad(0|1), pvad(probabilities):no_speech, non-target, target, target & non-target]
        boolean isSpeech = vad.isSpeech(vadBuffer);
        float[] probSpeech = pvad.processAudio(buffer[0]);
        float[][] dataOut = new float[outchannels][1+probSpeech.length];
        dataOut[outchannels-1][0] = isSpeech ? 1.0f : 0.0f;
        System.arraycopy(probSpeech, 0, dataOut[outchannels-1], 1, probSpeech.length);
        //System.out.printf("VAD: %s\n", java.util.Arrays.toString(dataOut[outchannels-1]));

        // add audio data
        if (passthrough) {
            for (int channel = 0; channel < buffer.length; channel++) {
                dataOut[channel] = new float[buffer[channel].length];
                System.arraycopy(buffer[channel], 0, dataOut[channel], 0, buffer[channel].length);
            }
        }

        // shift VAD buffer
        System.arraycopy(vadBuffer, 256, vadBuffer, 0, blockSize);

        // send to UI
        //sendMessage("VAD", String.valueOf(isSpeech));
        sendMessage("pVAD", String.valueOf(probSpeech[2]));

        send(dataOut);
    }

    @Override
    protected void cleanup() {
        vad.close();
        pvad.close();
    }


}
