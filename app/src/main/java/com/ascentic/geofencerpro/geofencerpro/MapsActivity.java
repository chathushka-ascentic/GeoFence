package com.ascentic.geofencerpro.geofencerpro;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ascentic.geofencerpro.geofencerpro.R.id.map;

public class MapsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener
 {

     private GoogleMap mMap;
     private GoogleApiClient googleApiClient;
     private Location lastLocation;
     private static String TAG="MAPACT";
     private LatLng geoFenceCoords;
     List<String> arrCoordsList;
     private static Double LONGT;
     private static Double LATTD;
     private static float RADS;
     private static String USERNAME;

     private static final int REQ_PERMISSION = 12;

     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.map_menu, menu);
         return true;
     }

     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case R.id.menu_default:
                 startLocationUpdates();
                 return true;
             case R.id.menu_geofence:
                 stopLocationUpdates();
                 gotoGeoFence();
                 return true;
             default:
                 return super.onOptionsItemSelected(item);
         }
     }

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

         Intent intent = getIntent();
         USERNAME = intent.getStringExtra("username");

        arrCoordsList = new ArrayList<String>();

        createGoogleApi();



    }

     @Override
     public void onLocationChanged(Location location) {
         writeActualLocation(location);
//         astLocation = location;
//         writeLastLocation  l();
     }

     private void gotoGeoFence(){
         LatLng newlatlong = new LatLng(LATTD,LONGT);
         markerForGeofence(newlatlong);
     }
     // Get last known location
     private void getLastKnownLocation() {
         Log.d(TAG, "getLastKnownLocation()");
         if ( checkPermission() ) {
             lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
             if ( lastLocation != null ) {
                 Log.i(TAG, "LasKnown location. " +
                         "Long: " + lastLocation.getLongitude() +
                         " | Lat: " + lastLocation.getLatitude());
                 writeLastLocation();
                 startLocationUpdates();
             } else {
                 Log.w(TAG, "No location retrieved yet");
                 startLocationUpdates();
             }
         }
         else askPermission();
     }

     private LocationRequest locationRequest;
     private Marker locationMarker;
     // Defined in mili seconds.
     // This number in extremely low, and should be used only for debug
     private final int UPDATE_INTERVAL =  1000; // 3 minutes
     private final int FASTEST_INTERVAL = 900;  // 30 secs
     private void startLocationUpdates(){
         Log.i(TAG, "startLocationUpdates()");
         locationRequest = LocationRequest.create()
                 .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                 .setInterval(UPDATE_INTERVAL)
                 .setFastestInterval(FASTEST_INTERVAL);

         if ( checkPermission() )
             LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
     }

     private void stopLocationUpdates(){
         LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
     }
     // Moving current location - update map
     private void writeActualLocation(Location location) {
         //textLat.setText( "Lat: " + location.getLatitude() );
         //textLong.setText( "Long: " + location.getLongitude() )
        // Marker
         LatLng room = new LatLng(location.getLatitude(), location.getLongitude());
         if(locationMarker!=null){
             locationMarker.remove();

         }

         locationMarker = mMap.addMarker(new MarkerOptions().position(room).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).title("BoardingRoom"));
         mMap.animateCamera(CameraUpdateFactory.newLatLng(room));

     }

     private Marker geoFenceMarker;
     // Create a marker for the geofence creation
     private void markerForGeofence(LatLng latLng) {

         LatLng company = new LatLng(latLng.latitude, latLng.longitude);
         if(geoFenceMarker!=null){
             geoFenceMarker.remove();
         }
         geoFenceMarker = mMap.addMarker(new MarkerOptions().position(company).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)).title("company"));
          mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(company,17.0f));
//
//         try {
//             Thread.sleep(1000);
//         }catch (Exception e)
//         {
//
//         }


     }

     private void writeLastLocation() {
         writeActualLocation(lastLocation);
     }

     // Check for permission to access Location
     private boolean checkPermission() {
         Log.d(TAG, "checkPermission()");
         // Ask for permission if it wasn't granted yet
         return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                 == PackageManager.PERMISSION_GRANTED );
     }

     // Asks for permission
     private void askPermission() {
         Log.d(TAG, "askPermission()");
         ActivityCompat.requestPermissions(
                 this,
                 new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                 REQ_PERMISSION
         );
     }

     @Override
     public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
         Log.d(TAG, "onRequestPermissionsResult()");
         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
         switch ( requestCode ) {
             case REQ_PERMISSION: {
                 if ( grantResults.length > 0
                         && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                     // Permission granted
                     getLastKnownLocation();

                 } else {
                     // Permission denied
                     permissionsDenied();
                 }
                 break;
             }
         }
     }

     // App cannot work without the permissions
     private void permissionsDenied() {
         Log.w(TAG, "permissionsDenied()");
     }

     @Override
     public void onConnected(@Nullable Bundle bundle) {
         getLastKnownLocation();

         Log.i(TAG, "onConnected()");
         String REQUEST_TAG = "MAPCOORDREQU";

         JsonArrayRequest jsonArrayReq = new JsonArrayRequest("http://zamzam.backupjob.cf/ascenticgeo/index.php/geofencesrest",
                 new Response.Listener<JSONArray>() {

                     @Override
                     public void onResponse(JSONArray response)  {
                         //   showProgress(false);
                         try {
                             JSONObject text = response.getJSONObject(0);
                             String lat = text.getString("latitude");
                             String longtd = text.getString("longitude");
                             String rad = text.getString("radius");

                             // GEOFENCE_RADIUS = Float.parseFloat(rad);
                             RADS = Float.parseFloat(rad);
                             LONGT = Double.parseDouble(longtd);
                             LATTD = Double.parseDouble(lat);

                             LatLng newlatlong = new LatLng(LATTD,LONGT);
                             //   Thread.sleep(1000);
                             markerForGeofence(newlatlong);
                             startGeofence();
                         }catch (Exception e)
                         {
                             VolleyLog.d(TAG, "Error: " + e.getMessage());
                         }
                         // progre
                     }
                 }, new Response.ErrorListener() {

             @Override
             public void onErrorResponse(VolleyError error) {
                 VolleyLog.d(TAG, "Error: " + error.getMessage());
                 //showProgress(false);
             }

         })
         {
             @Override
             protected Map<String, String> getParams() throws AuthFailureError {
                 Map<String, String> params = new HashMap<String, String>();
                 return params;
             }

             @Override
             public Map<String, String> getHeaders() throws AuthFailureError {
                 Map<String,String> headers = new HashMap<>();
                 // add headers <key,value>
                 String credentials = "ascenticlogin:$%password2017";
                 String auth = "Basic "
                         + Base64.encodeToString(credentials.getBytes(),
                         Base64.NO_WRAP);
                 headers.put("Authorization", auth);
                 return headers;
             }
         };

         AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonArrayReq, REQUEST_TAG);

         //GeoFenceCoordinatesRequest("http://zamzam.backupjob.cf/ascenticgeo/index.php/geofencesrest");

     }

     @Override
     public void onConnectionSuspended(int i) {
         Log.i(TAG, "onConnectionSuspended()");
     }

     @Override
     public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
         Log.i(TAG, "onConnectionFailed()");
     }

     @Override
     public void onMapClick(LatLng latLng) {

     }

     @Override
     public boolean onMarkerClick(Marker marker) {
         return false;
     }

     private void createGoogleApi() {
         Log.d(TAG, "createGoogleApi()");
         if ( googleApiClient == null ) {
             googleApiClient = new GoogleApiClient.Builder( this )
                     .addConnectionCallbacks( this )
                     .addOnConnectionFailedListener( this )
                     .addApi( LocationServices.API )
                     .build();
         }

     }

     @Override
     protected void onStart() {
         super.onStart();

         // Call GoogleApiClient connection when starting the Activity
         googleApiClient.connect();
     }

     @Override
     protected void onStop() {
         super.onStop();

         // Disconnect GoogleApiClient when stopping Activity
         googleApiClient.disconnect();
     }

     private static final long GEO_DURATION = 60 * 60 * 1000;
     private static final String GEOFENCE_REQ_ID = "My Geofence";
    // private float GEOFENCE_RADIUS = RADS; // in meters

     // Create a Geofence
     private Geofence createGeofence(LatLng latLng, float radius ) {
         Log.d(TAG, "createGeofence");
         return new Geofence.Builder()
                 .setRequestId(GEOFENCE_REQ_ID)
                 .setCircularRegion( latLng.latitude, latLng.longitude, radius)
                 .setExpirationDuration( GEO_DURATION )
                 .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                         | Geofence.GEOFENCE_TRANSITION_EXIT )
                 .build();
     }

     private GeofencingRequest createGeofenceRequest(Geofence geofence ) {
         Log.d(TAG, "createGeofenceRequest");
         return new GeofencingRequest.Builder()
                 .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                 .addGeofence( geofence )
                 .build();
     }

     private PendingIntent geoFencePendingIntent;
     private final int GEOFENCE_REQ_CODE = 0;
     private PendingIntent createGeofencePendingIntent() {
         Log.d(TAG, "createGeofencePendingIntent");
         if ( geoFencePendingIntent != null )
             return geoFencePendingIntent;

         Intent intent = new Intent( this, GeofenceTrasitionService.class);
         intent.putExtra("username",USERNAME);
         return PendingIntent.getService(
                 this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
     }

     // Add the created GeofenceRequest to the device's monitoring list
     private void addGeofence(GeofencingRequest request) {
         Log.d(TAG, "addGeofence");
         if (checkPermission())
             LocationServices.GeofencingApi.addGeofences(
                     googleApiClient,
                     request,
                     createGeofencePendingIntent()
             ).setResultCallback(new ResultCallback<Status>() {
                 @Override
                 public void onResult(Status status) {
                     if (status.isSuccess()) {
                         // Successfully registered
                        drawGeofence();
                     } else if (status.hasResolution()) {
                         // Google provides a way to fix the issue
                    /*
                    status.startResolutionForResult(
                            mContext,     // your current activity used to receive the result
                            RESULT_CODE); // the result code you'll look for in your
                    // onActivityResult method to retry registering
                    */
                     } else {
                         // No recovery. Weep softly or inform the user.
                         Log.e(TAG, "Registering failed: " + status.getStatusMessage());
                     }
                 }
             });
     }



     public static Intent makeNotificationIntent(Context geofenceService, String msg)
     {
         Log.d(TAG,"****************** "+msg);
         return new Intent(geofenceService,MapsActivity.class);
     }


     // Start Geofence creation process
     private void startGeofence() {
         Log.i(TAG, "startGeofence()");
         if( geoFenceMarker != null ) {
             Geofence geofence = createGeofence( geoFenceMarker.getPosition(), RADS );
             GeofencingRequest geofenceRequest = createGeofenceRequest( geofence );
             addGeofence( geofenceRequest );
         } else {
             Log.e(TAG, "Geofence marker is null");
         }
     }

     // Draw Geofence circle on GoogleMap
     private Circle geoFenceLimits;
     private void drawGeofence() {
         Log.d(TAG, "drawGeofence()");

         if ( geoFenceLimits != null )
             geoFenceLimits.remove();

         CircleOptions circleOptions = new CircleOptions()
                 .center( geoFenceMarker.getPosition())
                 .strokeColor(Color.argb(50, 70,70,70))
                 .fillColor( Color.argb(100, 150,150,150) )
                 .radius( RADS );
         geoFenceLimits = mMap.addCircle( circleOptions );
     }




     public void GeoFenceCoordinatesRequest(String url){

     }


     /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera

    }
}
