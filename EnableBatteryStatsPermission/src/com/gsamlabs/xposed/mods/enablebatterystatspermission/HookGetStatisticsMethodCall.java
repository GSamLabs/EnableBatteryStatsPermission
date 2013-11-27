/*
 * Copyright (C) 2013 GSam Labs, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gsamlabs.xposed.mods.enablebatterystatspermission;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.lang.reflect.Field;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Provide an XPosed framework alternate implementation of the BatteryStatsService
 * getStatistics() method (accessed via IBatteryStats.getStatistics()) that does
 * not check for the BATTERY_STATS permission.  
 * 
 * In addition, this will enable legacy apps which still use the 'batteryinfo' 
 * rather than 'batterystats' service name to function as-is.
 * 
 * Other changes in KitKat such as how to access Network data (getTcpBytesReceived
 * no longer exists for example) are not supported here.  So if your battery monitor 
 * of choice still crashes - check the logcat and report it to the author.
 * 
 * The purpose of this is for use in KitKat.  Since Google has decided to change 
 * the BATTERY_STATS permission to be signature|system 
 * (https://code.google.com/p/android/issues/detail?id=61975), thereby making it
 * unavailable to all, we need a mechanism to still get at the stats.  This of course
 * will require ROOT and the XPosed framework 
 * (http://forum.xda-developers.com/showthread.php?t=1574401)
 */
public class HookGetStatisticsMethodCall implements IXposedHookLoadPackage {
    
    private static final String BATTERY_STATS_SERVICE_NAME = "com.android.server.am.BatteryStatsService";
    private static final String SERVICE_MANAGER_NAME = "android.os.ServiceManager";
    private static final boolean DEBUG = false;
    private static Field STATS_FIELD = null;
    // Get and modify the mStats field once - can do this statically on class load.
    static
    {
        try {
            STATS_FIELD = Class.forName(BATTERY_STATS_SERVICE_NAME).getDeclaredField("mStats");
            STATS_FIELD.setAccessible(true);
        } catch (Exception e) {
            XposedBridge.log("EnableBatteryStatsPermission failed to load a field - we won't be able to bypass any permissions.  This should NEVER happen.");
            XposedBridge.log(e);
        }
    }
    
    /**
     * Track whether we've logged anything claiming to have hooked the method (we want to log once just to register).
     */
    private boolean mIsLogged = false;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // If initialization failed, we just need to return immediately.
        if (STATS_FIELD == null)
        {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
        {
            return;
        }
        
        // Check the package - only bother hooking for the system package.
        // We'll leave this commented out for now.  On my phone it's the
        // com.google.android.backup package that claims to be running this
        // servicemanager - this makes no sense..my guess is the xposed framework
        // is just picking up the first package from the shared userid it 
        // finds - which could vary based on device.  So we'll just hook all.
        /*
        if (!lpparam.packageName.equals("com.google.android.backup"))
        {
            return;
        } */
        
        /**
         * We'll hook the getStatistics method to avoid the permissions check.  Since
         * KitKat has moved the BATTERY_STATS to system/signature,  
         */
        findAndHookMethod(BATTERY_STATS_SERVICE_NAME, lpparam.classLoader, "getStatistics", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!mIsLogged && DEBUG)
                {
                    XposedBridge.log("EnableBatteryStatsPermission hooking com.android.server.am.BatteryStatsService.getStatistics to remove BATTERY_STATS permission...");
                    mIsLogged = true;
                }
                if (DEBUG)
                {
                    XposedBridge.log("EnableBatteryStatsPermission hooking getStatistics ->");
                }
                Object battStatsService = param.thisObject;
                Parcel out = Parcel.obtain();
                Parcelable mStatsValue = (Parcelable) STATS_FIELD.get(battStatsService);  
                mStatsValue.writeToParcel(out, 0);    
                byte[] data = out.marshall();            
                out.recycle();
                param.setResult(data);
                if (DEBUG)
                {
                    XposedBridge.log("EnableBatteryStatsPermission hooking getStatistics <-");
                }
            }
        });
        
        /**
         * Also hook android.os.ServiceManager.getService and check to see whether they are
         * passing in the 'old' service name.  This allows us to fully support a legacy app
         * that hasn't been re-written for KitKat.
         */
        findAndHookMethod(SERVICE_MANAGER_NAME, lpparam.classLoader, "getService", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!mIsLogged && DEBUG)
                {
                    XposedBridge.log("EnableBatteryStatsPermission hooking android.os.ServiceManager.getService to check for legacy use of batterystats service...");
                    mIsLogged = true;
                }
                if (DEBUG)
                {
                    XposedBridge.log("EnableBatteryStatsPermission hooking getService ->");
                    XposedBridge.log("EnableBatteryStatsPermission args: "+((param.args.length > 0) ? param.args[0] : "null"));
                }
                // If they sent in 'batterystats' as the service, we'll modify it to 'batteryinfo'
                // and send it on down the line.
                if ((param.args.length > 0) && "batteryinfo".equals(param.args[0]))
                {
                    if (DEBUG)
                    {
                        XposedBridge.log("EnableBatteryStatsPermission getService requested batteryinfo - changing to batterystats");
                    }
                    param.args[0] = "batterystats";
                }     
                if (DEBUG)
                {
                    XposedBridge.log("EnableBatteryStatsPermission hooking getService <-");
                }
            }
        });     
    }

}
