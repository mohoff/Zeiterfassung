# Zeiterfassung

Zeiterfassung is an Android app which allows users to track their location in the background without GPS. After specifying Zones for which you want to capture enter- and leave-events, it will store these events and display them to the user as long as the background service is running. My usecase was to track my working times automatically and that without the use of GPS which drains too much battery.

Although non-GPS-based location mechanisms can be very inaccurate, the app tries to maintain the most precise current position of the user.

Needs more testing as non-GPS location data might behave different in other regions.
