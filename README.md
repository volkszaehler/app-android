# Volkszaehler Frontend for Android 
App Language: english, deutsch  
(English description below)

Download: [https://github.com/volkszaehler/app-android/releases/latest](https://github.com/volkszaehler/app-android/releases/latest "Latest Release")

![ScreenShot1](http://wiki.volkszaehler.org/_media/software/frontends/vz_app/uebersicht.png "ScreenShots 1 of VolkszaehlerApp") 
![ScreenShot2](http://wiki.volkszaehler.org/_media/software/frontends/vz_app/details.png "ScreenShots 2 of VolkszaehlerApp") 
![ScreenShot3](http://wiki.volkszaehler.org/_media/software/frontends/vz_app/grafik.png "ScreenShots 3 of VolkszaehlerApp") 
![ScreenShot4](http://wiki.volkszaehler.org/_media/software/frontends/vz_app/einstellungen.png "ScreenShots 4 of VolkszaehlerApp") 

## VolkszählerApp für Android, Version 0.8.8
---

Funktionen:  
  
- Abrufen der Channelinformationen und Anzeige der letzten Werte
- Anzeigen von Details zu jedem Channel
- Anzeigen von Charts zu einzelnen Channels, incl. Zoom, auch mit „Touch and Move“ im Diagramm
- Anzeigen von Gruppen-Charts und Hinzufügen weiterer Graphen ist möglich ("Long Touch" im Chart)
- Unterstützung von Basic Authentication und HTTPS, **_ABER das Zertifikat wird nicht wirklich geprüft_**   , es werden auch ungültige oder Man-in-the-Middle-Zertifikate akzeptiert
- Einstellungen können (z.B. vor einem Update) gesichert und (nach dem Update/Neuinstallation) wieder eingespielt werden

Benötigte Android Berechtigungen:  
- _android.permission.INTERNET_ für den Zugriff auf die Volkszähler-Installation  
- _android.permission.WRITE_EXTERNAL_STORAGE_ für das Schreiben (und Lesen) des Backups

###Tips zum Clonen von Github in AndroidStudio (nur für Entwickler):###
---

Wenn das Clonen bzw. das Öffnen des Projektes wegen eines ausgegrauten "Use default gradle wrapper (not configured for the current project)" nicht möglich ist, dann  
- das Projekt nochmal normal als "Open an existing Android Studio project" öffnen und im "Gradle Sync" Dialog einfach OK klicken  
- Wenn nötig unter Settings => Version Control das "Unregistered Root" auswählen und auf das grüne "+" klicken, fertig
  
  
  
English:
## VolkszaehlerApp for Android, Version 0.8.8
---

Features:  
  
- Download of Channel Information and Display of recent Values
- Shows Details for all Channels
- Shows Charts for every Channel, supports Zoom, (Touch and Move)
- Shows Charts of Groups, it is possible to add further graphs (use "Long Touch" in chart screen)
- Supports Basic Authentication und HTTPS, **_BUT the certificate is not really checked_**, the app accepts also invalid or Man-in-the-Middle certificates
- Settings can be saved (e.g. before updating) and can be restored (after the Update/Re-installation)

Needed Android Permissions:  
- _android.permission.INTERNET_ for accessing the Volkszaehler installation  
- _android.permission.WRITE_EXTERNAL_STORAGE_ for writing (and reading) of settings backups

###Tips for Cloning from Github in AndroidStudio (only for developers):###
---

If the cloning resp. opening of the project fails due to a grayed "Use default gradle wrapper (not configured for the current project)", then  
- open the Project again as "Open an existing Android Studio project" and clock "OK" in the "Gradle Sync" Dialog  
- If necessary, choose the "Unregistered Root" in Settings => Version Control and click the green plus "+" 


###Version history:
---

###Version 0.8.8
####New  
 - It is now possible to have several graphs in one chart, use "Long Press" in chart view, Limitation: there is only one y-Axis
 - ChannelDetails show now the total value if an initial value is set for the channel
 - Number of Tuples can be set now (default is 1000), a higher number makes the Charts more detailled (as long as there are enough values in the database), but slows down the charts
   
####Fixed 
 - fixed display issues with Channels of type "water" and costs
 
####Changed
 - small optimizations

###Version 0.8.7
####Fixed  
 - ChannelInfo popup can't be closed when clicking twice or more
 
####Changed  
 - changed image URLs in README.md to match the new Wiki image URL pattern

###Version 0.8.6
####Fixed  
 - empty Channel Color causes crash  
 - Umlauts in host name causes crash

###Version 0.8.5
####Changed  
 - Extended Chart Details Dialog
 - Improved Chart, especially when using Groups 
 - Buttons instead of Text in Details
 - Completed README.md, removed history.txt
 - small improvmenets

####Fixed  
 - Consumption and Cost values in ChartDetails Dialog visible again, also fixed for "Gas" channel  
 - Unit correctly displayed now with Channels from Group  
 - Chart-Range keeps the same after orientation change
 - Details now can be scrolled


###Version 0.8.4
####Changed  
 - improved logging  

####Fixed
 - No Costs in Chart Details for type "gas"  
 - Mixed decimal format in Chart Details    
 - Now graph skipped when no data in time range, useful especially with empty channel in groups    


###Version 0.8.3
####New  
 - Backup/Restore of Settings

####Changed  
 - Details background not transparent anymore

####Fixed  
 - Calculation and display of costs in Details dialog


###Version 0.8.2
####Changed  
 - Date/Time selection dialog

####Fixed  
 - Cost for power meters no correct in "Channel Details"    
 - Some small fixes and optimizations  

###Version 0.8.1
####New
 - Group support  
 - Several lines in one chart (when using groups)  
 - Start and end date/time can be set to define a range to display in chart  
 - Automatic reload can be configured

####Changed    
 - Chart info now when clicking on the graph line

####Fixed  
 - Single touch in chart doesn't cause zoom anymore, 50px move is necessary to zoom  
 - "About" is now translated in German version  
 - From/to translated below chart  
 - Some small fixes


###Version 0.8.0
####New  
 - Initial BETA version
