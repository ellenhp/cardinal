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

package earth.maps.cardinal.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.AppPreferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appPreferenceRepository: AppPreferenceRepository
) : ViewModel() {

    val offlineMode = appPreferenceRepository.offlineMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val allowTransitInOfflineMode = appPreferenceRepository.allowTransitInOfflineMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val contrastLevel = appPreferenceRepository.contrastLevel.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val animationSpeed = appPreferenceRepository.animationSpeed.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val peliasApiConfig = appPreferenceRepository.peliasApiConfig.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        appPreferenceRepository.peliasApiConfig.value
    )

    val valhallaApiConfig = appPreferenceRepository.valhallaApiConfig.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        appPreferenceRepository.valhallaApiConfig.value
    )

    val continuousLocationTracking =
        appPreferenceRepository.continuousLocationTracking.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    fun setOfflineMode(enabled: Boolean) {
        appPreferenceRepository.setOfflineMode(enabled)
    }

    fun setAllowTransitInOfflineMode(enabled: Boolean) {
        appPreferenceRepository.setAllowTransitInOfflineMode(enabled)
    }

    fun setContrastLevel(level: Int) {
        appPreferenceRepository.setContrastLevel(level)
    }

    fun setAnimationSpeed(speed: Int) {
        appPreferenceRepository.setAnimationSpeed(speed)
    }

    fun setPeliasBaseUrl(baseUrl: String) {
        appPreferenceRepository.setPeliasBaseUrl(baseUrl)
    }

    fun setPeliasApiKey(apiKey: String?) {
        appPreferenceRepository.setPeliasApiKey(apiKey)
    }

    fun setValhallaBaseUrl(baseUrl: String) {
        appPreferenceRepository.setValhallaBaseUrl(baseUrl)
    }

    fun setValhallaApiKey(apiKey: String?) {
        appPreferenceRepository.setValhallaApiKey(apiKey)
    }

    fun setContinuousLocationTrackingEnabled(enabled: Boolean) {
        appPreferenceRepository.setContinuousLocationTracking(enabled)
    }

    fun getVersionName(): String? {
        try {
            val pInfo: PackageInfo =
                context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun onCallToActionClicked() {
        val url = "https://github.com/ellenhp/cardinal"
        val i = Intent(Intent.ACTION_VIEW)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.setData(url.toUri())
        context.startActivity(i)
    }
}
