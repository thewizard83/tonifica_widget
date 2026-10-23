import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Ponte verso il widget Android della schermata home e le notifiche locali.
/// Su piattaforme diverse da Android tutte le chiamate sono no-op.
class TonificaWidget {
  static const MethodChannel _ch = MethodChannel('tonifica_widget');

  static bool get _android =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  /// Salva i dati mostrati dal widget e lo ridisegna.
  static Future<void> aggiorna(Map<String, dynamic> dati) async {
    if (!_android) return;
    try {
      await _ch.invokeMethod('aggiorna', {'json': jsonEncode(dati)});
    } catch (_) {}
  }

  /// Mostra una notifica locale (canale "Coach").
  static Future<void> notifica(String titolo, String testo, {int id = 1}) async {
    if (!_android) return;
    try {
      await _ch.invokeMethod('notifica', {'id': id, 'titolo': titolo, 'testo': testo});
    } catch (_) {}
  }

  /// Chiede il permesso per le notifiche (Android 13+). Restituisce true se concesso.
  static Future<bool> richiediPermessoNotifiche() async {
    if (!_android) return false;
    try {
      return await _ch.invokeMethod<bool>('richiediPermessoNotifiche') ?? false;
    } catch (_) {
      return false;
    }
  }

  /// True se le notifiche dell'app sono abilitate.
  static Future<bool> notificheAbilitate() async {
    if (!_android) return false;
    try {
      return await _ch.invokeMethod<bool>('notificheAbilitate') ?? false;
    } catch (_) {
      return false;
    }
  }

  /// Passi totali dal sensore contapassi (dall'ultimo riavvio), -1 se non disponibile.
  static Future<int> passiTotali() async {
    if (!_android) return -1;
    try {
      return await _ch.invokeMethod<int>('passiTotali') ?? -1;
    } catch (_) {
      return -1;
    }
  }

  /// Chiede il permesso "attività fisica" per leggere i passi (Android 10+).
  static Future<bool> richiediPermessoPassi() async {
    if (!_android) return false;
    try {
      return await _ch.invokeMethod<bool>('richiediPermessoPassi') ?? false;
    } catch (_) {
      return false;
    }
  }

  /// Chiede al launcher di aggiungere il widget alla schermata home (Android 8+).
  static Future<bool> aggiungiAllaHome() async {
    if (!_android) return false;
    try {
      return await _ch.invokeMethod<bool>('aggiungiAllaHome') ?? false;
    } catch (_) {
      return false;
    }
  }
}
