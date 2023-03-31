/*
 * Copyright (C) 2022 Benzo Rom
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
package com.google.android.systemui

import android.content.Context
import com.android.internal.logging.UiEventLogger
import com.android.systemui.Dumpable
import com.android.systemui.KtR
import com.android.systemui.VendorServices
import com.android.systemui.dagger.SysUISingleton
import com.google.android.systemui.autorotate.AutorotateDataService
import com.google.android.systemui.columbus.ColumbusServiceWrapper
import com.google.android.systemui.coversheet.CoversheetService
import com.google.android.systemui.elmyra.ElmyraContext
import com.google.android.systemui.elmyra.ElmyraService
import com.google.android.systemui.elmyra.ServiceConfigurationGoogle
import com.google.android.systemui.face.FaceNotificationService
import com.google.android.systemui.input.TouchContextService
import dagger.Lazy
import java.io.PrintWriter
import javax.inject.Inject

@SysUISingleton
class GoogleServices
@Inject
constructor(
    private val context: Context,
    private val serviceConfigurationGoogle: Lazy<ServiceConfigurationGoogle>,
    private val uiEventLogger: UiEventLogger,
    private val columbusServiceLazy: Lazy<ColumbusServiceWrapper>,
    private val autorotateDataService: AutorotateDataService,
    private val faceNotificationServiceLazy: Lazy<FaceNotificationService>
) : VendorServices() {
    private val services = ArrayList<Any>()
    override fun start() {
        addService(DisplayCutoutEmulationAdapter(context))
        addService(CoversheetService(context))
        autorotateDataService.run {
            init()
            addService(this)
        }
        if (context.packageManager.hasSystemFeature("android.hardware.context_hub")
            && ElmyraContext(context).isAvailable
        ) {
            addService(ElmyraService(context, serviceConfigurationGoogle.get(), uiEventLogger))
        }
        if (context.packageManager.hasSystemFeature("com.google.android.feature.QUICK_TAP")) {
            addService(columbusServiceLazy.get())
        }
        if (context.packageManager.hasSystemFeature("android.hardware.biometrics.face")) {
            addService(faceNotificationServiceLazy.get())
        }
        if (context.resources.getBoolean(KtR.bool.config_touch_context_enabled)) {
            addService(TouchContextService(context))
        }
    }

    private fun addService(service: Any?) {
        when { service != null -> services.add(service) }
    }

    override fun dump(pw: PrintWriter, args: Array<String>) {
        services.indices.forEach {
            if (services[it] is Dumpable) {
                (services[it] as Dumpable).dump(pw, args)
            }
        }
    }
}
