# Miku Companion (Phase 1) – Minecraft 26.1.2, Fabric, rein clientseitig

Eine lokal gerenderte 3D-Begleiterin, **von Miku Nakano inspiriert** (braun-orange lange Haare, blaue Augen,
dunkelblaue Kopfhörer um den Hals, helle Bluse, dunkler Rock). Sie folgt dir, wartet auf Wunsch und kehrt
kontrolliert zurück. Andere Spieler sehen sie nicht. Es ist **keine** echte Server-Entity.

> **Ehrlicher Status:** Dieses Projekt wurde in einer Umgebung ohne Netzwerk und ohne Java-Compiler (nur Java-Laufzeit)
> geschrieben. Es wurde **nicht kompiliert, nicht gestartet und nicht getestet**. Die Rendering-, Tasten- und Build-Teile
> folgen den offiziellen Fabric-Dokumentationsseiten für 26.1.2. Beim ersten Build können trotzdem Compilerfehler auftreten.
> Schick mir dann das Build-Log, ich korrigiere es.

## Versionen (vor dem Bau bitte prüfen)

| Komponente | Version | Quelle |
|---|---|---|
| Minecraft | 26.1.2 | Fabric-Dokumentation 26.1.2 |
| Java | 25 | Fabric-Porting-Guide 26.1 |
| Fabric Loom | 1.15.5 (`net.fabricmc.fabric-loom`) | Fabric-Porting-Guide 26.1 und Projekte für 26.1.2 |
| Fabric Loader | 0.19.2 (Minimum laut Mod: 0.19.1) | Fabric-Doku, andere 26.1.2-Mods |
| Fabric API | 0.154.0+26.1.2 | Modrinth-Liste (neuere Builds existieren evtl.) |

Wenn eine Version nicht gefunden wird: auf <https://fabricmc.net/develop/> die empfohlenen Versionen nachsehen und in `gradle.properties` eintragen.

## Bauen ohne eigene Java-Installation (GitHub Actions, kostenlos)

1. Kostenloses Konto auf <https://github.com> anlegen und ein **neues, leeres Repository** erstellen (z. B. `miku-companion`).
2. Das ZIP entpacken. Im Repository **„Add file“ → „Upload files“** wählen und den **Inhalt** des Ordners hineinziehen
   (inklusive des versteckten Ordners `.github`). Dann **„Commit changes“**.
3. Oben auf **„Actions“** klicken. Der Workflow „Miku Companion bauen“ startet automatisch (sonst: Workflow auswählen → „Run workflow“).
4. Nach einigen Minuten den fertigen Lauf öffnen. Unten unter **„Artifacts“** liegt `miku-companion-jar`. Herunterladen und entpacken.
5. Schlägt der Lauf fehl (roter Haken): den Lauf öffnen, den fehlgeschlagenen Schritt aufklappen und den Text an mich schicken.

## Installation

Du brauchst zwei Dateien im Mods-Ordner: die **Miku-JAR** und die **Fabric API** für 26.1.2 (von Modrinth oder CurseForge).

**Lunar Client:** Laut einer Anleitung, die ich gefunden habe, unterstützt Lunar selbst hinzugefügte Fabric-Mods ab 1.16.5. Ein Modpack mit
26.1.2 existiert ebenfalls. Lege in Lunar ein Profil mit Minecraft 26.1.2 und **Fabric** an. Öffne im Profil den Reiter „Mods“ und dort den
Mods-Ordner des Profils, falls vorhanden. Kopiere beide JARs hinein. Ich habe die genauen Menüs nicht selbst geprüft.
**Ob diese Mod in Lunar läuft, ist ungetestet.**

**Alternative (normaler Fabric-Client):** Offiziellen Minecraft Launcher + Fabric-Installer (<https://fabricmc.net>) für 26.1.2 installieren.
JARs nach `%appdata%\.minecraft\mods` kopieren, das Fabric-Profil starten. Java 25 muss je nach Launcher separat vorhanden sein,
moderne Launcher bringen oft eine passende Laufzeit mit.

## Bedienung (alle Tasten änderbar: Optionen → Steuerung → „Miku Companion“)

| Taste | Aktion |
|---|---|
| K | Miku herbeirufen (oder zu dir holen, wenn sie weit weg ist) |
| G | Warten / Folgen umschalten |
| H | Miku wegschicken |

Beim ersten Start erscheint einmalig eine Chat-Meldung mit diesen Tasten. Miku erscheint nur an einem erkannten sicheren Platz neben dir
(sonst sagt sie, dass es ihr zu gefährlich ist). Einstellungen: `config/mikucompanion.properties`.

## Entwicklungsstatus

| Funktion | Status | Getestet? | Bekannte Einschränkungen |
|---|---|---|---|
| Sichtbare 3D-Figur in der Welt | Implementiert | Nein | Aus Quadern, ohne Texturen; ohne Licht-/Schatten-Abhängigkeit (leuchtet nachts etwas zu hell) |
| Miku-inspiriertes Aussehen | Teilweise implementiert | Nein | Vereinfachte Würfelform; kein detailliertes Anime-Modell, keine Original-Assets |
| Folgen, Warten, Herbeirufen, Wegschicken | Implementiert | Nein | Halbblöcke/Treppen werden wie ganze Blöcke behandelt |
| Kontrollierte Rückkehr / Notfall-Teleport | Implementiert | Nein | Geschlossene Türen kann sie nicht öffnen (wartet auf dich, dann Teleport) |
| Konfigurierbare Tasten | Implementiert | Nein | Konflikte mit Lunar-Tasten ungeprüft |
| Weltwechsel, Respawn, Verbindungsabbruch | Implementiert | Nein | Nach Verbindungsabbruch muss sie neu gerufen werden |
| Laufen/Stehen/Kopfdrehung/Blinzeln/Erröten | Teilweise implementiert | Nein | Einfache Animation, kein Sitzen, kein Sprechen |
| Dandere-Persönlichkeit, Dialoge, Vertrauen 0–100 | Geplant | Nein | Phase 2; aktuell nur wenige schüchterne Chat-Zeilen |
| Zustandsmaschine (Idle/Follow/Wait/Return/Recovery) | Teilweise implementiert | Nein | Talk/Happy/Shy/Curious folgen in Phase 2 |
| HUD, Sounds, Speicherung des Vertrauens | Geplant | Nein | Phase 3 |
| Sichtbar für andere Spieler / echte Server-Entity | Nicht möglich in der aktuellen Architektur | – | Rein clientseitig gewollt |

## Bekannte Risiken beim ersten Build

- Die Klasse `KeyMappingHelper` liegt vermutlich in `net.fabricmc.fabric.api.client.keymapping.v1`. Die Doku nennt den Klassennamen, aber nicht das Paket.
- `ClientTickEvents` im Paket `...client.event.lifecycle.v1` entspricht dem bisherigen Stand und ist für 26.1.2 nicht eigens bestätigt.
- Die Methode `getTranslatedKeyMessage()` (Tastenname im Chat) ist nicht in der Doku bestätigt.
- Die Gradle-Version wird im Workflow als „current“ geholt. Loom 1.15 braucht laut Quellen Gradle 9.2 oder neuer.
- Einen Gradle-Wrapper (`gradlew`) konnte ich ohne Netzwerk nicht erzeugen. Der Workflow benötigt ihn nicht.

## Datenschutz und Dateien

Keine Netzwerkzugriffe, keine Konto- oder Zugangsdaten, keine Telemetrie. Es wird nur `config/mikucompanion.properties` angelegt.
Alle Inhalte sind selbst erstellt (siehe `ASSETS.md`). Lizenz: MIT (`LICENSE`).
