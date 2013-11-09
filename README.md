EnableBatteryStatsPermission
============================

Enable BATTERY_STATS Permission On KitKat - Xposed Module

This Xposed mod will enable any Battery Monitor application which consumes Android 
battery statistics to work when running KitKat (or later).  The app developer may 
chose to integrate this function directly into their in which case this standalone
module is not needed.

* How do I install this?  HELP!

This mod requires that your device is rooted, and the Xposed framework is installed,
and that this module is enabled via the Xposed installer app.  After a phone reboot,
your battery monitoring apps should start to work as they did before KitKat rolled
around.

1. You must have ROOT on your phone.  If you do not, this is not for you.
2. Install this package
3. Download and install the Xposed installer if you do not yet have it: http://goo.gl/CKTWXZ
4. Install the “Framework" in the "Xposed Installer" and activate "Enable Battery Stats Permission" in "Modules".
5. Reboot
6. Run your battery monitor of choice.

This app does not 'launch', so don't be surprised to not see any way to start this app.


* Why is this needed? / Do I need this? 

Are you running KitKat or later?  Is your battery monitor of choice working?  If so,
you do not need this.  If not, it likely will do the trick - if it does not, contact
the battery monitor developer and ask that they read the 'Developers' section of this
app.

Google decided to change the BATTERY_STATS permission to be signature|system, which
on KitKat means an app cannot access battery statistics.  Functions such as 
identifying which apps are consuming more resources than others will not function.
https://code.google.com/p/android/issues/detail?id=61975

* What does this do?

This uses the Xposed framework to provide an alternate implementation of the
com.android.server.am.BatteryStatsService.getStatistics() method which will not
enforce that the calling application have the BATTERY_STATS permission.

* Is this safe?

Yes - the code is open source, and very simple.  Do keep in mind however that the 
Xposed framework allows you to enable modules that may be very dangerous - this 
module is not, but the  
https://github.com/GSamLabs/EnableBatteryStatsPermission

* I'm a developer, but my app still doesn't work in KitKat!

Google did two things in KitKat.  The first is that they changed the name of the
battery stats service from 'batteryinfo' to 'batterystats'.  The second is that 
they changed the BATTERY_STATS permission to be signature|system.  This mod will
fix the second, but not the first.  To fix the first, you should do something of
the form:
			String batteryServiceName = "batterystats";
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            {
                batteryServiceName = "batteryinfo";
            }
Then call your getService method to bind to the appropriate service.

* I'm a developer, can I pull this code directy into my app?

Yes - by all means.  Follow the great tutorial that the Xposed author has, and feel
free to copy HookGetStatisticsMethodCall class directly into your app.
https://github.com/rovo89/XposedBridge/wiki/Development-tutorial
