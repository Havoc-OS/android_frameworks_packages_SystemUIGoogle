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
package com.google.android.systemui.dagger

import com.android.systemui.dagger.GlobalModule
import com.android.systemui.dagger.GlobalRootComponent
import javax.inject.Singleton
import dagger.Component

/**
 * Root component for Dagger injection for System UI Google.
 */
@Singleton
@Component(modules = [GlobalModule::class])
interface SysUIGoogleGlobalRootComponent : GlobalRootComponent {
    /**
     * Component Builder interface. This allows to bind Context instance in the component
     */
    @Component.Builder
    interface Builder : GlobalRootComponent.Builder {
        override fun build(): SysUIGoogleGlobalRootComponent
    }

    /**
     * Builder method for the Google System UI subcomponent.
     */
    override fun getSysUIComponent(): SysUIGoogleSysUIComponent.Builder
}
