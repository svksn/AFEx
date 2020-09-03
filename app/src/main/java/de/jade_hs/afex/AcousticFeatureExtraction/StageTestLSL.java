package de.jade_hs.afex.AcousticFeatureExtraction;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

import edu.ucsd.sccn.LSL;


/**
 * Test Data Producer
 */

public class StageTestLSL extends Stage {

    final static String LOG = "StageTestLSL";

    private int channels, frames;
    private boolean stopProducing = false;


    public StageTestLSL(HashMap parameters) {
        super(parameters);

        hasInput = false;
    }

    @Override
    protected void process(float[][] temp) {

        LSL.StreamOutlet outlet = null;
        int[] data = new int[10];

        for (int i = 0; i < 10; i++) {
            data[i] = i;
        }

        LSL.StreamInfo info = new LSL.StreamInfo(
                "AFEx",
                "EEG",
                1,
                10,
                LSL.ChannelFormat.int32,
                "123" );

        try {
            outlet = new LSL.StreamOutlet(info);
        } catch (IOException e) {
            System.err.println(e.toString());
        }

        Log.d(LOG, "Started producing");


        while (!stopProducing & !Thread.currentThread().isInterrupted()) {

            if (outlet == null) {
                System.err.println("break...");
                break;
            }

            outlet.push_sample(data);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        outlet.close();
        info.destroy();

        Log.d(LOG, "Stopped producing");

    }


    public void setStopProducing() {

        stopProducing = true;

    }

}
