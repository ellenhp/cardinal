package earth.maps.cardinal.data

data class Area(
    val id: Int,
    val name: String,
    val size: String,
    val status: AreaStatus,
    val progress: Int = 0,
    val downloadDate: String = ""
)

enum class AreaStatus {
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADED
}
