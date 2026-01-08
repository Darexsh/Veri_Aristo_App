* * *

<div align="center">

📱 Veri Aristo App
============================

**An Android app for tracking and managing contraceptive ring cycles**  
📅⏰📝🎨📊

![Projekt-Status](https://img.shields.io/badge/Status-Aktiv-brightgreen) ![Lizenz](https://img.shields.io/badge/Lizenz-NonCommercial%20MIT-blue) ![Version](https://img.shields.io/badge/Version-1.2.0-orange)

</div>


* * *

✨ Authors
---------

| Name | GitHub | Role | Contact | Contributions |
| --- | --- | --- | --- | --- |
| **[Darexsh by Daniel Sichler](https://github.com/Darexsh)** | [Link](https://github.com/Darexsh?tab=repositories) | Android App Development 📱🛠️, UI/UX Design 🎨 | 📧 [E-Mail](mailto:sichler.daniel@gmail.com) | Concept, Feature Implementation, Calendar & Reminder Logic, Notes Management, UI Design |

* * *

🚀 About the Project
==============

**Veri Aristo** is an Android application designed to help users track and manage contraceptive ring cycles. The app provides a calendar-based visualization of insertion, removal, and ring-free days, along with reminders, personal notes, and customizable settings.

* * *

✨ Features
----------

* 📅 **Cycle Tracking**: Visualize insertion, removal, ring-free, and active days with color-coded calendar highlights.
    
* 🔔 **Reminders**: Custom reminder lead times for insertion/removal, with automatic rescheduling.
    
* 📝 **Notes**: Notebook-style notes with autosave, character counter, and quick delete.
    
* 🎨 **Customization**: Set cycle length, insertion date/time, calendar range, language, and background image.
    
* 📊 **Cycle History**: Review past and upcoming cycles to track patterns and durations.

* ⏩ **Wear Ring Longer**: Extend the current cycle once and keep history intact.

* 🧩 **Home Screen Widgets**: Small widget shows days left; large widget shows days left plus next dates.

* 💾 **Backup / Restore**: One‑tap export and import of all settings and notes.

* 🛠️ **Debug View**: Long‑press settings title for detailed diagnostic information.
    

* * *

📸 Screenshots
--------------

<table>
  <tr>
    <td align="center"><b>Home Screen</b><br><img src="Screenshots/Home.png" width="200" height="450"></td>
    <td align="center"><b>Calendar</b><br><img src="Screenshots/Calendar.png" width="200" height="450"></td>
    <td align="center"><b>Notes</b><br><img src="Screenshots/Notes.png" width="200" height="450"></td>
    <td align="center"><b>Cycles</b><br><img src="Screenshots/Cycles.png" width="200" height="450"></td>
  </tr>
</table>

<table>
  <tr>
    <td align="center"><b>Settings</b><br><img src="Screenshots/Settings.png" width="200" height="450"></td>
    <td align="center"><b>Widgets</b><br><img src="Screenshots/Widgets.png" width="200" height="450"></td>
    <td align="center"><b>About</b><br><img src="Screenshots/About.png" width="200" height="450"></td>
  </tr>
</table>

* * *

📥 Installation
---------------

1. **Build from source**:
    
    * Clone or download the repository from GitHub:
        
        ```bash
        git clone https://github.com/Darexsh/Veri_Aristo_App.git
        ```
        
    * Open the project in **Android Studio**.
        
    * Sync Gradle and build the project.
        
    * Run the app on an Android device or emulator (Android 8+ recommended).
        
2. **Install via the provided APK**:
    
    * Download the APK from the repository (`veri_aristo_app.apk`).
        
    * 🔒 Enable installation from unknown sources if prompted (required on Android 8+).
        
    * 📂 Open the APK on your device and follow the installation steps.
    

* * *

📝 Usage
--------

1. **Setup Cycle**:
    
    * Go to **Settings**.
        
    * Select the insertion date, cycle length, and reminder lead times.
        
    * Optionally choose a background image.

    * Switch the app language (German/English).
        
2. **View Calendar**:
    
    * Open the **Calendar** tab to see color-coded days:
        
        * 🟦 Cyan: Ring insertion
            
        * 🟨 Yellow: Ring removal
            
        * 🔴 Red: Ring-free days
            
        * 🟩 Green: Active cycle days
            
3. **Get Notifications**:
    
    * Receive reminders for insertion and removal at your selected times.
        
4. **Use Widgets**:
    
    * Add the small or large widget to your home screen for quick status.
        
5. **Take Notes**:
    
    * Use the **Notes** tab to store private notes, automatically saved locally.
        
6. **Track History**:
    
    * Check the **Cycles** tab for past and upcoming cycles.

7. **Backup / Restore**:

    * Use **Settings → Advanced** to export or import all settings and notes.
        

* * *

🔑 Permissions
--------------

* 💾 **Storage / Media Access**: Required to select a custom background image.
    
* 🔔 **Notifications**: Required to receive cycle reminders.

* ⏰ **Exact Alarms**: Used to schedule precise reminder notifications.
    

* * *

⚙️ Technical Details
--------------------

* 📦 Built with **Java** and **Android MVVM** architecture.
    
* 🗓️ Calendar rendering and cycle logic are computed locally (no backend).
    
* 🛠️ Stores user settings and notes in **SharedPreferences**.
    
* 🔔 Notifications implemented via **BroadcastReceiver** and **NotificationManagerCompat**.
    
* 📊 State sharing between fragments is managed via **SharedViewModel** and **LiveData**.

* 🧩 Home‑screen widgets implemented via **AppWidgetProvider**.
    

* * *

📜 License
----------

This project is licensed under the **Non-Commercial MIT License** and was developed as an educational project. You are free to use, modify, and distribute the code for **non-commercial purposes only**, and must credit the author:

**Copyright (c) 2025 Daniel Sichler aka Darexsh**

Please include the following notice with any use or distribution:

> Developed by Daniel Sichler aka Darexsh. Licensed under the Non-Commercial MIT License. See `LICENSE` for details.

The full license is available in the [LICENSE](LICENSE) file.

* * *
<div align="center"> <sub>Created with ❤️ by Daniel Sichler</sub> </div>
