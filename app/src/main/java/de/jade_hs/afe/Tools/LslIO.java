package de.jade_hs.afe.Tools;

import java.io.IOException;

import edu.ucsd.sccn.LSL;

public class LslIO {

    LSL.StreamInfo info;
    LSL.StreamOutlet outlet;

    public LslIO() {

        info = new LSL.StreamInfo(
                "MyMarkers",
                "Markers",
                1,
                LSL.IRREGULAR_RATE,
                LSL.ChannelFormat.string,
                "AFEx42");

        try {
            outlet = new LSL.StreamOutlet(info);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void LslSend() {

        String[] sample = {"Boink!"};
        try {
            outlet.push_sample(sample);
        } catch(Exception e)  {
            e.printStackTrace();
        }

    }

}
