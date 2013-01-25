package net.sksi.mapstest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import net.sksi.mapstest.util.Utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    static final LatLng HAMBURG = new LatLng(53.558, 9.927);
    static final LatLng KIEL = new LatLng(53.551, 9.993);
    private GoogleMap map;

    protected LocationService locationService = null;
    protected ServiceConnection locationServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainActivity.this.locationService = ( (LocationService.LocationServiceBinder)service ).getService();
            MainActivity.this.onServiceDoneBinding();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locationService = null;
        }
    };

    protected void onServiceDoneBinding() {
        if( this.locationService != null ) {
            this.startLocationWatcherThread();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.bindService(new Intent(this, LocationService.class), this.locationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        map = ((SupportMapFragment)this.getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        Marker hamburg = map.addMarker(new MarkerOptions().position(HAMBURG).title("Hamburg"));
        Marker kiel = map.addMarker(new MarkerOptions().position(KIEL).title("Kiel").snippet("Kiel is cool").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));

        this.panMapToLocation(this.latLngToLocation(HAMBURG));
    }

    protected Location latLngToLocation(LatLng latLng) {
        Location ret = new Location("stuff");
        ret.setLatitude(latLng.latitude);
        ret.setLongitude(latLng.longitude);
        ret.setAltitude(0);
        return ret;
    }

    protected double lastLocationFudgeFactor = 10.0;
    protected Location lastLocation = null;
    protected void panMapToLocation( final Location loc ) {
        if( this.lastLocation != null ) {
            if( this.lastLocation.distanceTo( loc ) < this.lastLocationFudgeFactor ) {
                // already panned very close, leave
                return;
            }
        }
        this.lastLocation = loc;

        this.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                // Move the camera instantly to hamburg with a zoom of 15.
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 15));

                // Zoom in, animating the camera.
                map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
            }
        });
    }

    protected Map<String, Marker> markers = new LinkedHashMap<String, Marker>();
    protected void dropMarkerAtLocation( final String name, final Location loc ) {
        this.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                markers.put( name, map.addMarker(new MarkerOptions().position(new LatLng(loc.getLatitude(), loc.getLongitude())).title(name)) );
            }
        });
    }

    @Override
    protected void onPause() {
        this.stopLocationWatcherThread();
        this.unbindService( this.locationServiceConnection );

        super.onPause();
    }

    protected void startLocationWatcherThread() {
        this.isRunning = true;
        if( this.locationWatcherThread == null ) {
            this.locationWatcherThread = new LocationWatcherThread();
            this.locationWatcherThread.start();
        }
    }

    protected void stopLocationWatcherThread() {
        this.isRunning = false;
        this.locationWatcherThread.interrupt();
        this.locationWatcherThread = null;
    }

    class LocationWatcherThread extends Thread {
        @Override
        public void run() {
            while( isRunning ) {
                try {
                    Location loc = MainActivity.this.locationService.getLastBestLocation();
                    if( loc != null ) {
                        panMapToLocation( loc );
                        dropMarkerAtLocation( "currentPosition", loc );
                    }
                    Thread.sleep( 5000 );
                } catch (InterruptedException e) {
                    // probably means we should quit, the isRunning should catch it
                }
            }
        }
    }
    LocationWatcherThread locationWatcherThread = null;

    Boolean isRunning = true;

    public static final String LOGTAG = Utils.logTagPrefix + MainActivity.class.getSimpleName();
}
