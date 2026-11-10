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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowSizeClass
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.ui.theme.fontStyle_header
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.place_form_altitude_max
import nebuni.composeapp.generated.resources.place_form_altitude_max_hint
import nebuni.composeapp.generated.resources.place_form_altitude_min
import nebuni.composeapp.generated.resources.place_form_altitude_min_hint
import nebuni.composeapp.generated.resources.place_form_azimuth_max
import nebuni.composeapp.generated.resources.place_form_azimuth_max_hint
import nebuni.composeapp.generated.resources.place_form_azimuth_min
import nebuni.composeapp.generated.resources.place_form_azimuth_min_hint
import nebuni.composeapp.generated.resources.place_form_default_name
import nebuni.composeapp.generated.resources.place_form_gps_coordinates
import nebuni.composeapp.generated.resources.place_form_latitude
import nebuni.composeapp.generated.resources.place_form_latitude_hint
import nebuni.composeapp.generated.resources.place_form_longitude
import nebuni.composeapp.generated.resources.place_form_longitude_hint
import nebuni.composeapp.generated.resources.place_form_name
import nebuni.composeapp.generated.resources.place_form_name_hint
import nebuni.composeapp.generated.resources.place_form_submit
import nebuni.composeapp.generated.resources.place_form_title
import nebuni.composeapp.generated.resources.place_form_visible_sky_area
import org.jetbrains.compose.resources.stringResource

/** Whether the form creates a new place or edits an existing one (the id is then preserved). */
enum class ObservationPlaceFormMode { Create, Edit }

/** [this] as the starting values of a new place: same location and sky area, blank name so the form proposes its default. */
fun ObservationPlace.asNewPlaceTemplate(): ObservationPlace = copy(name = "")

/** Width of the dialog on non-compact windows. */
internal val ObservationPlaceFormDialogWidth = 560.dp

@Composable
fun ObservationPlaceFormDialog(
    initial: ObservationPlace,
    mode: ObservationPlaceFormMode,
    onDismissRequest: () -> Unit,
    onSubmit: (ObservationPlace) -> Unit,
) {
    val isCompact = !currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        if (isCompact) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                ) {
                    ObservationPlaceForm(initial, mode, onSubmit)
                }
            }
        } else {
            Surface(
                modifier = Modifier.width(ObservationPlaceFormDialogWidth).padding(vertical = 24.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ObservationPlaceForm(initial, mode, onSubmit)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ObservationPlaceForm(
    initial: ObservationPlace,
    mode: ObservationPlaceFormMode,
    onSubmit: (ObservationPlace) -> Unit,
) {
    val nameState = rememberTextFieldState(
        initialText = initial.name.ifBlank { stringResource(Res.string.place_form_default_name) },
    )

    val latState = rememberTextFieldState(initialText = initial.latitude.toString())
    val lonState = rememberTextFieldState(initialText = initial.longitude.toString())

    val altMinState = rememberTextFieldState(initialText = initial.altMin.toString())
    val altMaxState = rememberTextFieldState(initialText = initial.altMax.toString())

    val azMinState = rememberTextFieldState(initialText = initial.azMin.toString())
    val azMaxState = rememberTextFieldState(initialText = initial.azMax.toString())

    var inputError by remember { mutableStateOf<ObservationPlaceInputError?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(stringResource(Res.string.place_form_title), style = fontStyle_header)

            OutlinedTextField(
                state = nameState,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(stringResource(Res.string.place_form_name))
                },
                supportingText = {
                    Text(stringResource(Res.string.place_form_name_hint))
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                )
            )

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(Res.string.place_form_gps_coordinates), style = fontStyle_header)

                OutlinedTextField(
                    state = latState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.place_form_latitude))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_latitude_hint))
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = lonState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.place_form_longitude))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_longitude_hint))
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    )
                )
            }

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(Res.string.place_form_visible_sky_area), style = fontStyle_header)

                OutlinedTextField(
                    state = altMinState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.place_form_altitude_min))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_altitude_min_hint))
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = altMaxState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.place_form_altitude_max))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_altitude_max_hint))
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = azMinState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.place_form_azimuth_min))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_azimuth_min_hint))
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = azMaxState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.place_form_azimuth_max))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_azimuth_max_hint))
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }

            inputError?.let { error ->
                Text(stringResource(error.message), color = MaterialTheme.colorScheme.error)
            }

            val size = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = {
                    val result = validateObservationPlaceInput(
                        name = nameState.text.toString(),
                        latitude = latState.text.toString(),
                        longitude = lonState.text.toString(),
                        altMin = altMinState.text.toString(),
                        altMax = altMaxState.text.toString(),
                        azMin = azMinState.text.toString(),
                        azMax = azMaxState.text.toString()
                    )
                    when (result) {
                        is ObservationPlaceInputResult.Invalid -> inputError = result.error
                        is ObservationPlaceInputResult.Valid -> onSubmit(
                            when (mode) {
                                ObservationPlaceFormMode.Create -> result.place
                                ObservationPlaceFormMode.Edit -> result.place.copy(id = initial.id)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(size),
                contentPadding = ButtonDefaults.contentPaddingFor(size)
            ) {
                Text(stringResource(Res.string.place_form_submit), style = ButtonDefaults.textStyleFor(size))
            }
        }
    }
}