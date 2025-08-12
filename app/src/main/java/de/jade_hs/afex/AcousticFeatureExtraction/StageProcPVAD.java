package de.jade_hs.afex.AcousticFeatureExtraction;

import android.content.Context;

import com.konovalov.vad.silero.Vad;
import com.konovalov.vad.silero.VadSilero;
import com.konovalov.vad.silero.config.FrameSize;
import com.konovalov.vad.silero.config.Mode;
import com.konovalov.vad.silero.config.SampleRate;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.Map;
//import javax.sound.sampled.AudioInputStream;
//import javax.sound.sampled.AudioSystem;

import java.util.HashMap;

/**
 * pVAD (Fraunhofer IDMT, Java implementation by Christian Bartsch)
 */

public class StageProcPVAD extends Stage {

    final static String LOG = "StageProcPVAD";

    // Parameters:
    public static final int HOP_SIZE = 256; // this is our actual processing blocksize
    private static final int FFT_SIZE = 512;

    // private static final int HIDDEN_SIZE = 32; // depends on selected model
    // private static final String MODEL_NAME = "GRU_with_FiLM_advanced_v0";
    private static final int HIDDEN_SIZE = 512; // depends on selected model
    private static final String MODEL_NAME = "GRU_with_FiLM_advanced_v24";

    // onnx runtime fields
    private OrtEnvironment env;
    private OrtSession session;

    // Buffer fields for processing
    private FloatBuffer xBuffer;
    private FloatBuffer embedBuffer;
    private FloatBuffer hiddenStateBuffer;
    private FloatBuffer hiddenStates1Buffer;

    private long[] xDimensions = {FFT_SIZE};
    private long[] embedDimensions = {HOP_SIZE};
    private long[] hiddenStateDimensions = {1, HIDDEN_SIZE};
    private long[] hiddenStates1Dimensions = {1, HIDDEN_SIZE};

    public StageProcPVAD(HashMap parameter) {
        super(parameter);
    }

    @Override
    void start(){
        // get onnx model path from assets
        File modelFile;
        try {
            modelFile = copyAssetToFile(context, "GRU_with_FiLM_advanced_v24.onnx");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Initialize ONNX Runtime
        env = OrtEnvironment.getEnvironment();

        try {
            session = this.env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());
        } catch (OrtException e) {
            e.printStackTrace();
        }

        // Initialize FloatBuffers
        xBuffer = FloatBuffer.allocate((int) xDimensions[0]);
        embedBuffer = FloatBuffer.allocate((int) embedDimensions[0]);
        hiddenStateBuffer = FloatBuffer.allocate((int) (hiddenStateDimensions[0] * hiddenStateDimensions[1]));
        hiddenStates1Buffer = FloatBuffer.allocate((int) (hiddenStates1Dimensions[0] * hiddenStates1Dimensions[1]));

        // DEBUG: Set embed test data (TestAudioDogBark.wav)
        float[] embedValues = new float[] {
                0.07045266f, 1.8098123f, 0.036292594f, 1.0127343f, -0.30012915f, -0.433575f, 0.17987597f, 0.04543436f,
                -1.189438f, -2.6509147f, -4.792768f, 0.008515392f, -1.3274196f, 1.7883815f, 0.2719817f, 1.4061042f,
                -0.12853947f, -0.11055592f, 0.26300752f, 1.2605424f, 0.8490934f, -0.5063484f, -2.055487f, 2.3199158f,
                -0.76996255f, 0.66305286f, 1.0066231f, 0.8575206f, 0.9689431f, -1.8951228f, -0.41089f, -0.2170667f,
                1.6658324f, -4.879502f, -2.0089588f, -0.22732463f, 0.8733929f, -1.0363663f, 0.18266311f, 0.8067686f,
                -1.008023f, -2.5758662f, -5.158366f, -0.1141682f, 1.4111761f, 1.0963273f, 4.879478f, -1.0852644f,
                -1.3043777f, -0.7506726f, -0.5949589f, -0.657501f, -1.2771069f, -0.50754994f, -0.14510627f, 2.1515365f,
                -0.53579587f, 0.47248474f, 0.8972875f, -0.44127616f, 2.5183406f, -1.7491239f, 1.3318224f, -1.3501236f,
                3.7025518f, -1.0162286f, -0.46269214f, -0.9064112f, -1.7918395f, -0.0029602884f, -1.0079447f, -1.1506599f,
                1.2664022f, -0.5879123f, -0.11925873f, 12.985995f, -1.2695014f, 0.37313092f, -0.14434758f, -0.31131425f,
                -0.46862325f, -1.106949f, 2.5084138f, -0.4747431f, 0.31739736f, 0.5606988f, 0.14770103f, 1.6917579f,
                -1.1862148f, -0.08606411f, -2.1097937f, 1.5031627f, -1.0045923f, -1.7290345f, 0.010898492f, -0.18555795f,
                4.9201164f, -1.7165015f, 0.62910706f, -4.217679f, -0.5833427f, -3.9233994f, 0.41531247f, -0.23233956f,
                0.09976364f, 0.26773292f, -0.74315035f, 0.862082f, -2.4274528f, 2.139965f, 0.08651884f, 0.038329247f,
                -4.672029f, -1.0990324f, -0.66788137f, -0.5946673f, 0.47247297f, -1.0934832f, 0.43813986f, 0.79689133f,
                0.9563563f, 1.9981399f, -1.5769968f, 0.99155205f, -2.218236f, -1.2244054f, -2.7259417f, 0.46854395f,
                0.61687404f, 2.9811976f, 0.67541116f, -1.6807075f, 0.533077f, -1.9851289f, -0.67051005f, -1.1352404f,
                -0.035152927f, -0.22854184f, -0.84867674f, -0.13637447f, 1.0104464f, -0.6981093f, -0.5066279f, 0.20918697f,
                2.2568941f, -3.5333738f, -2.6066303f, -0.7224276f, -0.2699807f, -1.1420099f, 0.3571903f, 1.9366289f,
                -1.3286632f, -5.0927973f, 1.9434243f, 0.12605448f, 0.92328537f, 1.57631004f, -0.09154462f, 0.2744248f,
                0.30834985f, 0.9121863f, 0.07164918f, 0.397378f, 0.8869716f, -0.9744089f, -0.75201184f, -0.18948819f,
                -0.8630247f, 0.41848472f, 0.63966697f, -0.3789029f, -0.037405916f, -0.5376635f, -1.2486885f, -0.41896465f,
                -1.3228973f, 0.42182037f, -0.7856258f, 0.8485312f, 0.5336577f, 0.116618305f, 0.62209314f, 1.25396705f,
                1.1269604f, 1.4158525f, 1.7842406f, -0.6629311f, 1.4904709f, 1.2439862f, -1.2257454f, 0.78449845f,
                0.8791182f, -0.3140904f, -1.2563474f, -1.1489135f, 0.07800856f, -0.3297195f, -0.28799245f, -0.48327708f,
                1.0389975f, -0.48272502f, 1.5461727f, 0.346708f, 1.1923765f, 0.9172932f, 0.118387796f, -2.5330329f,
                -1.0690134f, -0.066308804f, -0.36472896f, -0.65508735f, -1.3324463f, -0.59017396f, -0.8526603f, -0.15497302f,
                1.2475492f, 0.8104485f, -0.8857118f, 0.57788455f, -3.1196346f, -1.0892111f, 4.8694053f, 0.84693027f,
                -0.57184494f, -0.3023462f, -0.6100776f, -0.19041677f, 0.014727005f, 1.878172f, 0.44666377f, -2.3690662f,
                0.38614675f, -0.63041186f, -0.50797665f, 0.2854266f, 4.0994234f, -0.45003006f, -0.22109914f, -0.9495697f,
                0.2271766f, 0.3602022f, -0.67559636f, -1.3346792f, -0.645316f, -0.27103692f, -0.98542905f, -3.1642318f,
                -0.16425563f, -1.947996f, -2.496127f, -0.21207838f, -0.35469946f, 1.1275872f, -0.841618f, 1.7097485f
        };

        embedBuffer.position(0); // Reset position to start
        embedBuffer.put(embedValues); // Fill buffer with values
        embedBuffer.position(0); // Reset again if needed before inference

        super.start();
    }

    public float[] processAudio(float[] audioData) throws OrtException{

        // check input length
        if (audioData.length != HOP_SIZE) {
            throw new IllegalArgumentException("Audio data must have exactly " + HOP_SIZE + " samples.");
        }

        // Copy old frame to the front
        float[] xArray = xBuffer.array();
        System.arraycopy(xArray, HOP_SIZE, xArray, 0, HOP_SIZE);

        // Copy new frame to the back
        System.arraycopy(audioData, 0, xArray, HOP_SIZE, HOP_SIZE);
        xBuffer.position(0);

        // also reset embed buffer position
        embedBuffer.position(0);

        // Initialize Tensors
        OnnxTensor x = OnnxTensor.createTensor(env, xBuffer, xDimensions);
        OnnxTensor embed = OnnxTensor.createTensor(env, embedBuffer, embedDimensions);
        OnnxTensor hiddenState = OnnxTensor.createTensor(env, hiddenStateBuffer, hiddenStateDimensions);
        OnnxTensor hiddenStates1 = OnnxTensor.createTensor(env, hiddenStates1Buffer, hiddenStates1Dimensions);

        // Update inputs map
        Map<String, OnnxTensor> inputs = Map.of("x", x, "embed", embed,
                "hidden_state", hiddenState, "hidden_states_1", hiddenStates1);

        // Run actual processing
        Result results = session.run(inputs);

        // get the probabilities from the results
        float[][] outputArray = (float[][]) results.get(0).getValue();
        float[] probabilities = outputArray[0];

        // Update hiddenStateBuffer
        float[][] hiddenStateArrayNew = (float[][]) results.get(1).getValue();
        float[] hiddenStateArray = hiddenStateBuffer.array();
        System.arraycopy(hiddenStateArrayNew[0], 0, hiddenStateArray, 0, HIDDEN_SIZE);
        hiddenStateBuffer.position(0);

        // Update hiddenStates1Buffer
        float[][] hiddenStates1ArrayNew = (float[][]) results.get(2).getValue();
        float[] hiddenStates1Array = hiddenStates1Buffer.array();
        System.arraycopy(hiddenStates1ArrayNew[0], 0, hiddenStates1Array, 0, HIDDEN_SIZE);
        hiddenStates1Buffer.position(0);

        return probabilities;
    }

    @Override
    protected void process(float[][] buffer) {

        // Normalise to [-1, 1], apparently this isn't done in Silero...
        /*for (int channel = 0; channel < buffer.length; channel++) {
            float max = 0f;
            for (float sample : buffer[channel]) {
                if (Math.abs(sample) > max) {
                    max = Math.abs(sample);
                }
            }
            if (max > 0) {
                for (int sample = 0; sample < buffer[channel].length; sample++) {
                    buffer[channel][sample] /= max;
                }
            }
        }*/

        float[][] dataOut = new float[buffer.length][4];
        for (int channel = 0; channel < buffer.length; channel++) {
            try {
                dataOut[channel] = processAudio(buffer[channel]);
                System.out.printf("%s\n", java.util.Arrays.toString(dataOut[channel]));
            } catch (OrtException e) {
                throw new RuntimeException(e);
            }
        }

        send(dataOut);
    }

    @Override
    protected void cleanup() {
        try {
            session.close();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
        env.close();
    }

    private File copyAssetToFile(Context context, String assetName) throws IOException {
        File outFile = new File(context.getFilesDir(), assetName);
        if (!outFile.exists()) { // Only copy once
            try (InputStream is = context.getAssets().open(assetName);
                 OutputStream os = new FileOutputStream(outFile)) {

                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
        }
        return outFile;
    }

}
