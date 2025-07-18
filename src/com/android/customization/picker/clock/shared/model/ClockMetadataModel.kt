/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.android.customization.picker.clock.shared.model

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import com.android.systemui.plugins.clocks.AxisPresetConfig

/** Model for clock metadata. */
data class ClockMetadataModel(
    val clockId: String,
    val isSelected: Boolean,
    val description: String,
    val thumbnail: Drawable,
    val isReactiveToTone: Boolean,
    val axisPresetConfig: AxisPresetConfig?, // Null indicates the preset list should be disabled.
    val selectedColorId: String?,
    @IntRange(from = 0, to = 100) val colorToneProgress: Int,
    @ColorInt val seedColor: Int?,
) {
    companion object {
        const val MIN_COLOR_TONE_PROGRESS = 0
        const val MAX_COLOR_TONE_PROGRESS = 100
        const val DEFAULT_COLOR_TONE_PROGRESS = 75
    }
}
