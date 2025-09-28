/*
 *    Copyright 2025 The Cardinal Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package earth.maps.cardinal.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling location services.
 * Provides a centralized way to access current location across the app.
 */
@Singleton
class LocationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appPreferenceRepository: AppPreferenceRepository
) {

    private companion object {
        private const val LOCATION_REQUEST_INTERVAL_MS = 15000L // 15 seconds
        private const val LOCATION_REQUEST_TIMEOUT_MS = 10000L // 10 seconds
        private const val CONTINUOUS_LOCATION_UPDATE_INTERVAL_MS = 5000L // 5 seconds
        private const val CONTINUOUS_LOCATION_UPDATE_DISTANCE_M = 5f // 5 meters
    }

    // Location caching with thread safety
    private var lastRequestedLocation: Location? = null
    private val locationMutex = Mutex()
    private val locationLock = Any()

    // State flows for UI components
    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating

    private val _locationFlow: MutableStateFlow<Location?> = MutableStateFlow(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    // Location listener for continuous updates
    private var locationListener: LocationListener? = null

    /**
     * Gets the current location, either from cache or by requesting a fresh one.
     * Returns null if location cannot be determined.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        _isLocating.value = true
        try {
            val currentTime = System.currentTimeMillis()

            // Return cached location if it's recent
            val cachedLocation = locationMutex.withLock {
                lastRequestedLocation
            }

            cachedLocation?.let { location ->
                if (isLocationRecent(location, currentTime)) {
                    return location
                }
            }

            return try {
                val locationManager = getLocationManager(context)

                // Try to get last known location from any provider
                val lastKnownLocation = getLastKnownLocation(locationManager)

                // If we have a recent last known location, use it
                lastKnownLocation?.let { location ->
                    if (isLocationRecent(location, currentTime)) {
                        locationMutex.withLock {
                            lastRequestedLocation = location
                        }
                        return location
                    }
                }

                // If no recent location available, request a fresh location
                requestFreshLocation(locationManager)
            } catch (_: Exception) {
                // Handle exceptions during location fetching
                null
            }
        } finally {
            _isLocating.value = false
        }
    }

    fun fromNameAndLatLng(name: String?, latLng: LatLng): Place {
        return Place(
            name = name ?: context.getString(R.string.unnamed_location),
            latLng = latLng,
        )
    }

    /**
     * Starts continuous location updates.
     * This should be called from the UI when the map is ready.
     */
    @SuppressLint("MissingPermission")
    fun startContinuousLocationUpdates(context: Context) {
        // Check if continuous location tracking is disabled
        if (!appPreferenceRepository.continuousLocationTracking.value) {
            return
        }

        try {
            val locationManager = getLocationManager(context)

            @Suppress("DEPRECATION") val bestProvider = locationManager.getBestProvider(
                android.location.Criteria().apply {
                    accuracy = android.location.Criteria.ACCURACY_FINE
                }, true
            )

            bestProvider?.let { provider ->
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        // Update the location flow with the new location
                        _locationFlow.value = location
                        synchronized(locationLock) {
                            lastRequestedLocation = location
                        }
                    }

                    override fun onProviderDisabled(provider: String) {
                        // Stop location updates if provider is disabled
                        try {
                            locationManager.removeUpdates(this)
                        } catch (_: Exception) {
                            // Ignore exceptions during cleanup
                        }
                        locationListener = null
                        _locationFlow.value = null
                    }

                    override fun onProviderEnabled(provider: String) {}

                    override fun onStatusChanged(
                        provider: String?,
                        status: Int,
                        extras: Bundle?
                    ) {
                    }
                }

                locationManager.requestLocationUpdates(
                    provider,
                    CONTINUOUS_LOCATION_UPDATE_INTERVAL_MS,
                    CONTINUOUS_LOCATION_UPDATE_DISTANCE_M,
                    locationListener!!
                )
            }
        } catch (_: Exception) {
            // Handle exceptions during setup
            locationListener = null
            _locationFlow.value = null
        }
    }

    /**
     * Stops location updates.
     */
    fun stopLocationUpdates(context: Context) {
        locationListener?.let { listener ->
            try {
                val locationManager = getLocationManager(context)
                locationManager.removeUpdates(listener)
            } catch (_: Exception) {
                // Ignore exceptions during cleanup
            }
            locationListener = null
        }
    }

    /**
     * Checks if a location is recent (less than LOCATION_REQUEST_INTERVAL_MS old).
     */
    private fun isLocationRecent(location: Location, currentTime: Long): Boolean {
        return currentTime - location.time < LOCATION_REQUEST_INTERVAL_MS
    }

    /**
     * Gets the LocationManager system service.
     */
    private fun getLocationManager(context: Context): LocationManager {
        return try {
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        } catch (e: Exception) {
            // Handle exceptions during service retrieval
            throw IllegalStateException("Failed to get LocationManager", e)
        }
    }

    /**
     * Gets the most recent last known location from all available providers.
     */
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        return try {
            var lastKnownLocation: Location? = null
            val providers = locationManager.getProviders(true)

            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && (lastKnownLocation == null || location.time > lastKnownLocation.time)) {
                    lastKnownLocation = location
                }
            }

            lastKnownLocation
        } catch (_: Exception) {
            // Handle exceptions during location retrieval
            null
        }
    }

    /**
     * Requests a fresh location from the location manager.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(locationManager: LocationManager): Location? {
        return withContext(Dispatchers.Main) {
            val locationDeferred = CompletableDeferred<Location?>()

            val locationListener = createLocationListener(locationManager, locationDeferred)

            try {
                // Request location updates from the best provider
                @Suppress("DEPRECATION") val bestProvider = locationManager.getBestProvider(
                    android.location.Criteria().apply {
                        accuracy = android.location.Criteria.ACCURACY_FINE
                    }, true
                )

                bestProvider?.let { provider ->
                    locationManager.requestLocationUpdates(
                        provider,
                        0, // min time in ms
                        0f, // min distance in meters
                        locationListener
                    )

                    // Set a timeout for location request
                    setupLocationRequestTimeout(locationManager, locationListener, locationDeferred)
                } ?: run {
                    // If no provider is available, complete immediately
                    locationDeferred.complete(null)
                }

                // Wait for location or timeout
                val location = locationDeferred.await()
                location?.let {
                    synchronized(locationLock) {
                        lastRequestedLocation = it
                    }
                }
                location
            } catch (_: Exception) {
                // Ensure cleanup in case of exceptions
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (_: Exception) {
                    // Ignore cleanup exceptions
                }
                locationDeferred.complete(null)
                null
            }
        }
    }

    /**
     * Creates a location listener that handles location updates and provider events.
     */
    private fun createLocationListener(
        locationManager: LocationManager,
        locationDeferred: CompletableDeferred<Location?>
    ): LocationListener {
        return object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Remove updates to avoid continuous location requests
                try {
                    locationManager.removeUpdates(this)
                } catch (_: Exception) {
                    // Ignore exceptions during cleanup
                }

                // Complete the deferred with the location
                if (locationDeferred.isActive) {
                    synchronized(locationLock) {
                        lastRequestedLocation = location
                    }
                    locationDeferred.complete(location)
                }
            }

            override fun onProviderDisabled(provider: String) {
                try {
                    locationManager.removeUpdates(this)
                } catch (_: Exception) {
                    // Ignore exceptions during cleanup
                }

                if (locationDeferred.isActive) {
                    locationDeferred.complete(null)
                }
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
    }

    /**
     * Sets up a timeout for location requests to prevent hanging.
     */
    private fun setupLocationRequestTimeout(
        locationManager: LocationManager,
        locationListener: LocationListener,
        locationDeferred: CompletableDeferred<Location?>
    ) {
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(LOCATION_REQUEST_TIMEOUT_MS)
            if (locationDeferred.isActive) {
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (_: Exception) {
                    // Ignore exceptions during cleanup
                }
                locationDeferred.complete(null)
            }
        }
    }

    /**
     * Creates a standardized "My Location" Place object.
     * This centralizes the creation of "My Location" places to ensure consistency.
     */
    fun createMyLocationPlace(latLng: LatLng): Place {
        return Place(
            name = context.getString(R.string.my_location),
            description = "Current location",
            icon = "location",
            latLng = latLng,
            isMyLocation = true
        )
    }

    fun createSearchResultPlace(result: GeocodeResult): Place {
        return Place(
            name = result.displayName,
            description = mapOsmTagsToDescription(result.properties),
            icon = "search",
            latLng = LatLng(
                latitude = result.latitude,
                longitude = result.longitude,
            ),
            address = result.address
        )
    }

    fun mapOsmTagsToDescription(properties: Map<String, String>): String {
        Log.d("LocationRepository", "$properties")
        // Check for common amenity tags first
        properties["amenity"]?.let { amenity ->
            return when (amenity) {
                "bar" -> context.getString(R.string.osm_amenity_bar)
                "biergarten" -> context.getString(R.string.osm_amenity_biergarten)
                "cafe" -> context.getString(R.string.osm_amenity_cafe)
                "fast_food" -> context.getString(R.string.osm_amenity_fast_food)
                "food_court" -> context.getString(R.string.osm_amenity_food_court)
                "ice_cream" -> context.getString(R.string.osm_amenity_ice_cream)
                "pub" -> context.getString(R.string.osm_amenity_pub)
                "restaurant" -> context.getString(R.string.osm_amenity_restaurant)
                "college" -> context.getString(R.string.osm_amenity_college)
                "dancing_school" -> context.getString(R.string.osm_amenity_dancing_school)
                "driving_school" -> context.getString(R.string.osm_amenity_driving_school)
                "first_aid_school" -> context.getString(R.string.osm_amenity_first_aid_school)
                "kindergarten" -> context.getString(R.string.osm_amenity_kindergarten)
                "language_school" -> context.getString(R.string.osm_amenity_language_school)
                "library" -> context.getString(R.string.osm_amenity_library)
                "surf_school" -> context.getString(R.string.osm_amenity_surf_school)
                "toy_library" -> context.getString(R.string.osm_amenity_toy_library)
                "research_institute" -> context.getString(R.string.osm_amenity_research_institute)
                "training" -> context.getString(R.string.osm_amenity_training)
                "music_school" -> context.getString(R.string.osm_amenity_music_school)
                "school" -> context.getString(R.string.osm_amenity_school)
                "traffic_park" -> context.getString(R.string.osm_amenity_traffic_park)
                "university" -> context.getString(R.string.osm_amenity_university)
                "bicycle_parking" -> context.getString(R.string.osm_amenity_bicycle_parking)
                "bicycle_repair_station" -> context.getString(R.string.osm_amenity_bicycle_repair_station)
                "bicycle_rental" -> context.getString(R.string.osm_amenity_bicycle_rental)
                "bicycle_wash" -> context.getString(R.string.osm_amenity_bicycle_wash)
                "boat_rental" -> context.getString(R.string.osm_amenity_boat_rental)
                "boat_sharing" -> context.getString(R.string.osm_amenity_boat_sharing)
                "bus_station" -> context.getString(R.string.osm_amenity_bus_station)
                "car_rental" -> context.getString(R.string.osm_amenity_car_rental)
                "car_sharing" -> context.getString(R.string.osm_amenity_car_sharing)
                "car_wash" -> context.getString(R.string.osm_amenity_car_wash)
                "compressed_air" -> context.getString(R.string.osm_amenity_compressed_air)
                "vehicle_inspection" -> context.getString(R.string.osm_amenity_vehicle_inspection)
                "charging_station" -> context.getString(R.string.osm_amenity_charging_station)
                "driver_training" -> context.getString(R.string.osm_amenity_driver_training)
                "ferry_terminal" -> context.getString(R.string.osm_amenity_ferry_terminal)
                "fuel" -> context.getString(R.string.osm_amenity_fuel)
                "grit_bin" -> context.getString(R.string.osm_amenity_grit_bin)
                "motorcycle_parking" -> context.getString(R.string.osm_amenity_motorcycle_parking)
                "parking" -> context.getString(R.string.osm_amenity_parking)
                "parking_entrance" -> context.getString(R.string.osm_amenity_parking_entrance)
                "parking_space" -> context.getString(R.string.osm_amenity_parking_space)
                "taxi" -> context.getString(R.string.osm_amenity_taxi)
                "weighbridge" -> context.getString(R.string.osm_amenity_weighbridge)
                "atm" -> context.getString(R.string.osm_amenity_atm)
                "payment_terminal" -> context.getString(R.string.osm_amenity_payment_terminal)
                "bank" -> context.getString(R.string.osm_amenity_bank)
                "bureau_de_change" -> context.getString(R.string.osm_amenity_bureau_de_change)
                "money_transfer" -> context.getString(R.string.osm_amenity_money_transfer)
                "payment_centre" -> context.getString(R.string.osm_amenity_payment_centre)
                "baby_hatch" -> context.getString(R.string.osm_amenity_baby_hatch)
                "clinic" -> context.getString(R.string.osm_amenity_clinic)
                "dentist" -> context.getString(R.string.osm_amenity_dentist)
                "doctors" -> context.getString(R.string.osm_amenity_doctors)
                "hospital" -> context.getString(R.string.osm_amenity_hospital)
                "nursing_home" -> context.getString(R.string.osm_amenity_nursing_home)
                "pharmacy" -> context.getString(R.string.osm_amenity_pharmacy)
                "social_facility" -> context.getString(R.string.osm_amenity_social_facility)
                "veterinary" -> context.getString(R.string.osm_amenity_veterinary)
                "arts_centre" -> context.getString(R.string.osm_amenity_arts_centre)
                "brothel" -> context.getString(R.string.osm_amenity_brothel)
                "casino" -> context.getString(R.string.osm_amenity_casino)
                "cinema" -> context.getString(R.string.osm_amenity_cinema)
                "community_centre" -> context.getString(R.string.osm_amenity_community_centre)
                "conference_centre" -> context.getString(R.string.osm_amenity_conference_centre)
                "events_venue" -> context.getString(R.string.osm_amenity_events_venue)
                "exhibition_centre" -> context.getString(R.string.osm_amenity_exhibition_centre)
                "fountain" -> context.getString(R.string.osm_amenity_fountain)
                "gambling" -> context.getString(R.string.osm_amenity_gambling)
                "love_hotel" -> context.getString(R.string.osm_amenity_love_hotel)
                "music_venue" -> context.getString(R.string.osm_amenity_music_venue)
                "nightclub" -> context.getString(R.string.osm_amenity_nightclub)
                "planetarium" -> context.getString(R.string.osm_amenity_planetarium)
                "public_bookcase" -> context.getString(R.string.osm_amenity_public_bookcase)
                "social_centre" -> context.getString(R.string.osm_amenity_social_centre)
                "stage" -> context.getString(R.string.osm_amenity_stage)
                "stripclub" -> context.getString(R.string.osm_amenity_stripclub)
                "studio" -> context.getString(R.string.osm_amenity_studio)
                "swingerclub" -> context.getString(R.string.osm_amenity_swingerclub)
                "theatre" -> context.getString(R.string.osm_amenity_theatre)
                "courthouse" -> context.getString(R.string.osm_amenity_courthouse)
                "fire_station" -> context.getString(R.string.osm_amenity_fire_station)
                "police" -> context.getString(R.string.osm_amenity_police)
                "post_box" -> context.getString(R.string.osm_amenity_post_box)
                "post_depot" -> context.getString(R.string.osm_amenity_post_depot)
                "post_office" -> context.getString(R.string.osm_amenity_post_office)
                "prison" -> context.getString(R.string.osm_amenity_prison)
                "ranger_station" -> context.getString(R.string.osm_amenity_ranger_station)
                "townhall" -> context.getString(R.string.osm_amenity_townhall)
                "bbq" -> context.getString(R.string.osm_amenity_bbq)
                "bench" -> context.getString(R.string.osm_amenity_bench)
                "dog_toilet" -> context.getString(R.string.osm_amenity_dog_toilet)
                "dressing_room" -> context.getString(R.string.osm_amenity_dressing_room)
                "drinking_water" -> context.getString(R.string.osm_amenity_drinking_water)
                "give_box" -> context.getString(R.string.osm_amenity_give_box)
                "lounge" -> context.getString(R.string.osm_amenity_lounge)
                "mailroom" -> context.getString(R.string.osm_amenity_mailroom)
                "parcel_locker" -> context.getString(R.string.osm_amenity_parcel_locker)
                "shelter" -> context.getString(R.string.osm_amenity_shelter)
                "shower" -> context.getString(R.string.osm_amenity_shower)
                "telephone" -> context.getString(R.string.osm_amenity_telephone)
                "toilets" -> context.getString(R.string.osm_amenity_toilets)
                "water_point" -> context.getString(R.string.osm_amenity_water_point)
                "watering_place" -> context.getString(R.string.osm_amenity_watering_place)
                "sanitary_dump_station" -> context.getString(R.string.osm_amenity_sanitary_dump_station)
                "recycling" -> context.getString(R.string.osm_amenity_recycling)
                "waste_basket" -> context.getString(R.string.osm_amenity_waste_basket)
                "waste_disposal" -> context.getString(R.string.osm_amenity_waste_disposal)
                "waste_transfer_station" -> context.getString(R.string.osm_amenity_waste_transfer_station)
                "animal_boarding" -> context.getString(R.string.osm_amenity_animal_boarding)
                "animal_breeding" -> context.getString(R.string.osm_amenity_animal_breeding)
                "animal_shelter" -> context.getString(R.string.osm_amenity_animal_shelter)
                "animal_training" -> context.getString(R.string.osm_amenity_animal_training)
                "baking_oven" -> context.getString(R.string.osm_amenity_baking_oven)
                "clock" -> context.getString(R.string.osm_amenity_clock)
                "crematorium" -> context.getString(R.string.osm_amenity_crematorium)
                "dive_centre" -> context.getString(R.string.osm_amenity_dive_centre)
                "funeral_hall" -> context.getString(R.string.osm_amenity_funeral_hall)
                "grave_yard" -> context.getString(R.string.osm_amenity_grave_yard)
                "hunting_stand" -> context.getString(R.string.osm_amenity_hunting_stand)
                "internet_cafe" -> context.getString(R.string.osm_amenity_internet_cafe)
                "kitchen" -> context.getString(R.string.osm_amenity_kitchen)
                "lounger" -> context.getString(R.string.osm_amenity_lounger)
                "marketplace" -> context.getString(R.string.osm_amenity_marketplace)
                "monastery" -> context.getString(R.string.osm_amenity_monastery)
                "mortuary" -> context.getString(R.string.osm_amenity_mortuary)
                "photo_booth" -> context.getString(R.string.osm_amenity_photo_booth)
                "place_of_mourning" -> context.getString(R.string.osm_amenity_place_of_mourning)
                "place_of_worship" -> context.getString(R.string.osm_amenity_place_of_worship)
                "public_bath" -> context.getString(R.string.osm_amenity_public_bath)
                "public_building" -> context.getString(R.string.osm_amenity_public_building)
                "refugee_site" -> context.getString(R.string.osm_amenity_refugee_site)
                "vending_machine" -> context.getString(R.string.osm_amenity_vending_machine)
                else -> "amenity=$amenity"
            }
        }

        // Check for other common tags
        properties["shop"]?.let { shop ->
            return when (shop) {
                "alcohol" -> context.getString(R.string.osm_shop_alcohol)
                "bakery" -> context.getString(R.string.osm_shop_bakery)
                "beverages" -> context.getString(R.string.osm_shop_beverages)
                "brewing_supplies" -> context.getString(R.string.osm_shop_brewing_supplies)
                "butcher" -> context.getString(R.string.osm_shop_butcher)
                "cheese" -> context.getString(R.string.osm_shop_cheese)
                "chocolate" -> context.getString(R.string.osm_shop_chocolate)
                "coffee" -> context.getString(R.string.osm_shop_coffee)
                "confectionery" -> context.getString(R.string.osm_shop_confectionery)
                "convenience" -> context.getString(R.string.osm_shop_convenience)
                "dairy" -> context.getString(R.string.osm_shop_dairy)
                "deli" -> context.getString(R.string.osm_shop_deli)
                "farm" -> context.getString(R.string.osm_shop_farm)
                "food" -> context.getString(R.string.osm_shop_food)
                "frozen_food" -> context.getString(R.string.osm_shop_frozen_food)
                "greengrocer" -> context.getString(R.string.osm_shop_greengrocer)
                "health_food" -> context.getString(R.string.osm_shop_health_food)
                "ice_cream" -> context.getString(R.string.osm_shop_ice_cream)
                "nuts" -> context.getString(R.string.osm_shop_nuts)
                "pasta" -> context.getString(R.string.osm_shop_pasta)
                "pastry" -> context.getString(R.string.osm_shop_pastry)
                "seafood" -> context.getString(R.string.osm_shop_seafood)
                "spices" -> context.getString(R.string.osm_shop_spices)
                "tea" -> context.getString(R.string.osm_shop_tea)
                "tortilla" -> context.getString(R.string.osm_shop_tortilla)
                "water" -> context.getString(R.string.osm_shop_water)
                "wine" -> context.getString(R.string.osm_shop_wine)
                "department_store" -> context.getString(R.string.osm_shop_department_store)
                "general" -> context.getString(R.string.osm_shop_general)
                "kiosk" -> context.getString(R.string.osm_shop_kiosk)
                "mall" -> context.getString(R.string.osm_shop_mall)
                "supermarket" -> context.getString(R.string.osm_shop_supermarket)
                "wholesale" -> context.getString(R.string.osm_shop_wholesale)
                "baby_goods" -> context.getString(R.string.osm_shop_baby_goods)
                "bag" -> context.getString(R.string.osm_shop_bag)
                "boutique" -> context.getString(R.string.osm_shop_boutique)
                "clothes" -> context.getString(R.string.osm_shop_clothes)
                "fabric" -> context.getString(R.string.osm_shop_fabric)
                "fashion" -> context.getString(R.string.osm_shop_fashion)
                "fashion_accessories" -> context.getString(R.string.osm_shop_fashion_accessories)
                "jewelry" -> context.getString(R.string.osm_shop_jewelry)
                "leather" -> context.getString(R.string.osm_shop_leather)
                "sewing" -> context.getString(R.string.osm_shop_sewing)
                "shoes" -> context.getString(R.string.osm_shop_shoes)
                "shoe_repair" -> context.getString(R.string.osm_shop_shoe_repair)
                "tailor" -> context.getString(R.string.osm_shop_tailor)
                "watches" -> context.getString(R.string.osm_shop_watches)
                "wool" -> context.getString(R.string.osm_shop_wool)
                "charity" -> context.getString(R.string.osm_shop_charity)
                "second_hand" -> context.getString(R.string.osm_shop_second_hand)
                "variety_store" -> context.getString(R.string.osm_shop_variety_store)
                "beauty" -> context.getString(R.string.osm_shop_beauty)
                "chemist" -> context.getString(R.string.osm_shop_chemist)
                "cosmetics" -> context.getString(R.string.osm_shop_cosmetics)
                "erotic" -> context.getString(R.string.osm_shop_erotic)
                "hairdresser" -> context.getString(R.string.osm_shop_hairdresser)
                "hairdresser_supply" -> context.getString(R.string.osm_shop_hairdresser_supply)
                "hearing_aids" -> context.getString(R.string.osm_shop_hearing_aids)
                "herbalist" -> context.getString(R.string.osm_shop_herbalist)
                "massage" -> context.getString(R.string.osm_shop_massage)
                "medical_supply" -> context.getString(R.string.osm_shop_medical_supply)
                "nutrition_supplements" -> context.getString(R.string.osm_shop_nutrition_supplements)
                "optician" -> context.getString(R.string.osm_shop_optician)
                "perfumery" -> context.getString(R.string.osm_shop_perfumery)
                "tattoo" -> context.getString(R.string.osm_shop_tattoo)
                "agrarian" -> context.getString(R.string.osm_shop_agrarian)
                "appliance" -> context.getString(R.string.osm_shop_appliance)
                "bathroom_furnishing" -> context.getString(R.string.osm_shop_bathroom_furnishing)
                "country_store" -> context.getString(R.string.osm_shop_country_store)
                "doityourself" -> context.getString(R.string.osm_shop_doityourself)
                "electrical" -> context.getString(R.string.osm_shop_electrical)
                "energy" -> context.getString(R.string.osm_shop_energy)
                "fireplace" -> context.getString(R.string.osm_shop_fireplace)
                "florist" -> context.getString(R.string.osm_shop_florist)
                "garden_centre" -> context.getString(R.string.osm_shop_garden_centre)
                "garden_furniture" -> context.getString(R.string.osm_shop_garden_furniture)
                "gas" -> context.getString(R.string.osm_shop_gas)
                "glaziery" -> context.getString(R.string.osm_shop_glaziery)
                "groundskeeping" -> context.getString(R.string.osm_shop_groundskeeping)
                "hardware" -> context.getString(R.string.osm_shop_hardware)
                "houseware" -> context.getString(R.string.osm_shop_houseware)
                "locksmith" -> context.getString(R.string.osm_shop_locksmith)
                "paint" -> context.getString(R.string.osm_shop_paint)
                "pottery" -> context.getString(R.string.osm_shop_pottery)
                "security" -> context.getString(R.string.osm_shop_security)
                "tool_hire" -> context.getString(R.string.osm_shop_tool_hire)
                "trade" -> context.getString(R.string.osm_shop_trade)
                "antiques" -> context.getString(R.string.osm_shop_antiques)
                "bed" -> context.getString(R.string.osm_shop_bed)
                "candles" -> context.getString(R.string.osm_shop_candles)
                "carpet" -> context.getString(R.string.osm_shop_carpet)
                "curtain" -> context.getString(R.string.osm_shop_curtain)
                "doors" -> context.getString(R.string.osm_shop_doors)
                "flooring" -> context.getString(R.string.osm_shop_flooring)
                "furniture" -> context.getString(R.string.osm_shop_furniture)
                "household_linen" -> context.getString(R.string.osm_shop_household_linen)
                "interior_decoration" -> context.getString(R.string.osm_shop_interior_decoration)
                "kitchen" -> context.getString(R.string.osm_shop_kitchen)
                "lighting" -> context.getString(R.string.osm_shop_lighting)
                "tiles" -> context.getString(R.string.osm_shop_tiles)
                "window_blind" -> context.getString(R.string.osm_shop_window_blind)
                "computer" -> context.getString(R.string.osm_shop_computer)
                "electronics" -> context.getString(R.string.osm_shop_electronics)
                "hifi" -> context.getString(R.string.osm_shop_hifi)
                "mobile_phone" -> context.getString(R.string.osm_shop_mobile_phone)
                "printer_ink" -> context.getString(R.string.osm_shop_printer_ink)
                "radiotechnics" -> context.getString(R.string.osm_shop_radiotechnics)
                "telecommunication" -> context.getString(R.string.osm_shop_telecommunication)
                "vacuum_cleaner" -> context.getString(R.string.osm_shop_vacuum_cleaner)
                "atv" -> context.getString(R.string.osm_shop_atv)
                "bicycle" -> context.getString(R.string.osm_shop_bicycle)
                "boat" -> context.getString(R.string.osm_shop_boat)
                "car" -> context.getString(R.string.osm_shop_car)
                "car_parts" -> context.getString(R.string.osm_shop_car_parts)
                "car_repair" -> context.getString(R.string.osm_shop_car_repair)
                "caravan" -> context.getString(R.string.osm_shop_caravan)
                "fishing" -> context.getString(R.string.osm_shop_fishing)
                "fuel" -> context.getString(R.string.osm_shop_fuel)
                "golf" -> context.getString(R.string.osm_shop_golf)
                "hunting" -> context.getString(R.string.osm_shop_hunting)
                "military_surplus" -> context.getString(R.string.osm_shop_military_surplus)
                "motorcycle" -> context.getString(R.string.osm_shop_motorcycle)
                "motorcycle_repair" -> context.getString(R.string.osm_shop_motorcycle_repair)
                "outdoor" -> context.getString(R.string.osm_shop_outdoor)
                "running" -> context.getString(R.string.osm_shop_running)
                "scooter" -> context.getString(R.string.osm_shop_scooter)
                "scuba_diving" -> context.getString(R.string.osm_shop_scuba_diving)
                "ski" -> context.getString(R.string.osm_shop_ski)
                "snowmobile" -> context.getString(R.string.osm_shop_snowmobile)
                "sports" -> context.getString(R.string.osm_shop_sports)
                "surf" -> context.getString(R.string.osm_shop_surf)
                "swimming_pool" -> context.getString(R.string.osm_shop_swimming_pool)
                "trailer" -> context.getString(R.string.osm_shop_trailer)
                "truck" -> context.getString(R.string.osm_shop_truck)
                "tyres" -> context.getString(R.string.osm_shop_tyres)
                "art" -> context.getString(R.string.osm_shop_art)
                "camera" -> context.getString(R.string.osm_shop_camera)
                "collector" -> context.getString(R.string.osm_shop_collector)
                "craft" -> context.getString(R.string.osm_shop_craft)
                "frame" -> context.getString(R.string.osm_shop_frame)
                "games" -> context.getString(R.string.osm_shop_games)
                "model" -> context.getString(R.string.osm_shop_model)
                "music" -> context.getString(R.string.osm_shop_music)
                "musical_instrument" -> context.getString(R.string.osm_shop_musical_instrument)
                "photo" -> context.getString(R.string.osm_shop_photo)
                "trophy" -> context.getString(R.string.osm_shop_trophy)
                "video" -> context.getString(R.string.osm_shop_video)
                "video_games" -> context.getString(R.string.osm_shop_video_games)
                "anime" -> context.getString(R.string.osm_shop_anime)
                "books" -> context.getString(R.string.osm_shop_books)
                "gift" -> context.getString(R.string.osm_shop_gift)
                "lottery" -> context.getString(R.string.osm_shop_lottery)
                "newsagent" -> context.getString(R.string.osm_shop_newsagent)
                "stationery" -> context.getString(R.string.osm_shop_stationery)
                "ticket" -> context.getString(R.string.osm_shop_ticket)
                "bookmaker" -> context.getString(R.string.osm_shop_bookmaker)
                "cannabis" -> context.getString(R.string.osm_shop_cannabis)
                "copyshop" -> context.getString(R.string.osm_shop_copyshop)
                "dry_cleaning" -> context.getString(R.string.osm_shop_dry_cleaning)
                "e_cigarette" -> context.getString(R.string.osm_shop_e_cigarette)
                "funeral_directors" -> context.getString(R.string.osm_shop_funeral_directors)
                "laundry" -> context.getString(R.string.osm_shop_laundry)
                "money_lender" -> context.getString(R.string.osm_shop_money_lender)
                "outpost" -> context.getString(R.string.osm_shop_outpost)
                "party" -> context.getString(R.string.osm_shop_party)
                "pawnbroker" -> context.getString(R.string.osm_shop_pawnbroker)
                "pest_control" -> context.getString(R.string.osm_shop_pest_control)
                "pet" -> context.getString(R.string.osm_shop_pet)
                "pet_grooming" -> context.getString(R.string.osm_shop_pet_grooming)
                "pyrotechnics" -> context.getString(R.string.osm_shop_pyrotechnics)
                "religion" -> context.getString(R.string.osm_shop_religion)
                "rental" -> context.getString(R.string.osm_shop_rental)
                "storage_rental" -> context.getString(R.string.osm_shop_storage_rental)
                "tobacco" -> context.getString(R.string.osm_shop_tobacco)
                "toys" -> context.getString(R.string.osm_shop_toys)
                "travel_agency" -> context.getString(R.string.osm_shop_travel_agency)
                "vacant" -> context.getString(R.string.osm_shop_vacant)
                "weapons" -> context.getString(R.string.osm_shop_weapons)
                "unknown" -> context.getString(R.string.osm_shop_unknown)
                else -> "shop=$shop"
            }
        }

        // Check for leisure tags
        properties["leisure"]?.let { leisure ->
            return when (leisure) {
                "adult_gaming_centre" -> context.getString(R.string.osm_leisure_adult_gaming_centre)
                "amusement_arcade" -> context.getString(R.string.osm_leisure_amusement_arcade)
                "bandstand" -> context.getString(R.string.osm_leisure_bandstand)
                "bathing_place" -> context.getString(R.string.osm_leisure_bathing_place)
                "beach_resort" -> context.getString(R.string.osm_leisure_beach_resort)
                "bird_hide" -> context.getString(R.string.osm_leisure_bird_hide)
                "bleachers" -> context.getString(R.string.osm_leisure_bleachers)
                "bowling_alley" -> context.getString(R.string.osm_leisure_bowling_alley)
                "common" -> context.getString(R.string.osm_leisure_common)
                "dance" -> context.getString(R.string.osm_leisure_dance)
                "disc_golf_course" -> context.getString(R.string.osm_leisure_disc_golf_course)
                "dog_park" -> context.getString(R.string.osm_leisure_dog_park)
                "escape_game" -> context.getString(R.string.osm_leisure_escape_game)
                "firepit" -> context.getString(R.string.osm_leisure_firepit)
                "fishing" -> context.getString(R.string.osm_leisure_fishing)
                "fitness_centre" -> context.getString(R.string.osm_leisure_fitness_centre)
                "fitness_station" -> context.getString(R.string.osm_leisure_fitness_station)
                "garden" -> context.getString(R.string.osm_leisure_garden)
                "golf_course" -> context.getString(R.string.osm_leisure_golf_course)
                "hackerspace" -> context.getString(R.string.osm_leisure_hackerspace)
                "high_ropes_course" -> context.getString(R.string.osm_leisure_high_ropes_course)
                "horse_riding" -> context.getString(R.string.osm_leisure_horse_riding)
                "ice_rink" -> context.getString(R.string.osm_leisure_ice_rink)
                "marina" -> context.getString(R.string.osm_leisure_marina)
                "miniature_golf" -> context.getString(R.string.osm_leisure_miniature_golf)
                "nature_reserve" -> context.getString(R.string.osm_leisure_nature_reserve)
                "outdoor_seating" -> context.getString(R.string.osm_leisure_outdoor_seating)
                "park" -> context.getString(R.string.osm_leisure_park)
                "picnic_table" -> context.getString(R.string.osm_leisure_picnic_table)
                "pitch" -> context.getString(R.string.osm_leisure_pitch)
                "playground" -> context.getString(R.string.osm_leisure_playground)
                "resort" -> context.getString(R.string.osm_leisure_resort)
                "sauna" -> context.getString(R.string.osm_leisure_sauna)
                "slipway" -> context.getString(R.string.osm_leisure_slipway)
                "sports_centre" -> context.getString(R.string.osm_leisure_sports_centre)
                "sports_hall" -> context.getString(R.string.osm_leisure_sports_hall)
                "stadium" -> context.getString(R.string.osm_leisure_stadium)
                "summer_camp" -> context.getString(R.string.osm_leisure_summer_camp)
                "sunbathing" -> context.getString(R.string.osm_leisure_sunbathing)
                "swimming_area" -> context.getString(R.string.osm_leisure_swimming_area)
                "swimming_pool" -> context.getString(R.string.osm_leisure_swimming_pool)
                "tanning_salon" -> context.getString(R.string.osm_leisure_tanning_salon)
                "track" -> context.getString(R.string.osm_leisure_track)
                "trampoline_park" -> context.getString(R.string.osm_leisure_trampoline_park)
                "water_park" -> context.getString(R.string.osm_leisure_water_park)
                "wildlife_hide" -> context.getString(R.string.osm_leisure_wildlife_hide)
                else -> "leisure=$leisure"
            }
        }

        // Default case - return empty string or a generic description
        return context.getString(R.string.point_of_interest)
    }


    /**
     * Creates a "My Location" Place object using the current location.
     * Returns null if current location is not available.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationAsPlace(): Place? {
        return getCurrentLocation(context)?.let { location ->
            createMyLocationPlace(LatLng(location.latitude, location.longitude))
        }
    }

    /**
     * Forces a fresh location fetch and creates a "My Location" Place object.
     * This bypasses the cache and always gets a new location.
     * Returns null if fresh location cannot be obtained.
     */
    @SuppressLint("MissingPermission")
    suspend fun getFreshCurrentLocationAsPlace(): Place? {
        return requestFreshLocation(getLocationManager(context))?.let { location ->
            createMyLocationPlace(LatLng(location.latitude, location.longitude))
        }
    }
}
