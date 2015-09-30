package io.pavlov.closeness;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class RealtimeGraph extends Fragment {
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private LineGraphSeries<DataPoint> mSeries;
    private double graphLastXValue = 20d;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main2, container, false);

        GraphView graph = (GraphView) rootView.findViewById(R.id.graph2);
        mSeries = new LineGraphSeries<>();
        mSeries.setDrawDataPoints(true);
        mSeries.setDataPointsRadius(10);
        mSeries.setThickness(8);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.addSeries(mSeries);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        graph.getViewport().setMinY(20);
        graph.getViewport().setMaxY(40);


        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }


    @Override
    public void onResume() {
        super.onResume();

        mTimer = new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 3d;
                mSeries.appendData(new DataPoint(graphLastXValue, getLastTempValue()), true, 40);
                mHandler.postDelayed(this, 200);
            }
        };
        mHandler.postDelayed(mTimer, 2500);
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(mTimer);
        super.onPause();
    }

    private double getLastTempValue() {

        Uri uri = Uri.parse("content://io.pavlov.closeness.KEYVAL/keyvalues");
        Cursor cursor;
        double temperature_double = 0.0;
        ContentResolver resolver = getActivity().getContentResolver();
        cursor = resolver.query(uri, null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToLast();
            String temperature;
            temperature = cursor.getString(cursor.getColumnIndex(KeyValueProvider.KeyValue.VALUE));
            temperature_double = Double.parseDouble(temperature);
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return temperature_double;
    }
}