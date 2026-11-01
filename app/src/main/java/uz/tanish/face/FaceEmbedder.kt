package uz.tanish.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MobileFaceNet orqali yuz izini (embedding) oladi.
 *
 * Model kirishi: [2, 112, 112, 3] float32, (piksel − 127.5) / 128, RGB.
 * Model chiqishi: [2, 192]. Batch'ning ikki o'rniga yuz va uning ko'zgudagi aksi beriladi,
 * natijalar qo'shilib L2 bo'yicha normallashtiriladi — bu izni barqarorroq qiladi.
 */
class FaceEmbedder(context: Context) : AutoCloseable {

    private val interpreter: Interpreter
    private val batch: Int
    private val inputSize: Int
    private val embeddingSize: Int
    private val inputBuffer: ByteBuffer

    init {
        val bytes = context.assets.open(RecognitionConfig.MODEL_FILE).use { it.readBytes() }
        val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
            put(bytes)
            rewind()
        }
        interpreter = Interpreter(model, Interpreter.Options().setNumThreads(4))

        val input = interpreter.getInputTensor(0)
        val output = interpreter.getOutputTensor(0)
        require(input.dataType() == DataType.FLOAT32) { "Model kirishi float32 emas: ${input.dataType()}" }
        val inShape = input.shape()
        require(inShape.size == 4 && inShape[3] == 3) { "Kutilmagan kirish shakli: ${inShape.contentToString()}" }
        batch = inShape[0]
        inputSize = inShape[1]
        embeddingSize = output.shape().last()
        inputBuffer = ByteBuffer.allocateDirect(batch * inputSize * inputSize * 3 * 4).order(ByteOrder.nativeOrder())
    }

    /** [face] — qirqilgan yuz (istalgan o'lchamda). Natija — L2 bo'yicha normallashtirilgan vektor. */
    @Synchronized
    fun embed(face: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(face, inputSize, inputSize, true)
        val flipped = if (batch >= 2) mirror(scaled) else null

        inputBuffer.rewind()
        for (b in 0 until batch) {
            val img = if (b % 2 == 1 && flipped != null) flipped else scaled
            putPixels(img)
        }
        inputBuffer.rewind()

        val output = Array(batch) { FloatArray(embeddingSize) }
        interpreter.run(inputBuffer, output)

        val sum = FloatArray(embeddingSize)
        val used = if (batch >= 2) 2 else 1
        for (b in 0 until used) {
            val n = EmbeddingCodec.l2Normalize(output[b])
            for (i in sum.indices) sum[i] += n[i]
        }
        return EmbeddingCodec.l2Normalize(sum)
    }

    private fun putPixels(img: Bitmap) {
        val pixels = IntArray(inputSize * inputSize)
        img.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        val mean = RecognitionConfig.IMAGE_MEAN
        val std = RecognitionConfig.IMAGE_STD
        for (p in pixels) {
            inputBuffer.putFloat(((p shr 16 and 0xFF) - mean) / std)
            inputBuffer.putFloat(((p shr 8 and 0xFF) - mean) / std)
            inputBuffer.putFloat(((p and 0xFF) - mean) / std)
        }
    }

    private fun mirror(src: Bitmap): Bitmap {
        val m = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    override fun close() = interpreter.close()
}
