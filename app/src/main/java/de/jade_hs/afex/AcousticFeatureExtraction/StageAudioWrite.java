package de.jade_hs.afex.AcousticFeatureExtraction;

import android.util.Log;

import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Locale;

import de.jade_hs.afex.Tools.AudioFileIO;


/**
 * Write raw audio to disk
 *
 * To prevent unintentional audio recordings, this feature needs to be enabled specifically by
 * setting the corresponding flag below, in addition to an entry in features.xml
 */

public class StageAudioWrite extends Stage {

    final static String LOG = "StageConsumer";

    final static Boolean ENABLED = true;

    AudioFileIO io;
    DataOutputStream stream;

    DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssSSS")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    public StageAudioWrite(HashMap parameter) {
        super(parameter);
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

        Log.d(LOG, id + ": Stopped consuming");
    }


    @Override
    protected void process(float[][] data) {


        ByteBuffer buffer = ByteBuffer.allocate(data.length * data[0].length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < data[0].length; i++) {
            buffer.putShort((short) (data[0][i] * 32767));
            buffer.putShort((short) (data[1][i] * 32767));
        }

        try {
            stream.write(buffer.array());
            Log.d(LOG, id + ": Boink!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
