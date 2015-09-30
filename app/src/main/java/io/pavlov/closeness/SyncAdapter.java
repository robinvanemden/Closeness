package io.pavlov.closeness;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.dreamfactory.api.DbApi;
import com.dreamfactory.api.UserApi;
import com.dreamfactory.client.ApiException;
import com.dreamfactory.model.Login;
import com.dreamfactory.model.RecordRequest;
import com.dreamfactory.model.RecordResponse;
import com.dreamfactory.model.RecordsRequest;
import com.dreamfactory.model.Session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SyncAdapter extends AbstractThreadedSyncAdapter {

    private UserApi userApi = null;
    private Session session = null;
    private Login login = null;
    private DbApi dbApi = null;

    private List<RecordRequest> recordList = new ArrayList<>();
    private ContentProviderClient mContentProviderClient;

    public SyncAdapter(Context context, boolean autoInitialize) {

        super(context, autoInitialize);
        sessionControl();
    }

    private void sessionControl() {

        if (userApi == null || login == null || session == null || dbApi == null) {
            userApi = new UserApi();
            userApi.addHeader("X-DreamFactory-Application-Name", "closeness_key");
            userApi.setBasePath("http://104.155.22.38/rest");
            login = new Login();
            login.setEmail("rss@pwy.nl");
            login.setPassword("d323mb3r!rd");
            try {
                session = userApi.login(login);
                if (MainActivity.LOG) Log.i("SessionSession", session.toString());
            } catch (Exception e) {
                if (MainActivity.LOG) Log.i("ErrorLogin", e.toString());
            }
            if (session != null) {
                dbApi = new DbApi();
                dbApi.addHeader("X-DreamFactory-Application-Name", "closeness_key");
                dbApi.setBasePath("http://104.155.22.38/rest");
                dbApi.addHeader("X-DreamFactory-Session-Token", session.getSession_id());
            }
        }

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient contentProviderClient, SyncResult syncResult) {

        sessionControl();
        mContentProviderClient = contentProviderClient;

        try {
            createListOfDirty(syncResult);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }


    public void createListOfDirty(SyncResult syncResult) throws RemoteException {

        String[] mSelectionArgs = {""};
        String mSelectionClause = KeyValueProvider.KeyValue.STATUSLOCAL + " = ?";
        mSelectionArgs[0] = "1";


        Uri uri = Uri.parse("content://io.pavlov.closeness.KEYVAL/keyvalues");
        Cursor cursor = null;
        try {
            cursor = mContentProviderClient.query(uri, null, mSelectionClause, mSelectionArgs, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        KeyValueModel record = new KeyValueModel();
                        record.key = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.KEY));
                        record.systemtime = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.SYSTEMTIME));
                        record.ext = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.EXT));
                        record.android_id = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.ANDROID_ID));
                        record.device_id = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.DEVICE_ID));
                        record.type = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.TYPE));
                        record.value = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.VALUE));
                        record.address = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.ADDRESS));
                        recordList.add(record);
                        syncResult.stats.numInserts++;
                        syncResult.stats.numEntries++;
                    } while (cursor.moveToNext());
                }
            }
        }
        try {
            //syncBatchToInternet();
            syncOneByOneToInternet();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }


    // remove exceptions to check ...
    public void syncOneByOneToInternet() throws ApiException, RemoteException {

        if (recordList.size() > 0) {
            Iterator<RecordRequest> rreq = recordList.iterator();
            while (rreq.hasNext()) {

                KeyValueModel rreqLocal = (KeyValueModel) rreq.next();
                RecordResponse response = dbApi.createRecord("keyvalue", "123", rreqLocal, null, null, null, null);
                if (MainActivity.LOG) Log.i("RETURNED_SINGLE", response.toString());


                Uri uri = Uri.parse("content://io.pavlov.closeness.KEYVAL/keyvalues");
                String[] mSelectionArgs = {""};
                String mSelectionClause;

                ContentValues values = new ContentValues();
                values.put(KeyValueProvider.KeyValue.STATUSLOCAL, 0);
                mSelectionClause = KeyValueProvider.KeyValue.SYSTEMTIME + " = ?";
                mSelectionArgs[0] = String.valueOf(rreqLocal.getSystemtime());

                int returnUpdate = mContentProviderClient.update(uri, values, mSelectionClause, mSelectionArgs);
                if (MainActivity.LOG) Log.i("SET_TO_CLEAN_ROUND_NR", String.valueOf(rreqLocal.getSystemtime()));

                if (MainActivity.LOG) Log.i("SET_TO_CLEAN_ROUND", String.valueOf(returnUpdate));

                rreq.remove();

            }

        }
    }


    /**
     * @throws ApiException
     */
    @SuppressWarnings("unused")
    public void syncBatchToInternet() throws ApiException {
        Log.i("RETURNED_BATCH", recordList.toString());
        if (recordList.size() > 0) {
            RecordsRequest rqr = new RecordsRequest();
            rqr.setRecord(recordList);
            dbApi.createRecords("keyvalue", rqr, null, null, null, null, null, null, null);
            recordList.clear();
            try {
                setBatchClean();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void setBatchClean() throws RemoteException {

        String[] mSelectionArgs = {""};
        String mSelectionClause;
        mSelectionClause = KeyValueProvider.KeyValue.STATUSLOCAL + " = ?";
        mSelectionArgs[0] = "1";


        Uri uri = Uri.parse("content://io.pavlov.closeness.KEYVAL/keyvalues");
        Cursor cursor = null;
        try {
            cursor = mContentProviderClient.query(uri, null, mSelectionClause, mSelectionArgs, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        ContentValues values = new ContentValues();
                        values.put(KeyValueProvider.KeyValue.STATUSLOCAL, 0); // added so set to clean
                        mSelectionClause = KeyValueProvider.KeyValue.KEY_ID + " = ?";
                        mSelectionArgs[0] = String.valueOf(cursor.getInt(cursor.getColumnIndex(KeyValueProvider.KeyValue.KEY_ID)));
                        int returnUpdate = mContentProviderClient.update(uri, values, mSelectionClause, mSelectionArgs); //id is the id of the row you wan to update
                        Log.i("SET_TO_CLEAN", String.valueOf(returnUpdate));

                    } while (cursor.moveToNext());
                }
            }
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

}