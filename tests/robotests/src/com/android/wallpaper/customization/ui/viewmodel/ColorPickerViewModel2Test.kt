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

package com.android.wallpaper.customization.ui.viewmodel

import android.content.Context
import android.stats.style.StyleEnums
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.model.color.ColorOptionsProvider
import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.color.data.repository.FakeColorPickerRepository2
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor2
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.mode.data.repository.DarkModeStateRepository
import com.android.systemui.monet.Style
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dagger.hilt.android.internal.lifecycle.RetainedLifecycleImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class ColorPickerViewModel2Test {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private val logger = TestThemesUserEventLogger()
    private lateinit var underTest: ColorPickerViewModel2
    private lateinit var colorUpdateViewModel: ColorUpdateViewModel

    private lateinit var context: Context
    private lateinit var testScope: TestScope

    @Inject lateinit var repository: FakeColorPickerRepository2
    @Inject lateinit var interactor: ColorPickerInteractor2
    @Inject lateinit var darkModeStateRepository: DarkModeStateRepository

    @Before
    fun setUp() {
        hiltRule.inject()

        context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)

        colorUpdateViewModel =
            ColorUpdateViewModel(context, RetainedLifecycleImpl(), darkModeStateRepository)

        underTest =
            ColorPickerViewModel2(
                context = context,
                interactor = interactor,
                logger = logger,
                colorUpdateViewModel = colorUpdateViewModel,
                viewModelScope = testScope.backgroundScope,
            )

        repository.setOptions(4, 4, ColorType.WALLPAPER_COLOR, 0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onApply_suspendsUntilOnApplyCompleteIsCalled() =
        testScope.runTest {
            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Select "Wallpaper colors" tab
            colorTypes()?.get(0)?.onClick?.invoke()
            // Select a color option to preview
            selectColorOption(colorOptions, 1)
            // Apply the selected color option
            val job = testScope.launch { onApply()?.invoke() }

            assertThat(job.isActive).isTrue()

            colorUpdateViewModel.updateColors()

            assertThat(job.isActive).isFalse()
        }

    @Test
    fun onApply_wallpaperColor_shouldLogColor() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorOptionsProvider.COLOR_SOURCE_LOCK,
                        Style.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(Style.FRUIT_SALAD, -54321)),
                ColorType.PRESET_COLOR,
                0,
            )

            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)

            // Select "Wallpaper colors" tab
            colorTypes()?.get(0)?.onClick?.invoke()
            // Select a color option to preview
            selectColorOption(colorOptions, 0)
            // Apply the selected color option
            applySelectedColorOption()

            assertThat(logger.themeColorSource)
                .isEqualTo(StyleEnums.COLOR_SOURCE_LOCK_SCREEN_WALLPAPER)
            assertThat(logger.themeColorStyle)
                .isEqualTo(Style.toString(Style.EXPRESSIVE).hashCode())
            assertThat(logger.themeSeedColor).isEqualTo(121212)
        }

    @Test
    fun onApply_presetColor_shouldLogColor() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorOptionsProvider.COLOR_SOURCE_LOCK,
                        Style.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(Style.FRUIT_SALAD, -54321)),
                ColorType.WALLPAPER_COLOR,
                0,
            )

            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)

            // Select "Wallpaper colors" tab
            colorTypes()?.get(1)?.onClick?.invoke()
            // Select a color option to preview
            selectColorOption(colorOptions, 0)
            // Apply the selected color option
            applySelectedColorOption()

            assertThat(logger.themeColorSource).isEqualTo(StyleEnums.COLOR_SOURCE_PRESET_COLOR)
            assertThat(logger.themeColorStyle)
                .isEqualTo(Style.toString(Style.FRUIT_SALAD).hashCode())
            assertThat(logger.themeSeedColor).isEqualTo(-54321)
        }

    @Test
    fun selectColorOption() =
        testScope.runTest {
            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)

            // Initially, the wallpaper color tab should be selected
            assertPickerUiState(
                colorTypes = colorTypes(),
                colorOptions = colorOptions(),
                selectedColorTypeText = "Wallpaper colors",
                selectedColorOptionIndex = 0,
            )

            // Select "Basic colors" tab
            colorTypes()?.get(1)?.onClick?.invoke()
            assertPickerUiState(
                colorTypes = colorTypes(),
                colorOptions = colorOptions(),
                selectedColorTypeText = "Basic colors",
                selectedColorOptionIndex = -1,
            )

            // Select a color option
            selectColorOption(colorOptions, 2)

            // Check original option is no longer selected
            colorTypes()?.get(0)?.onClick?.invoke()
            assertPickerUiState(
                colorTypes = colorTypes(),
                colorOptions = colorOptions(),
                selectedColorTypeText = "Wallpaper colors",
                selectedColorOptionIndex = -1,
            )

            // Check new option is selected
            colorTypes()?.get(1)?.onClick?.invoke()
            assertPickerUiState(
                colorTypes = colorTypes(),
                colorOptions = colorOptions(),
                selectedColorTypeText = "Basic colors",
                selectedColorOptionIndex = 2,
            )
        }

    /** Simulates a user selecting the color option at the given index. */
    private fun TestScope.selectColorOption(
        colorOptions: () -> List<OptionItemViewModel2<ColorOptionIconViewModel>>?,
        index: Int,
    ) {
        val onClickedFlow = colorOptions()?.get(index)?.onClicked
        val onClickedLastValueOrNull: (() -> (() -> Unit)?)? =
            onClickedFlow?.let { collectLastValue(it) }
        onClickedLastValueOrNull?.let { onClickedLastValue ->
            val onClickedOrNull: (() -> Unit)? = onClickedLastValue()
            onClickedOrNull?.invoke()
        }
    }

    /** Simulates a user applying the color option at the given index, and the apply completes. */
    private suspend fun TestScope.applySelectedColorOption() {
        val onApply = collectLastValue(underTest.onApply)()
        testScope.launch { onApply?.invoke() }
        colorUpdateViewModel.updateColors()
    }

    /**
     * Asserts the entire picker UI state is what is expected. This includes the color type tabs and
     * the color options list.
     *
     * @param colorTypes The observed color type view-models, keyed by ColorType
     * @param colorOptions The observed color options
     * @param selectedColorTypeText The text of the color type that's expected to be selected
     * @param selectedColorOptionIndex The index of the color option that's expected to be selected,
     *   -1 stands for no color option should be selected
     */
    private fun TestScope.assertPickerUiState(
        colorTypes: List<FloatingToolbarTabViewModel>?,
        colorOptions: List<OptionItemViewModel2<ColorOptionIconViewModel>>?,
        selectedColorTypeText: String,
        selectedColorOptionIndex: Int,
    ) {
        assertColorTypeTabUiState(
            colorTypes = colorTypes,
            colorTypeId = ColorType.WALLPAPER_COLOR,
            isSelected = "Wallpaper colors" == selectedColorTypeText,
        )
        assertColorTypeTabUiState(
            colorTypes = colorTypes,
            colorTypeId = ColorType.PRESET_COLOR,
            isSelected = "Basic colors" == selectedColorTypeText,
        )
        assertColorOptionUiState(colorOptions, selectedColorOptionIndex)
    }

    /**
     * Asserts the picker section UI state is what is expected.
     *
     * @param colorOptions The observed color options
     * @param selectedColorOptionIndex The index of the color option that's expected to be selected,
     *   -1 stands for no color option should be selected
     */
    private fun TestScope.assertColorOptionUiState(
        colorOptions: List<OptionItemViewModel2<ColorOptionIconViewModel>>?,
        selectedColorOptionIndex: Int,
    ) {
        var foundSelectedColorOption = false
        assertThat(colorOptions).isNotNull()
        if (colorOptions != null) {
            for (i in colorOptions.indices) {
                val colorOptionHasSelectedIndex = i == selectedColorOptionIndex
                val isSelected: Boolean? = collectLastValue(colorOptions[i].isSelected).invoke()
                assertWithMessage(
                        "Expected color option with index \"${i}\" to have" +
                            " isSelected=$colorOptionHasSelectedIndex but it was" +
                            " ${isSelected}, num options: ${colorOptions.size}"
                    )
                    .that(isSelected)
                    .isEqualTo(colorOptionHasSelectedIndex)
                foundSelectedColorOption = foundSelectedColorOption || colorOptionHasSelectedIndex
            }
            if (selectedColorOptionIndex == -1) {
                assertWithMessage(
                        "Expected no color options to be selected, but a color option is" +
                            " selected"
                    )
                    .that(foundSelectedColorOption)
                    .isFalse()
            } else {
                assertWithMessage(
                        "Expected a color option to be selected, but no color option is" +
                            " selected"
                    )
                    .that(foundSelectedColorOption)
                    .isTrue()
            }
        }
    }

    /**
     * Asserts that a color type tab has the correct UI state.
     *
     * @param colorTypes The observed color type view-models, keyed by ColorType enum
     * @param colorTypeId the ID of the color type to assert
     * @param isSelected Whether that color type should be selected
     */
    private fun assertColorTypeTabUiState(
        colorTypes: List<FloatingToolbarTabViewModel>?,
        colorTypeId: ColorType,
        isSelected: Boolean,
    ) {
        val position = if (colorTypeId == ColorType.WALLPAPER_COLOR) 0 else 1
        val viewModel =
            colorTypes?.get(position) ?: error("No color type with ID \"$colorTypeId\"!")
        assertThat(viewModel.isSelected).isEqualTo(isSelected)
    }
}
