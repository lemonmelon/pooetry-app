package dk.lemonmelon.pooetry

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log

class LocationLoader(context: Context) {
    private var waitingForLocation = false
    private var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    init {
        if(location == null && !waitingForLocation) {
            triggerLoadingOfLocation()
        }
    }

    fun triggerLoadingOfLocation() {
        if(waitingForLocation) {
            return
        }
        waitingForLocation = true

        var criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE

        locationManager.requestSingleUpdate(criteria, object: LocationListener {
            override fun onLocationChanged(newLocation: Location) {
                location = newLocation
                waitingForLocation = false
                Log.i("Pooetry location", "Got a new location")
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }, null)
    }
}
