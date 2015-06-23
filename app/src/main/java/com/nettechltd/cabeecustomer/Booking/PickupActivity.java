package com.nettechltd.cabeecustomer.Booking;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.easyandroidanimations.library.BounceAnimation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nettechltd.cabeecustomer.Core.PlaceAutocompleteAdapter;
import com.nettechltd.cabeecustomer.Core.SearchAddressAutocompleteAdapter;
import com.nettechltd.cabeecustomer.Core.Webservices;
import com.nettechltd.cabeecustomer.CustomAdapters.Booking;
import com.nettechltd.cabeecustomer.HomeActivity;
import com.nettechltd.cabeecustomer.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Asim on 28/5/2015.
 */
public class PickupActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener
{
    Context con = PickupActivity.this;

    @InjectView(R.id.pickup) AutoCompleteTextView pickup_at;
    @InjectView(R.id.standard) Button standard_bt;
    @InjectView(R.id.exec) Button exec_bt;
    @InjectView(R.id.luxury) Button luxury_bt;
    @InjectView(R.id.mpv) Button mpv_bt;
    @InjectView(R.id.viano) Button viano_bt;
    @InjectView(R.id.seater16) Button seater16_bt;
    @InjectView(R.id.search_bt) Button search_bt;
    @InjectView(R.id.next) FloatingActionButton next_fab;

    Button[] carTypeArray;

    private PlaceAutocompleteAdapter placeAdapter;
    private static final LatLngBounds BOUNDS_UNITED_KINGDOM = new LatLngBounds(
            new LatLng(49.435737, -6.871058), new LatLng(58.765154, 1.961949));

    Map<String, String> carTypeMap;

    String selectedCarType = "";
    String selectedPickupAddress = "";

    Location lastKnownLocation = null;
    LocationRequest locationRequest;
    GoogleMap map;
    GoogleApiClient mGoogleApiClient;

    private static final String TAG = "PickupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup);
        ButterKnife.inject(this);

        pickup_at.getBackground().setColorFilter(getResources().getColor(R.color.ColorPrimary), PorterDuff.Mode.SRC_ATOP);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        map = mapFragment.getMap();

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener()
        {
            @Override
            public void onMapLongClick(LatLng latLng)
            {
                new AddressFromLatLng().execute(latLng.latitude + "," + latLng.longitude);
            }
        });

        carTypeArray = new Button[]{standard_bt, exec_bt, luxury_bt, mpv_bt, viano_bt, seater16_bt};

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setSmallestDisplacement(0);
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API).build();

        placeAdapter = new PlaceAutocompleteAdapter(this, R.layout.dropdown_autocomplete, mGoogleApiClient, BOUNDS_UNITED_KINGDOM, null);
        pickup_at.setThreshold(7);
        pickup_at.setAdapter(placeAdapter);

        pickup_at.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if (s.toString().length() >= 6 && s.toString().length() <= 7)
                {
                    new BounceAnimation(search_bt).setDuration(300).animate();
                }
            }
        });

        pickup_at.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                /*PlaceAutocompleteAdapter.PlaceAutocomplete place = (PlaceAutocompleteAdapter.PlaceAutocomplete) parent.getSelectedItem();
                Log.d(TAG, place.getPlaceTypes().toString());*/
                selectedPickupAddress = ((TextView) view).getText().toString();
                String pickupWithoutSpaces = selectedPickupAddress.replace(" ", "%20");
                new LatLngFromAddress().execute(pickupWithoutSpaces);
            }
        });

        carTypeMap = new HashMap<>();
        carTypeMap.put(standard_bt.getTag().toString(), "Std");
        carTypeMap.put(exec_bt.getTag().toString(), "Exec");
        carTypeMap.put(luxury_bt.getTag().toString(), "Lux");
        carTypeMap.put(mpv_bt.getTag().toString(), "Mpv");
        carTypeMap.put(viano_bt.getTag().toString(), "Viano");
        carTypeMap.put(seater16_bt.getTag().toString(), "Minibus");

        standard_bt.setOnClickListener(this);
        exec_bt.setOnClickListener(this);
        luxury_bt.setOnClickListener(this);
        mpv_bt.setOnClickListener(this);
        viano_bt.setOnClickListener(this);
        seater16_bt.setOnClickListener(this);

        search_bt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!pickup_at.getText().toString().equalsIgnoreCase(""))
                {
                    new SearchAddress().execute(pickup_at.getText().toString());
                }
            }
        });

        next_fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(selectedPickupAddress.equalsIgnoreCase(""))
                {
                    final Snackbar snack = Snackbar.make(findViewById(R.id.root_layout), "Please select a Pickup Address", Snackbar.LENGTH_LONG);
                    snack.setAction("Dismiss", new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            snack.dismiss();
                        }
                    });
                    snack.show();
                }
                else if(selectedCarType.equalsIgnoreCase(""))
                {
                    final Snackbar snack = Snackbar.make(findViewById(R.id.root_layout), "Please select Vehicle type", Snackbar.LENGTH_LONG);
                    snack.setAction("Dismiss", new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            snack.dismiss();
                        }
                    });
                    snack.show();
                }
                else
                {
                    Booking b = new Booking();
                    b.setPickup(selectedPickupAddress);
                    b.setCartype(selectedCarType);

                    startActivity(new Intent(con, DestinationActivity.class).putExtra("booking", b));
                    finish();
                }
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (mGoogleApiClient != null)
        {
            mGoogleApiClient.connect();
        }
    }

    private void moveMap()
    {
        LatLng location = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));

        map.addMarker(new MarkerOptions()
                .title("You")
                .position(location));

        Log.d("Map function: ", "Moved map");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.d("Autocomplete Error: ", ""+connectionResult.getErrorCode());
    }

    @Override
    public void onClick(View v)
    {
        PorterDuff.Mode m = PorterDuff.Mode.SRC_ATOP;
        Button b = (Button) v;

        for(int i=0; i<6; i++)
        {
            Button current = carTypeArray[i];

            Drawable currentDrawable = current.getCompoundDrawables()[0]; // Left drawable corresponds to the 0th element of the returned array
            currentDrawable.setColorFilter(con.getResources().getColor(R.color.car_default_color), m);
            currentDrawable.setBounds(currentDrawable.copyBounds());

            current.setCompoundDrawables(currentDrawable, null, null, null);
            current.setTextColor(getResources().getColor(R.color.text_dark_primary));
            current.setTypeface(null, Typeface.NORMAL);
        }

        Drawable d = b.getCompoundDrawables()[0];
        d.setColorFilter(con.getResources().getColor(R.color.ColorAccent), m);
        d.setBounds(d.copyBounds());

        b.setCompoundDrawables(d, null, null, null);
        b.setTextColor(getResources().getColor(R.color.ColorAccent));
        b.setTypeface(null, Typeface.BOLD);

        selectedCarType = carTypeMap.get(b.getTag().toString());
    }

    private int getButtonImageId(String tag)
    {
        int id = 0;

        switch(tag)
        {
            case("Std"):
                return R.drawable.standard;

            case("Exec"):
                return R.drawable.executive;

            case("Lux"):
                return R.drawable.luxury;

            case("Mpv"):
                return R.drawable.mpv;

            case("Viano"):
                return R.drawable.viano;

            case("16 Seater"):
                return R.drawable.minibus;

            default:
                return R.drawable.standard;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.pickup, menu);
        getSupportActionBar().setTitle("Book A Car");
        getSupportActionBar().setSubtitle("Pickup");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch ((id))
        {
            case R.id.info:
                // show help
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        googleMap.setMyLocationEnabled(false);

        if(lastKnownLocation != null)
        {
            moveMap();
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG, "GMS Client connected");
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(lastKnownLocation == null)
        {
            Log.d(TAG, "Requesting location updates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        }
        else
        {
            moveMap();
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        lastKnownLocation = location;
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        moveMap();
    }

    private class SearchAddress extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            return new Webservices(con).SearchAddress(params[0]);
        }

        @Override
        protected void onPostExecute(String result)
        {
            if(!result.equalsIgnoreCase("ServiceError"))
            {
                ArrayList<String> addressList = new ArrayList<String>(Arrays.asList(result.split(";")));
                pickup_at.setAdapter(new SearchAddressAutocompleteAdapter(con, R.layout.dropdown_autocomplete, addressList));
                pickup_at.showDropDown();
            }
        }
    }

    private class AddressFromLatLng extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            return (getAddressFromLatLng(params[0]) + "::" + params[0]);
        }

        @Override
        protected void onPostExecute(String result)
        {
            String address = result.split("::")[0];
            String latLng = result.split("::")[1];
            if(!address.equalsIgnoreCase(""))
            {
                map.clear();
                map.addMarker(new MarkerOptions()
                        .title("Pickup")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .snippet(address)
                        .position(new LatLng(Double.parseDouble(latLng.split(",")[0]), Double.parseDouble(latLng.split(",")[1]))));

                selectedPickupAddress = address;
                pickup_at.setText(address);
                pickup_at.dismissDropDown();
            }
        }
    }

    public String getAddressFromLatLng(String latLng)
    {
        String address = "";
        String sUrl = "http://maps.google.com/maps/api/geocode/json?latlng=" +
                latLng + "&sensor=false";

        URL url = null;
        try
        {
            url = new URL(sUrl);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        URLConnection ucon = null;

        try
        {
            ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            String line = "";
            String result = "";
            while ((line = bufferedReader.readLine()) != null)
            {
                result += line;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject = new JSONObject(result);

            address = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                    .getString("formatted_address");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return address;
    }

    private class LatLngFromAddress extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            return (getLatLngFromAddress(params[0]) + "::" + params[0]);
        }

        @Override
        protected void onPostExecute(String result)
        {
            String latLng = result.split("::")[0];
            String address = result.split("::")[1].replace("%20", " ");
            if(!latLng.equalsIgnoreCase(""))
            {
                map.clear();
                map.addMarker(new MarkerOptions()
                        .title("Pickup")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .snippet(address)
                        .position(new LatLng(Double.parseDouble(latLng.split(",")[0]), Double.parseDouble(latLng.split(",")[1]))));

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(latLng.split(",")[0]), Double.parseDouble(latLng.split(",")[1])), 13));
            }
        }
    }

    public String getLatLngFromAddress(String address)
    {
        String latLng = "";
        String sUrl = "http://maps.google.com/maps/api/geocode/json?address=" +
                address + "&sensor=false";

        URL url = null;
        try
        {
            url = new URL(sUrl);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        URLConnection ucon = null;

        try
        {
            ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            String line = "";
            String result = "";
            while ((line = bufferedReader.readLine()) != null)
            {
                result += line;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject = new JSONObject(result);

            double lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lat");

            double lng = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lng");

            latLng = lat + "," + lng;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return latLng;
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
     public void onBackPressed()
    {
        startActivity(new Intent(con, HomeActivity.class));
        finish();
    }
}
