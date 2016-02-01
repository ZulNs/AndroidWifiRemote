package id.zulns.androidwifiremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class WifiStateReceiver extends BroadcastReceiver {

    public WifiStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // My experience notes:
        // On the Android KitKat device NETWORK_STATE_CHANGED_ACTION only needed for catch both
        // connected or disconnected state event of the WiFi Network connectivity.
        // But on the Android GingerBread device, you will also need
        // SUPPLICANT_CONNECTION_CHANGE_ACTION to catch the disconnected state event
        // and NETWORK_STATE_CHANGED_ACTION to catch the connected state event.

        String action = intent.getAction();
        boolean ready = false;
        if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            if (!intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                ready = false;
            }
        }
        else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (netInfo.isConnected()) {
                ready = true;
            }
        }
        else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            if (state > 10) state -= 10;
            ready = (state == WifiManager.WIFI_STATE_ENABLED);
        }

        if (Main.getService() != null) {
            Main.getService().updateWifiState(ready);
        }

        if (Main.getActivity() != null) {
            Main.getActivity().updateWifiState(ready);
        }

        // throw new UnsupportedOperationException("Not yet implemented");
    }

    public static String getURL() {
        return "http://" + getIpAddress();
    }

    public static boolean isWifiConnectionAvailable() {
        return isWifiConnected() || isWifiApEnabled();
    }

    private static String getIpAddress() {
        String ipAddr = "";
        if (isWifiConnected()) {
            WifiManager wm = (WifiManager) Main.getContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm.getConnectionInfo();
            int ip = wi.getIpAddress();
            ipAddr = String.format("%d.%d.%d.%d", ip & 0xFF, ip >> 8 & 0xFF, ip >> 16 & 0xFF, ip >> 24 & 0xFF);
        }
        else if (isWifiApEnabled()) {
            ipAddr = Main.DEFAULT_AP_ADDRESS;
        }
        return ipAddr;
    }

    private static boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) Main.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(cm.TYPE_WIFI);
        return ni != null &&  ni.isConnected();
    }

    private static boolean isWifiApEnabled() {
        try {
            WifiManager wm = (WifiManager) Main.getContext().getSystemService(Context.WIFI_SERVICE);
            final Method method = wm.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true); //in the case of visibility change in future APIs
            return (Boolean) method.invoke(wm);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        /*
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        */

        return false;
    }
}
