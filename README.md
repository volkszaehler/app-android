# app-android
Volkszaehler frontend for Android 

VolkszählerApp (Android) Version 0.8.4

- sie kann die Channels abrufen und die letzten Werte anzeigen
- ein paar Details zu jedem Channel anzeigen
- charts zu einzelnen Channels anzeigen, incl. Zoom, auch mit „Touch and Move“ im Diagramm
- HTTPS und Basic Authentication wird unterstützt, ABER das Zertifikat wird nicht wirklich geprüft, es werden auch ungültige oder Man-in-the-Middle-Zertifikate akzeptiert
- Die URL zur Middleware lautet z.B. http://raspberrypi/middleware.php. D.h. die middelware.php befindet sich im Rootordner der Installation.
- Einstellungen können (z.B. vor einem Update) gesichert und (nach dem Update/Neuinstallation) wieder eingespielt werden

Das ganze soll selbsterklärend sein, falls nicht, kann hier noch eine Anleitung rein bzw. die App verbessert werden.

Benötigte Berechtigungen:
- android.permission.INTERNET für den Zugriff auf die Volkszähler-Installation
- android.permission.WRITE_EXTERNAL_STORAGE für das Schreiben (und Lesen) des Backups

Tips zum Clonen von Github in AndroidStudio (nur für Entwickler):
Wenn das Clonen bzw. das Öffnen des Projektes wegen eines ausgegrauten "Use default gradle wrapper (not configured for the current project)" nicht möglich ist, dann
- das Projekt nochmal normal als "Open an existing Android Studio project" öffnen und im "Gradle Sync" Dialog einfach OK klicken
- Wenn nötig unter Settings => Version Control das "Unregistered Root" auswählen und auf das grüne "+" klicken, fertig