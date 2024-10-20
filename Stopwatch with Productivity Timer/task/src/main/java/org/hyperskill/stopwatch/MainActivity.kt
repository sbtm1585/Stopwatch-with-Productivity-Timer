package org.hyperskill.stopwatch

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    var seconds: Int = 0
    var minutes: Long = 0L
    var secondsRemain: Int = 0
    var isAlive = false
    val handler = Handler(Looper.getMainLooper())
    lateinit var textView: TextView
    lateinit var startButton: Button
    lateinit var resetButton: Button
    lateinit var settingsButton: Button
    lateinit var progressBar: ProgressBar
    lateinit var colorList: List<Int>
    lateinit var colorArray: ArrayDeque<Int>
    lateinit var editText: EditText
    lateinit var notificationManager: NotificationManager
    lateinit var notificationBuilder: NotificationCompat.Builder
    lateinit var notificationRequestPermissionLauncher: ActivityResultLauncher<String>
    var timeLimit: Int = 0
    var counter = 0

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.resetButton)
        settingsButton = findViewById(R.id.settingsButton)
        textView = findViewById<TextView?>(R.id.textView).also { it.text = "00:00" }
        progressBar = findViewById<ProgressBar?>(R.id.progressBar).also { it.isVisible = false }
        val dialogBox = layoutInflater.inflate(R.layout.alert_dialog, null)
        editText = dialogBox.findViewById(R.id.upperLimitEditText)

        val intent = packageManager.getLaunchIntentForPackage("org.hyperskill.stopwatch")
        val pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder = NotificationCompat.Builder(this, "org.hyperskill")
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reminder")
            .setContentText("Take a break!")
            .setAutoCancel(true)
            .setContentIntent(pIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminder"
            val descriptionText = "Your reminder"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("org.hyperskill", name, importance).apply {
                description = descriptionText
            }

            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        notificationRequestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

        notificationPermission()

        colorList = listOf(
            Color.parseColor("#FF595E"),
            Color.parseColor("#FFCA3A"),
            Color.parseColor("#8AC926"),
            Color.parseColor("#1982C4"),
            Color.parseColor("#6A4C93"))

        colorArray = ArrayDeque<Int>().apply {
            colorList.forEach { add(it) }
        }

        startButton.setOnClickListener {
            if (!isAlive) {
                isAlive = true
                progressBar.isVisible = true
                settingsButton.isEnabled = false
                thread().join()
            }
        }

        resetButton.setOnClickListener {
            handler.removeCallbacks(updateTimer)
            progressBar.isVisible = false
            settingsButton.isEnabled = true
            textView.apply {
                text = "00:00"
                setTextColor(ColorStateList.valueOf(getColor(R.color.colorText)))
            }
            timeLimit = 0
            seconds = 0
            minutes = 0L
            isAlive = false
            counter = 0
        }

        settingsButton.setOnClickListener {
            notificationPermission()
            try {
                editText.text.clear()
                AlertDialog.Builder(this)
                    .setView(dialogBox)
                    .setPositiveButton("OK") { _, _ ->
                        timeLimit = editText.text.toString().toInt()
                        val parentView = dialogBox.parent as ViewGroup
                        parentView.removeAllViews()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun thread(): Thread {
        return thread {
            updateTimer.run()
        }
    }

    fun colorChange() {
        progressBar.indeterminateTintList = ColorStateList.valueOf(colorArray[0])
        colorArray.apply {
            addLast(first())
            removeFirst()
        }
    }

    private val updateTimer: Runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun run() {
            secondsRemain = seconds % 60
            minutes = (seconds / 60).toLong()
            handler.postDelayed(this, 1000)
            textView.text = String.format("%02d:%02d", minutes, secondsRemain)
            if (timeLimit < counter) {
                textView.setTextColor(ColorStateList.valueOf(Color.RED))
                if (timeLimit + 1 == counter) {
                    val notification = notificationBuilder.build()
                   // notification.flags = NotificationCompat.FLAG_INSISTENT or NotificationCompat.FLAG_ONLY_ALERT_ONCE
                    notificationManager.notify(393939, notification)
                }
            }
            seconds++
            colorChange()
            if (timeLimit > 0) {
                counter++
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun notificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> {

                    }

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS) ->
                AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("This app needs permission to send notifications.")
                    .setPositiveButton("OK") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null),
                        )
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(this, "Cannot open settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            else -> {
                notificationRequestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}