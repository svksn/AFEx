package de.jade_hs.afex.AcousticFeatureExtraction;

import android.util.Log;

import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import de.jade_hs.afex.Tools.AudioFileIO;

import edu.ucsd.sccn.LSL;

/**
 * Write raw audio to disk
 *
 * To prevent unintentional audio recordings, this feature needs to be enabled specifically by
 * setting the corresponding flag below, in addition to an entry in features.xml
 */

public class StageAudioWrite extends Stage {

    final static String LOG = "StageConsumer";

    final static Boolean ENABLED = false;

    private LSL.StreamInfo info;
    private LSL.StreamOutlet outlet;

    private int isLsl;

    AudioFileIO io;
    DataOutputStream stream;

    DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssSSS")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    public StageAudioWrite(HashMap parameter) {
        super(parameter);

        if (ENABLED) {
            if (parameter.get("lsl") == null)
                isLsl = 0;
            else
                isLsl = Integer.parseInt((String) parameter.get("lsl"));

            if (isLsl == 1) {

                Log.d(LOG, "----------> " + id + ": LSL enabled");

                info = new LSL.StreamInfo(
                        "RawAudio",
                        "Audio",
                        channels,
                        blockSize,
                        LSL.ChannelFormat.int8,
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

    }

    @Override
    void start() {

        if (ENABLED) {
            io = new AudioFileIO("cache_" + timeFormat.format(Stage.startTime));

            stream = io.openDataOutStream(
                    samplingrate,
                    channels,
                    16,
                    true);

            super.start();
        }
    }


    void rebuffer() {

        // we do not want rebuffering in a writer stage, just get the data and and pass it on.

        boolean abort = false;

        Log.d(LOG, "----------> " + id + ": Start processing");

        while (!Thread.currentThread().isInterrupted() & !abort) {

            float[][] data = receive();

            if (data != null) {

                process(data);

            } else {
                abort = true;
            }
        }

        io.closeDataOutStream();

        if (isLsl == 1) {
            //outlet.close();
            //info.destroy();
        }

        Log.d(LOG, id + ": Stopped consuming");
    }


    @Override
    protected void process(float[][] buffer) {

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
            if (isLsl == 1) {
                outlet.push_sample(dataOut);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
