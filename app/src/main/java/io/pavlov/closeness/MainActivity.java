/**************************************************************************
 *
 * Closeness            MainActivity
 *
 * Version:             1.1.2
 *
 * Developer:           robinvanemden@gmail.com
 *
 * Acknowledgements:    Hans IJzerman, https://sites.google.com/site/hijzerman/
 *                      Cor Wit Fund, http://www.corwitfonds.nl/
 *
 * License:             Attribution-ShareAlike 4.0 International
 *                      http://creativecommons.org/licenses/by-sa/4.0/
 *
 **************************************************************************/

package io.pavlov.closeness;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    static final boolean LOG;

    static {
        LOG = false;
    }

    private BroadcastReceiver receiver;
    private boolean isReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_main);


        Intent intent = new Intent(this, AutoStartUp.class);
        startService(intent);
        startSyncAdapter();
        doLineGraph();
    }

    /**
     * Start an asynchronous sync operation immediately. </br>
     */
    public static void requestSyncImmediately(Account account) {
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        // Request sync with settings
        ContentResolver.requestSync(account, "io.pavlov.closeness.KEYVAL", settingsBundle);
    }


    private void startSyncAdapter() {
        // Create the account type and default account
        Account account = new Account("Closeness", "io.pavlov");
        AccountManager accountManager = (AccountManager) this.getSystemService(ACCOUNT_SERVICE);
        // If the account already exists no harm is done but
        // a warning will be logged.

        if (accountManager.addAccountExplicitly(account, null, null)) {
            // Set auto regulatr
            ContentResolver.setIsSyncable(account, "io.pavlov.closeness.KEYVAL", 1);
            ContentResolver.setSyncAutomatically(
                    account, "io.pavlov.closeness.KEYVAL", true);
            ContentResolver.addPeriodicSync(
                    account, "io.pavlov.closeness.KEYVAL", new Bundle(), 60 * 5);

            // Do now too
            //ContentResolver.requestSync(
            //      newAccount,"io.pavlov.closeness.KEYVAL", Bundle.EMPTY);
            requestSyncImmediately(account);

        }
    }

    public void doLineGraph() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Closeness - v1.1.2 - 2015/09/30");
        alert.setMessage("Android App registering temperatures of nearby Blue Meastro Discs. \n\nDeveloped by Robin van Emden, \nrobin@pavlov.io,\nEmotion Regulation Lab, \nVU University, \nAmsterdam, the Netherlands.\n\nSupported by a Cor Wit Fonds Grant\nawarded to Dr. Hans IJzerman in 2014\n. ");
        alert.setPositiveButton("OK", null);
        alert.show();

        return id == R.id.action_settings || super.onOptionsItemSelected(item);

    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BleServiceConstants.ACTION_DEVICE_DISCOVERED);
        if (!isReceiverRegistered) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    BluetoothDevice device = intent.getParcelableExtra(BleServiceConstants.EXTRA_DEVICE_DISCOVERED_DEVICE);
                    String temp = intent.getStringExtra(BleServiceConstants.EXTRA_DEVICE_DISCOVERED_LIVE_TEMP);
                    if (MainActivity.LOG) Log.i("BroadCastReceived", device.getAddress());
                    TextView t = (TextView) findViewById(R.id.textViewTemp);
                    t.setText("Temperature: " + temp + " C");
                }
            };
            registerReceiver(receiver, filter);
            isReceiverRegistered = true;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                // Do nothing
            }
            isReceiverRegistered = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}

