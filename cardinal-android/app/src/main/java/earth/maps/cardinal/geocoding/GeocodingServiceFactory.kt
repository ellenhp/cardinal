package earth.maps.cardinal.geocoding

object GeocodingServiceFactory {
    enum class ServiceType {
        ONLINE,
        OFFLINE
    }

    fun createService(type: ServiceType): GeocodingService {
        return when (type) {
            ServiceType.ONLINE -> PeliasGeocodingService()
            ServiceType.OFFLINE -> OfflineGeocodingService()
        }
    }

    // Default to online service for now since offline isn't ready
    fun createDefaultService(): GeocodingService {
        return createService(ServiceType.ONLINE)
    }
}
