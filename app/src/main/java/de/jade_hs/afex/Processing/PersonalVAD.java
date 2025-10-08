package de.jade_hs.afex.Processing;

import android.content.Context;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Map;

import java.util.HashMap;

public class PersonalVAD {

    // Parameters:
    public static final int HOP_SIZE = 256; // this is our actual processing blocksize
    private static final int FFT_SIZE = 512;

    // private static final int HIDDEN_SIZE = 32; // depends on selected model
    // private static final String MODEL_NAME = "GRU_with_FiLM_advanced_v0";
    private static final int HIDDEN_SIZE = 512; // depends on selected model
    private static final String MODEL_NAME = "GRU_with_FiLM_advanced_v24";

    // onnx runtime fields
    private final OrtEnvironment env;
    private OrtSession session;

    // Buffer fields for processing
    private final FloatBuffer xBuffer;
    private final FloatBuffer embedBuffer;
    private final FloatBuffer hiddenStateBuffer;
    private final FloatBuffer hiddenStates1Buffer;

    private final long[] xDimensions = {FFT_SIZE};
    private final long[] embedDimensions = {HOP_SIZE};
    private final long[] hiddenStateDimensions = {1, HIDDEN_SIZE};
    private final long[] hiddenStates1Dimensions = {1, HIDDEN_SIZE};

    public PersonalVAD(Context context) {

        // get onnx model from assets
        File modelFile;
        try {
            modelFile = Utilities.copyAssetToFile(context, "GRU_with_FiLM_advanced_v24.onnx");
            Utilities.copyAssetToFile(context, "GRU_with_FiLM_advanced_v24.onnx.data");
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
        /*float[] embedValues = new float[] {
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
        };*/

        // cache_20250904_111517395_jb_mono.wav
        float[] embedValues = new float[] {
                -1.46362305e-01f,  8.01269531e-01f, -6.29394531e-01f,  6.38671875e-01f,
                1.61743164e-01f, -3.94531250e-01f,  2.58789062e+00f, -3.20068359e-01f,
                -6.00585938e-01f, -7.25097656e-01f,  5.24609375e+00f, -3.03125000e+00f,
                -4.16748047e-01f,  4.06738281e-01f,  3.18115234e-01f, -1.98046875e+00f,
                -1.21215820e-01f,  8.57421875e-01f, -3.78662109e-01f,  3.69140625e+00f,
                3.94921875e+00f,  1.88110352e-01f, -3.50976562e+00f, -4.00146484e-01f,
                3.52783203e-01f, -4.63134766e-01f,  1.20971680e-01f,  2.17578125e+00f,
                -6.23046875e-01f,  1.11621094e+00f, -7.80273438e-01f,  3.59375000e-01f,
                -8.50585938e-01f,  3.07617188e+00f,  1.51074219e+00f, -2.52441406e-01f,
                1.86035156e+00f,  7.18261719e-01f, -1.61254883e-01f, -7.00683594e-01f,
                1.28222656e+00f, -2.44921875e+00f, -2.08984375e+00f, -4.66308594e-01f,
                6.10839844e-01f, -1.45898438e+00f,  1.40429688e+00f,  2.31445312e-01f,
                3.54492188e-01f, -8.39843750e-01f,  4.19677734e-01f, -4.89257812e-01f,
                6.42578125e-01f, -2.22534180e-01f, -5.65917969e-01f,  1.70898438e+00f,
                -2.96875000e-01f, -8.35449219e-01f, -1.60839844e+00f,  8.60351562e-01f,
                -7.38281250e-01f, -3.24707031e-01f,  2.65869141e-01f,  1.19433594e+00f,
                7.26562500e+00f, -3.04882812e+00f,  8.86230469e-01f,  8.69140625e-01f,
                8.55468750e-01f,  1.42382812e+00f,  1.54980469e+00f, -2.05566406e-01f,
                1.14355469e+00f,  1.41845703e-01f,  1.58886719e+00f,  6.98828125e+00f,
                5.62988281e-01f, -3.67919922e-01f, -1.91040039e-01f,  5.35964966e-03f,
                8.10546875e-01f,  1.29062500e+01f, -1.49609375e+00f, -1.82714844e+00f,
                -5.54687500e-01f, -3.39843750e-01f,  1.56021118e-02f,  8.68164062e-01f,
                -1.21154785e-01f,  1.46850586e-01f,  1.22436523e-01f,  3.67126465e-02f,
                -6.18164062e-01f, -6.03027344e-01f, -4.36279297e-01f,  7.92480469e-01f,
                4.80468750e+00f, -1.43457031e+00f,  1.31250000e+00f, -2.44140625e+00f,
                3.40820312e+00f,  2.71875000e+00f,  3.80615234e-01f, -2.65380859e-01f,
                4.56787109e-01f, -1.29394531e+00f,  1.70800781e+00f, -7.83691406e-01f,
                -1.99316406e+00f,  1.67968750e-01f, -8.07189941e-03f,  1.64160156e+00f,
                3.96484375e-01f, -6.08398438e-01f,  7.97271729e-03f,  1.82812500e+00f,
                -2.39062500e+00f,  1.04199219e+00f, -3.30664062e+00f,  7.90039062e-01f,
                -1.51977539e-01f,  6.82128906e-01f,  5.56640625e-01f, -8.97460938e-01f,
                -2.42968750e+00f,  4.37744141e-01f,  1.30468750e+00f, -8.57910156e-01f,
                -1.11230469e+00f,  1.04980469e+00f,  7.30957031e-01f, -1.93481445e-01f,
                -1.27246094e+00f,  2.72705078e-01f, -1.34472656e+00f,  8.53027344e-01f,
                -5.33691406e-01f,  1.34201050e-02f,  7.32910156e-01f, -1.70117188e+00f,
                -2.94335938e+00f,  3.07128906e-01f, -1.48046875e+00f,  1.24023438e+00f,
                -1.02050781e+00f, -3.55468750e+00f, -3.56835938e+00f,  7.39135742e-02f,
                2.02539062e+00f, -5.42480469e-01f, -2.40039062e+00f,  1.67089844e+00f,
                -2.63671875e+00f, -6.20117188e-01f, -7.30957031e-01f,  1.34960938e+00f,
                1.31640625e+00f, -9.70703125e-01f,  1.12988281e+00f,  4.99023438e-01f,
                6.50878906e-01f, -6.02050781e-01f,  9.55078125e-01f,  1.43676758e-01f,
                1.64355469e+00f,  1.50195312e+00f,  1.60351562e+00f,  2.83203125e-01f,
                7.87597656e-01f,  1.68945312e-01f,  8.22753906e-01f,  1.78613281e+00f,
                6.34277344e-01f, -1.05468750e+00f, -8.61328125e-01f,  2.51367188e+00f,
                7.80639648e-02f, -2.43041992e-01f, -1.66015625e+00f,  1.36328125e+00f,
                -2.06604004e-02f,  5.65917969e-01f,  1.07324219e+00f,  2.03320312e+00f,
                3.22265625e-01f,  1.77148438e+00f,  7.38281250e-01f, -2.03710938e+00f,
                -8.56933594e-02f,  1.33203125e+00f, -5.63671875e+00f,  7.96386719e-01f,
                -8.73046875e-01f, -1.94335938e+00f, -1.41113281e-01f,  4.12841797e-01f,
                -5.07324219e-01f, -9.22363281e-01f,  7.03735352e-02f, -3.88671875e-01f,
                -2.81738281e-01f, -6.27441406e-01f, -6.76269531e-01f,  2.20336914e-01f,
                -2.08129883e-02f,  8.98925781e-01f, -2.28271484e-01f, -1.37817383e-01f,
                3.72070312e-01f, -2.16601562e+00f,  1.34375000e+00f, -1.21875000e+00f,
                -1.34570312e+00f, -7.17163086e-02f,  4.80712891e-01f,  3.21044922e-01f,
                1.35351562e+00f,  3.84521484e-01f, -6.10351562e-01f, -1.56347656e+00f,
                1.35058594e+00f, -1.21887207e-01f, -1.82812500e+00f,  4.52804565e-03f,
                -4.82666016e-01f,  1.10058594e+00f, -1.07299805e-01f, -7.39746094e-01f,
                3.17382812e-01f,  1.60644531e+00f,  2.72460938e+00f, -3.62548828e-01f,
                -5.52343750e+00f,  2.34179688e+00f,  5.98754883e-02f,  4.75585938e-01f,
                -2.09179688e+00f,  1.79394531e+00f, -6.96289062e-01f, -1.16503906e+00f,
                1.06738281e+00f, -8.73535156e-01f, -2.57324219e-01f, -7.16308594e-01f,
                1.62695312e+00f, -1.80468750e+00f,  1.15905762e-01f,  5.76171875e+00f,
                -1.42871094e+00f,  1.90039062e+00f,  2.26928711e-01f,  7.00683594e-01f,
                1.48535156e+00f,  3.12744141e-01f,  6.09436035e-02f,  2.54101562e+00f
        };

        embedBuffer.position(0); // Reset position to start
        embedBuffer.put(embedValues); // Fill buffer with values
        embedBuffer.position(0); // Reset again if needed before inference
    }

    public float[] processAudio(float[] audioData) {

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

        try {

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

        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

     public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
        env.close();
    }

}

