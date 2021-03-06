package com.github.allink28.fourdimensional;

import java.text.DateFormat;
import java.util.Date;

import com.github.allink28.android_gps_buildexample.R;
import com.github.allink28.fourdimensional.models.Trip;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements LocationListener {
    public static final DateFormat DATE_FORMAT = DateFormat.getTimeInstance(); // new SimpleDateFormat("h:mm a EEE, MMM d");
    private EditText startTimeTB, endTimeTB;
    private EditText startLatTB, startLongTB;
    private EditText currentLat, currentLong;
    private EditText endLatTB, endLongTB;
    private TextView summaryTV;
    private ToggleButton start;
    private float distance;
    private SharedPreferences settings;
    private LocationManager locationManager;
    private Location currentLocation;
    private Trip trip;

    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 0;

    // -------------- Activity Lifecycle methods --------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        init();
    }

    private void init() {
        startTimeTB = (EditText) this.findViewById(R.id.startTime);
        startLatTB = (EditText) this.findViewById(R.id.startLatitude);
        startLongTB = (EditText) this.findViewById(R.id.startLongitude);
        currentLat = (EditText) this.findViewById(R.id.currentLat);
        currentLong = (EditText) this.findViewById(R.id.currentLong);
        endTimeTB = (EditText) this.findViewById(R.id.endTime);
        endLatTB = (EditText) this.findViewById(R.id.endLat);
        endLongTB = (EditText) this.findViewById(R.id.endLong);
        summaryTV = (TextView) this.findViewById(R.id.summary);
        start = (ToggleButton) this.findViewById(R.id.start_button);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000 * Integer.valueOf(settings.getString(getString(R.string.update_time), "5")),//ms to update after
                    Integer.valueOf(settings.getString(getString(R.string.update_dist), "3")),//meters to update after
                    this);
        } catch (SecurityException se) {
            Log.d("MainActivity.onResume", "SecurityException onResume: " + se);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ----------------------- Activity Methods ---------------------------------

    @Override
    public void onLocationChanged(Location newLocation) {
        if (start.isChecked() && newLocation != null && currentLocation != null) {
            distance += newLocation.distanceTo(currentLocation);
            boolean useMiles = settings.getBoolean(getString(R.string.useMiles), false);
            summaryTV.setText("Distance traveled: " + Converter.formatDistance(distance, useMiles));
        }
        currentLocation = newLocation;
        setLocationDisplay(currentLocation, currentLat, currentLong);
    }

    /**
     * Executes the correct action when the start/stop button is pressed,
     * and resets values when the action is finished.
     */
    public void toggleTimer(View v) {
        if (start.isChecked()) {
            startTimer();
        } else {
            stopTimer();
        }
    }

    private void startTimer() {
        trip = new Trip(currentLocation);
        String formattedDate = DATE_FORMAT.format( new Date(trip.getStartTime()));
        startTimeTB.setText(formattedDate);
        setLocationDisplay(trip.getStartLocation(), startLatTB, startLongTB);
        clearLocationDisplay();
        setNotification(formattedDate);
    }

    private void stopTimer() {
        trip.setEndTime(System.currentTimeMillis());
        endTimeTB.setText(DATE_FORMAT.format(new Date(trip.getEndTime())));
        trip.setEndLocation(currentLocation);
        notificationManager.cancel(NOTIFICATION_ID);
        setLocationDisplay(trip.getEndLocation(), endLatTB, endLongTB);
        setLocationDisplay(trip.getStartLocation(), startLatTB, startLongTB);


        StringBuilder sb = new StringBuilder();
        boolean useMiles = settings.getBoolean(getString(R.string.useMiles), false);
        if (distance != 0) {
            sb.append("Distance traveled: ").append(Converter.formatDistance(distance, useMiles)).append("\n");
        }
        sb.append("Time: ").append(Converter.formatTime(trip.getEndTime() - trip.getStartTime()));
        if (trip.getStartLocation() != null && trip.getEndLocation() != null) {
            sb.append("\nDisplacement: ")
                    .append(Converter.formatDistance(trip.getStartLocation().distanceTo(trip.getEndLocation()), useMiles));
        }
        summaryTV.setText(sb.toString());
    }

    @SuppressWarnings("deprecation")
    public void setNotification(String startDate) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification();
        notification.icon = R.drawable.ic_launcher;
        notification.tickerText = "Timer running. Started at: " + startDate;
        notification.number = 0;
        notification.setLatestEventInfo(getApplicationContext(), //api 14 compatible
                "4Dimensional - Timer running",
                "Started at: " + startDate,
                pendingIntent);
//    Notification notification = new Notification.Builder(getApplicationContext()) //for api 16
//          .setContentTitle("Content title")
//          .setContentText("content text") //setSmallIcon(R.drawable.new_email).setLargeIcon(aBitmap)
//          .setContentIntent(pendingIntent)
//          .build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Save the current coordinate
     */
    public void mark(View v) {
        //TODO save coordinate
        Toast.makeText(MainActivity.this, R.string.marked, Toast.LENGTH_SHORT).show();
    }


    // ------------------------- Helper methods ---------------------------------

    private void setLocationDisplay(Location l, EditText lat, EditText lon) {
        if (l != null) {
            boolean useDMS = settings.getBoolean(getString(R.string.useDMS), false);
            lat.setText(String.valueOf(Converter.formatCoordinate(l.getLatitude(), useDMS)));
            lon.setText(String.valueOf(Converter.formatCoordinate(l.getLongitude(), useDMS)));
        } else {
            lat.setText(null);
            lon.setText(null);
        }
    }

    private void clearLocationDisplay() {
        String emptyString = "";
        summaryTV.setText(emptyString);
        endTimeTB.setText(emptyString);
        endLatTB.setText(emptyString);
        endLongTB.setText(emptyString);
    }


    // ------------------------ Unimplemented Methods ---------------------------

    @Override
    public void onProviderDisabled(String provider) {
        // TODO: blank out current location?
        Toast.makeText(MainActivity.this, String.format(getResources().getString(R.string.providerDisabled), (provider != null) ? provider.toUpperCase() : null ), Toast.LENGTH_LONG).show();
        Log.i("onProviderDisabled", "onProviderDisabled(" + provider + ") not yet implemented");
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        Log.i("onProviderEnabled", "onProviderEnabled(" + provider + ") not yet implemented");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        Log.i("onStatusChanged", "onStatusChanged(" + provider + ", " + status + ", " + extras + ") not yet implemented");
    }

}
