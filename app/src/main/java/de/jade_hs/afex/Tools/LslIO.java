package de.jade_hs.afex.Tools;

import java.io.IOException;

import edu.ucsd.sccn.LSL;

public class LslIO {

    LSL.StreamInfo info;
    LSL.StreamOutlet outlet;

    public LslIO(int samplerate, int blocksize) {

        info = new LSL.StreamInfo(
                "AFEMarkers",
                "Markers",
                1,
                samplerate / blocksize,
                LSL.ChannelFormat.float32,
                "AFEx");

        try {
            outlet = new LSL.StreamOutlet(info);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void LslSend(float data) {

        float[] sample = new float[1];
        sample[0] = data;

        try {
            outlet.push_sample(sample);
        } catch(Exception e)  {
            e.printStackTrace();
        }

    }

}
