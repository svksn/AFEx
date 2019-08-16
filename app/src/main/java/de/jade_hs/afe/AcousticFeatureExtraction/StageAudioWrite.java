package de.jade_hs.afe.AcousticFeatureExtraction;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import de.jade_hs.afe.Tools.AudioFileIO;
import de.jade_hs.afe.Tools.Timestamp;

/**
 * Write raw audio to disk
 */

public class StageAudioWrite extends Stage {

    final static String LOG = "StageConsumer";

    AudioFileIO io;
    DataOutputStream stream;

    public StageAudioWrite(HashMap parameter) {
        super(parameter);

        io = new AudioFileIO((String) parameter.get("filename") + "_" +Timestamp.getTimestamp(3));
        stream = io.openDataOutStream(
                samplingrate,
                channels,
                16,
                true);

    }

    @Override
    protected void process(float[][] buffer) {

//        boolean abort = false;

        Log.d(LOG, "Start consuming");

        byte[] dataOut = new byte[buffer.length * buffer[0].length * 2];

        for (int i = 0; i < buffer[0].length; i++) {

            short tmp = (short) buffer[0][i];
            dataOut[i * 4] = (byte) (tmp & 0xff);
            dataOut[i * 4 + 1] = (byte) ((tmp >> 8) & 0xff);

            tmp = (short) buffer[1][i];
            dataOut[i * 4 + 2] = (byte) (tmp & 0xff);
            dataOut[i * 4 + 3] = (byte) ((tmp >> 8) & 0xff);

        }

        try {
            stream.write(dataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(LOG, "Stopped consuming");

        io.closeDataOutStream();

    }


}
