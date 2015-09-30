package io.pavlov.closeness;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.Intent;
import android.provider.Settings.Secure;

import java.util.Arrays;

@SuppressWarnings("JavaDoc")
public class AutoStartUp extends Service implements BluetoothAdapter.LeScanCallback {

    private final static String TAG = AutoStartUp.class.getSimpleName();
    private String android_id = "";
    private String device_id = "";
    private final IBinder mBinder = new LocalBinder();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;


    public class LocalBinder extends Binder {
        public AutoStartUp getService() {
            return AutoStartUp.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
        android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        device_id = telephonyManager.getDeviceId();
    }

    /**
     * Broadcasts a message with the given device.
     *
     * @param device
     * @param scanRecord
     */
    protected void broadcastOnDeviceFound(final BluetoothDevice device, byte[] scanRecord, String temp) {
        assert device != null : "Device should not be null.";

        Intent intent = new Intent(BleServiceConstants.ACTION_DEVICE_DISCOVERED);
        intent.putExtra(BleServiceConstants.EXTRA_DEVICE_DISCOVERED_DEVICE, device);
        intent.putExtra(BleServiceConstants.EXTRA_DEVICE_DISCOVERED_SCAN_RECORD, scanRecord);
        intent.putExtra(BleServiceConstants.EXTRA_DEVICE_DISCOVERED_LIVE_TEMP, temp);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Initializes a reference to the local bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                if (MainActivity.LOG) Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                if (MainActivity.LOG) Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            } else {
                if (!mBluetoothAdapter.isEnabled()) {
                    // IF in APPSTORE you have to ASK THE USER PERMISSION whether to enable BT
                    // Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    // startActivity(i);

                    // Bluetooth is not enabled -> just turn it on without user intervention for now
                    mBluetoothAdapter.enable();
                }
            }
        }

        if (mHandler == null) {
            mHandler = new Handler();
        }


        if (MainActivity.LOG) Log.d(TAG, "Initialized scanner.");

        startScan();

        return true;
    }


    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

        String s = new String(Arrays.copyOfRange(scanRecord, 2, 12));

        if (s.equals("Tempo Disc")) {
            if (MainActivity.LOG) Log.d(TAG, "Found a Tempo Disc " + device.getAddress());
            if (MainActivity.LOG) Log.i("onLeScanRSSI", Integer.toString(rssi));
            if (MainActivity.LOG) Log.i("onLeScanRecord", s);
            String instTemp = parseSkinTemp(scanRecord);
            String extTEmp = parseExtTemp(scanRecord);

            if (!instTemp.equals("0.0")) {
                ContentValues values = new ContentValues();
                values.put(KeyValueProvider.KeyValue.ADDRESS, device.getAddress());
                values.put(KeyValueProvider.KeyValue.ANDROID_ID, android_id);
                values.put(KeyValueProvider.KeyValue.DEVICE_ID, device_id);
                values.put(KeyValueProvider.KeyValue.TYPE, "temp");
                values.put(KeyValueProvider.KeyValue.KEY, "inst");
                values.put(KeyValueProvider.KeyValue.EXT, extTEmp);
                if (MainActivity.LOG) Log.i("getContentResolver", instTemp);
                values.put(KeyValueProvider.KeyValue.VALUE, instTemp);
                values.put(KeyValueProvider.KeyValue.SYSTEMTIME, String.valueOf(System.currentTimeMillis()));
                values.put(KeyValueProvider.KeyValue.STATUSLOCAL, 1); // added so set to dirty
                Uri uri = Uri.parse("content://io.pavlov.closeness.KEYVAL/keyvalues");
                Uri newUri = getContentResolver().insert(uri, values);
                if (MainActivity.LOG) Log.i("getContentResolver", newUri.toString());
                if (MainActivity.LOG) Log.d(TAG, "Temp received Tempo Disc " + instTemp + " " + device.getAddress());
                broadcastOnDeviceFound(device, scanRecord, instTemp);

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

            } else {
                if (MainActivity.LOG) Log.d(TAG, "Error Tempo Disc 0.0 " + device.getAddress());
            }
        } else {
            if (MainActivity.LOG) Log.d(TAG, "Found a ble device " + device.getAddress());
        }
    }

    public static void requestSyncImmediately(Account account) {
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        // Request sync with settings
        ContentResolver.requestSync(account, "io.pavlov.closeness.KEYVAL", settingsBundle);
    }

    /**
     * Starts the bluetooth low energy scan.
     *
     * @return <code>true</code> if the scan is successfully started.
     */
    public boolean startScan() {
        if (!mBluetoothAdapter.isEnabled()) {
            // IF in APPSTORE you have to ASK THE USER PERMISSION whether to enable BT
            // Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // startActivity(i);
            // Bluetooth is not enabled -> just turn it on without user intervention for now
            mBluetoothAdapter.enable();
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothAdapter != null)
                    scanLeDevice(true);
                else {
                    if (MainActivity.LOG) Log.d(TAG, "BluetoothAdapter is null.");
                }
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanLeDevice(false);
                        startScan();
                    }
                }, 1000);
            }
        }, 2500);
        return true;
    }


    @SuppressWarnings("deprecation")
    public void scanLeDevice(final boolean enable) {
        if(enable){
            if (MainActivity.LOG) Log.d(TAG, "####STARTSCAN");
            mBluetoothAdapter.startLeScan(this);
        }else {
            if (MainActivity.LOG) Log.d(TAG, "####STOPSCAN");
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent("YouWillNeverKillMe"));
    }

    public static String parseSkinTemp(byte[] scanRecord) {
        String localstring;
        String manfData = byteArrayToHex(scanRecord);
        localstring = manfData.substring(64, 68);
        localstring = localstring.substring(2) + localstring.substring(0, 2);
        if (MainActivity.LOG) Log.d(TAG, "string is " + localstring);
        return  Float.toString(((float)Integer.parseInt(localstring, 16))/ 10);
    }

    public static String parseExtTemp(byte[] scanRecord) {
        String localstring;
        String manfData = byteArrayToHex(scanRecord);
        localstring = manfData.substring(88, 92);
        localstring = localstring.substring(2) + localstring.substring(0, 2);
        if (MainActivity.LOG) Log.d(TAG, "string is "+localstring);
        return  Float.toString(((float)Integer.parseInt(localstring, 16))/ 10);
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

}