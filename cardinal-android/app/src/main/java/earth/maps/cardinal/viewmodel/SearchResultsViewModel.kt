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

package earth.maps.cardinal.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LocationRepository
import earth.maps.cardinal.data.Place
import javax.inject.Inject

@HiltViewModel
class SearchResultsViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    fun generatePlace(geocodeResult: GeocodeResult): Place {
        return locationRepository.createSearchResultPlace(geocodeResult)
    }
}
