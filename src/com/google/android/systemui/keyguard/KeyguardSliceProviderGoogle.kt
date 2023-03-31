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
@file:Suppress("DEPRECATION")
package com.google.android.systemui.keyguard

import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import android.text.TextUtils
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.Slice
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.ListBuilder.*
import androidx.slice.builders.SliceAction
import com.android.systemui.KtR
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.keyguard.KeyguardSliceProvider
import com.google.android.systemui.smartspace.*
import java.lang.ref.WeakReference
import javax.inject.Inject

@SysUISingleton
class KeyguardSliceProviderGoogle : KeyguardSliceProvider(), SmartSpaceUpdateListener {
    @Inject lateinit var smartSpaceController: SmartSpaceController
    private var smartSpaceData: SmartSpaceData? = null
    private var hideSensitiveContent = false
    private var hideWorkContent = true
    override fun updateClockLocked() = notifyChange()

    override fun onCreateSliceProvider(): Boolean {
        val created = super.onCreateSliceProvider()
        smartSpaceData = SmartSpaceData()
        smartSpaceController.addListener(this)
        return created
    }

    override fun onDestroy() {
        super.onDestroy()
        smartSpaceController.removeListener(this)
    }

    override fun onBindSlice(sliceUri: Uri): Slice {
        val builder = ListBuilder(context!!, mSliceUri, INFINITY)
        synchronized(this) {
            var hasAction = false
            val currentCard = smartSpaceData!!.currentCard
            if (
                currentCard != null &&
                    !currentCard.isExpired &&
                    !TextUtils.isEmpty(currentCard.title)
            ) {
                if (
                    !currentCard.isSensitive ||
                        currentCard.isSensitive &&
                            !hideSensitiveContent &&
                            !currentCard.isWorkProfile ||
                        currentCard.isSensitive && !hideWorkContent && currentCard.isWorkProfile
                )
                    hasAction = true
            }
            if (hasAction) {
                var action: SliceAction? = null
                val cardBitmap =
                    if (currentCard!!.icon != null) IconCompat.createWithBitmap(currentCard.icon)
                    else null
                if (cardBitmap != null && currentCard.pendingIntent != null)
                    action =
                        SliceAction.create(
                            currentCard.pendingIntent,
                            cardBitmap,
                            SMALL_IMAGE,
                            currentCard.title
                        )
                val cardTitle = HeaderBuilder(mHeaderUri).setTitle(currentCard.formattedTitle)
                if (action != null) {
                    cardTitle.setPrimaryAction(action)
                }
                builder.setHeader(cardTitle)
                if (currentCard.subTitle != null) {
                    val calendarTitle = RowBuilder(calendarUri).setTitle(currentCard.subTitle)
                    if (cardBitmap != null) {
                        calendarTitle.addEndItem(cardBitmap, SMALL_IMAGE)
                    }
                    if (action != null) {
                        calendarTitle.setPrimaryAction(action)
                    }
                    builder.addRow(calendarTitle)
                }
                addZenModeLocked(builder)
                addPrimaryActionLocked(builder)
                return builder.build()
            }
            if (needsMediaLocked()) {
                addMediaLocked(builder)
            } else {
                builder.addRow(RowBuilder(mDateUri).setTitle(formattedDateLocked))
            }
            addWeather(builder)
            addNextAlarmLocked(builder)
            addZenModeLocked(builder)
            addPrimaryActionLocked(builder)
            val slice = builder.build()
            if (isDebug) Log.d(logTag, "Binding slice: $slice")
            return slice
        }
    }

    private fun addWeather(sliceBuilder: ListBuilder) {
        val weatherCard = smartSpaceData!!.weatherCard
        if (weatherCard != null && !weatherCard.isExpired) {
            val card = RowBuilder(weatherUri).setTitle(weatherCard.title)
            if (weatherCard.icon != null) {
                val weatherIcon = IconCompat.createWithBitmap(weatherCard.icon)
                weatherIcon.setTintMode(PorterDuff.Mode.DST)
                card.addEndItem(weatherIcon, SMALL_IMAGE)
            }
            sliceBuilder.addRow(card)
        }
    }

    override fun onSensitiveModeChanged(hidePrivateData: Boolean, isWorkProfile: Boolean) {
        var notify = false
        synchronized(this) {
            if (hideSensitiveContent != hidePrivateData) {
                hideSensitiveContent = hidePrivateData
                if (isDebug) Log.d(logTag, "Public mode changed, hide data: $hidePrivateData")
                notify = true
            }
            if (hideWorkContent != isWorkProfile) {
                hideWorkContent = isWorkProfile
                if (isDebug) Log.d(logTag, "Public work mode changed, hide data: $isWorkProfile")
                notify = true
            }
        }
        when {
            notify -> notifyChange()
        }
    }

    override fun onSmartSpaceUpdated(data: SmartSpaceData) {
        synchronized(this) { smartSpaceData = data }
        val card = data.weatherCard
        if (card != null && card.icon != null && !card.isIconProcessed) {
            card.isIconProcessed = true
            AddShadowTask(this, card).execute(card.icon)
        } else {
            notifyChange()
        }
    }

    private class AddShadowTask(
        sliceProviderGoogle: KeyguardSliceProviderGoogle,
        smartSpaceCard: SmartSpaceCard
    ) : AsyncTask<Bitmap?, Void?, Bitmap>() {
        private val blurRadius: Float
        private val providerReference: WeakReference<KeyguardSliceProviderGoogle?>
        private val weatherCard: SmartSpaceCard

        init {
            blurRadius =
                sliceProviderGoogle.context!!
                    .resources
                    .getDimension(KtR.dimen.smartspace_icon_shadow)
            providerReference = WeakReference(sliceProviderGoogle)
            weatherCard = smartSpaceCard
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(bitmap: Bitmap) {
            var sliceProviderGoogle: KeyguardSliceProviderGoogle?
            synchronized(this) {
                weatherCard.icon = bitmap
                sliceProviderGoogle = providerReference.get()
            }
            if (sliceProviderGoogle != null) {
                sliceProviderGoogle?.notifyChange()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Bitmap?): Bitmap {
            val image: Bitmap = params[0]!!
            val blurMaskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            val blurPaint = Paint()
            blurPaint.maskFilter = blurMaskFilter
            val offset = IntArray(2)
            val extractAlpha = image.extractAlpha(blurPaint, offset)
            val iconWithShadow =
                Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val paint = Paint()
            Canvas(iconWithShadow).run {
                paint.alpha = 70
                drawBitmap(extractAlpha, offset[0].toFloat(), blurRadius / offset[1] + 2.0f, paint)
                extractAlpha.recycle()
                paint.alpha = 255
                drawBitmap(image, 0.0f, 0.0f, paint)
            }
            return iconWithShadow
        }
    }
}

private const val logTag = "KeyguardSliceProvider"
private val isDebug = Log.isLoggable(logTag, Log.DEBUG)
private val calendarUri = Uri.parse("content://com.android.systemui.keyguard/smartSpace/calendar")
private val weatherUri = Uri.parse("content://com.android.systemui.keyguard/smartSpace/weather")
