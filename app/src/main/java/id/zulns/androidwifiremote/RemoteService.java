package id.zulns.androidwifiremote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**
 * Created by ZulNs on 15-Jan-15.
 */
public class RemoteService extends Service implements TextToSpeech.OnInitListener {

    private static final String[] TTS_VOICE = {
        "nld_NLD", "eng_IND", "eng_GBR", "eng_USA", "fra_FRA", "deu_DEU", "hin_IND", "ind_IDN",
        "ita_ITA", "jpn_JPN", "kor_KOR", "pol_POL", "por_BRA", "rus_RUS", "spa_ESP", "spa_USA",
        "yue_HKG", "zho_CHN", "tha_THA", "tur_TUR"
    };

    private WifiRemote mWifiRemote;
    private Notification mNotification;
    private Notification.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private PendingIntent mPendingIntent;
    private static final int notifyId = 1;
    private TextToSpeech mTTS;
    private AudioManager mAudioManager;
    private List<String> mVoices;

    /*public class LocalBinder extends Binder {
        ServerService getService() {
            return ServerService.this;
        }
    }*/

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPendingIntent = PendingIntent.getActivity(
            this.getApplicationContext(),
            Intent.FLAG_ACTIVITY_NEW_TASK,
            new Intent(this.getApplicationContext(),
            MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT
        );
        createNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (Main.getNotified()) {
            mNotificationManager.cancel(notifyId);
        }

        if (mWifiRemote != null) {
            mWifiRemote.stop();
            mWifiRemote = null;
            Main.setUserIp("");
        }

        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
            mTTS = null;
        }

        Main.saveSharedPreferences();
        Main.setService(null);

        Toast.makeText(this, getText(R.string.service_name) + " stopped...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Main.setService(this);
        if (intent == null) {
            Main.initSharedPreferences();
            Toast.makeText(this, getText(R.string.service_name) + " restarted...", Toast.LENGTH_SHORT).show();
            if (mTTS != null) {
                mTTS.stop();
                mTTS.shutdown();
                mTTS = null;
            }
            if (mWifiRemote != null) {
                mWifiRemote.stop();
                mWifiRemote = null;
            }
            Main.initUsers();
        }
        else {
            Toast.makeText(this, getText(R.string.service_name) + " started...", Toast.LENGTH_SHORT).show();
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (Main.getSupportTts()) {
            mTTS = new TextToSpeech(this, this);
        }

        mWifiRemote = new WifiRemote(Main.getPortNumber(), Main.getTimeout());

        int n = Camera.getNumberOfCameras();
        if (n > 0) {
            Camera.CameraInfo ci = new Camera.CameraInfo();
            for (int i = 0; i < n; i++) {
                Camera.getCameraInfo(i, ci);
                if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    Main.setCameraId(i);
                }
                if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Main.setCameraFrontId(i);
                }
            }
        }

        updateWifiState(true);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mNotifyBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(getText(R.string.app_name))
                .setContentTitle(getText(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(mPendingIntent);
        }
        else {
            mNotification = new Notification(R.drawable.ic_launcher, getText(R.string.app_name), System.currentTimeMillis());
            mNotification.flags |= Notification.FLAG_NO_CLEAR;
        }
    }

    private void updateNotificationContent(String msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mNotifyBuilder.setContentText(msg);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                mNotification = mNotifyBuilder.getNotification();
            }
            else {
                mNotification = mNotifyBuilder.build();
            }
        }
        else {
            // Method which removed from API 23
            // mNotification.setLatestEventInfo(this, getText(R.string.app_name), msg, mPendingIntent);
            try {
                Method deprecatedMethod = mNotification.getClass().getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
                deprecatedMethod.invoke(mNotification, this, R.string.app_name, msg, mPendingIntent);
            }
            catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void updateWifiState(boolean ready) {
        if (ready) {
            try {
                mWifiRemote.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            updateNotificationContent(WifiStateReceiver.getURL() + ":" + Main.getPortNumber());
        }
        else {
            if (Main.getActivity() != null) {
                stopSelf();
            }
            else {
                mWifiRemote.stop();
                updateNotificationContent("Waiting for available WiFi connection...");
            }
        }
        updateNotification();
    }

    public void updateNotification() {
        if (Main.getNotified()) {
            mNotificationManager.notify(notifyId, mNotification);
        }
        else {
            mNotificationManager.cancel(notifyId);
        }
    }

    public void shutdown() {
        if (Main.getCameraActivity() != null) {
            Main.getCameraActivity().finish();
        }
        if (Main.getActivity() != null) {
            Main.getActivity().finish();
        }
        stopSelf();
        //System.runFinalization();
        //System.exit(0);
    }

    public int getMaxVolume() {
        return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public int getCurrentVolume() {
        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public void setVolume(int volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
    }

    public List<String> getVoices() {
        return mVoices;
    }

    public Locale getVoice() {
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return mTTS.getLanguage();
        } else {
            return mTTS.getVoice().getLocale();
        }*/
        return mTTS.getLanguage();
    }

    public boolean isSpeaking() {
        return mTTS.isSpeaking();
    }

    public boolean setVoice(Locale voice) {
        int status = mTTS.setLanguage(voice);
        return (status != TextToSpeech.LANG_MISSING_DATA && status != TextToSpeech.LANG_NOT_SUPPORTED);
    }

    public boolean setPitch(float pitch) {
        return (mTTS.setPitch(pitch) != TextToSpeech.ERROR);
    }

    public boolean setSpeechRate(float speechRate) {
        return (mTTS.setSpeechRate(speechRate) != TextToSpeech.ERROR);
    }

    public boolean speak(String msg) {
        return (mTTS.speak(msg, TextToSpeech.QUEUE_ADD, null) != TextToSpeech.ERROR);
    }

    public boolean flush() {
        return (mTTS.speak("", TextToSpeech.QUEUE_FLUSH, null) != TextToSpeech.ERROR);
    }

    public void setCameraOn(int cameraId) {
        if (Main.getCurrentCameraId() == cameraId || cameraId < 0) {
            return;
        }
        if (Main.getCameraId() != cameraId && cameraId != Main.getCameraFrontId()) {
            return;
        }
        Main.setCurrentCameraId(cameraId);
        if (Main.getCameraActivity() != null) {
            Main.getCameraActivity().finish();
        }
        if (Main.getActivity() != null) {
            Main.getActivity().finish();
        }
        if (Main.getCameraActivity() == null) {
            Intent i = new Intent(getBaseContext(), CameraActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(i);
        }
    }

    public void setCameraOff() {
        if (Main.getCameraActivity() != null) {
            Main.getCameraActivity().finish();
        }
        Main.setCurrentCameraId(-1);
    }

    // TextToSpeech.OnInitListener implementation
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.ERROR) {
            Main.setSupportTts(false);
            return;
        }

        /*String engine = mTTS.getDefaultEngine();
        Toast.makeText(this, "engine:" + engine, Toast.LENGTH_SHORT).show();
        if (! engine.contentEquals("com.google.android.tts")) {
            isSupportTTS = false;
            return;
        }*/

        mVoices = new ArrayList<>();
        for (String v : TTS_VOICE) {
            Locale voice = new Locale(v.substring(0, 3), v.substring(4));
            if (mTTS.isLanguageAvailable(voice) == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                mVoices.add(voice.getDisplayName() + voice.toString());
            }
        }

        if (mVoices.isEmpty()) {
            Main.setSupportTts(false);
        }
        else {
            Collections.sort(mVoices, String.CASE_INSENSITIVE_ORDER);
        }
    }
}
