-include ../../../frameworks/base/packages/SystemUI/proguard.flags

-keep class com.android.systemui.smartspace.** { *; }
-keep class android.frameworks.** { *; }
-keep class android.hardware.** { *; }
-keep class com.google.android.systemui.SystemUIGoogleInitializer
-keep class ** extends com.android.systemui.SystemUIInitializer { *;}
-keep class com.google.android.systemui.dagger.DaggerSysUIGoogleGlobalRootComponent$SysUIGoogleSysUIComponentImpl { *; }
-keep class com.google.** { *; }
-keep class vendor.google.** { *; }

-dontwarn com.google.android.systemui.dreamliner.*
