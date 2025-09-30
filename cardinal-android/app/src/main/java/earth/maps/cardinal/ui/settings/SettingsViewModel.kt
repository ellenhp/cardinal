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

package earth.maps.cardinal.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.text.format.DateFormat
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

    val showZoomFabs = appPreferenceRepository.showZoomFabs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val use24HourFormat = appPreferenceRepository.use24HourFormat.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DateFormat.is24HourFormat(context)
    )

    val distanceUnit = appPreferenceRepository.distanceUnit.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
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

    fun setShowZoomFabsEnabled(enabled: Boolean) {
        appPreferenceRepository.setShowZoomFabs(enabled)
    }

    fun setUse24HourFormat(use24Hour: Boolean) {
        appPreferenceRepository.setUse24HourFormat(use24Hour)
    }

    fun setDistanceUnit(distanceUnit: Int) {
        appPreferenceRepository.setDistanceUnit(distanceUnit)
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
