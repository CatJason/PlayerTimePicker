package com.bilibili.playerbizcommon.widget.function.timer

import android.content.Context
import android.hardware.SensorManager
import android.view.ViewConfiguration
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sign

class PlayerTimerPickerScroller(
    context: Context, interpolator: Interpolator? = null
) {
    private var mInterpolator: Interpolator? = null
    private var mMode = 0

    /**
     * Returns the start X offset in the scroll.
     *
     * @return The start X offset as an absolute distance from the origin.
     */
    var mStartX = 0
        private set

    /**
     * Returns the start Y offset in the scroll.
     *
     * @return The start Y offset as an absolute distance from the origin.
     */
    var mStartY = 0
        private set
    var mFinalX = 0
        private set
    var mFinalY = 0
        private set
    private var mMinX = 0
    private var mMaxX = 0
    private var mMinY = 0
    private var mMaxY = 0

    /**
     * Returns the current X offset in the scroll.
     *
     * @return The new X offset as an absolute distance from the origin.
     */
    var mCurrX = 0
        private set

    /**
     * Returns the current Y offset in the scroll.
     *
     * @return The new Y offset as an absolute distance from the origin.
     */
    var mCurrY = 0
        private set
    private var mStartTime: Long = 0

    /**
     * Returns how long the scroll event will take, in milliseconds.
     *
     * @return The duration of the scroll in milliseconds.
     */
    var duration = 0
        private set
    private var mDurationReciprocal = 0f
    private var mDeltaX = 0f
    private var mDeltaY = 0f

    /**
     *
     * Returns whether the scroller has finished scrolling.
     *
     * @return True if the scroller has finished scrolling, false otherwise.
     */
    var mIsFinished = true
        private set
    private var mVelocity = 0f
    private var mCurrVelocity = 0f
    private var mDistance = 0
    private var mFlingFriction = ViewConfiguration.getScrollFriction()
    private var mDeceleration: Float
    private val mPpi: Float

    // A context-specific coefficient adjusted to physical values.
    private val mPhysicalCoeff: Float

    companion object {
        private const val DEFAULT_DURATION = 250
        private const val SCROLL_MODE = 0
        private const val FLING_MODE = 1
        private val DECELERATION_RATE = (Math.log(0.78) / Math.log(0.9)).toFloat()
        private const val INFLEXION = 0.35f // Tension lines cross at (INFLEXION, 1)
        private const val START_TENSION = 0.5f
        private const val END_TENSION = 1.0f
        private val P1: Float =
            START_TENSION * INFLEXION
        private val P2: Float =
            1.0f - END_TENSION * (1.0f - INFLEXION)
        private const val NB_SAMPLES = 100
        private val SPLINE_POSITION =
            FloatArray(NB_SAMPLES + 1)
        private val SPLINE_TIME =
            FloatArray(NB_SAMPLES + 1)

        init {
            var minX = 0.0f
            var minY = 0.0f
            for (i in 0 until NB_SAMPLES) {
                val alpha: Float = i.toFloat() / NB_SAMPLES
                var maxX = 1.0f
                var x = 0f
                var tx = 0f
                var coeF = 0f
                while (true) {
                    x = minX + (maxX - minX) / 2.0f
                    coeF = 3.0f * x * (1.0f - x)
                    tx = coeF * ((1.0f - x) * P1 + x * P2) + x * x * x
                    if (abs(tx - alpha) < 1E-5) break
                    if (tx > alpha) {
                        maxX = x
                    } else {
                        minX = x
                    }
                }
                SPLINE_POSITION[i] = coeF * ((1.0f - x) * START_TENSION + x) + x * x * x
                var maxY = 1.0f
                var y = 0f
                var dy = 0f
                while (true) {
                    y = minY + (maxY - minY) / 2.0f
                    coeF = 3.0f * y * (1.0f - y)
                    dy = coeF * ((1.0f - y) * START_TENSION + y) + y * y * y
                    if (abs(dy - alpha) < 1E-5) break
                    if (dy > alpha) {
                        maxY = y
                    } else {
                        minY = y
                    }
                }
                SPLINE_TIME[i] = coeF * ((1.0f - y) * P1 + y * P2) + y * y * y
            }
            SPLINE_TIME[NB_SAMPLES] = 1.0f
            SPLINE_POSITION[NB_SAMPLES] = SPLINE_TIME[NB_SAMPLES]
        }
    }

    /**
     * The amount of friction applied to flings. The default value
     * is [ViewConfiguration.getScrollFriction].
     *
     * @param friction A scalar dimension-less value representing the coefficient of
     * friction.
     */
    fun setFriction(friction: Float) {
        mDeceleration = computeDeceleration(friction)
        mFlingFriction = friction
    }

    private fun computeDeceleration(friction: Float): Float {
        return (SensorManager.GRAVITY_EARTH // g (m/s^2)
            * 39.37f // inch/meter
            * mPpi // pixels per inch
            * friction)
    }

    /**
     * Force the finished field to a particular value.
     *
     * @param finished The new finished value.
     */
    fun forceFinished(finished: Boolean) {
        mIsFinished = finished
    }

    /**
     * Returns the current velocity.
     *
     * @return The original velocity less the deceleration. Result may be
     * negative.
     */
    val currVelocity: Float
        get() = if (mMode == FLING_MODE) mCurrVelocity else mVelocity - mDeceleration * timePassed() / 2000.0f
    /**
     * Returns where the scroll will end. Valid only for "fling" scrolls.
     *
     * @return The final X offset as an absolute distance from the origin.
     */
    /**
     * Sets the final position (X) for this scroller.
     *
     * @param newX The new X offset as an absolute distance from the origin.
     * @see .extendDuration
     * @see .setFinalY
     */
    var finalX: Int
        get() = mFinalX
        set(newX) {
            mFinalX = newX
            mDeltaX = (mFinalX - mStartX).toFloat()
            mIsFinished = false
        }
    /**
     * Returns where the scroll will end. Valid only for "fling" scrolls.
     *
     * @return The final Y offset as an absolute distance from the origin.
     */
    /**
     * Sets the final position (Y) for this scroller.
     *
     * @param newY The new Y offset as an absolute distance from the origin.
     * @see .extendDuration
     * @see .setFinalX
     */
    var finalY: Int
        get() = mFinalY
        set(newY) {
            mFinalY = newY
            mDeltaY = (mFinalY - mStartY).toFloat()
            mIsFinished = false
        }

    /**
     * Call this when you want to know the new location.  If it returns true,
     * the animation is not yet finished.
     */
    fun computeScrollOffset(): Boolean {
        if (mIsFinished) {
            return false
        }
        val timePassed = (AnimationUtils.currentAnimationTimeMillis() - mStartTime).toInt()
        if (timePassed < duration) {
            when (mMode) {
                SCROLL_MODE -> {
                    val x = mInterpolator!!.getInterpolation(timePassed * mDurationReciprocal)
                    mCurrX = mStartX + (x * mDeltaX).roundToInt()
                    mCurrY = mStartY + (x * mDeltaY).roundToInt()
                }
                FLING_MODE -> {
                    val t = timePassed.toFloat() / duration
                    val index = (NB_SAMPLES * t).toInt()
                    var distanceCoeF = 1f
                    var velocityCoeF = 0f
                    if (index < NB_SAMPLES) {
                        val infT: Float =
                            index.toFloat() / NB_SAMPLES
                        val supT: Float = (index + 1).toFloat() / NB_SAMPLES
                        val infD: Float =
                            SPLINE_POSITION[index]
                        val supD: Float =
                            SPLINE_POSITION[index + 1]
                        velocityCoeF = (supD - infD) / (supT - infT)
                        distanceCoeF = infD + (t - infT) * velocityCoeF
                    }
                    mCurrVelocity = velocityCoeF * mDistance / duration * 1000.0f
                    mCurrX = mStartX + (distanceCoeF * (mFinalX - mStartX)).roundToInt()
                    // Pin to mMinX <= mCurrX <= mMaxX
                    mCurrX = mCurrX.coerceAtMost(mMaxX)
                    mCurrX = mCurrX.coerceAtLeast(mMinX)
                    mCurrY = mStartY + Math.round(distanceCoeF * (mFinalY - mStartY))
                    // Pin to mMinY <= mCurrY <= mMaxY
                    mCurrY = mCurrY.coerceAtMost(mMaxY)
                    mCurrY = mCurrY.coerceAtLeast(mMinY)
                    if (mCurrX == mFinalX && mCurrY == mFinalY) {
                        mIsFinished = true
                    }
                }
            }
        } else {
            mCurrX = mFinalX
            mCurrY = mFinalY
            mIsFinished = true
        }
        return true
    }
    /**
     * Start scrolling by providing a starting point, the distance to travel,
     * and the duration of the scroll.
     *
     * @param startX Starting horizontal scroll offset in pixels. Positive
     * numbers will scroll the content to the left.
     * @param startY Starting vertical scroll offset in pixels. Positive numbers
     * will scroll the content up.
     * @param dx Horizontal distance to travel. Positive numbers will scroll the
     * content to the left.
     * @param dy Vertical distance to travel. Positive numbers will scroll the
     * content up.
     * @param duration Duration of the scroll in milliseconds.
     */
    /**
     * Start scrolling by providing a starting point and the distance to travel.
     * The scroll will use the default value of 250 milliseconds for the
     * duration.
     *
     * @param startX Starting horizontal scroll offset in pixels. Positive
     * numbers will scroll the content to the left.
     * @param startY Starting vertical scroll offset in pixels. Positive numbers
     * will scroll the content up.
     * @param dx Horizontal distance to travel. Positive numbers will scroll the
     * content to the left.
     * @param dy Vertical distance to travel. Positive numbers will scroll the
     * content up.
     */
    @JvmOverloads
    fun startScroll(
        startX: Int,
        startY: Int,
        dx: Int,
        dy: Int,
        duration: Int = DEFAULT_DURATION
    ) {
        mMode = Companion.SCROLL_MODE
        mIsFinished = false
        this.duration = duration
        mStartTime = AnimationUtils.currentAnimationTimeMillis()
        this.mStartX = startX
        this.mStartY = startY
        mFinalX = startX + dx
        mFinalY = startY + dy
        mDeltaX = dx.toFloat()
        mDeltaY = dy.toFloat()
        mDurationReciprocal = 1.0f / this.duration.toFloat()
    }

    /**
     * Start scrolling based on a fling gesture. The distance travelled will
     * depend on the initial velocity of the fling.
     *
     * @param startX Starting point of the scroll (X)
     * @param startY Starting point of the scroll (Y)
     * @param velocityX Initial velocity of the fling (X) measured in pixels per
     * second.
     * @param velocityY Initial velocity of the fling (Y) measured in pixels per
     * second
     * @param minX Minimum X value. The scroller will not scroll past this
     * point.
     * @param maxX Maximum X value. The scroller will not scroll past this
     * point.
     * @param minY Minimum Y value. The scroller will not scroll past this
     * point.
     * @param maxY Maximum Y value. The scroller will not scroll past this
     * point.
     */
    fun fling(
        startX: Int, startY: Int, velocityX: Int, velocityY: Int,
        minX: Int, maxX: Int, minY: Int, maxY: Int
    ) {
        // Continue a scroll or fling in progress
        var velocityX = velocityX
        var velocityY = velocityY
        if (!mIsFinished) {
            val oldVel = currVelocity
            val dx = (mFinalX - this.mStartX).toFloat()
            val dy = (mFinalY - this.mStartY).toFloat()
            val hyp = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val ndx = dx / hyp
            val ndy = dy / hyp
            val oldVelocityX = ndx * oldVel
            val oldVelocityY = ndy * oldVel
            if (sign(velocityX.toFloat()) == sign(oldVelocityX) &&
                sign(velocityY.toFloat()) == sign(oldVelocityY)
            ) {
                velocityX += oldVelocityX.toInt()
                velocityY += oldVelocityY.toInt()
            }
        }
        mMode = FLING_MODE
        mIsFinished = false
        val velocity = hypot(velocityX.toDouble(), velocityY.toDouble()).toFloat()
        mVelocity = velocity
        duration = getSplineFlingDuration(velocity)
        mStartTime = AnimationUtils.currentAnimationTimeMillis()
        this.mStartX = startX
        this.mStartY = startY
        val coeFFx = if (velocity == 0f) 1.0f else velocityX / velocity
        val coeFFy = if (velocity == 0f) 1.0f else velocityY / velocity
        val totalDistance = getSplineFlingDistance(velocity)
        mDistance = (totalDistance * sign(velocity)).toInt()
        mMinX = minX
        mMaxX = maxX
        mMinY = minY
        mMaxY = maxY
        mFinalX = startX + (totalDistance * coeFFx).roundToInt().toInt()
        // Pin to mMinX <= mFinalX <= mMaxX
        mFinalX = mFinalX.coerceAtMost(mMaxX)
        mFinalX = mFinalX.coerceAtLeast(mMinX)
        mFinalY = startY + (totalDistance * coeFFy).roundToInt().toInt()
        // Pin to mMinY <= mFinalY <= mMaxY
        mFinalY = mFinalY.coerceAtMost(mMaxY)
        mFinalY = mFinalY.coerceAtLeast(mMinY)
    }

    private fun getSplineDeceleration(velocity: Float): Double {
        return ln((INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff)).toDouble())
    }

    private fun getSplineFlingDuration(velocity: Float): Int {
        val l = getSplineDeceleration(velocity)
        val decelMinusOne: Double =
            DECELERATION_RATE - 1.0
        return (1000.0 * exp(l / decelMinusOne)).toInt()
    }

    private fun getSplineFlingDistance(velocity: Float): Double {
        val l = getSplineDeceleration(velocity)
        val decelMinusOne: Double =
            DECELERATION_RATE - 1.0
        return mFlingFriction * mPhysicalCoeff * exp(DECELERATION_RATE / decelMinusOne * l)
    }

    /**
     * Stops the animation. Contrary to [.forceFinished],
     * aborting the animating cause the scroller to move to the final x and y
     * position
     *
     * @see .forceFinished
     */
    fun abortAnimation() {
        mCurrX = mFinalX
        mCurrY = mFinalY
        mIsFinished = true
    }

    /**
     * Extend the scroll animation. This allows a running animation to scroll
     * further and longer, when used with [.setFinalX] or [.setFinalY].
     *
     * @param extend Additional time to scroll in milliseconds.
     * @see .setFinalX
     * @see .setFinalY
     */
    fun extendDuration(extend: Int) {
        val passed = timePassed()
        duration = passed + extend
        mDurationReciprocal = 1.0f / duration
        mIsFinished = false
    }

    /**
     * Returns the time elapsed since the beginning of the scrolling.
     *
     * @return The elapsed time in milliseconds.
     */
    fun timePassed(): Int {
        return (AnimationUtils.currentAnimationTimeMillis() - mStartTime).toInt()
    }

    /**
     * @hide
     */
    fun isScrollingInDirection(velX: Float, velY: Float): Boolean {
        return !mIsFinished && sign(velX) == sign((mFinalX - mStartX).toFloat())
            && sign(velY) == sign((mFinalY - mStartY).toFloat())
    }

    internal class ViscousFluidInterpolator : Interpolator {
        companion object {
            /** Controls the viscous fluid effect (how much of it).  */
            private const val VISCOUS_FLUID_SCALE = 8.0f
            private var VISCOUS_FLUID_NORMALIZE = 0f
            private var VISCOUS_FLUID_OFFSET = 0f
            private fun viscousFluid(x: Float): Float {
                var x = x
                x *= VISCOUS_FLUID_SCALE
                if (x < 1.0f) {
                    x -= 1.0f - Math.exp(-x.toDouble()).toFloat()
                } else {
                    val start = 0.36787944117f // 1/e == exp(-1)
                    x = 1.0f - Math.exp((1.0f - x).toDouble()).toFloat()
                    x = start + x * (1.0f - start)
                }
                return x
            }

            init {

                // must be set to 1.0 (used in viscousFluid())
                VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f)
                // account for very small floating-point error
                VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f)
            }
        }

        override fun getInterpolation(input: Float): Float {
            val interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input)
            return if (interpolated > 0) {
                interpolated + VISCOUS_FLUID_OFFSET
            } else interpolated
        }
    }
    /**
     * Create a Scroller with the specified interpolator. If the interpolator is
     * null, the default (viscous) interpolator will be used. Specify whether or
     * not to support progressive "flywheel" behavior in flinging.
     */
    /**
     * Create a Scroller with the specified interpolator. If the interpolator is
     * null, the default (viscous) interpolator will be used. "Flywheel" behavior will
     * be in effect for apps targeting Honeycomb or newer.
     */
    /**
     * Create a Scroller with the default duration and interpolator.
     */
    init {
        mInterpolator = interpolator ?: ViscousFluidInterpolator()
        mPpi = context.resources.displayMetrics.density * 160.0f
        mDeceleration = computeDeceleration(ViewConfiguration.getScrollFriction())
        mPhysicalCoeff = computeDeceleration(0.84f) // look and feel tuning
    }
}