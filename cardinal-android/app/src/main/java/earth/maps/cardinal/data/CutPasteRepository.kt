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

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CutPasteRepository @Inject constructor() {

    /**
     * The set of list item UUIDs and ItemTypes in the clipboard.
     */
    val clipboard: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
}