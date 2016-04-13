package dk.lemonmelon.pooetry

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log

class LocationLoader(context: Context) {
    var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun triggerLoadingOfLocation() {
        var criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE

        locationManager.requestSingleUpdate(criteria, object: LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.i("Pooetry location", "Loaded a location!")
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }, null)
    }
}
