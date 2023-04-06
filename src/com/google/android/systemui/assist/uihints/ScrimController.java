package com.google.android.systemui.assist.uihints;

import android.animation.TimeInterpolator;
import android.content.res.ColorStateList;
import android.animation.Animator;
import android.util.Log;
import android.util.MathUtils;
import android.view.SurfaceControl;
import android.graphics.Rect;
import android.graphics.Region;
import java.util.Optional;
import com.google.android.systemui.assist.uihints.edgelights.mode.FullListening;
import com.google.android.systemui.assist.uihints.edgelights.EdgeLightsView;
import android.graphics.BlendMode;
import com.android.systemui.R;
import javax.inject.Named;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import com.google.android.systemui.assist.uihints.input.TouchInsideRegion;
import com.google.android.systemui.assist.uihints.edgelights.EdgeLightsListener;

public class ScrimController implements TranscriptionController.TranscriptionSpaceListener, NgaMessageHandler.CardInfoListener, EdgeLightsListener, TouchInsideRegion
{
    public static final LinearInterpolator ALPHA_INTERPOLATOR;
    public final LightnessProvider mLightnessProvider;
    public float mMedianLightness;
    public final OverlappedElementController mOverlappedElement;
    public final ViewGroup mParent;
    public final View mScrimView;
    public VisibilityListener mVisibilityListener;
    public ValueAnimator mAlphaAnimator;
    public float mInvocationProgress;
    public boolean mTranscriptionVisible;
    public boolean mCardVisible;
    public boolean mHaveAccurateLightness;
    public boolean mInFullListening;
    public boolean mCardTransitionAnimated;
    public boolean mCardForcesScrimGone;
    public boolean mIsDozing;
    
    public ScrimController(@Named("overlay_ui_host_parent_view_group") final ViewGroup viewGroup, final OverlappedElementController overlappedElementController, final LightnessProvider lightnessProvider, final TouchInsideHandler touchInsideHandler) {
        this.mAlphaAnimator = new ValueAnimator();
        this.mInvocationProgress = 0.0f;
        this.mTranscriptionVisible = false;
        this.mCardVisible = false;
        this.mHaveAccurateLightness = false;
        this.mInFullListening = false;
        this.mCardTransitionAnimated = false;
        this.mCardForcesScrimGone = false;
        this.mIsDozing = false;
        this.mParent = viewGroup;
        (this.mScrimView = viewGroup.findViewById(R.id.scrim)).setBackgroundTintBlendMode(BlendMode.SRC_IN);
        this.mLightnessProvider = lightnessProvider;
        this.mScrimView.setOnClickListener((View.OnClickListener)touchInsideHandler);
        this.mScrimView.setOnTouchListener((View.OnTouchListener)touchInsideHandler);
        this.mOverlappedElement = overlappedElementController;
    }
    
    public void onCardInfo(final boolean z, final boolean z2, final int i, final boolean z3) {
        this.mCardVisible = z;
        this.mCardTransitionAnimated = z2;
        this.mCardForcesScrimGone = z3;
        this.refresh();
    }
    
    public void onModeStarted(final EdgeLightsView.Mode mode) {
        this.mInFullListening = (mode instanceof FullListening);
        this.refresh();
    }
    
    public void onStateChanged(final TranscriptionController.State state, final TranscriptionController.State state2) {
        final boolean z = state2 != TranscriptionController.State.NONE;
        if (this.mTranscriptionVisible == z) {
            return;
        }
        this.mTranscriptionVisible = z;
        this.refresh();
    }
    
    public boolean isVisible() {
        return this.mScrimView.getVisibility() == 0;
    }
    
    public void setVisibilityListener(final VisibilityListener visibilityListener) {
        this.mVisibilityListener = visibilityListener;
    }
    
    public Optional<Region> getTouchInsideRegion() {
        if (!this.isVisible()) {
            return Optional.empty();
        }
        final Rect rect = new Rect();
        this.mScrimView.getHitRect(rect);
        rect.top = rect.bottom - this.mScrimView.getResources().getDimensionPixelSize(R.dimen.scrim_touchable_height);
        return Optional.of(new Region(rect));
    }
    
    public SurfaceControl getSurfaceControllerHandle() {
        if (this.mScrimView.getViewRootImpl() == null) {
            return null;
        }
        return this.mScrimView.getViewRootImpl().getSurfaceControl();
    }
    
    public void setInvocationProgress(final float f) {
        final float constrain = MathUtils.constrain(f, 0.0f, 1.0f);
        if (this.mInvocationProgress == constrain) {
            return;
        }
        this.mInvocationProgress = constrain;
        this.refresh();
    }
    
    public void setIsDozing(final boolean z) {
        this.mIsDozing = z;
        this.refresh();
    }
    
    public void setHasMedianLightness(final float f) {
        this.mHaveAccurateLightness = true;
        this.mMedianLightness = f;
        this.refresh();
    }
    
    public void onLightnessInvalidated() {
        this.mHaveAccurateLightness = false;
        this.refresh();
    }
    
    public void refresh() {
        if (!this.mHaveAccurateLightness || this.mIsDozing) {
            this.setRelativeAlpha(0.0f, false);
            return;
        }
        final boolean z = this.mCardVisible;
        if (z && this.mCardForcesScrimGone) {
            this.setRelativeAlpha(0.0f, this.mCardTransitionAnimated);
        }
        else if (this.mInFullListening || this.mTranscriptionVisible) {
            if (z && !this.isVisible()) {
                return;
            }
            this.setRelativeAlpha(1.0f, false);
        }
        else if (z) {
            this.setRelativeAlpha(0.0f, this.mCardTransitionAnimated);
        }
        else {
            final float f = this.mInvocationProgress;
            if (f > 0.0f) {
                this.setRelativeAlpha(Math.min(1.0f, f), false);
            }
            else {
                this.setRelativeAlpha(0.0f, true);
            }
        }
    }
    
    public void setRelativeAlpha(final float f, final boolean z) {
        if (this.mHaveAccurateLightness || f <= 0.0f) {
            if (f < 0.0f || f > 1.0f) {
                Log.e("ScrimController", "Got unexpected alpha: " + f + ", ignoring");
                return;
            }
            if (this.mAlphaAnimator.isRunning()) {
                this.mAlphaAnimator.cancel();
            }
            if (f <= 0.0f) {
                if (z) {
                    final ValueAnimator createRelativeAlphaAnimator = this.createRelativeAlphaAnimator(f);
                    (this.mAlphaAnimator = createRelativeAlphaAnimator).addListener((Animator.AnimatorListener)new ScrimController$1(this));
                    this.mAlphaAnimator.start();
                    return;
                }
                this.setAlpha(0.0f);
                this.setVisibility(8);
            }
            else {
                if (this.mScrimView.getVisibility() != 0) {
                    this.mScrimView.setBackgroundTintList(ColorStateList.valueOf((this.mMedianLightness <= 0.4f) ? -16777216 : -1));
                    this.setVisibility(0);
                }
                if (z) {
                    final ValueAnimator createRelativeAlphaAnimator2 = this.createRelativeAlphaAnimator(f);
                    (this.mAlphaAnimator = createRelativeAlphaAnimator2).start();
                    return;
                }
                this.setAlpha(f);
            }
        }
    }
    
    public void setVisibility(final int i) {
        if (i == this.mScrimView.getVisibility()) {
            return;
        }
        this.mScrimView.setVisibility(i);
        final VisibilityListener visibilityListener = this.mVisibilityListener;
        if (visibilityListener != null) {
            visibilityListener.onVisibilityChanged(i);
        }
        this.mLightnessProvider.setMuted(i == 0);
        final View view = this.mScrimView;
        view.setBackground((i == 0) ? view.getContext().getDrawable(R.drawable.scrim_strip) : null);
        if (i == 0) {
            return;
        }
        this.mOverlappedElement.setAlpha(1.0f);
        this.refresh();
    }
    
    public void setAlpha(final float f) {
        this.mScrimView.setAlpha(f);
        this.mOverlappedElement.setAlpha(1.0f - f);
    }
    
    public ValueAnimator createRelativeAlphaAnimator(final float f) {
        final ValueAnimator duration = ValueAnimator.ofFloat(new float[] { this.mScrimView.getAlpha(), f }).setDuration((long)(Math.abs(f - this.mScrimView.getAlpha()) * 300.0f));
        duration.setInterpolator((TimeInterpolator)ScrimController.ALPHA_INTERPOLATOR);
        duration.addUpdateListener(valueAnimator -> this.setAlpha((float)valueAnimator.getAnimatedValue()));
        return duration;
    }
    
    static {
        ALPHA_INTERPOLATOR = new LinearInterpolator();
    }
}
