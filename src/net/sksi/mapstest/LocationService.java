package net.sksi.mapstest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class LocationService extends Service {
    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind( Intent intent ) {
        startService( new Intent( this, LocationService.class ) );
        return binder;
    }

    private void destroyLocationManager() {
        this.locationManager.removeUpdates( this.locationListener );
        this.locationManager = null;
    }

    private void createLocationManager() {
        if( this.locationManager != null ) {
            this.destroyLocationManager();
        }
        this.locationManager = (LocationManager)this.getSystemService( Context.LOCATION_SERVICE );
        this.locationListener = new MyLocationListener();
        this.locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, 0, 0, locationListener );
    }

    @Override
    public void onCreate() {
        this.createLocationManager();
    }


    @Override
    public void onDestroy() {
        this.destroyLocationManager();
    }


    public Location getLastBestLocation() {
        return locationListener.getLastBestLocation();
    }

    class MyLocationListener implements LocationListener {
        public void onLocationChanged( Location location ) {
            if( LocationUtils.isBetterLocation( location, this.lastBestLocation ) ) {
                Log.d( LOGTAG, "Choosing a new location that is better.  New is:  " + location.toString() );
                this.lastBestLocation = location;
            } else {
                Log.d( LOGTAG, "Discarding a new location that is not better than what I have.  New is:  " + location.toString() );
            }
        }

        public void onStatusChanged( String s, int i, Bundle bundle ) {
            Log.d( LOGTAG, "Location status changed:  " + s + " " + i );
        }

        public void onProviderEnabled( String s ) {
            Log.d( LOGTAG, "Location Provider enabled:  " + s );
        }

        public void onProviderDisabled( String s ) {
            Log.d( LOGTAG, "Location Provider disabled:  " + s );
        }

        public Location getLastBestLocation() {
            return this.lastBestLocation;
        }

        protected Location lastBestLocation = null;

    }

    LocationManager locationManager = null;
    MyLocationListener locationListener = null;

    SharedPreferences settings = null;

    final private IBinder binder = new LocationServiceBinder();

    private static final String LOGTAG = "MapsTest_LocationService";

    public static class LocationUtils {
        private static final int TWO_MINUTES = 1000 * 60 * 2;

        /**
         * Determines whether one Location reading is better than the current Location fix
         * This function and the isSameProvider below blatantly stolen/borrowed from android docs at:
         * http://developer.android.com/guide/topics/location/obtaining-user-location.html
         *
         * @param location            The new Location that you want to evaluate
         * @param currentBestLocation The current Location fix, to which you want to compare the new one
         * @return true if better, false otherwise.  duh.
         */
        public static boolean isBetterLocation( Location location, Location currentBestLocation ) {
            if( currentBestLocation == null ) {
                // A new location is always better than no location
                return true;
            }

            // Check whether the new location fix is newer or older
            long timeDelta = location.getTime() - currentBestLocation.getTime();
            boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
            boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
            boolean isNewer = timeDelta > 0;

            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if( isSignificantlyNewer ) {
                return true;
                // If the new location is more than two minutes older, it must be worse
            } else if( isSignificantlyOlder ) {
                return false;
            }

            // Check whether the new location fix is more or less accurate
            int accuracyDelta = (int)( location.getAccuracy() - currentBestLocation.getAccuracy() );
            boolean isLessAccurate = accuracyDelta > 0;
            boolean isMoreAccurate = accuracyDelta < 0;
            boolean isSignificantlyLessAccurate = accuracyDelta > 200;

            // Check if the old and new location are from the same provider
            boolean isFromSameProvider = isSameProvider( location.getProvider(), currentBestLocation.getProvider() );

            // Determine location quality using a combination of timeliness and accuracy
            if( isMoreAccurate ) {
                return true;
            } else if( isNewer && !isLessAccurate ) {
                return true;
            } else if( isNewer && !isSignificantlyLessAccurate && isFromSameProvider ) {
                return true;
            }
            return false;
        }

        /**
         * Checks whether two providers are the same
         *
         * @param provider1 first provider
         * @param provider2 second provider
         * @return true if same, false otherwise.  again, duh, but intellij is bugging me to have a @return.
         */
        public static boolean isSameProvider( String provider1, String provider2 ) {
            if( provider1 == null ) {
                return provider2 == null;
            }
            return provider1.equals( provider2 );
        }
    }

}
