/*
 * Nebuni
 * Copyright (c) 2025 Sylvain.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.domnis.nebuni.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.ui.theme.fontStyle_header
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.place_coordinates
import nebuni.composeapp.generated.resources.place_lat_north
import nebuni.composeapp.generated.resources.place_lat_south
import nebuni.composeapp.generated.resources.place_lon_east
import nebuni.composeapp.generated.resources.place_lon_west
import nebuni.composeapp.generated.resources.place_switcher_add
import nebuni.composeapp.generated.resources.place_switcher_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToLong

/** Formats the absolute value of [degrees] with 4 decimals, the hemisphere being given apart. */
fun formatAbsoluteDegrees(degrees: Double): String {
    val tenThousandths = (abs(degrees) * 10_000).roundToLong()
    return "${tenThousandths / 10_000}.${(tenThousandths % 10_000).toString().padStart(4, '0')}"
}

/** The coordinates of [place] as "43.2841° N, 5.3713° E". */
@Composable
fun placeCoordinatesText(place: ObservationPlace): String {
    val latitude = formatAbsoluteDegrees(place.latitude)
    val longitude = formatAbsoluteDegrees(place.longitude)
    return stringResource(
        Res.string.place_coordinates,
        if (place.latitude >= 0) stringResource(Res.string.place_lat_north, latitude)
        else stringResource(Res.string.place_lat_south, latitude),
        if (place.longitude >= 0) stringResource(Res.string.place_lon_east, longitude)
        else stringResource(Res.string.place_lon_west, longitude)
    )
}

/** Lists the saved [places]; tapping one calls [onSelectPlace] with its id, the button calls [onAddPlace]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSwitcherSheet(
    places: List<ObservationPlace>,
    selectedPlaceId: String,
    onSelectPlace: (String) -> Unit,
    onAddPlace: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                stringResource(Res.string.place_switcher_title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = fontStyle_header
            )

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).selectableGroup()) {
                items(places, key = { it.id }) { place ->
                    val isSelected = place.id == selectedPlaceId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelectPlace(place.id) }
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // the row handles the click, so the radio button stays a visual cue
                        RadioButton(selected = isSelected, onClick = null)

                        Column {
                            Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                placeCoordinatesText(place),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            FilledTonalButton(
                onClick = onAddPlace,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(stringResource(Res.string.place_switcher_add))
            }
        }
    }
}
