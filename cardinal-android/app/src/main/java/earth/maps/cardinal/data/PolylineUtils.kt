/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package earth.maps.cardinal.data

import io.github.dellisd.spatialk.geojson.Position

/**
 * Utility functions for working with encoded polylines and route geometry.
 */
object PolylineUtils {

    /**
     * Decodes a Google encoded polyline string into a list of Position coordinates.
     *
     * @param encoded The encoded polyline string
     * @param precision The precision of the encoding (default is 5, some APIs use 6)
     * @return List of Position objects representing the decoded coordinates
     */
    fun decodePolyline(encoded: String, precision: Int = 5): List<Position> {
        val poly = mutableListOf<Position>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        val factor = Math.pow(10.0, precision.toDouble()).toInt()

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0

            // Decode latitude
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0

            // Decode longitude
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latitude = lat.toDouble() / factor
            val longitude = lng.toDouble() / factor
            poly.add(Position(longitude, latitude))
        }

        return poly
    }

    /**
     * Calculates the bounding box for a list of positions.
     *
     * @param positions List of Position objects
     * @return BoundingBox containing min/max longitude and latitude
     */
    fun calculateBoundingBox(positions: List<Position>): BoundingBox? {
        if (positions.isEmpty()) return null

        var minLng = positions[0].longitude
        var maxLng = positions[0].longitude
        var minLat = positions[0].latitude
        var maxLat = positions[0].latitude

        for (position in positions) {
            minLng = minOf(minLng, position.longitude)
            maxLng = maxOf(maxLng, position.longitude)
            minLat = minOf(minLat, position.latitude)
            maxLat = maxOf(maxLat, position.latitude)
        }

        return BoundingBox(
            north = maxLat,
            south = minLat,
            east = maxLng,
            west = minLng
        )
    }

    /**
     * Calculates the bounding box for multiple lists of positions.
     *
     * @param positionLists List of position lists
     * @return Combined BoundingBox
     */
    fun calculateCombinedBoundingBox(positionLists: List<List<Position>>): BoundingBox? {
        val allPositions = positionLists.flatten()
        return calculateBoundingBox(allPositions)
    }
}
