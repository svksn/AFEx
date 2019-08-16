package de.jade_hs.afe.AcousticFeatureExtraction;

/**
 * Contains audio or feature data and its description
 */

public class DataPackage {

    static final int TYPE_TIME = 1;
    static final int TYPE_FREQUENCY = 2;

    static int source_samplerate; // samplerate and bitdepth of
    static int source_bitdepth;   // signal source, i.e. audio

    int type;
    int blockSize;  // samples
    int hopSize;    // samples
    int samplerate; // samples per second
    long timestamp;

    float[][] data;
}
