package earth.maps.cardinal.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.routing.FerrostarWrapperRepository
import javax.inject.Inject

@HiltViewModel
class TurnByTurnNavigationViewModel @Inject constructor(
    val ferrostarWrapperRepository: FerrostarWrapperRepository
) : ViewModel()
