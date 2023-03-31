/*
 * Copyright (C) 2023 Benzo Rom
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
package com.google.android.systemui.statusbar.policy

import android.content.Context
import android.os.Handler
import android.os.PowerManager
import com.android.systemui.broadcast.BroadcastDispatcher
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.demomode.DemoModeController
import com.android.systemui.dump.DumpManager
import com.android.systemui.power.EnhancedEstimates
import com.android.systemui.settings.UserContentResolverProvider
import com.android.systemui.statusbar.policy.AospPolicyModule
import com.google.android.systemui.reversecharging.ReverseChargingController
import dagger.Module
import dagger.Provides

/**
 * com.google.android.systemui.statusbar.policy related providers that others may want to override.
 */
@Module(includes = [AospPolicyModule::class])
object GooglePolicyModule {
    @Provides
    @SysUISingleton
    fun provideBatteryControllerGoogle(
        context: Context,
        enhancedEstimates: EnhancedEstimates,
        powerManager: PowerManager,
        broadcastDispatcher: BroadcastDispatcher,
        demoModeController: DemoModeController,
        dumpManager: DumpManager,
        @Main mainHandler: Handler,
        @Background bgHandler: Handler,
        contentResolver: UserContentResolverProvider,
        reverseChargingController: ReverseChargingController
    ): BatteryControllerImplGoogle {
        val bC = BatteryControllerImplGoogle(
            context,
            enhancedEstimates,
            powerManager,
            broadcastDispatcher,
            demoModeController,
            dumpManager,
            mainHandler,
            bgHandler,
            contentResolver,
            reverseChargingController
        )
        bC.init()
        return bC
    }
}
