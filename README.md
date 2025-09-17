* * *

🚀 Veri Aristo
==============

**Veri Aristo** is an Android application designed to help users track and manage contraceptive ring cycles. The app provides a calendar-based visualization of insertion, removal, and ring-free days, along with reminders, personal notes, and customizable settings.

* * *

✨ Features
----------

* 📅 **Cycle Tracking**: Visualize insertion, removal, ring-free, and active days with color-coded calendar highlights.
    
* 🔔 **Reminders**: Receive notifications for ring insertion and removal at your preferred time.
    
* 📝 **Notes**: Add and save personal notes securely using SharedPreferences.
    
* 🎨 **Customization**: Set cycle length, start date, reminder time, and choose a custom background image.
    
* 📊 **Cycle History**: Review past and upcoming cycles to track patterns and durations.
    

* * *

📸 Screenshots
--------------

| Home Screen | Calendar | Notes | Cycles | Settings |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

> _Screenshots are placeholders; replace with actual images before publishing._

* * *

📥 Installation
---------------

1. Clone or download the repository from GitHub:
    
    ```bash
    git clone https://github.com/yourusername/veri-aristo.git
    ```
    
2. Open the project in **Android Studio**.
    
3. Sync Gradle and build the project.
    
4. Run the app on an Android device or emulator (Android 8+ recommended).
    

* * *

📝 Usage
--------

1. **Setup Cycle**:
    
    * Go to **Settings**.
        
    * Select the start date, cycle length, and reminder time.
        
    * Optionally choose a background image.
        
2. **View Calendar**:
    
    * Open the **Calendar** tab to see color-coded days:
        
        * 🟦 Cyan: Ring insertion
            
        * 🟨 Yellow: Ring removal
            
        * 🔴 Red: Ring-free days
            
        * 🟩 Green: Active cycle days
            
3. **Get Notifications**:
    
    * Receive reminders for insertion and removal at your selected times.
        
4. **Take Notes**:
    
    * Use the **Notes** tab to store private notes, automatically saved locally.
        
5. **Track History**:
    
    * Check the **Cycles** tab for past and upcoming cycles.
        

* * *

🔑 Permissions
--------------

* 🌐 **Internet**: Optional, for future online features.
    
* 💾 **Storage / Media Access**: Required to select a custom background image.
    
* 🔔 **Notifications**: Required to receive cycle reminders.
    

* * *

⚙️ Technical Details
--------------------

* 📦 Built with **Java** and **Android MVVM** architecture.
    
* 🗓️ Uses **MaterialCalendarView** for the calendar interface.
    
* 🛠️ Stores user settings and notes in **SharedPreferences**.
    
* 🔔 Notifications implemented via **BroadcastReceiver** and **NotificationManagerCompat**.
    
* 📊 State sharing between fragments is managed via **SharedViewModel** and **LiveData**.
    

* * *

📝 License
----------

This project is provided as-is under the MIT License.

* * *

📞 Contact / Support
--------------------

For issues, feature requests, or contributions, please refer to the [GitHub repository](https://github.com/Darexsh/Veri_Aristo_App).

* * *
