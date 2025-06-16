package dora.widget

import android.content.Context
import android.content.res.TypedArray
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

class DoraSplashView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val imageView: ImageView
    private val skipView: TextView
    private var images: IntArray = intArrayOf()
    private var switchInterval: Long = 3000L
    private var countdownTime: Long = 5000L
    private var skipTextTemplate: String = context.getString(R.string.format_dview_skip)

    private var timer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentIndex = 0
    private var switchRunnable: Runnable? = null

    // 回调
    var onCountdownFinish: (() -> Unit)? = null
    var onSkipClicked: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.dview_layout_splash_view, this, true)
        imageView = findViewById(R.id.ivSplash)
        skipView = findViewById(R.id.tvSkip)
        attrs?.let {
            val ta: TypedArray = context.obtainStyledAttributes(it, R.styleable.DoraSplashView)
            val arrResId = ta.getResourceId(R.styleable.DoraSplashView_ds_images, 0)
            if (arrResId != 0) {
                val taImg = resources.obtainTypedArray(arrResId)
                images = IntArray(taImg.length()) { idx -> taImg.getResourceId(idx, 0) }
                taImg.recycle()
            }
            switchInterval = ta.getInt(R.styleable.DoraSplashView_ds_switchInterval, switchInterval.toInt()).toLong()
            countdownTime = ta.getInt(R.styleable.DoraSplashView_ds_countdown, countdownTime.toInt()).toLong()
            skipView.setTextColor(ta.getColor(R.styleable.DoraSplashView_ds_skipTextColor, skipView.currentTextColor))
            skipView.setBackgroundColor(ta.getColor(R.styleable.DoraSplashView_ds_skipBgColor, 0x66000000))
            skipView.textSize = ta.getDimension(R.styleable.DoraSplashView_ds_skipTextSize, skipView.textSize)
            ta.getString(R.styleable.DoraSplashView_ds_skipTextTemplate)?.let {
                skipTextTemplate = it
            }
            ta.recycle()
        }

        skipView.setOnClickListener {
            timer?.cancel()
            stopSwitch()
            onSkipClicked?.invoke()
        }
    }

    fun start() {
        if (images.isEmpty()) return
        imageView.setImageDrawable(ContextCompat.getDrawable(context, images[0]))
        currentIndex = 0
        switchRunnable = object : Runnable {
            override fun run() {
                currentIndex = (currentIndex + 1) % images.size
                imageView.setImageDrawable(ContextCompat.getDrawable(context, images[currentIndex]))
                handler.postDelayed(this, switchInterval)
            }
        }
        handler.postDelayed(switchRunnable!!, switchInterval)

        // 倒计时
        timer = object : CountDownTimer(countdownTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                skipView.text = String.format(skipTextTemplate, secondsLeft)
            }
            override fun onFinish() {
                skipView.text = skipTextTemplate.replace("%d", "0")
                stopSwitch()
                onCountdownFinish?.invoke()
            }
        }.start()
    }

    private fun stopSwitch() {
        switchRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timer?.cancel()
        stopSwitch()
    }
}