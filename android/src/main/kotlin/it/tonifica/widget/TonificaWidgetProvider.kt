package it.tonifica.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject

/** Widget Tonifica: settimana X/12, prossima seduta (scheda A/B/C/D o casa), fatte/saltate, passi. */
class TonificaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, costruisci(context))
    }

    companion object {
        const val PREFS = "tonifica_widget"
        const val CHIAVE = "dati"

        fun aggiornaTutti(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TonificaWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val v = costruisci(context)
            for (id in ids) manager.updateAppWidget(id, v)
        }

        private fun sfondoLettera(s: String): Int = when (s) {
            "A" -> R.drawable.tonifica_lettera_a
            "B" -> R.drawable.tonifica_lettera_b
            "C" -> R.drawable.tonifica_lettera_c
            "D" -> R.drawable.tonifica_lettera_d
            "casa" -> R.drawable.tonifica_lettera_casa
            else -> R.drawable.tonifica_lettera_vuota
        }

        fun costruisci(context: Context): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.tonifica_widget)
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                v.setOnClickPendingIntent(
                    R.id.t_radice,
                    PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
            }
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHIAVE, null) ?: return v
            try {
                val d = JSONObject(raw)
                v.setTextViewText(R.id.t_settimana, "Settimana ${d.optInt("settimana", 1)}/12")
                v.setTextViewText(R.id.t_blocco, d.optString("blocco", ""))
                v.setProgressBar(R.id.t_barra, 100, d.optInt("percentuale", 0).coerceIn(2, 100), false)
                v.setTextViewText(R.id.t_statistiche, "✓ ${d.optInt("fatte", 0)} fatte · ✕ ${d.optInt("saltate", 0)} saltate")
                val passi = d.optInt("passi", -1)
                v.setTextViewText(R.id.t_passi, if (passi >= 0) "👟 ${String.format("%,d", passi).replace(',', '.')}" else "")

                val p = d.optJSONObject("prossima")
                if (p == null) {
                    v.setTextViewText(R.id.t_lettera, "✓")
                    v.setInt(R.id.t_lettera, "setBackgroundResource", sfondoLettera("vuota"))
                    v.setTextViewText(R.id.t_etichetta, "PROGRAMMA COMPLETATO")
                    v.setTextViewText(R.id.t_titolo, "🎉 12 settimane completate!")
                    v.setTextViewText(R.id.t_quando, "")
                    v.setViewVisibility(R.id.t_nota, View.GONE)
                } else {
                    val lettera = p.optString("lettera", "A")
                    v.setTextViewText(R.id.t_lettera, if (lettera == "casa") "⌂" else lettera)
                    v.setInt(R.id.t_lettera, "setBackgroundResource", sfondoLettera(lettera))
                    v.setTextViewText(R.id.t_etichetta, p.optString("etichetta", "PROSSIMA"))
                    v.setTextViewText(R.id.t_titolo, p.optString("titolo", ""))
                    v.setTextViewText(R.id.t_quando, p.optString("quando", ""))
                    val nota = p.optString("nota", "")
                    if (nota.isNotEmpty()) {
                        v.setTextViewText(R.id.t_nota, "↻ $nota")
                        v.setViewVisibility(R.id.t_nota, View.VISIBLE)
                    } else {
                        v.setViewVisibility(R.id.t_nota, View.GONE)
                    }
                }
            } catch (_: Exception) {
            }
            return v
        }
    }
}
