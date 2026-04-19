package com.example.lab11

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var btnCalculate: Button
    private lateinit var btnLoadImages: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var imagesContainer: LinearLayout

    private lateinit var executor: ExecutorService
    private lateinit var mainHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCalculate = findViewById(R.id.btnCalculate)
        btnLoadImages = findViewById(R.id.btnLoadImages)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        imagesContainer = findViewById(R.id.imagesContainer)

        executor = Executors.newSingleThreadExecutor()
        mainHandler = Handler(Looper.getMainLooper())

        btnCalculate.setOnClickListener {
            startCalculation()
        }

        btnLoadImages.setOnClickListener {
            startImagesLoading()
        }
    }

    private fun startCalculation() {
        progressBar.visibility = ProgressBar.VISIBLE
        progressBar.progress = 0
        tvStatus.text = "Идут вычисления..."
        setButtonsEnabled(false)

        executor.execute {
            try {
                // Вариант 3: массив целых чисел
                val array = generateIntArrayWithZeros(100)

                // Первая половина прогресса
                for (i in array.indices) {
                    Thread.sleep(15)
                    val progress = ((i + 1) * 50.0 / array.size).roundToInt()
                    mainHandler.post {
                        progressBar.progress = progress
                    }
                }

                val productEvenIndexes = multiplyElementsAtEvenIndexes(array)

                // Вторая половина прогресса
                for (i in array.indices) {
                    Thread.sleep(15)
                    val progress = 50 + ((i + 1) * 50.0 / array.size).roundToInt()
                    mainHandler.post {
                        progressBar.progress = progress
                    }
                }

                val sumBetweenZeros = sumBetweenFirstAndLastZero(array)

                val firstZeroIndex = array.indexOfFirst { it == 0 }
                val lastZeroIndex = array.indexOfLast { it == 0 }

                val resultText = buildString {
                    appendLine("Сгенерированный массив:")
                    appendLine(array.joinToString(prefix = "[", postfix = "]"))
                    appendLine()
                    appendLine("Вариант 3")
                    appendLine("1) Произведение элементов с чётными индексами: $productEvenIndexes")

                    if (firstZeroIndex == -1 || lastZeroIndex == -1 || firstZeroIndex == lastZeroIndex) {
                        appendLine("2) Между первым и последним нулевыми элементами сумму посчитать нельзя,")
                        appendLine("   потому что в массиве недостаточно нулей.")
                    } else {
                        appendLine("2) Сумма элементов между первым и последним нулём: $sumBetweenZeros")
                        appendLine("   Первый ноль: индекс $firstZeroIndex")
                        appendLine("   Последний ноль: индекс $lastZeroIndex")
                    }
                }

                mainHandler.post {
                    tvStatus.text = resultText
                    progressBar.visibility = ProgressBar.GONE
                    setButtonsEnabled(true)
                    Toast.makeText(
                        this@MainActivity,
                        "Вычисления завершены",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    progressBar.visibility = ProgressBar.GONE
                    setButtonsEnabled(true)
                    tvStatus.text = "Ошибка при вычислениях: ${e.message}"
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка вычислений",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startImagesLoading() {
        progressBar.visibility = ProgressBar.VISIBLE
        progressBar.progress = 0
        tvStatus.text = "Идёт загрузка изображений..."
        imagesContainer.removeAllViews()
        setButtonsEnabled(false)

        val imageUrls = listOf(
            "https://picsum.photos/400/200?random=1",
            "https://picsum.photos/400/200?random=2",
            "https://picsum.photos/400/200?random=3",
            "https://picsum.photos/400/200?random=4",
            "https://picsum.photos/400/200?random=5"
        )

        executor.execute {
            try {
                val total = imageUrls.size

                for ((index, url) in imageUrls.withIndex()) {
                    val bitmap = loadImage(url)

                    mainHandler.post {
                        addImageToContainer(bitmap)
                    }

                    val progress = ((index + 1) * 100.0 / total).roundToInt()
                    mainHandler.post {
                        progressBar.progress = progress
                        tvStatus.text = "Загружено изображений: ${index + 1} из $total"
                    }
                }

                mainHandler.post {
                    progressBar.visibility = ProgressBar.GONE
                    setButtonsEnabled(true)
                    tvStatus.text = "Загрузка изображений завершена. Загружено: $total"
                    Toast.makeText(
                        this@MainActivity,
                        "Все изображения загружены",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    progressBar.visibility = ProgressBar.GONE
                    setButtonsEnabled(true)
                    tvStatus.text = "Ошибка загрузки изображений: ${e.message}"
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка загрузки",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun generateIntArrayWithZeros(size: Int): IntArray {
        val array = IntArray(size)

        for (i in array.indices) {
            array[i] = Random.nextInt(-10, 11)
        }

        // Специально гарантируем несколько нулей,
        // чтобы второе задание почти всегда корректно считалось
        array[10] = 0
        array[50] = 0
        array[90] = 0

        return array
    }

    // Вариант 3, пункт а:
    // произведение элементов массива с чётными индексами
    private fun multiplyElementsAtEvenIndexes(array: IntArray): Long {
        var product = 1L

        for (i in array.indices step 2) {
            product *= array[i].toLong()
        }

        return product
    }

    // Вариант 3, пункт б:
    // сумма элементов между первым и последним нулевыми элементами
    private fun sumBetweenFirstAndLastZero(array: IntArray): Int {
        val firstZeroIndex = array.indexOfFirst { it == 0 }
        val lastZeroIndex = array.indexOfLast { it == 0 }

        if (firstZeroIndex == -1 || lastZeroIndex == -1 || firstZeroIndex == lastZeroIndex) {
            return 0
        }

        var sum = 0
        for (i in firstZeroIndex + 1 until lastZeroIndex) {
            sum += array[i]
        }

        return sum
    }

    private fun loadImage(urlString: String): Bitmap {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.connect()

        val inputStream: InputStream = connection.inputStream
        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw Exception("Не удалось декодировать изображение")

        inputStream.close()
        connection.disconnect()

        return bitmap
    }

    private fun addImageToContainer(bitmap: Bitmap) {
        val imageView = ImageView(this)

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            600
        )
        params.bottomMargin = 24

        imageView.layoutParams = params
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        imagesContainer.addView(imageView)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnCalculate.isEnabled = enabled
        btnLoadImages.isEnabled = enabled
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}