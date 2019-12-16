package de.jade_hs.afe.AcousticFeatureExtraction;

import android.os.Environment;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Locale;

import de.jade_hs.afe.Tools.AudioFileIO;

/**
 * Write feature data to disk
 *
 * - Per default the writer assumes that incoming data represents one frame.
 *   If the array is a multidimensional, the data is concatenated.
 *   TODO: Check if allocating for each small buffer is problematic of if it is reasonable to cache
 *     small buffers (e.g. RMS).
 *     Alternatively, all features could be required to concatenate before sending, so the number of
 *     frames can be calculated from the incoming array size.
 *
 * - Length of a feature file is determined by time, corresponding to e.g. 60 seconds of audio data.
 *
 * - Timestamp calculation is based on time set in Stage and relative block sizes. Implement
 *   sanity check to compare calculated to actual time? Take into account the delay of the
 *   processing queue?
 */

public class StageFeatureWrite extends Stage {


    private static final String EXTENSION = ".feat";

    private RandomAccessFile featureRAF = null;

    private Instant startTime;

    private String timestamp;
    private String feature;

    private int nFeatures;
    private int blockCount;

    private float hopDuration;
    private float[] relTimestampMs = new float[]{0, 0};

    private float featFileSizeMs = 10000; // size of feature files in ms.

    DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssSSS")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

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
    protected void process(float[][] data) {

        System.out.println("id: " + id + " buffer: " + data.length + "|" + data[0].length);
        appendFeature(data);
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

        // add length of last feature file to timestamp
        timestamp = timeFormat.format(startTime.plusMillis((long) relTimestampMs[1]));

        try {

            featureRAF = new RandomAccessFile(new File(directory +
                    "/" + feature + "_" + timestamp + EXTENSION),
                    "rw");

            // write header
            featureRAF.writeInt(0);             // block count will be written on close
            featureRAF.writeInt(nFeatures + 2); // feature dimension count + timestamps (relative)
            featureRAF.writeInt(inStage.blockSize);        // [samples]
            featureRAF.writeInt(inStage.hopSize);          // [samples]

            featureRAF.writeInt(samplingrate);

            featureRAF.writeBytes(timestamp.substring(9));    // HHMMssSSS, 9 bytes (absolute timestamp)

            blockCount = 0;
            relTimestampMs[0] = 0;

            hopDuration = (float) inStage.hopSize / samplingrate;
            relTimestampMs[1] = (float) inStage.blockSize / samplingrate;

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    protected void appendFeature(float[][] data) {

        // start a new feature file?
        if (relTimestampMs[1] >= featFileSizeMs) {
            // Update timestamp based on samples processed. This only considers block- and hopsize
            // of the previous stage. If another stage uses different hopping, averaging or any
            // other mechanism to obscure samples vs. time, this has to be tracked elsewhere!


            openFeatureFile();
        }

        // buffer size: number of (nfeatures + 2 timestamps) * 4 bytes
        ByteBuffer bbuffer = ByteBuffer.allocate(4 * (nFeatures + 2));
        FloatBuffer fbuffer = bbuffer.asFloatBuffer();

        fbuffer.put(relTimestampMs);

        for (float[] aData : data) {
            fbuffer.put(aData);
        }

        relTimestampMs[0] += hopDuration;
        relTimestampMs[1] += hopDuration;

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
