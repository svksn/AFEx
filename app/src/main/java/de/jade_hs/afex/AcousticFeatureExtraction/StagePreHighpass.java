package de.jade_hs.afex.AcousticFeatureExtraction;

import java.util.HashMap;

import de.jade_hs.afex.Processing.Preprocessing.FilterHP;

/**
 * Audio preprocessing
 */

public class StagePreHighpass extends Stage {

    final static String LOG = "StagePreHighpass";

    int cutoff_hz;

    FilterHP[] filterHP;


    public StagePreHighpass(HashMap parameter) {
        super(parameter);

        cutoff_hz = Integer.parseInt((String) parameter.get("cutoff_hz"));

        filterHP = new FilterHP[channels];
        for (int i = 0; i < channels; i++) {
            filterHP[i] = new FilterHP(samplingrate, cutoff_hz);
        }
    }

    @Override
    protected void process(float[][] buffer) {

        float[][] dataOut = new float[buffer.length][buffer[0].length];

        for (int i = 0; i < 2; i++) {
            System.arraycopy(buffer[i], 0, dataOut[i], 0, buffer[i].length);
            filterHP[i].filter(dataOut[i]);
        }

        send(dataOut);
    }

}
