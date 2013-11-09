EnableBatteryStatsPermission
============================

Enable BATTERY_STATS Permission On KitKat - XPosed Module

/**
 * Provide an XPosed framework alternate implementation of the BatteryStatsService
 * getStatistics() method (accessed via IBatteryStats.getStatistics()) that does
 * not check for the BATTERY_STATS permission.  
 * 
 * The purpose of this is for use in KitKat.  Since Google has decided to change 
 * the BATTERY_STATS permission to be signature|system 
 * (https://code.google.com/p/android/issues/detail?id=61975), thereby making it
 * unavailable to all, we need a mechanism to still get at the stats.  This of course
 * will require ROOT and the XPosed framework 
 * (http://forum.xda-developers.com/showthread.php?t=1574401)
 */
