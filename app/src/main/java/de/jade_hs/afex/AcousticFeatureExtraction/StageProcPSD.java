package de.jade_hs.afex.AcousticFeatureExtraction;

import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;

import edu.ucsd.sccn.LSL;

/**
 * Feature extraction: Auto- and cross correlation
 *
 * Data is smoothed so no information can be recovered when recreating a time signal from these
 * spectra. It contains a few magic numbers and makes certain assumptions about the input buffer
 * (25 ms, 50 % overlap) and sends spectra representing chunks of 125 ms.
 *
 * Use the following feature definition:
 * <stage feature="StageProcPSD" id="7" blocksize="400" hopsize="200" blockout="2000" hopout="2000"/>
 *
 */

public class StageProcPSD extends Stage {

    final static String LOG = "StageProcPSD";

    CPSD cpsd;

    private LSL.StreamInfo info;
    private LSL.StreamOutlet outlet;
    private int isLsl;
    private int fsLsl;

    public StageProcPSD(HashMap parameter) {
        super(parameter);
        cpsd = new CPSD();

        if (parameter.get("lsl") == null)
            isLsl = 0;
        else
            isLsl = Integer.parseInt((String) parameter.get("lsl"));

        if (isLsl == 1) {

            if (parameter.get("lsl_rate") == null)
                fsLsl = 8;
            else
                fsLsl = Integer.parseInt((String) parameter.get("lsl"));

            Log.d(LOG, "----------> " + id + ": LSL enabled (rate: " + fsLsl +" Hz)");

            info = new LSL.StreamInfo(
                    "AFEx",
                    "psd",
                    2 * cpsd.nfft + 2,
                    fsLsl,
                    LSL.ChannelFormat.float32,
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

    @Override
    protected void cleanup() {
        if (isLsl == 1) {
            //outlet.close();
            //info.destroy();
        }
        Log.d(LOG, "Stopped " + LOG);
    }

    @Override
    protected void process(float[][] buffer) {
        cpsd.calculate(buffer);
    }


    private class CPSD {

        float alpha, win_energy = 0;
        float[] window, Xx, Xy;
        float[][] data, dataConj, P, Ptemp;
        int nfft, samples, block = 0;
        FloatFFT_1D fft;

        CPSD() {
            nfft = nextpow2(blockSize);
            System.out.println("----------------> NFFT: " + nfft);

            // window & energy
            window = hann(blockSize);
            for (float i : window) {
                win_energy += i * i;
            }

            fft = new FloatFFT_1D(nfft);
            alpha = (float) Math.exp(-hopSize / (samplingrate * 0.125));
            samples = blockSize;

            dataConj = new float[channels][];
            P = new float[3][nfft * 2];
        }

        void calculate(float[][] input) {

            data = new float[channels][nfft * 2];

            for (int iChannel = 0; iChannel < channels; iChannel++) {

                // window
                for (int i = 0; i < samples; i++) {
                    data[iChannel][i] = input[iChannel][i] * window[i];
                }

                // FFT
                fft.realForwardFull(data[iChannel]);

                // complex conjugate
                dataConj[iChannel] = Arrays.copyOf(data[iChannel], data[iChannel].length);
                for (int iSample = 3; iSample < data[iChannel].length; iSample += 2) {
                    dataConj[iChannel][iSample] = -data[iChannel][iSample];
                }
            }

            // correlation
            for (int i = 0; i < 2 * nfft - 1; i += 2) {
                P[0][i] = data[0][i] * dataConj[1][i] - data[0][i + 1] * dataConj[1][i + 1];
                P[0][i + 1] = data[0][i] * dataConj[1][i + 1] + data[0][i + 1] * dataConj[1][i];

                P[1][i] = data[0][i] * dataConj[0][i] - data[0][i + 1] * dataConj[0][i + 1];
                P[1][i + 1] = data[0][i] * dataConj[0][i + 1] + data[0][i + 1] * dataConj[0][i];

                P[2][i] = data[1][i] * dataConj[1][i] - data[1][i + 1] * dataConj[1][i + 1];
                P[2][i + 1] = data[1][i] * dataConj[1][i + 1] + data[1][i + 1] * dataConj[1][i];
            }

            if (Ptemp == null) {
                // 1st block? Initialize Ptemp with (1-alpha) * P
                Ptemp = new float[3][nfft * 2];
                for (int k = 0; k < 3; k++) {
                    for (int i = 0; i < nfft * 2; i++) {
                        Ptemp[k][i] = (1 - alpha) * P[k][i];
                    }
                }
            } else {
                // recursive averaging & store data for next average (Ptemp)
                for (int k = 0; k < 3; k++) {
                    for (int i = 0; i < nfft * 2; i++) {
                        Ptemp[k][i] = alpha * Ptemp[k][i] + (1 - alpha) * P[k][i];
                    }
                }
            }


            // count blocks
            block++;

            // write a block every 125 ms, i.e. every 10th block (for blocksize of 25ms)
            if (block >= 10) {

                float[][] dataOut = new float[3][];
                dataOut[0] = new float[nfft + 2]; // complex spectrum (cross-correlation)
                dataOut[1] = new float[nfft / 2 + 1]; // real spectrum (auto-correlation)
                dataOut[2] = new float[nfft / 2 + 1]; // real spectrum (auto-correlation)

                // copy one-sided spectrum for cross-correlation and scale
                dataOut[0][0]    = Ptemp[0][0] / samplingrate / win_energy; // 0 Hz
                dataOut[0][nfft] = Ptemp[0][nfft] / samplingrate / win_energy; // fs/2
                for (int i = 2; i < nfft; i++) {
                    dataOut[0][i] = 2 * Ptemp[0][i] / samplingrate / win_energy;
                }


                // copy one sided spectrum for auto correlation, scale, omit imaginary parts
                for (int k = 1; k < 3; k++) {
                    dataOut[k][0] = Ptemp[k][0] / samplingrate / win_energy;;
                    dataOut[k][nfft / 2] = Ptemp[k][nfft] / samplingrate / win_energy;;
                    for (int i = 1; i < nfft / 2; i++) {
                        dataOut[k][i] = 2 * Ptemp[k][2 * i] / samplingrate / win_energy;
                    }
                }

                if (isLsl == 1) {

                    float[] dataLsl = new float[dataOut[0].length +
                            dataOut[1].length + dataOut[2].length];

                    System.arraycopy(dataOut[0], 0, dataLsl, 0, dataOut[0].length);
                    System.arraycopy(dataOut[1], 0, dataLsl, dataOut[0].length, dataOut[1].length);
                    System.arraycopy(dataOut[2], 0, dataLsl, dataOut[0].length + dataOut[1].length, dataOut[2].length);

                    outlet.push_sample(dataLsl);
                }

                send(dataOut);
                block = 0;

            }

        }


        private int nextpow2(int x) {

            return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));

        }


        private float[] hann(int samples) {

            float[] window = new float[samples];

            // calculate window
            for (int i = 0; i < samples; i++) {
                window[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * (float) i / samples));
            }

            return window;

        }

    }

}
