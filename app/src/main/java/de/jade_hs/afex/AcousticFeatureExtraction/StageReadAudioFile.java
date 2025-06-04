package de.jade_hs.afex.AcousticFeatureExtraction;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import de.jade_hs.afex.Tools.AudioFileIO;


/**
 * Read audio file for testing and debugging
 */

public class StageReadAudioFile extends Stage {

    final static String LOG = "StageProducer";

    private int channels, frames;
    AudioFileIO io;
    InputStream stream;
    private boolean stopProducing = false;


    public StageReadAudioFile(HashMap parameters) {
        super(parameters);

        hasInput = false;

        channels = 2;
        frames = 1024;

    }

    @Override
    void start() {

        AssetManager assetManager = context.getAssets();
        try {
            stream = assetManager.open("2902-9006-0007.wav");
            // skip wav header (44 bytes)
            stream.skip(44);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        super.start();
    }

    @Override
    protected void process(float[][] temp) {


        int bytesRead, idx = 0;
        byte[] byteArray = new byte[frames * channels * 2];
        float[][] dataOut = new float[channels][frames];

        Log.d(LOG, "Started producing");

        while (!stopProducing & !Thread.currentThread().isInterrupted()) {

            // read data from audio file
            try {
                bytesRead = stream.read(byteArray);
                if (bytesRead == -1) break; // EOF

                // Create a ByteBuffer only for the valid bytes read
                ByteBuffer buffer = ByteBuffer.wrap(byteArray, 0, bytesRead);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.position(0);

                for (int k = 0; k < bytesRead / 4; k++) {
                    dataOut[0][k] = (float) buffer.getShort() / 32768.0f;
                    dataOut[1][k] = (float) buffer.getShort() / 32768.0f;
                }

                send(dataOut);
                dataOut = new float[channels][frames];

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        Log.d(LOG, "Stopped producing");

    }



    public void setStopProducing() {

        stopProducing = true;

    }

}
