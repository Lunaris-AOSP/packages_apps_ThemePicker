/*
 * Copyright (C) 2024 The Android Open Source Project
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
 */

package com.android.wallpaper.picker.common.preview.ui.binder

import android.os.Bundle
import android.os.Message
import androidx.core.os.bundleOf
import com.android.customization.model.grid.DefaultShapeGridManager.Companion.COL_GRID_NAME
import com.android.customization.model.grid.DefaultShapeGridManager.Companion.COL_SHAPE_KEY
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.color.data.util.MaterialColorsGenerator
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.CLOCK_SIZE_DYNAMIC
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.CLOCK_SIZE_SMALL
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_CLOCK_SIZE
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_HIDE_SMART_SPACE
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_INITIALLY_SELECTED_SLOT_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_QUICK_AFFORDANCE_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_SLOT_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_DEFAULT_PREVIEW
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_HIDE_SMART_SPACE
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_PREVIEW_CLOCK_SIZE
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_SLOT_SELECTED
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_START_CUSTOMIZING_QUICK_AFFORDANCES
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder.Companion.sendMessage
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Singleton
class ThemePickerWorkspaceCallbackBinder
@Inject
constructor(
    private val defaultWorkspaceCallbackBinder: DefaultWorkspaceCallbackBinder,
    private val materialColorsGenerator: MaterialColorsGenerator,
) : WorkspaceCallbackBinder {

    override suspend fun bind(
        workspaceCallback: Message,
        viewModel: CustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        screen: Screen,
        clockViewFactory: ClockViewFactory,
    ) {
        defaultWorkspaceCallbackBinder.bind(
            workspaceCallback = workspaceCallback,
            viewModel = viewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            screen = screen,
            clockViewFactory = clockViewFactory,
        )

        if (viewModel !is ThemePickerCustomizationOptionsViewModel) {
            throw IllegalArgumentException(
                "viewModel $viewModel is not a ThemePickerCustomizationOptionsViewModel."
            )
        }

        when (screen) {
            Screen.LOCK_SCREEN ->
                coroutineScope {
                    launch {
                        viewModel.selectedOption.collect {
                            when (it) {
                                ThemePickerLockCustomizationOption.SHORTCUTS ->
                                    workspaceCallback.sendMessage(
                                        MESSAGE_ID_START_CUSTOMIZING_QUICK_AFFORDANCES,
                                        Bundle().apply {
                                            putString(
                                                KEY_INITIALLY_SELECTED_SLOT_ID,
                                                SLOT_ID_BOTTOM_START,
                                            )
                                        },
                                    )
                                else ->
                                    workspaceCallback.sendMessage(
                                        MESSAGE_ID_DEFAULT_PREVIEW,
                                        Bundle.EMPTY,
                                    )
                            }
                        }
                    }

                    launch {
                        viewModel.keyguardQuickAffordancePickerViewModel2.selectedSlotId.collect {
                            workspaceCallback.sendMessage(
                                MESSAGE_ID_SLOT_SELECTED,
                                Bundle().apply { putString(KEY_SLOT_ID, it) },
                            )
                        }
                    }

                    launch {
                        viewModel.keyguardQuickAffordancePickerViewModel2.previewingQuickAffordances
                            .collect {
                                it[SLOT_ID_BOTTOM_START]?.let {
                                    workspaceCallback.sendMessage(
                                        MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED,
                                        Bundle().apply {
                                            putString(KEY_SLOT_ID, SLOT_ID_BOTTOM_START)
                                            putString(KEY_QUICK_AFFORDANCE_ID, it)
                                        },
                                    )
                                }
                                it[SLOT_ID_BOTTOM_END]?.let {
                                    workspaceCallback.sendMessage(
                                        MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED,
                                        Bundle().apply {
                                            putString(KEY_SLOT_ID, SLOT_ID_BOTTOM_END)
                                            putString(KEY_QUICK_AFFORDANCE_ID, it)
                                        },
                                    )
                                }
                            }
                    }

                    launch {
                        combine(
                                viewModel.clockPickerViewModel.previewingClock,
                                viewModel.clockPickerViewModel.previewingClockSize,
                                ::Pair,
                            )
                            .collect { (previewingClock, previewingClockSize) ->
                                val hideSmartspace =
                                    clockViewFactory.getController(previewingClock.clockId)?.let {
                                        when (previewingClockSize) {
                                            ClockSize.DYNAMIC ->
                                                it.largeClock.config.hasCustomWeatherDataDisplay
                                            ClockSize.SMALL ->
                                                it.smallClock.config.hasCustomWeatherDataDisplay
                                        }
                                    } ?: false
                                workspaceCallback.sendMessage(
                                    MESSAGE_ID_HIDE_SMART_SPACE,
                                    Bundle().apply {
                                        putBoolean(KEY_HIDE_SMART_SPACE, hideSmartspace)
                                    },
                                )

                                workspaceCallback.sendMessage(
                                    MESSAGE_ID_PREVIEW_CLOCK_SIZE,
                                    Bundle().apply {
                                        putString(
                                            KEY_CLOCK_SIZE,
                                            when (previewingClockSize) {
                                                ClockSize.DYNAMIC -> CLOCK_SIZE_DYNAMIC
                                                ClockSize.SMALL -> CLOCK_SIZE_SMALL
                                            },
                                        )
                                    },
                                )
                            }
                    }
                }
            Screen.HOME_SCREEN ->
                coroutineScope {
                    launch {
                        viewModel.shapeGridPickerViewModel.previewingShapeKey.collect {
                            workspaceCallback.sendMessage(
                                MESSAGE_ID_UPDATE_SHAPE,
                                bundleOf(COL_SHAPE_KEY to it),
                            )
                        }
                    }

                    launch {
                        viewModel.shapeGridPickerViewModel.previewingGridKey.collect {
                            workspaceCallback.sendMessage(
                                MESSAGE_ID_UPDATE_GRID,
                                bundleOf(COL_GRID_NAME to it),
                            )
                        }
                    }

                    launch {
                        combine(
                                viewModel.colorPickerViewModel2.previewingColorOption,
                                viewModel.darkModeViewModel.overridingIsDarkMode,
                                ::Pair,
                            )
                            .collect { (colorOption, darkMode) ->
                                val bundle =
                                    Bundle().apply {
                                        if (colorOption != null) {
                                            val (ids, colors) =
                                                materialColorsGenerator.generate(
                                                    colorOption.seedColor,
                                                    colorOption.style,
                                                )
                                            putIntArray(KEY_COLOR_RESOURCE_IDS, ids)
                                            putIntArray(KEY_COLOR_VALUES, colors)
                                        }

                                        if (darkMode != null) {
                                            putBoolean(KEY_DARK_MODE, darkMode)
                                        }
                                    }
                                workspaceCallback.sendMessage(MESSAGE_ID_UPDATE_COLOR, bundle)
                            }
                    }

                    launch {
                        viewModel.themedIconViewModel.isActivated.collect {
                            workspaceCallback.sendMessage(MESSAGE_ID_UPDATE_COLOR, Bundle.EMPTY)
                        }
                    }

                    launch {
                        viewModel.appIconPickerViewModel.previewingIsThemeIconEnabled.collect {
                            workspaceCallback.sendMessage(
                                MESSAGE_ID_UPDATE_ICON_THEMED,
                                Bundle().apply { putBoolean(KEY_BOOLEAN_VALUE, it) },
                            )
                        }
                    }
                }
        }
    }

    companion object {
        const val MESSAGE_ID_UPDATE_SHAPE = 2586
        const val MESSAGE_ID_UPDATE_GRID = 7414
        const val MESSAGE_ID_UPDATE_COLOR = 856
        const val MESSAGE_ID_UPDATE_ICON_THEMED = 311
        const val KEY_COLOR_RESOURCE_IDS: String = "color_resource_ids"
        const val KEY_COLOR_VALUES: String = "color_values"
        const val KEY_DARK_MODE: String = "use_dark_mode"
        const val KEY_BOOLEAN_VALUE: String = "boolean_value"
    }
}
