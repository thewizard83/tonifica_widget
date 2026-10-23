# tonifica_widget

Plugin Flutter (solo Android) per l'app **Tonifica**:

- **Widget della schermata home**: settimana X/12 del programma, blocco, sedute fatte e saltate, passi di oggi, prossima seduta (scheda A/B/C/D o sessione a casa, giorno, ora, luogo, nota di ripianificazione). Toccandolo si apre l'app.
- **Notifiche locali** (canale "Allenamento").
- **Passi**: lettura del contatore passi del telefono (permesso "attività fisica").

Non contiene dati personali: riceve un JSON dall'app.

```dart
await TonificaWidget.aggiorna({...});
await TonificaWidget.notifica('Titolo', 'Testo');
await TonificaWidget.richiediPermessoNotifiche();
await TonificaWidget.richiediPermessoPassi();
final totali = await TonificaWidget.passiTotali(); // -1 se non disponibile
await TonificaWidget.aggiungiAllaHome();
```
