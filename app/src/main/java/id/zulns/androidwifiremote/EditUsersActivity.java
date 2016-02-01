package id.zulns.androidwifiremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;


public class EditUsersActivity extends Activity implements TextWatcher,
        DialogInterface.OnClickListener {

    private static final int DIALOG_DELETE_USER = 1;
    private static final int DIALOG_REPLACE_USER = 2;

    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private Button mAddUserButton;
    private Button mSaveButton;
    private ArrayAdapter<String> mArrayAdapter;
    private AlertDialog.Builder mDialogBuilder;
    private int mWhichDialog;
    private String mUsername;
    private InputMethodManager mInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_users);

        ListView usersListView;

        usersListView = (ListView) findViewById(R.id.usersListView);
        mUsernameEditText = (EditText) findViewById(R.id.usernameEditText);
        mPasswordEditText = (EditText) findViewById(R.id.passwordEditText);
        mAddUserButton = (Button) findViewById(R.id.addUserButton);
        mSaveButton = (Button) findViewById(R.id.saveButton);
        mDialogBuilder = new AlertDialog.Builder(this);
        mInput = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);

        mAddUserButton.setEnabled(false);

        mArrayAdapter = new ArrayAdapter<>(this, R.layout.text_view);
        for (Map.Entry<String, String> entry : Main.entrySet()) {
            mArrayAdapter.add(entry.getKey());
        }
        usersListView.setAdapter(mArrayAdapter);

        // Register event handler
        registerForContextMenu(usersListView);
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText.addTextChangedListener(this);
        mDialogBuilder.setPositiveButton("Ok", this);
        mDialogBuilder.setNegativeButton("Cancel", this);
    }

    protected void onPause() {
        super.onPause();

        // Cancel all editing
        Main.readUsers();
        finish();
    }

    public void onClickSaveButton(View view) {
        Main.saveUsers();
        finish();
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    public void onClickAddUserButton (View view) {
        String un = mUsernameEditText.getText().toString();
        if (Main.getUser(un) != null) {
            showDialogAction("Replace user: " + un + "?", DIALOG_REPLACE_USER);
        }
        else {
            addUser();
            changeButton();
        }
    }

    public void onClickShowPasswordCheck (View view) {
        if (((CheckBox) view).isChecked()) {
            mPasswordEditText.setTransformationMethod(
                    HideReturnsTransformationMethod.getInstance());
        }
        else {
            mPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        mPasswordEditText.setSelection(mPasswordEditText.getText().length());
    }

    // Registered via registerForContextMenu(View view)
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        getMenuInflater().inflate(R.menu.context_menu_user, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        mUsername = ((TextView) info.targetView).getText().toString();
        switch (item.getItemId()) {
            case R.id.context_menu_edit:
                String pw = Main.getUser(mUsername);
                mUsernameEditText.setText(mUsername);
                mUsernameEditText.setSelection(mUsername.length());
                mPasswordEditText.setText(pw);
                mPasswordEditText.setSelection(pw.length());
                mPasswordEditText.requestFocus();
                return true;
            case R.id.context_menu_delete:
                showDialogAction("Are you sure want to delete: " + mUsername + "?", DIALOG_DELETE_USER);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    // TextWathcer implementation
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    // TextWathcer implementation
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    // TextWathcer implementation
    @Override
    public void afterTextChanged(Editable s) {
        if (s.toString().contains(" ")) {
            try {
                mInput.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "Space character not allowed...", Toast.LENGTH_SHORT).show();
        }

        String un = mUsernameEditText.getText().toString();
        String pw = mPasswordEditText.getText().toString();
        mAddUserButton.setEnabled(un.length() > 0 && !un.contains(" ") && pw.length() > 0 && !pw.contains(" "));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                switch (mWhichDialog) {
                    case DIALOG_REPLACE_USER:
                        mArrayAdapter.remove(mUsernameEditText.getText().toString());
                        addUser();
                        break;
                    case DIALOG_DELETE_USER:
                        Main.removeUser(mUsername, false);
                        mArrayAdapter.remove(mUsername);
                        Toast.makeText(this, mUsername + " has been deleted", Toast.LENGTH_SHORT).show();
                }
                changeButton();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
        }
        dialog.dismiss();
    }

    private void showDialogAction(String msg, int which) {
        mWhichDialog = which;
        mDialogBuilder.setMessage(msg);
        AlertDialog dialog = mDialogBuilder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void addUser() {
        Main.appendUser(mUsernameEditText.getText().toString(),
                mPasswordEditText.getText().toString(), false);
        mArrayAdapter.add(mUsernameEditText.getText().toString());
        mPasswordEditText.setText("");
        mUsernameEditText.setText("");
        mUsernameEditText.requestFocus();
        try {
            mInput.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void changeButton() {
        if (getString(R.string.label_back).contentEquals(mSaveButton.getText())) {
            mSaveButton.setText(getText(R.string.label_save_users));
        }
    }
}
