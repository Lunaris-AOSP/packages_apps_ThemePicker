package com.android.customization.testing

import android.app.WallpaperColors
import android.content.Context
import android.content.res.Resources
import androidx.activity.ComponentActivity
import com.android.customization.model.color.WallpaperColorResources
import com.android.customization.module.CustomizationInjector
import com.android.customization.module.CustomizationPreferences
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.wallpaper.module.NetworkStatusNotifier
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.network.Requester
import com.android.wallpaper.picker.category.wrapper.WallpaperCategoryWrapper
import com.android.wallpaper.picker.customization.data.repository.WallpaperColorsRepository
import com.android.wallpaper.picker.customization.domain.interactor.WallpaperInteractor
import com.android.wallpaper.testing.FakeCurrentWallpaperInfoFactory
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.FakeWallpaperRefresher
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestPackageStatusNotifier
import com.android.wallpaper.util.DisplayUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class TestCustomizationInjector
@Inject
constructor(
    private val customPrefs: TestDefaultCustomizationPreferences,
    private val themesUserEventLogger: ThemesUserEventLogger,
    displayUtils: DisplayUtils,
    requester: Requester,
    networkStatusNotifier: NetworkStatusNotifier,
    partnerProvider: PartnerProvider,
    wallpaperClient: FakeWallpaperClient,
    injectedWallpaperInteractor: WallpaperInteractor,
    prefs: WallpaperPreferences,
    private val fakeWallpaperCategoryWrapper: WallpaperCategoryWrapper,
    testStatusNotifier: TestPackageStatusNotifier,
    currentWallpaperInfoFactory: FakeCurrentWallpaperInfoFactory,
    wallpaperRefresher: FakeWallpaperRefresher,
) :
    TestInjector(
        themesUserEventLogger,
        displayUtils,
        requester,
        networkStatusNotifier,
        partnerProvider,
        wallpaperClient,
        injectedWallpaperInteractor,
        prefs,
        fakeWallpaperCategoryWrapper,
        testStatusNotifier,
        currentWallpaperInfoFactory,
        wallpaperRefresher,
    ),
    CustomizationInjector {
    /////////////////
    // CustomizationInjector implementations
    /////////////////

    override fun getCustomizationPreferences(context: Context): CustomizationPreferences {
        return customPrefs
    }

    override fun getKeyguardQuickAffordancePickerInteractor(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getWallpaperColorResources(
        wallpaperColors: WallpaperColors,
        context: Context,
    ): WallpaperColorResources {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getColorPickerViewModelFactory(context: Context): ColorPickerViewModel.Factory {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockCarouselViewModelFactory(
        interactor: ClockPickerInteractor,
        clockViewFactory: ClockViewFactory,
        resources: Resources,
    ): ClockCarouselViewModel.Factory {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockViewFactory(activity: ComponentActivity): ClockViewFactory {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockSettingsViewModelFactory(
        context: Context,
        wallpaperColorsRepository: WallpaperColorsRepository,
    ): ClockSettingsViewModel.Factory {
        throw UnsupportedOperationException("not implemented")
    }

    /////////////////
    // TestInjector overrides
    /////////////////

    override fun getUserEventLogger(): UserEventLogger {
        return themesUserEventLogger
    }

    override fun getWallpaperCategoryWrapper(): WallpaperCategoryWrapper {
        return fakeWallpaperCategoryWrapper
    }
}
