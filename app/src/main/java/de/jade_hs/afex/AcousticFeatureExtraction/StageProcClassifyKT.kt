package de.jade_hs.afex.AcousticFeatureExtraction

import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.core.BaseOptions

/**
 * Feature: Audio Classification using yamnet
 */

internal class StageProcClassifyKT(parameter: HashMap<*, *>) : Stage(parameter) {

    companion object {
        private const val LOG = "StageProcClassify"
    }

    private val audioClassifier: AudioClassifier
    private val audioData: AudioData

    init {

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("yamnet.tflite")
            .build()

        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.AUDIO_CLIPS)
            .setMaxResults(5)
            .build()

        audioClassifier = AudioClassifier.createFromOptions(context, options)

        // input length for yamnet is 0.96 * 16000 = 15360 samples = blockSize
        audioData = AudioData.create(
            AudioData.AudioDataFormat.builder()
                .setNumOfChannels(1)
                .setSampleRate(samplingrate.toFloat())
                .build(),
            blockSize
        )
    }

    override fun process(buffer: Array<FloatArray>) {
        for (i in buffer.indices) {
            audioData.load(buffer[i])
            val result = audioClassifier.classify(audioData)
            println("----------------> CLASSIFY: $result")
        }
    }
}
