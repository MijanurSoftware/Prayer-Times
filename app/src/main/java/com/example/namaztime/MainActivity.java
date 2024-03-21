package com.example.namaztime;




import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private TextView tvLocation, tvArabicDate, tvEnglishDate, tvBanglaDate, tvFajr, tvSunrise, tvDhuhr, tvAsr, tvMagrib, tvIshaa, tvNextPrayerName, tvNextPrayerTime, tvNextPrayerTimeLeft;
    private LocationManager locationManager;

    private CountDownTimer countDownTimer;

    private JSONObject timings;

    String nextPrayerName = null;
    String nextPrayerTime = null;




    private static final long UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // Update interval set to 24 hours
    private Timer timer;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvArabicDate = findViewById(R.id.tvArabicDate);
        tvLocation = findViewById(R.id.tvLocation);
        tvEnglishDate = findViewById(R.id.tvEnglishDate);
        tvBanglaDate = findViewById(R.id.tvBanglaDate);
        tvFajr = findViewById(R.id.tvFajr);
        tvSunrise = findViewById(R.id.tvSunrise);
        tvDhuhr = findViewById(R.id.tvDhuhr);
        tvAsr = findViewById(R.id.tvAsr);
        tvMagrib = findViewById(R.id.tvMagrib);
        tvIshaa = findViewById(R.id.tvIshaa);
        tvNextPrayerName = findViewById(R.id.tvNextPrayerName);
        tvNextPrayerTime = findViewById(R.id.tvNextPrayerTime);
        tvNextPrayerTimeLeft = findViewById(R.id.tvNextPrayerTimeLeft);

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            // Permission already granted, start detecting location
            getLocation();
        }



        // Start a timer task to update prayer times periodically
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updatePrayerTimes();
            }
        }, 0, UPDATE_INTERVAL);






    }




    //===================================App open with update Ui================================

    // Fetches prayer times based on current date and updates UI
    private void updatePrayerTimes() {
        // Get current date
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        // Fetch prayer times based on current location and current date
        updatePrayerTimesForDay(currentDay);
    }


    // Fetches prayer times based on the given day and updates UI
    private void updatePrayerTimesForDay(int day) {
        if (timings == null) {
            // Timings data is not available, cannot update prayer times
            return;
        }

        try {
            JSONObject prayerData = timings.getJSONObject(Integer.toString(day));
            JSONObject prayerTimings = prayerData.getJSONObject("timings");

            // Extract prayer times for the given day
            String fajr = prayerTimings.getString("Fajr");
            String sunrise = prayerTimings.getString("Sunrise");
            String dhuhr = prayerTimings.getString("Dhuhr");
            String asr = prayerTimings.getString("Asr");
            String maghrib = prayerTimings.getString("Maghrib");
            String isha = prayerTimings.getString("Isha");

            // Update UI with the fetched prayer times
            tvFajr.setText(timeFormat(fajr));
            tvSunrise.setText(timeFormat(sunrise));
            tvDhuhr.setText(timeFormat(dhuhr));
            tvAsr.setText(timeFormat(asr));
            tvMagrib.setText(timeFormat(maghrib));
            tvIshaa.setText(timeFormat(isha));

            // Update English date in UI
            JSONObject dateObject = prayerData.getJSONObject("date");
            String date = dateObject.getString("readable");
            tvEnglishDate.setText(date);

            // Update next prayer details
            String[] prayerNames = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
            int nextPrayerIndex = findNextPrayerIndex(prayerTimings, prayerNames);
            if (nextPrayerIndex != -1) {
                String nextPrayerName = prayerNames[nextPrayerIndex];
                String nextPrayerTime = prayerTimings.getString(nextPrayerName);
                tvNextPrayerName.setText(nextPrayerName);
                tvNextPrayerTime.setText(timeFormat(nextPrayerTime));
                startCountdown();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("PrayerTimesError", "Error fetching prayer times for the given day");
        }
    }


    // Finds the index of the next prayer in the array of prayer names
    private int findNextPrayerIndex(JSONObject prayerTimings, String[] prayerNames) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date currentTime = sdf.parse(sdf.format(new Date()));

            for (int i = 0; i < prayerNames.length; i++) {
                String prayerName = prayerNames[i];
                String prayerTime = prayerTimings.getString(prayerName);
                Date prayerDateTime = sdf.parse(prayerTime);

                // Compare current time with prayer time
                if (currentTime.before(prayerDateTime)) {
                    return i; // Return the index of the next prayer
                }
            }
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
            Log.e("FindNextPrayerError", "Error finding the next prayer index");
        }
        return -1; // Return -1 if next prayer index is not found or error occurs
    }



    //==========================================================================





    private String timeFormat(String time) {
        String[] parts = time.split(" ");
        String[] timeParts = parts[0].split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        String suffix = (hour >= 12) ? "PM" : "AM";
        hour = (hour > 12) ? hour - 12 : hour;
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour, minute, suffix);
    }


    private void fetchPrayerTimes(double latitude, double longitude) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.aladhan.com/v1/calendar?latitude=" + latitude + "&longitude=" + longitude + "&method=1"; // Karachi

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            JSONArray data = response.getJSONArray("data");

                            Calendar calendar = Calendar.getInstance();
                            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                            int day = dayOfMonth - 1;

                            JSONObject prayerData = data.getJSONObject(day);
                            timings = prayerData.getJSONObject("timings");

                            timings = prayerData.getJSONObject("timings");



                            String fajr = timings.getString("Fajr");
                            String sunrise = timings.getString("Sunrise");
                            String dhuhr = timings.getString("Dhuhr");
                            String asr = timings.getString("Asr");
                            String magrib = timings.getString("Maghrib");
                            String isha = timings.getString("Isha");

                            JSONObject dates = prayerData.getJSONObject("date");
                            String date = dates.getString("readable");

                            tvFajr.setText(timeFormat(fajr));
                            tvSunrise.setText(timeFormat(sunrise));
                            tvDhuhr.setText(timeFormat(dhuhr));
                            tvAsr.setText(timeFormat(asr));
                            tvMagrib.setText(timeFormat(magrib));
                            tvIshaa.setText(timeFormat(isha));
                            tvEnglishDate.setText(date);

                            // Get current time in 24-hour format
                            Calendar currentTime = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            String currentFormattedTime = sdf.format(currentTime.getTime());

                            // Array of prayer names
                            String[] prayerNames = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};

                            for (String prayer : prayerNames) {
                                String prayerTime = timings.optString(prayer);

                                if (currentFormattedTime.compareTo(prayerTime) < 0) {
                                    nextPrayerName = prayer;
                                    nextPrayerTime = prayerTime;
                                    break;
                                }
                            }

                            // If all prayer times are passed, set the next prayer time to Fajr of the next day
                            if (nextPrayerTime == null) {
                                nextPrayerName = prayerNames[0];
                                nextPrayerTime = timings.optString(prayerNames[0]);
                            }

                            // Display current prayer and next prayer details
                            tvNextPrayerName.setText(nextPrayerName);
                            tvNextPrayerTime.setText(timeFormat(nextPrayerTime));

                            startCountdown();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("PrayerTimesError", "Error parsing JSON");
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("PrayerTimesError", error.toString());
                        Toast.makeText(MainActivity.this, "Failed to fetch prayer times", Toast.LENGTH_SHORT).show();
                    }
                });

        queue.add(jsonObjectRequest);
    }







    private void startCountdown() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            Date current = sdf.parse(sdf.format(new Date()));
            Date nextPrayer = sdf.parse(nextPrayerTime);

            long diffInMillis = nextPrayer.getTime() - current.getTime();
            if (diffInMillis < 0) {
                diffInMillis += TimeUnit.DAYS.toMillis(1); // Add one day in milliseconds to handle cases where next prayer is tomorrow
            }

            if (diffInMillis <= 0) {
                tvNextPrayerTimeLeft.setText("Time Left for " + nextPrayerName + ": 0:00:00");
            } else {
                countDownTimer = new CountDownTimer(diffInMillis, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long totalSecondsLeft = millisUntilFinished / 1000;
                        long hoursLeft = totalSecondsLeft / 3600;
                        long minutesLeft = (totalSecondsLeft % 3600) / 60;
                        long secondsLeft = totalSecondsLeft % 60;

                        String timeLeft = String.format(Locale.getDefault(), "%02d:%02d:%02d", hoursLeft, minutesLeft, secondsLeft);
                        tvNextPrayerTimeLeft.setText("Time Left for " + nextPrayerName + ": " + timeLeft);
                    }

                    @Override
                    public void onFinish() {
                        tvNextPrayerTimeLeft.setText("Time Left for " + nextPrayerName + ": 0:00:00");

                        // Update next prayer time and name
                        String[] prayerNames = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
                        int nextPrayerIndex = Arrays.asList(prayerNames).indexOf(nextPrayerName);
                        nextPrayerIndex = (nextPrayerIndex + 1) % prayerNames.length;
                        nextPrayerName = prayerNames[nextPrayerIndex];
                        nextPrayerTime = timings.optString(nextPrayerName);

                        // Restart countdown for the next prayer
                        startCountdown();
                    }
                };
                countDownTimer.start();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("TimeLeftError", "Error parsing time");
        }
    }










    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation(); // Permission granted, start detecting location
            } else {
                Toast.makeText(this, "Location Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            } else {
                // Check if GPS and network providers are enabled
                boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (gpsEnabled || networkEnabled) {
                    // Request location updates from both GPS and network providers
                    if (gpsEnabled) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 5F, this);
                    }
                    if (networkEnabled) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500L, 5F, this);
                    }
                } else {
                    // Handle case where neither GPS nor network providers are enabled
                    Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Remove location updates once location is received
        locationManager.removeUpdates(this);

        // Display the location
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String subAdminArea = address.getSubAdminArea();
                if (subAdminArea != null && !subAdminArea.isEmpty()) {
                    // Get latitude and longitude of SubAdminArea
                    double subAdminAreaLatitude = address.getLatitude();
                    double subAdminAreaLongitude = address.getLongitude();

                    // Now you can use subAdminAreaLatitude and subAdminAreaLongitude to fetch prayer times
                    fetchPrayerTimes(subAdminAreaLatitude, subAdminAreaLongitude);

                    // Set location text
                    String locationText = subAdminArea + ", " + address.getCountryName();
                    tvLocation.setText(locationText);
                } else {
                    tvLocation.setText("SubAdminArea not available");
                }
            } else {
                tvLocation.setText("Location not available");
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvLocation.setText("Error fetching location");
        }
    }

    // Other overridden methods of LocationListener, not needed for this implementation
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}





    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            getLocation();
        }
    }







}



