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

package com.android.customization.picker.clock.domain.interactor

import android.text.TextUtils
import android.util.Log
import com.android.customization.picker.clock.data.repository.ClockPickerRepository
import com.android.customization.picker.clock.shared.model.ClockSnapshotModel
import com.android.systemui.plugins.clocks.ClockAxisStyle
import com.android.wallpaper.picker.undo.domain.interactor.SnapshotRestorer
import com.android.wallpaper.picker.undo.domain.interactor.SnapshotStore
import com.android.wallpaper.picker.undo.shared.model.RestorableSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray

/** Handles state restoration for clocks. */
@Singleton
class ClockPickerSnapshotRestorer
@Inject
constructor(private val repository: ClockPickerRepository) : SnapshotRestorer {
    private var snapshotStore: SnapshotStore = SnapshotStore.NOOP
    private var originalOption: ClockSnapshotModel? = null

    override suspend fun setUpSnapshotRestorer(store: SnapshotStore): RestorableSnapshot {
        snapshotStore = store
        originalOption =
            ClockSnapshotModel(
                clockId =
                    repository.selectedClock
                        .map { clock -> clock.clockId }
                        .distinctUntilChanged()
                        .firstOrNull(),
                clockSize = repository.selectedClockSize.firstOrNull(),
                colorToneProgress =
                    repository.selectedClock.map { clock -> clock.colorToneProgress }.firstOrNull(),
                selectedColorId =
                    repository.selectedClock
                        .map { clock -> clock.selectedColorId }
                        .distinctUntilChanged()
                        .firstOrNull(),
                seedColor = repository.selectedClock.map { clock -> clock.seedColor }.firstOrNull(),
                axisSettings =
                    repository.selectedClock
                        .map { clock -> clock.axisPresetConfig?.current?.style ?: ClockAxisStyle() }
                        .firstOrNull(),
            )
        return snapshot(originalOption)
    }

    override suspend fun restoreToSnapshot(snapshot: RestorableSnapshot) {
        originalOption?.let { optionToRestore ->
            if (
                TextUtils.isEmpty(optionToRestore.clockId) ||
                    optionToRestore.clockId != snapshot.args[KEY_CLOCK_ID] ||
                    optionToRestore.clockSize?.toString() != snapshot.args[KEY_CLOCK_SIZE] ||
                    optionToRestore.colorToneProgress?.toString() !=
                        snapshot.args[KEY_COLOR_TONE_PROGRESS] ||
                    optionToRestore.seedColor?.toString() != snapshot.args[KEY_SEED_COLOR] ||
                    optionToRestore.selectedColorId != snapshot.args[KEY_COLOR_ID] ||
                    (optionToRestore.axisSettings ?: ClockAxisStyle()) !=
                        ClockAxisStyle.fromJson(JSONArray(snapshot.args[KEY_FONT_AXES]))
            ) {
                Log.wtf(
                    TAG,
                    """ Original clock option does not match snapshot option to restore to. The
                        | current implementation doesn't support undo, only a reset back to the
                        | original clock option."""
                        .trimMargin(),
                )
            }

            optionToRestore.clockSize?.let { repository.setClockSize(it) }
            optionToRestore.colorToneProgress?.let {
                repository.setClockColor(
                    selectedColorId = optionToRestore.selectedColorId,
                    colorToneProgress = optionToRestore.colorToneProgress,
                    seedColor = optionToRestore.seedColor,
                )
            }
            optionToRestore.clockId?.let { repository.setSelectedClock(it) }
            optionToRestore.axisSettings?.let { repository.setClockAxisStyle(it) }
        }
    }

    fun storeSnapshot(clockSnapshotModel: ClockSnapshotModel) {
        snapshotStore.store(snapshot(clockSnapshotModel))
    }

    private fun snapshot(clockSnapshotModel: ClockSnapshotModel? = null): RestorableSnapshot {
        val options =
            if (clockSnapshotModel == null) emptyMap()
            else {
                buildMap {
                    clockSnapshotModel.clockId?.let { put(KEY_CLOCK_ID, it) }
                    clockSnapshotModel.clockSize?.let { put(KEY_CLOCK_SIZE, it.toString()) }
                    clockSnapshotModel.selectedColorId?.let { put(KEY_COLOR_ID, it) }
                    clockSnapshotModel.colorToneProgress?.let {
                        put(KEY_COLOR_TONE_PROGRESS, it.toString())
                    }
                    clockSnapshotModel.seedColor?.let { put(KEY_SEED_COLOR, it.toString()) }
                    clockSnapshotModel.axisSettings?.let {
                        put(KEY_FONT_AXES, ClockAxisStyle.toJson(it).toString())
                    }
                }
            }

        return RestorableSnapshot(options)
    }

    companion object {
        private const val TAG = "ClockPickerSnapshotRestorer"
        private const val KEY_CLOCK_ID = "clock_id"
        private const val KEY_CLOCK_SIZE = "clock_size"
        private const val KEY_COLOR_ID = "color_id"
        private const val KEY_COLOR_TONE_PROGRESS = "color_tone_progress"
        private const val KEY_SEED_COLOR = "seed_color"
        private const val KEY_FONT_AXES = "font_axes"
    }
}
