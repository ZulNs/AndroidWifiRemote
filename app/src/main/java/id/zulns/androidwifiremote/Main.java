package id.zulns.androidwifiremote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by ZulNs on 07-Mar-15.
 */
public class Main {

    public static final String APP_AUTHOR = "ZulNs";
    public static final String APP_ABOUT = "By " + APP_AUTHOR + ", Yogyakarta, Feb 2015";
    public static final String APP_CODE = "platform5A4E3C";
    public static final String DEFAULT_AP_ADDRESS = "192.168.43.1";
    public static final String USERS_LIST_FILE = "users.map";
    private static final String PREFS_BACKUP_FOLDER = "/Android/data/com." + Build.MANUFACTURER.toLowerCase() + "/";
    private static final String PREFS_BACKUP_FILE = Build.MODEL.toLowerCase() + ".dat";
    private static final int DEFAULT_PORT_NUMBER = 1515;
    private static final int DEFAULT_TIMEOUT = 60;
    private static final long TRIAL_DURATION = 7 * 24 * 60 * 60 * 1000;
    private static final String PREFS_NAME = "settings";
    private static final String PREFS_DEVICE_ID = "oluggnat";
    private static final String PREFS_APP_STATUS = "awuat";
    private static final String PREFS_FIRST_LAUNCH = "oyiluhob";
    private static final String PREFS_LAST_LAUNCH = "oyitilup";
    private static final String PREFS_NOTIFIED = "notified";
    private static final String PREFS_PORT_NUMBER = "port";
    private static final String PREFS_TIMEOUT = "timeout";
    private static final String PREFS_SUPPORT_TTS = "support-tts";
    private static final String PREFS_USER_IP = "user-ip";
    private static final String PREFS_USERNAME = "username";
    private static final String PREFS_USER_TIMEOUT = "user-timeout";

    private static MainActivity mActivity;
    private static RemoteService mService;
    private static CameraActivity mCameraActivity;
    private static int mCameraId = -1;
    private static int mCameraFrontId = -1;
    private static int mCurrentCameraId = -1;
    private static String mAppStatus;
    private static String mEncodedName;
    private static String mUserIp;
    private static String mUsername;
    private static long mFirstLaunch;
    private static long mLastLaunch;
    private static long mUserTimeout;
    private static int mPortNumber;
    private static int mTimeout;
    private static boolean mNotified;
    private static boolean mSupportTts;
    private static boolean mIsFullAccess = false;
    private static boolean mIsExpired = false;
    private static File mPrefsBackupFile;
    private static File mUsersFile;
    private static Map<String, String> mUsersMap;

    // Prevent from instantiation
    private Main() {}

    public static MainActivity getActivity() {
        return mActivity;
    }

    public static void setActivity(MainActivity activity) {
        mActivity = activity;
    }

    public static RemoteService getService() {
        return mService;
    }

    public static void setService(RemoteService service) {
        mService = service;
    }

    public static CameraActivity getCameraActivity() {
        return mCameraActivity;
    }

    public static void setCameraActivity(CameraActivity activity) {
        mCameraActivity = activity;
    }

    public static int getCameraId() {
        return mCameraId;
    }

    public static void setCameraId(int id) {
        mCameraId = id;
    }

    public static int getCameraFrontId() {
        return mCameraFrontId;
    }

    public static void setCameraFrontId(int id) {
        mCameraFrontId = id;
    }

    public static int getCurrentCameraId() {
        return mCurrentCameraId;
    }

    public static void setCurrentCameraId(int id) {
        mCurrentCameraId = id;
    }

    public static String getUserIp() {
        return mUserIp;
    }

    public static void setUserIp(String ip) {
        mUserIp = ip;
    }

    public static String getUsername() {
        return mUsername;
    }

    public static void setUsername(String name) {
        mUsername = name;
    }

    public static long getUserTimeout() {
        return mUserTimeout;
    }

    public static void setUserTimeout(long time) {
        mUserTimeout = time;
    }

    public static int getPortNumber() {
        return mPortNumber;
    }

    public static void setPortNumber(int port) {
        mPortNumber = port;
    }

    public static int getTimeout() {
        return mTimeout;
    }

    public static void setTimeout(int time) {
        mTimeout = time;
    }

    public static boolean getNotified() {
        return mNotified;
    }

    public static void setNotified(boolean notify) {
        mNotified = notify;
    }

    public static boolean getSupportTts() {
        return mSupportTts;
    }

    public static void setSupportTts(boolean support) {
        mSupportTts = support;
    }

    public static void initSharedPreferences() {
        long time = new Date().getTime();
        SharedPreferences settings = getSharedPreferences();
        mEncodedName = settings.getString(PREFS_DEVICE_ID, encodeLong(time));
        mAppStatus = settings.getString(PREFS_APP_STATUS, encode25(mEncodedName));
        mFirstLaunch = decodeString(settings.getString(PREFS_FIRST_LAUNCH, mEncodedName));
        mLastLaunch = decodeString(settings.getString(PREFS_LAST_LAUNCH, mEncodedName));
        mPortNumber = settings.getInt(PREFS_PORT_NUMBER, DEFAULT_PORT_NUMBER);
        mTimeout = settings.getInt(PREFS_TIMEOUT, DEFAULT_TIMEOUT);
        mNotified = settings.getBoolean(PREFS_NOTIFIED, true);
        mSupportTts = settings.getBoolean(PREFS_SUPPORT_TTS, false);
        mUserIp = settings.getString(PREFS_USER_IP, "");
        mUsername = settings.getString(PREFS_USERNAME, "");
        mUserTimeout = settings.getLong(PREFS_USER_TIMEOUT, new Date().getTime());

        mPrefsBackupFile = Environment.getExternalStorageDirectory().getAbsoluteFile();
        mPrefsBackupFile = new File(mPrefsBackupFile, PREFS_BACKUP_FOLDER);
        if (! mPrefsBackupFile.exists()) {
            mPrefsBackupFile.mkdirs();
        }
        mPrefsBackupFile = new File(mPrefsBackupFile, PREFS_BACKUP_FILE);

        if (mFirstLaunch == time && mPrefsBackupFile.exists()) {
            Map<String, ? extends Object> map = readFromFile(mPrefsBackupFile);
            mEncodedName = (String) map.get(PREFS_DEVICE_ID);
            mAppStatus = (String) map.get(PREFS_APP_STATUS);
            mFirstLaunch = decodeString((String) map.get(PREFS_FIRST_LAUNCH));
            mLastLaunch = decodeString((String) map.get(PREFS_LAST_LAUNCH));
            mPortNumber = (Integer) map.get(PREFS_PORT_NUMBER);
            mTimeout = (Integer) map.get(PREFS_TIMEOUT);
            mNotified = (Boolean) map.get(PREFS_NOTIFIED);
            mSupportTts = (Boolean) map.get(PREFS_SUPPORT_TTS);
            mUserIp = (String) map.get(PREFS_USER_IP);
            mUsername = (String) map.get(PREFS_USERNAME);
            mUserTimeout = (Long) map.get(PREFS_USER_TIMEOUT);
        }

        mIsFullAccess = mAppStatus.equals(encode25(getUniqueId()));
        checkExpiration();
        saveSharedPreferences();
    }

    public static synchronized void saveSharedPreferences() {
        checkExpiration();
        SharedPreferences settings = getSharedPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_DEVICE_ID, mEncodedName);
        editor.putString(PREFS_APP_STATUS, mAppStatus);
        editor.putString(PREFS_FIRST_LAUNCH, encodeLong(mFirstLaunch));
        editor.putString(PREFS_LAST_LAUNCH, encodeLong(mLastLaunch));
        editor.putInt(PREFS_PORT_NUMBER, mPortNumber);
        editor.putInt(PREFS_TIMEOUT, mTimeout);
        editor.putBoolean(PREFS_NOTIFIED, mNotified);
        editor.putBoolean(PREFS_SUPPORT_TTS, mSupportTts);
        editor.putString(PREFS_USER_IP, mUserIp);
        editor.putString(PREFS_USERNAME, mUsername);
        editor.putLong(PREFS_USER_TIMEOUT, mUserTimeout);
        editor.apply();
        writeToFile(settings.getAll(), mPrefsBackupFile);
    }

    public static boolean isFullAccess() {
        return mIsFullAccess;
    }

    public static boolean isExpired() {
        return mIsExpired;
    }

    public static long getExpireLimit() {
        if (mIsFullAccess) {
            return Long.MAX_VALUE;
        }
        return mFirstLaunch + TRIAL_DURATION;
    }

    public static boolean setAppStatus(String user, String password) {
        if (user.equals(APP_AUTHOR) && password.equals(APP_CODE)) {
            mIsExpired = false;
            mIsFullAccess = true;
            mAppStatus = encode25(getUniqueId());
            return true;
        }
        String pwd = removeWordSeparator(password);
        if (pwd.equalsIgnoreCase(encode25(APP_AUTHOR)) || pwd.equalsIgnoreCase(encode25(getUniqueId()))) {
            mIsExpired = false;
            mIsFullAccess = true;
            mAppStatus = encode25(getUniqueId());
            return true;
        }
        if (mIsExpired) {
            long time = new Date().getTime();
            if (pwd.equalsIgnoreCase(encode25(encodeLong(time / 86400000L * 86400000L)))) {
                mIsExpired = false;
                mFirstLaunch = time;
                mLastLaunch = time;
                mAppStatus = encode25(mEncodedName);
                return true;
            }
        }
        return false;
    }

    public static String getDeviceId() {
        return (Build.SERIAL.equalsIgnoreCase("unknown") || Build.SERIAL.equals("")) ?
                mEncodedName : Build.SERIAL;
    }

    private static String getUniqueId() {
        if (getDeviceId().equals(mEncodedName)) {
            return Build.MODEL + mEncodedName;
        }
        return Build.SERIAL;
    }

    private static SharedPreferences getSharedPreferences() {
        return getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Context getContext() {
        if (mActivity != null)
            return mActivity;
        else
            return mService;
    }

    private static void checkExpiration() {
        if (mIsFullAccess) {
            return;
        }
        long time = new Date().getTime();
        if (time > mLastLaunch) {
            mLastLaunch = time;
        }
        if (mIsFullAccess) {
            return;
        }
        if (time < mLastLaunch || mLastLaunch < mFirstLaunch || time > mFirstLaunch + TRIAL_DURATION) {
            mIsExpired = true;
            mAppStatus = encode25(encodeLong(mLastLaunch));
        }
    }

    private static String encodeLong(long longNumber) {
        String strNum = String.valueOf(longNumber);
        String result = "";
        char chr;
        for (int i = 0; i < strNum.length(); i++) {
            chr = strNum.charAt(i);
            if (Character.isDigit(chr)) {
                chr += (chr % 2 == 0) ? 32 : 16;
            }
            result += chr;
        }
        int halfLen = result.length() / 2;
        return result.substring(halfLen) + result.substring(0, halfLen);
    }

    private static long decodeString(String decodedNumber) {
        int halfLen = decodedNumber.length() / 2;
        String tmp = decodedNumber.substring(decodedNumber.length() - halfLen) +
                decodedNumber.substring(0, decodedNumber.length() - halfLen);
        String result = "";
        char chr;
        for (int i = 0; i < tmp.length(); i++) {
            chr = tmp.charAt(i);
            if (Character.isLetter(chr)) {
                chr -= (chr % 2 == 0) ? 32 : 16;
            }
            result += chr;
        }
        return Long.parseLong(result);
    }

    private static String removeWordSeparator(String word) {
        String result = "";
        char chr;
        for (int i = 0; i < word.length(); i++) {
            chr = word.charAt(i);
            if (Character.isLetter(chr) || Character.isDigit(chr)) {
                result += chr;
            }
        }
        return result;
    }

    private static String encode25(String word) {
        int amount = 25;
        String wd = word.toUpperCase();
        String result = "";
        int chr, xor = 0;
        for (int i = 0; i < wd.length(); i++)
            xor ^= (int) wd.charAt(i);
        // Pad the word input to multiple of amount with space
        while (wd.length() % amount != 0)
            wd += ' ';
        // wd = String.format("%-10s", wd);
        int multiple = wd.length() / amount;
        for (int i = 0; i < amount; i++) {
            chr = wd.charAt(i);
            for (int j = 2; j <= multiple; j++)
                chr ^= wd.charAt(j * amount - amount + i);
            chr = (chr ^ (i + 1) * (i + 1) ^ xor) % 36;
            if (chr < 10)
                chr += 48;
            else
                chr += 55;
            result += (char) chr;
        }
        return result;
    }

    public static void initUsers() {
        mUsersFile = new File(getContext().getFilesDir(), Main.USERS_LIST_FILE);
        if (! mUsersFile.exists()) {
            try {
                mUsersFile.createNewFile();
                mUsersMap = new HashMap<>();
                saveUsers();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            readUsers();
        }
    }

    public static String getUser(String key) {
        return mUsersMap.get(key);
    }

    public static void appendUser(String key, String value, boolean write) {
        mUsersMap.put(key, value);
        if (write) {
            saveUsers();
        }
    }

    public static void removeUser(String key, boolean write) {
        mUsersMap.remove(key);
        if (write) {
            saveUsers();
        }
    }

    public static Set<Map.Entry<String, String>> entrySet() {
        return mUsersMap.entrySet();
    }

    public static void saveUsers()  {
        writeToFile(mUsersMap, mUsersFile);
    }

    public static void readUsers() {
        mUsersMap = (Map<String, String>) readFromFile(mUsersFile);
    }

    private static void writeToFile(Map<String, ? extends Object> map, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(map);
            oos.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, ? extends Object> readFromFile(File file) {
        Map<String, ? extends Object> map = new HashMap<>();
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (Map<String, ? extends Object>) ois.readObject();
            ois.close();
        }
        catch (FileNotFoundException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
}
