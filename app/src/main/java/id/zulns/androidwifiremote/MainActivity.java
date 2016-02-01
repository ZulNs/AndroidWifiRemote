package id.zulns.androidwifiremote;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity implements TextWatcher {

    private static final int CHECK_TTS_DATA = 1;
    private CheckBox mNotificationCheck;
    private EditText mPortNumberText;
    private EditText mTimeoutText;
    private Button mApplySettingsButton;
    private TextView mInfoText;
    private Button mActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Main.getCameraActivity() != null) {
            Main.getCameraActivity().finish();
        }

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar ab = getActionBar();
            if (ab != null) {
                ab.setSubtitle(Main.APP_ABOUT);
            }
        }

        mNotificationCheck = (CheckBox) findViewById(R.id.notificationCheck);
        mPortNumberText = (EditText) findViewById(R.id.portNumberText);
        mTimeoutText = (EditText) findViewById(R.id.timeoutText);
        mApplySettingsButton = (Button) findViewById(R.id.applySettingsButton);
        mInfoText = (TextView) findViewById(R.id.infoText);
        mActionButton = (Button) findViewById(R.id.actionButton);

        mPortNumberText.addTextChangedListener(this);
        mTimeoutText.addTextChangedListener(this);

        Main.setActivity(this);
        Main.initSharedPreferences();

        mNotificationCheck.setChecked(Main.getNotified());

        mPortNumberText.setText(String.valueOf(Main.getPortNumber()));
        mPortNumberText.setSelection(String.valueOf(Main.getPortNumber()).length());

        mTimeoutText.setText(String.valueOf(Main.getTimeout()));
        mTimeoutText.setSelection(String.valueOf(Main.getTimeout()).length());

        mApplySettingsButton.setEnabled(false);
        if (Main.getService() != null) {
            mPortNumberText.setEnabled(false);
            mTimeoutText.setEnabled(false);
            return;
        }

        if (! Main.getSupportTts()) {
            startActivityForResult(new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA), CHECK_TTS_DATA);
        }

        Main.initUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateWifiState(WifiStateReceiver.isWifiConnectionAvailable());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Main.saveSharedPreferences();
        Main.setActivity(null);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHECK_TTS_DATA) {
            Main.setSupportTts(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS);
        }
    }

    public void onClickNotificationCheck(View view) {
        Main.setNotified(mNotificationCheck.isChecked());
        if (Main.getService() != null) {
            Main.getService().updateNotification();
        }
        Main.saveSharedPreferences();
    }

    public void onClickEditUsersButton(View view) {
        startActivity(new Intent(this.getApplicationContext(), EditUsersActivity.class));
        finish();
    }

    public void onClickApplySettingsButton(View view) {
        mApplySettingsButton.setEnabled(false);
        int pn = getIntVal(mPortNumberText);
        int to = getIntVal(mTimeoutText);
        if (pn != Main.getPortNumber()) {
            Main.setPortNumber(pn);
            updateWifiState(WifiStateReceiver.isWifiConnectionAvailable());
        }
        if (to != Main.getTimeout()) {
            Main.setTimeout(to);
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void onClickActionButton(View view) {
        if (getString(R.string.label_start_remote).contentEquals(mActionButton.getText())) {
            startService(new Intent(this.getApplicationContext(), RemoteService.class));
            //startActivity(new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA));
            finish();
        }
        else if (getString(R.string.label_stop_remote).contentEquals(mActionButton.getText())) {
            if (Main.getService() != null) {
                Main.getService().stopSelf();
                Main.setService(null);
            }
            mPortNumberText.setEnabled(true);
            mTimeoutText.setEnabled(true);
            updateWifiState(WifiStateReceiver.isWifiConnectionAvailable());
        }
        else {
            finish();
        }
    }

    public synchronized void updateWifiState(boolean ready) {
        String info;
        if (ready) {
            info = "\n" + WifiStateReceiver.getURL() + ":" + Main.getPortNumber();
            if (Main.getService() != null) {
                info = "Remote running at:" + info;
                mActionButton.setText(R.string.label_stop_remote);
            }
            else {
                info = "Remote ready at:" + info;
                mActionButton.setText(R.string.label_start_remote);
            }
            info += "\nModel: " + Build.MANUFACTURER + " " + Build.MODEL +
                    "\nSerial: " + Main.getDeviceId();
        }
        else {
            if (Main.getService() != null) {
                Main.getService().stopSelf();
            }
            info = "No available WiFi connection.\n" +
                    "Please try to connect to a WiFi network or " +
                    "activate the WiFi hotspot (tethering).";
            mActionButton.setText(R.string.label_exit_app);
        }

        mInfoText.setText(info);
    }

    private int getIntVal(EditText et) {
        int res = 0;
        if (et.getText().length() > 0) {
            res = Integer.parseInt(et.getText().toString());
        }
        return res;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        int pn = getIntVal(mPortNumberText);
        int to = getIntVal(mTimeoutText);
        mApplySettingsButton.setEnabled(1024 < pn && pn < 65536 && to > 0 && (pn != Main.getPortNumber() || to != Main.getTimeout()));
    }

    /*private boolean mIsServiceRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE))
            if (ServerService.class.getName().equals(service.service.getClassName()))
                return true;
        return false;
    }*/
}
