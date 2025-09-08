# Geocoding Service

This module provides an abstraction over geocoding services that can work with both online and offline implementations.

## Architecture

The geocoding service follows a factory pattern to allow switching between online and offline implementations without changing the client code.

### Components

1. **GeocodingService** - Interface defining the contract for geocoding operations
2. **PeliasGeocodingService** - Implementation that uses the Pelias API
3. **OfflineGeocodingService** - Placeholder for future offline implementation
4. **GeocodingServiceFactory** - Factory for creating service instances
5. **GeocodeResult** - Data class representing a geocoding result
6. **Address** - Data class representing address details

### Data Models

#### GeocodeResult
```kotlin
data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val address: Address? = null
)
```

#### Address
```kotlin
data class Address(
    val houseNumber: String? = null,
    val road: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null
)
```

## Usage

### Creating a Service Instance

```kotlin
// Create online service (default)
val geocodingService = GeocodingServiceFactory.createDefaultService()

// Or explicitly create online service
val onlineService = GeocodingServiceFactory.createService(GeocodingServiceFactory.ServiceType.ONLINE)

// Create offline service (placeholder)
val offlineService = GeocodingServiceFactory.createService(GeocodingServiceFactory.ServiceType.OFFLINE)
```

### Geocoding

```kotlin
// Geocode a query
viewModelScope.launch {
    geocodingService.geocode("New York City").collect { results ->
        // Handle results
        results.forEach { result ->
            println("Found: ${result.displayName} at ${result.latitude}, ${result.longitude}")
        }
    }
}
```

### Reverse Geocoding

```kotlin
// Reverse geocode coordinates
viewModelScope.launch {
    geocodingService.reverseGeocode(40.7128, -74.0060).collect { results ->
        // Handle results
        results.forEach { result ->
            println("Address: ${result.displayName}")
        }
    }
}
```

## Implementation Details

### Online Service

The `PeliasGeocodingService` uses the Pelias API:
- Base URL: https://maps.earth/pelias/v1
- Supports both forward and reverse geocoding
- Returns detailed address information when available

### Offline Service

The `OfflineGeocodingService` is a placeholder that currently returns empty results. It will be implemented when the offline geocoder is ready.

## Extending the Service

To add a new geocoding service implementation:

1. Create a new class that implements `GeocodingService`
2. Implement the `geocode` and `reverseGeocode` methods
3. Add a new enum value to `ServiceType` in `GeocodingServiceFactory`
4. Update the factory to return your new implementation

## Error Handling

All methods return Kotlin Flows, which makes error handling straightforward:

```kotlin
viewModelScope.launch {
    try {
        geocodingService.geocode(query).collect { results ->
            // Handle results
        }
    } catch (e: Exception) {
        // Handle error
    }
}
