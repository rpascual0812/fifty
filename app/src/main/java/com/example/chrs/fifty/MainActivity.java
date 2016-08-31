package com.example.chrs.fifty;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, LoaderManager.LoaderCallbacks<Cursor> {
    Bitmap b;
    LocationManager locationManager;
    private GoogleMap mMap;
    LatLng center;
    Location location;
    ConnectionDetector cd;
    SharedPreferences sharedPreferences;
    int locationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        b = BitmapFactory.decodeResource(getResources(), R.drawable.person);
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        cd=new ConnectionDetector(getApplicationContext());
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        center = new LatLng(location.getLatitude(), location.getLongitude());
        if(cd.isConnectingToInternet()) {
        }

        else {
            ShowAlertNoInternet();
        }

    }

    public void onSearch(View view) {
        EditText location_tf = (EditText) findViewById(R.id.Search);
        String location = location_tf.getText().toString();
        List<Address> addressList = null;
        int n = 0, m = 1;
        if (location != null || location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(n);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            n++;
        }
        if (location == null || location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(m);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            m++;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        CameraPosition cameraPosition = new CameraPosition.Builder().target(center).zoom(17).bearing(90).tilt(30).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        //mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("I'm Here!!").icon(BitmapDescriptorFactory.fromBitmap(b)));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                locationCount++;
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Place your mark here");
                final EditText input = new EditText(MainActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
                alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Write your code here to execute after dialog
                                Toast.makeText(getApplicationContext(),"Mark Placed", Toast.LENGTH_SHORT).show();
                            }
                        });
                alertDialog.setNegativeButton("NO",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Write your code here to execute after dialog
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
                drawMarker(latLng); // Drawing marker on the map
                ContentValues contentValues = new ContentValues(); // Creating an instance of ContentValues
                contentValues.put(LocationsDB.FIELD_LAT, latLng.latitude ); // Setting latitude in ContentValues
                contentValues.put(LocationsDB.FIELD_LNG, latLng.longitude); // Setting longitude in ContentValues
                contentValues.put(LocationsDB.FIELD_ZOOM, mMap.getCameraPosition().zoom); // Setting zoom in ContentValues
                LocationInsertTask insertTask = new LocationInsertTask(); // Creating an instance of LocationInsertTask
                insertTask.execute(contentValues); // Storing the latitude, longitude and zoom level to SQLite database
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("lat"+ Integer.toString((locationCount-1)), Double.toString(latLng.latitude)); // Storing the latitude for the i-th location
                editor.putString("lng"+ Integer.toString((locationCount-1)), Double.toString(latLng.longitude));  // Storing the longitude for the i-th location
                editor.putInt("locationCount", locationCount); // Storing the count of locations or marker count
                editor.putString("zoom", Float.toString(mMap.getCameraPosition().zoom)); /** Storing the zoom level to the shared preferences */
                editor.commit(); /** Saving the values stored in the shared preferences */
            }
        });
        sharedPreferences = getSharedPreferences("location", 0);
        locationCount = sharedPreferences.getInt("locationCount", 0); // Getting number of locations already stored
        String zoom = sharedPreferences.getString("zoom", "0"); // Getting stored zoom level if exists else return 0
        if(locationCount!=0){ // If locations are already saved
            String lat = "";
            String lng = "";
            for(int i=0;i<locationCount;i++){ // Iterating through all the locations stored
                lat = sharedPreferences.getString("lat"+i,"0"); // Getting the latitude of the i-th location
                lng = sharedPreferences.getString("lng"+i,"0"); // Getting the longitude of the i-th location
                drawMarker(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng))); // Drawing marker on the map
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)))); // Moving CameraPosition to last clicked position
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(Float.parseFloat(zoom))); // Setting the zoom level in the map on last position  is clicked
        }
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                mMap.clear(); // Removing the marker and circle from the Google Map
                SharedPreferences.Editor editor = sharedPreferences.edit(); // Opening the editor object to delete data from sharedPreferences
                editor.clear(); // Clearing the editor
                editor.commit(); // Committing the changes
                locationCount=0; // Setting locationCount to zero
            }
        });
    }

    public void ShowAlertNoInternet(){
        AlertDialog.Builder alertdialogbuilder=new AlertDialog.Builder(this);
        alertdialogbuilder.setTitle("Alert!");
        alertdialogbuilder.setMessage("Please check if your connected to the internet?");
        alertdialogbuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                minimizeApp();
            }
        });
        AlertDialog alertDialog=alertdialogbuilder.create();
        alertDialog.show();
    }

    private void drawMarker(LatLng point){
        MarkerOptions markerOptions = new MarkerOptions(); // Creating an instance of MarkerOptions
        markerOptions.position(point); // Setting latitude and longitude for the marker
        mMap.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))); // Adding marker on the Google Map
    }

    private class LocationInsertTask extends AsyncTask<ContentValues, Void, Void>{
        @Override
        protected Void doInBackground(ContentValues... contentValues) {
            getContentResolver().insert(LocationsContentProvider.CONTENT_URI, contentValues[0]); /** Setting up values to insert the clicked location into SQLite database */
            return null;
        }
    }

    private class LocationDeleteTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            getContentResolver().delete(LocationsContentProvider.CONTENT_URI, null, null);  /** Deleting all the locations stored in SQLite database */
            return null;
        }
    }


    public void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = LocationsContentProvider.CONTENT_URI; // Uri to the content provider LocationsContentProvider
        return new CursorLoader(this, uri, null, null, null, null); // Fetches all the rows from locations table
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor arg1) {
        int locationCount = 0;
        double lat = 0;
        double lng = 0;
        float zoom = 0;
        locationCount = arg1.getCount(); // Number of locations available in the SQLite database table
        arg1.moveToFirst(); // Move the current record pointer to the first row of the table
        for (int i = 0; i < locationCount; i++) {
            lat = arg1.getDouble(arg1.getColumnIndex(LocationsDB.FIELD_LAT)); // Get the latitude
            lng = arg1.getDouble(arg1.getColumnIndex(LocationsDB.FIELD_LNG)); // Get the longitude
            zoom = arg1.getFloat(arg1.getColumnIndex(LocationsDB.FIELD_ZOOM)); // Get the zoom level
            LatLng location = new LatLng(lat, lng); // Creating an instance of LatLng to plot the location in Google Maps
            drawMarker(location); // Drawing the marker in the Google Maps
            arg1.moveToNext(); // Traverse the pointer to the next row
        }if(locationCount>0){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat,lng))); // Moving CameraPosition to last clicked position
            mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom)); // Setting the zoom level in the map on last position  is clicked
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
