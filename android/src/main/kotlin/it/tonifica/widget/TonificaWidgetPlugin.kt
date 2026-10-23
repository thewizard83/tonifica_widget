package it.tonifica.widget

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

class TonificaWidgetPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware,
    PluginRegistry.RequestPermissionsResultListener {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: Activity? = null
    private var binding: ActivityPluginBinding? = null
    private val pendenti = mutableMapOf<Int, MethodChannel.Result>()

    override fun onAttachedToEngine(b: FlutterPlugin.FlutterPluginBinding) {
        context = b.applicationContext
        channel = MethodChannel(b.binaryMessenger, "tonifica_widget")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(b: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "aggiorna" -> {
                context.getSharedPreferences(TonificaWidgetProvider.PREFS, Context.MODE_PRIVATE)
                    .edit().putString(TonificaWidgetProvider.CHIAVE, call.argument<String>("json") ?: "{}").apply()
                TonificaWidgetProvider.aggiornaTutti(context)
                result.success(true)
            }
            "notifica" -> {
                notifica(call.argument<Int>("id") ?: 1, call.argument<String>("titolo") ?: "Tonifica", call.argument<String>("testo") ?: "")
                result.success(true)
            }
            "notificheAbilitate" -> result.success(NotificationManagerCompat.from(context).areNotificationsEnabled())
            "richiediPermessoNotifiche" ->
                if (Build.VERSION.SDK_INT < 33) result.success(NotificationManagerCompat.from(context).areNotificationsEnabled())
                else chiedi(Manifest.permission.POST_NOTIFICATIONS, 4711, result)
            "richiediPermessoPassi" ->
                if (Build.VERSION.SDK_INT < 29) result.success(true)
                else chiedi(Manifest.permission.ACTIVITY_RECOGNITION, 4712, result)
            "passiTotali" -> leggiPassi(result)
            "aggiungiAllaHome" -> {
                val m = AppWidgetManager.getInstance(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && m.isRequestPinAppWidgetSupported) {
                    result.success(m.requestPinAppWidget(ComponentName(context, TonificaWidgetProvider::class.java), null, null))
                } else result.success(false)
            }
            else -> result.notImplemented()
        }
    }

    private fun chiedi(permesso: String, codice: Int, result: MethodChannel.Result) {
        if (ContextCompat.checkSelfPermission(context, permesso) == PackageManager.PERMISSION_GRANTED) {
            result.success(true); return
        }
        val a = activity ?: run { result.success(false); return }
        pendenti[codice] = result
        ActivityCompat.requestPermissions(a, arrayOf(permesso), codice)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        val r = pendenti.remove(requestCode) ?: return false
        r.success(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        return true
    }

    /** Legge il contatore passi cumulativo (TYPE_STEP_COUNTER). */
    private fun leggiPassi(result: MethodChannel.Result) {
        if (Build.VERSION.SDK_INT >= 29 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) { result.success(-1); return }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensore = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: run { result.success(-1); return }
        var risposto = false
        val handler = Handler(Looper.getMainLooper())
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (risposto) return
                risposto = true
                sm.unregisterListener(this)
                result.success(e.values[0].toInt())
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensore, SensorManager.SENSOR_DELAY_NORMAL)
        handler.postDelayed({
            if (!risposto) {
                risposto = true
                sm.unregisterListener(listener)
                result.success(-1)
            }
        }, 4000)
    }

    private fun notifica(id: Int, titolo: String, testo: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CANALE, context.getString(R.string.tonifica_canale), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val pi = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val n = NotificationCompat.Builder(context, CANALE)
            .setSmallIcon(R.drawable.tonifica_notifica)
            .setColor(0xFF6B5BE6.toInt())
            .setContentTitle(titolo)
            .setContentText(testo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(testo))
            .setAutoCancel(true)
            .apply { if (pi != null) setContentIntent(pi) }
            .build()
        try { NotificationManagerCompat.from(context).notify(id, n) } catch (_: SecurityException) {}
    }

    override fun onAttachedToActivity(b: ActivityPluginBinding) {
        activity = b.activity; binding = b; b.addRequestPermissionsResultListener(this)
    }
    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()
    override fun onReattachedToActivityForConfigChanges(b: ActivityPluginBinding) = onAttachedToActivity(b)
    override fun onDetachedFromActivity() {
        binding?.removeRequestPermissionsResultListener(this); binding = null; activity = null
    }

    companion object { private const val CANALE = "tonifica" }
}
