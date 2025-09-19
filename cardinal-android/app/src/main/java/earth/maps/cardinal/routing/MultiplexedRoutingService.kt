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

package earth.maps.cardinal.routing

import earth.maps.cardinal.data.AppPreferenceRepository
import javax.inject.Inject

class MultiplexedRoutingService
@Inject constructor(
    private val appPreferenceRepository: AppPreferenceRepository,
    private val onlineRoutingService: ValhallaRoutingService,
    private val offlineRoutingService: OfflineRoutingService
) : RoutingService {

    override suspend fun getRoute(
        request: String,
    ): String {
        return if (appPreferenceRepository.offlineMode.value) {
            offlineRoutingService.getRoute(request)
        } else {
            onlineRoutingService.getRoute(request)
        }
    }
}
