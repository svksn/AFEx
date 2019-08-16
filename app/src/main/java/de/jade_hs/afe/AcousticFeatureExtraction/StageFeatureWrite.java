package de.jade_hs.afe.AcousticFeatureExtraction;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.jade_hs.afe.Tools.AudioFileIO;

/**
 * Write feature data to disk
 *
 * - Timestamp calculation is based on time set in Stage and relative block sizes. Implement
 *   sanity check to compare calculated to actual time. Take into account the delay of the
 *   processing queue.
 * - How to determine the size of feature files and when to start a new one?
 *    a) fixed number of blocks / samples
 *    b) file size, e.g. write current block as long as < 1 MB, start new file if > 1 MB
 *    c) fixed time, e.g. 1 minute as with the old system.
 */

public class StageFeatureWrite extends Stage {


    private static final String EXTENSION = ".feat";

    private RandomAccessFile featureRAF = null;

    private Date startTime;

    private String timestamp;
    private String feature;

    private int nFeatures;
    private int blockCount;

    private float hopDuration;
    private float[] relTimestamp = new float[2];

    private float featFileSizeMs = 60000; // size of feature files in ms.

    SimpleDateFormat timeformat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US);

    public StageFeatureWrite(HashMap parameter) {
        super(parameter);

        this.feature = (String) parameter.get("prefix");
        this.nFeatures = Integer.parseInt((String) parameter.get("nfeatures"));
    }

    @Override
    void start(){

        startTime = Stage.startTime;
        openFeatureFile();

        super.start();
    }

    @Override
    protected void process(float[][] buffer) {

        System.out.println("buffer: " + buffer.length + "|" + buffer[0].length);
        appendFeature(buffer);
    }

    @Override
    protected void cleanup() {

        closeFeatureFile();
    }

    private void openFeatureFile() {

        File directory = Environment.getExternalStoragePublicDirectory(AudioFileIO.FEATURE_FOLDER);
        if (!directory.exists()) {
            directory.mkdir();
        }

        if (featureRAF != null) {
            closeFeatureFile();
        }

        timestamp = timeformat.format(startTime);

        try {

            featureRAF = new RandomAccessFile(new File(directory +
                    "/" + feature + "_" + timestamp + EXTENSION),
                    "rw");

            // write header
            featureRAF.writeInt(0);             // block count will be written on close
            featureRAF.writeInt(nFeatures + 2); // feature dimension count + timestamps (relative)
            featureRAF.writeInt(blockSize);        // [samples]
            featureRAF.writeInt(hopSize);          // [samples]

            featureRAF.writeInt(samplingrate);

            featureRAF.writeBytes(timestamp.substring(9));    // HHMMssSSS, 9 bytes (absolute timestamp)

            blockCount = 0;
            relTimestamp[0] = 0;

            hopDuration = (float) hopSize / samplingrate;
            relTimestamp[1] = (float) blockSize / samplingrate;

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    protected void appendFeature(float[][] data) {

        // start a new feature file?
        if (relTimestamp[1] >= featFileSizeMs) {
            // Update timestamp based on samples processed. This only considers block- and hopsize
            // of the previous stage. If another stage uses different hopping, averaging or any
            // other mechanism to obscure samples vs. time, this has to be tracked elsewhere!


            openFeatureFile();
        }

        // buffer size: number of (nfeatures + 2 timestamps) * 4 bytes
        ByteBuffer bbuffer = ByteBuffer.allocate(4 * (nFeatures + 2));
        FloatBuffer fbuffer = bbuffer.asFloatBuffer();

        fbuffer.put(relTimestamp);

        for (float[] aData : data) {
            fbuffer.put(aData);
        }

        relTimestamp[0] += hopDuration;
        relTimestamp[1] += hopDuration;

        try {
            featureRAF.getChannel().write(bbuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        blockCount++;
    }


    private void closeFeatureFile() {
        try {

            featureRAF.seek(0);
            featureRAF.writeInt(blockCount);
            featureRAF.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
