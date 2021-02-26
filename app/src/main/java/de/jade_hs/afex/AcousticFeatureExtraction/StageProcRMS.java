package de.jade_hs.afex.AcousticFeatureExtraction;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

import edu.ucsd.sccn.LSL;

/**
 * Feature extraction: RMS
 */

public class StageProcRMS extends Stage {

    final static String LOG = "StageProcRMS";

    private LSL.StreamInfo info;
    private LSL.StreamOutlet outlet;
    private int isLsl;
    private int fsLsl;

    public StageProcRMS(HashMap parameter) {
        super(parameter);

        if (parameter.get("lsl") == null)
            isLsl = 0;
        else
            isLsl = Integer.parseInt((String) parameter.get("lsl"));

        if (isLsl == 1) {

            if (parameter.get("lsl_rate") == null)
                fsLsl = 40;
            else
                fsLsl = Integer.parseInt((String) parameter.get("lsl"));

            Log.d(LOG, "----------> " + id + ": LSL enabled (rate: " + fsLsl +" Hz)");

            info = new LSL.StreamInfo(
                    "AFEx",
                    "rms",
                    2,
                    fsLsl,
                    LSL.ChannelFormat.float32,
                    "AFEx");

            try {
                outlet = new LSL.StreamOutlet(info);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(LOG, "----------> " + id + ": LSL disabled");
        }
    }


    @Override
    protected void process(float[][] buffer) {

        float[][] dataOut = new float[buffer.length][1];

        for (int i = 0; i < buffer.length; i++) {
            dataOut[i][0] = rms(buffer[i]);
        }

        if (isLsl == 1) {

            float[] dataLsl = new float[channels];

            dataLsl[0] = dataOut[0][0];
            dataLsl[1] = dataOut[1][0];

            outlet.push_sample(dataLsl);
        }

        send(dataOut);
    }

    protected float rms(float[] data) {

        float temp = 0;

        for (float sample : data) {
            temp += sample * sample;
        }
        temp /= data.length;
        return (float) Math.sqrt(temp);

    }

}
