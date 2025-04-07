package com.bilibili.playerbizcommon.widget.function.timer

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.InputType
import android.text.Spanned
import android.text.TextUtils
import android.text.method.NumberKeyListener
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import java.text.NumberFormat
import java.util.Locale
import com.bilibili.playerbizcommon.R
import tv.danmaku.biliplayerv2.utils.DpUtils
import kotlin.math.abs

class PlayerTimePicker : LinearLayout {
    private val white = ContextCompat.getColor(context, com.bilibili.lib.theme.R.color.Wh0_u)

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        init()
    }

    /**
     * The text for showing the current value.
     */
    private lateinit var mSelectedText: EditText
    private var mSelectedTextCenterX = 0f
    private var mSelectedTextCenterY = 0f
    var mSelectedTextAlign = DEFAULT_TEXT_ALIGN
    private var mSelectedTextColor = white
    private var mSelectedTextSize = DEFAULT_SELECTED_TEXT_SIZE
    var mSelectedTextStrikeThru: Boolean = false
    var mSelectedTextUnderline: Boolean = false

    /**
     * The min/max size of this widget.
     */
    private var mMinHeight = 0
    private var mMaxHeight = 0
    private var mMinWidth = 0
    private var mMaxWidth = 0

    /**
     * Flag whether to compute the max width.
     */
    private val mComputeMaxWidth: Boolean = true

    /**
     * text.
     */
    var mTextAlign = DEFAULT_TEXT_ALIGN
    private var mTextColor = white
    private var mTextSize = DEFAULT_TEXT_SIZE
    var mTextStrikeThru: Boolean = false
    var mTextUnderline: Boolean = false

    /**
     * The height/width of the gap between text elements if the selector wheel.
     */
    private var mSelectorTextGapWidth = 0
    private var mSelectorTextGapHeight = 0

    /**
     * The values to be displayed instead the indices.
     */
    private var mDisplayedValues: Array<String>? = null

    /**
     * Upper/Lower value of the range of numbers allowed for the NumberPicker
     */
    private var mMinValue = DEFAULT_MIN_VALUE
    private var mMaxValue = DEFAULT_MAX_VALUE

    var mCurrentValue: Int = 0
        private set

    fun setCurrentValue(value: Int) {
        mCurrentValue = value
        setValueInternal(value, false)
    }

    /**
     * Listener
     */
    private var mOnClickListener: OnClickListener? = null
    private var mOnValueChangeListener: OnValueChangeListener? = null
    private var mOnScrollListener: OnScrollListener? = null

    /**
     * Formatter for for displaying the current value.
     */
    private var mTimePickerFormatter: TimePickerFormatter? = null

    /**
     * The speed for updating the value form long press.
     */
    private var mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL

    /**
     * Cache for the string representation of selector indices.
     */
    private val mSelectorIndexToStringCache = SparseArray<String?>()

    /**
     * The number of items show in the selector wheel.
     */
    private var mWheelItemCount = DEFAULT_WHEEL_ITEM_COUNT

    /**
     * The real number of items show in the selector wheel.
     */
    private var mRealWheelItemCount = DEFAULT_WHEEL_ITEM_COUNT

    /**
     * The index of the middle selector item.
     */
    private var mWheelMiddleItemIndex = mWheelItemCount / 2

    /**
     * The selector indices whose value are show by the selector.
     */
    private var mSelectorIndices = IntArray(mWheelItemCount)

    /**
     * The [Paint] for drawing the selector.
     */
    private lateinit var mSelectorWheelPaint: Paint

    /**
     * The size of a selector element (text + gap).
     */
    private var mSelectorElementSize = 0

    /**
     * The initial offset of the scroll selector.
     */
    private var mInitialScrollOffset = Int.MIN_VALUE

    /**
     * The current offset of the scroll selector.
     */
    private var mCurrentScrollOffset = 0

    /**
     * The [PlayerTimerPickerScroller] responsible for flinging the selector.
     */
    private lateinit var mFlingPlayerTimerPickerScroller: PlayerTimerPickerScroller

    /**
     * The [PlayerTimerPickerScroller] responsible for adjusting the selector.
     */
    private lateinit var mAdjustPlayerTimerPickerScroller: PlayerTimerPickerScroller

    /**
     * The previous X coordinate while scrolling the selector.
     */
    private var mPreviousScrollerX = 0

    /**
     * The previous Y coordinate while scrolling the selector.
     */
    private var mPreviousScrollerY = 0

    /**
     * Handle to the reusable command for setting the input text selection.
     */
    private var mSetSelectionCommand: SetSelectionCommand? = null

    /**
     * Handle to the reusable command for changing the current value from long press by one.
     */
    private var mChangeCurrentByOneFromLongPressCommand: ChangeCurrentByOneFromLongPressCommand? =
        null

    /**
     * The X position of the last down event.
     */
    private var mLastDownEventX = 0f

    /**
     * The Y position of the last down event.
     */
    private var mLastDownEventY = 0f

    /**
     * The X position of the last down or move event.
     */
    private var mLastDownOrMoveEventX = 0f

    /**
     * The Y position of the last down or move event.
     */
    private var mLastDownOrMoveEventY = 0f

    /**
     * Determines speed during touch scrolling.
     */
    private var mVelocityTracker: VelocityTracker? = null

    /**
     * @see ViewConfiguration.getScaledTouchSlop
     */
    private var mTouchSlop: Int = 0

    /**
     * @see ViewConfiguration.getScaledMinimumFlingVelocity
     */
    private var mMinimumFlingVelocity: Int = 0

    /**
     * @see ViewConfiguration.getScaledMaximumFlingVelocity
     */
    private var mMaximumFlingVelocity: Int = 0

    /**
     * Flag whether the selector should wrap around.
     */
    private var mWrapSelectorWheel: Boolean = true

    /**
     * User choice on whether the selector wheel should be wrapped.
     */
    private var mWrapSelectorWheelPreferred = true

    /**
     * Divider for showing item to be selected while scrolling
     */
    private var mDividerDrawable: Drawable? = null

    /**
     * The color of the divider.
     */
    private var mDividerColor = white

    /**
     * The distance between the two dividers.
     */
    private var mDividerDistance: Int = 0

    /**
     * The thickness of the divider.
     */
    private var mDividerLength: Int = 0

    /**
     * The thickness of the divider.
     */
    private var mDividerThickness: Int = 0

    /**
     * The top of the top divider.
     */
    private var mTopDividerTop = 0

    /**
     * The bottom of the bottom divider.
     */
    private var mBottomDividerBottom = 0

    /**
     * The left of the top divider.
     */
    private var mLeftDividerLeft = 0

    /**
     * The right of the right divider.
     */
    private var mRightDividerRight = 0

    /**
     * The type of the divider.
     */
    private var mDividerType: Int = 0

    /**
     * The current scroll state of the number picker.
     */
    private var mScrollState = OnScrollListener.SCROLL_STATE_IDLE

    /**
     * The keycode of the last handled DPAD down event.
     */
    private var mLastHandledDownDpadKeyCode = -1

    /**
     * Flag whether the selector wheel should hidden until the picker has focus.
     */
    private val mHideWheelUntilFocused: Boolean = false

    /**
     * The orientation of this widget.
     */
    private var mOrientation: Int = 0

    /**
     * The order of this widget.
     */
    private var mOrder: Int = 0

    /**
     * Flag whether the fading edge should enabled.
     */
    private var mFadingEdgeEnabled = true

    /**
     * The strength of fading edge while drawing the selector.
     */
    var mFadingEdgeStrength = DEFAULT_FADING_EDGE_STRENGTH

    /**
     * Flag whether the scroller should enabled.
     */
    var mScrollerEnabled = true

    /**
     * The line spacing multiplier of the text.
     */
    var mLineSpacingMultiplier = DEFAULT_LINE_SPACING_MULTIPLIER

    /**
     * The coefficient to adjust (divide) the max fling velocity.
     */
    private var mMaxFlingVelocityCoefficient = DEFAULT_MAX_FLING_VELOCITY_COEFFICIENT

    /**
     * Flag whether the accessibility description enabled.
     */
    var mAccessibilityDescriptionEnabled = true

    /**
     * The number formatter for current locale.
     */
    private lateinit var mNumberFormatter: NumberFormat

    /**
     * The view configuration of this widget.
     */
    private lateinit var mViewConfiguration: ViewConfiguration

    /**
     * Interface to listen for changes of the current value.
     */
    interface OnValueChangeListener {
        /**
         * Called upon a change of the current value.
         *
         * @param picker The NumberPicker associated with this listener.
         * @param oldVal The previous value.
         * @param newVal The new value.
         */
        fun onValueChange(picker: PlayerTimePicker?, oldVal: Int, newVal: Int)
    }

    /**
     * The amount of space between items.
     */
    var mItemSpacing = 0

    /**
     * Interface to listen for the picker scroll state.
     */
    interface OnScrollListener {
        annotation class ScrollState()

        /**
         * Callback invoked while the number picker scroll state has changed.
         *
         * @param view        The view whose scroll state is being reported.
         * @param scrollState The current scroll state. One of
         * [.SCROLL_STATE_IDLE],
         * [.SCROLL_STATE_TOUCH_SCROLL] or
         * [.SCROLL_STATE_IDLE].
         */
        fun onScrollStateChange(view: PlayerTimePicker?, @ScrollState scrollState: Int)

        companion object {
            /**
             * The view is not scrolling.
             */
            val SCROLL_STATE_IDLE = 0

            /**
             * The user is scrolling using touch, and his finger is still on the screen.
             */
            val SCROLL_STATE_TOUCH_SCROLL = 1

            /**
             * The user had previously been scrolling using touch and performed a fling.
             */
            val SCROLL_STATE_FLING = 2
        }
    }

    /**
     * Interface used to format current value into a string for presentation.
     */
    interface TimePickerFormatter {
        /**
         * Formats a string representation of the current value.
         *
         * @param value The currently selected value.
         * @return A formatted string representation.
         */
        fun format(value: Int): String
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val msrdWdth = measuredWidth
        val msrdHght = measuredHeight

        val inptTxtMsrdWdth = mSelectedText.measuredWidth
        val inptTxtMsrdHght = mSelectedText.measuredHeight
        val inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2
        val inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2
        val inptTxtRight = inptTxtLeft + inptTxtMsrdWdth
        val inptTxtBottom = inptTxtTop + inptTxtMsrdHght
        mSelectedText.layout(inptTxtLeft, inptTxtTop, inptTxtRight, inptTxtBottom)
        mSelectedTextCenterX = mSelectedText.x + mSelectedText.measuredWidth / 2f - 2f
        mSelectedTextCenterY = mSelectedText.y + mSelectedText.measuredHeight / 2f - 5f
        if (changed) {
            initializeSelectorWheel()
            initializeFadingEdges()
            val dividerDistance = 2 * mDividerThickness + mDividerDistance
            if (isHorizontalMode()) {
                mLeftDividerLeft = (width - mDividerDistance) / 2 - mDividerThickness
                mRightDividerRight = mLeftDividerLeft + dividerDistance
                mBottomDividerBottom = height
            } else {
                mTopDividerTop = (height - mDividerDistance) / 2 - mDividerThickness
                mBottomDividerBottom = mTopDividerTop + dividerDistance
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Try greedily to fit the max width and height.
        val newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mMaxWidth)
        val newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight)
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec)
        // Flag if we are measured with width or height less than the respective min.
        val widthSize = resolveSizeAndStateRespectingMinSize(
            mMinWidth,
            measuredWidth,
            widthMeasureSpec
        )
        val heightSize = resolveSizeAndStateRespectingMinSize(
            mMinHeight,
            measuredHeight,
            heightMeasureSpec
        )
        setMeasuredDimension(widthSize, heightSize)
    }

    /**
     * Move to the final position of a scroller. Ensures to force finish the scroller
     * and if it is not at its final position a scroll of the selector wheel is
     * performed to fast forward to the final position.
     *
     * @param playerTimerPickerScroller The scroller to whose final position to get.
     * @return True of the a move was performed, i.e. the scroller was not in final position.
     */
    private fun moveToFinalScrollerPosition(playerTimerPickerScroller: PlayerTimerPickerScroller): Boolean {
        playerTimerPickerScroller.forceFinished(true)
        if (isHorizontalMode()) {
            var amountToScroll: Int =
                playerTimerPickerScroller.mFinalX - playerTimerPickerScroller.mCurrX
            val futureScrollOffset = (mCurrentScrollOffset + amountToScroll) % mSelectorElementSize
            var overshootAdjustment = mInitialScrollOffset - futureScrollOffset
            if (overshootAdjustment != 0) {
                if (Math.abs(overshootAdjustment) > mSelectorElementSize / 2) {
                    if (overshootAdjustment > 0) {
                        overshootAdjustment -= mSelectorElementSize
                    } else {
                        overshootAdjustment += mSelectorElementSize
                    }
                }
                amountToScroll += overshootAdjustment
                scrollBy(amountToScroll, 0)
                return true
            }
        } else {
            var amountToScroll: Int =
                playerTimerPickerScroller.mFinalY - playerTimerPickerScroller.mCurrY
            val futureScrollOffset = (mCurrentScrollOffset + amountToScroll) % mSelectorElementSize
            var overshootAdjustment = mInitialScrollOffset - futureScrollOffset
            if (overshootAdjustment != 0) {
                if (abs(overshootAdjustment) > mSelectorElementSize / 2) {
                    if (overshootAdjustment > 0) {
                        overshootAdjustment -= mSelectorElementSize
                    } else {
                        overshootAdjustment += mSelectorElementSize
                    }
                }
                amountToScroll += overshootAdjustment
                scrollBy(0, amountToScroll)
                return true
            }
        }
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        val action = event.action and MotionEvent.ACTION_MASK
        if (action != MotionEvent.ACTION_DOWN) {
            return false
        }
        removeAllCallbacks()
        // Make sure we support flinging inside scrollables.
        parent.requestDisallowInterceptTouchEvent(true)
        if (isHorizontalMode()) {
            mLastDownEventX = event.x
            mLastDownOrMoveEventX = mLastDownEventX
            if (!mFlingPlayerTimerPickerScroller.mIsFinished) {
                mFlingPlayerTimerPickerScroller.forceFinished(true)
                mAdjustPlayerTimerPickerScroller.forceFinished(true)
                onScrollerFinished(mFlingPlayerTimerPickerScroller)
                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE)
            } else if (!mAdjustPlayerTimerPickerScroller.mIsFinished) {
                mFlingPlayerTimerPickerScroller.forceFinished(true)
                mAdjustPlayerTimerPickerScroller.forceFinished(true)
                onScrollerFinished(mAdjustPlayerTimerPickerScroller)
            } else if (mLastDownEventX >= mLeftDividerLeft
                && mLastDownEventX <= mRightDividerRight
            ) {
                if (mOnClickListener != null) {
                    mOnClickListener!!.onClick(this)
                }
            } else if (mLastDownEventX < mLeftDividerLeft) {
                postChangeCurrentByOneFromLongPress(false)
            } else if (mLastDownEventX > mRightDividerRight) {
                postChangeCurrentByOneFromLongPress(true)
            }
        } else {
            mLastDownEventY = event.y
            mLastDownOrMoveEventY = mLastDownEventY
            if (!mFlingPlayerTimerPickerScroller.mIsFinished) {
                mFlingPlayerTimerPickerScroller.forceFinished(true)
                mAdjustPlayerTimerPickerScroller.forceFinished(true)
                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE)
            } else if (!mAdjustPlayerTimerPickerScroller.mIsFinished) {
                mFlingPlayerTimerPickerScroller.forceFinished(true)
                mAdjustPlayerTimerPickerScroller.forceFinished(true)
            } else if (mLastDownEventY >= mTopDividerTop
                && mLastDownEventY <= mBottomDividerBottom
            ) {
                if (mOnClickListener != null) {
                    mOnClickListener!!.onClick(this)
                }
            } else if (mLastDownEventY < mTopDividerTop) {
                postChangeCurrentByOneFromLongPress(false)
            } else if (mLastDownEventY > mBottomDividerBottom) {
                postChangeCurrentByOneFromLongPress(true)
            }
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        if (!mScrollerEnabled) {
            return false
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(event)
        val action = event.action and MotionEvent.ACTION_MASK
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                if (isHorizontalMode()) {
                    val currentMoveX = event.x
                    if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                        val deltaDownX =
                            Math.abs(currentMoveX - mLastDownEventX).toInt()
                        if (deltaDownX > mTouchSlop) {
                            removeAllCallbacks()
                            onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
                        }
                    } else {
                        val deltaMoveX = (currentMoveX - mLastDownOrMoveEventX).toInt()
                        scrollBy(deltaMoveX, 0)
                        invalidate()
                    }
                    mLastDownOrMoveEventX = currentMoveX
                } else {
                    val currentMoveY = event.y
                    if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                        val deltaDownY =
                            Math.abs(currentMoveY - mLastDownEventY).toInt()
                        if (deltaDownY > mTouchSlop) {
                            removeAllCallbacks()
                            onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
                        }
                    } else {
                        val deltaMoveY = (currentMoveY - mLastDownOrMoveEventY).toInt()
                        scrollBy(0, deltaMoveY)
                        invalidate()
                    }
                    mLastDownOrMoveEventY = currentMoveY
                }
            }
            MotionEvent.ACTION_UP -> {
                removeChangeCurrentByOneFromLongPress()
                val velocityTracker = mVelocityTracker
                velocityTracker!!.computeCurrentVelocity(1000, mMaximumFlingVelocity.toFloat())
                if (isHorizontalMode()) {
                    val initialVelocity = velocityTracker.xVelocity.toInt()
                    if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                        fling(initialVelocity)
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING)
                    } else {
                        val eventX = event.x.toInt()
                        val deltaMoveX = Math.abs(eventX - mLastDownEventX).toInt()
                        if (deltaMoveX <= mTouchSlop) {
                            val selectorIndexOffset = (eventX / mSelectorElementSize
                                - mWheelMiddleItemIndex)
                            if (selectorIndexOffset > 0) {
                                changeValueByOne(true)
                            } else if (selectorIndexOffset < 0) {
                                changeValueByOne(false)
                            } else {
                                ensureScrollWheelAdjusted()
                            }
                        } else {
                            ensureScrollWheelAdjusted()
                        }
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE)
                    }
                } else {
                    val initialVelocity = velocityTracker.yVelocity.toInt()
                    if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                        fling(initialVelocity)
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING)
                    } else {
                        val eventY = event.y.toInt()
                        val deltaMoveY = Math.abs(eventY - mLastDownEventY).toInt()
                        if (deltaMoveY <= mTouchSlop) {
                            val selectorIndexOffset = (eventY / mSelectorElementSize
                                - mWheelMiddleItemIndex)
                            if (selectorIndexOffset > 0) {
                                changeValueByOne(true)
                            } else if (selectorIndexOffset < 0) {
                                changeValueByOne(false)
                            } else {
                                ensureScrollWheelAdjusted()
                            }
                        } else {
                            ensureScrollWheelAdjusted()
                        }
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE)
                    }
                }
                mVelocityTracker!!.recycle()
                mVelocityTracker = null
            }
        }
        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> removeAllCallbacks()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> removeAllCallbacks()
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP -> when (event.action) {
                KeyEvent.ACTION_DOWN -> if (mWrapSelectorWheel || (if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) mCurrentValue < getMaxValue() else mCurrentValue > getMinValue())) {
                    requestFocus()
                    mLastHandledDownDpadKeyCode = keyCode
                    removeAllCallbacks()
                    if (mFlingPlayerTimerPickerScroller.mIsFinished) {
                        changeValueByOne(keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> if (mLastHandledDownDpadKeyCode == keyCode) {
                    mLastHandledDownDpadKeyCode = -1
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        when (action) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> removeAllCallbacks()
        }
        return super.dispatchTrackballEvent(event)
    }

    override fun computeScroll() {
        if (!mScrollerEnabled) {
            return
        }
        var playerTimerPickerScroller: PlayerTimerPickerScroller = mFlingPlayerTimerPickerScroller
        if (playerTimerPickerScroller.mIsFinished) {
            playerTimerPickerScroller = mAdjustPlayerTimerPickerScroller
            if (playerTimerPickerScroller.mIsFinished) {
                return
            }
        }
        playerTimerPickerScroller.computeScrollOffset()
        if (isHorizontalMode()) {
            val currentScrollerX: Int = playerTimerPickerScroller.mCurrX
            if (mPreviousScrollerX == 0) {
                mPreviousScrollerX = playerTimerPickerScroller.mStartX
            }
            scrollBy(currentScrollerX - mPreviousScrollerX, 0)
            mPreviousScrollerX = currentScrollerX
        } else {
            val currentScrollerY: Int = playerTimerPickerScroller.mCurrY
            if (mPreviousScrollerY == 0) {
                mPreviousScrollerY = playerTimerPickerScroller.mStartY
            }
            scrollBy(0, currentScrollerY - mPreviousScrollerY)
            mPreviousScrollerY = currentScrollerY
        }
        if (playerTimerPickerScroller.mIsFinished) {
            onScrollerFinished(playerTimerPickerScroller)
        } else {
            postInvalidate()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        mSelectedText.isEnabled = enabled
    }

    override fun scrollBy(x: Int, y: Int) {
        if (!mScrollerEnabled) {
            return
        }
        val selectorIndices = getSelectorIndices()
        val startScrollOffset = mCurrentScrollOffset
        val gap = maxTextSize.toInt()
        if (isHorizontalMode()) {
            if (isAscendingOrder()) {
                if (!mWrapSelectorWheel && x > 0 && selectorIndices[mWheelMiddleItemIndex] <= mMinValue) {
                    mCurrentScrollOffset = mInitialScrollOffset
                    return
                }
                if (!mWrapSelectorWheel && x < 0 && selectorIndices[mWheelMiddleItemIndex] >= mMaxValue) {
                    mCurrentScrollOffset = mInitialScrollOffset
                    return
                }
            } else {
                if (!mWrapSelectorWheel && x > 0 && selectorIndices[mWheelMiddleItemIndex] >= mMaxValue) {
                    mCurrentScrollOffset = mInitialScrollOffset
                    return
                }
                if (!mWrapSelectorWheel && x < 0 && selectorIndices[mWheelMiddleItemIndex] <= mMinValue) {
                    mCurrentScrollOffset = mInitialScrollOffset
                    return
                }
            }
            mCurrentScrollOffset += x
        } else {
            if (isAscendingOrder()) {
                if (!mWrapSelectorWheel && y > 0 && selectorIndices[mWheelMiddleItemIndex] <= mMinValue) {
                    mCurrentScrollOffset = mInitialScrollOffset
                    return
                }
                if (!mWrapSelectorWheel && y < 0 && selectorIndices[mWheelMiddleItemIndex] >= mMaxValue) {
                    mCurrentScrollOffset = mInitialScrollOffset
                    return
                }
            } else {
                if (!mWrapSelectorWheel && y > 0 && selectorIndices[mWheelMiddleItemIndex] >= mMaxValue) {
                    mCurrentScrollOffset = mInitialScrollOffset
                    return
                }
                if (!mWrapSelectorWheel && y < 0 && selectorIndices[mWheelMiddleItemIndex] <= mMinValue) {
                    mCurrentScrollOffset = mInitialScrollOffset
                    return
                }
            }
            mCurrentScrollOffset += y
        }
        while (mCurrentScrollOffset - mInitialScrollOffset > gap) {
            mCurrentScrollOffset -= mSelectorElementSize
            if (isAscendingOrder()) {
                decrementSelectorIndices(selectorIndices)
            } else {
                incrementSelectorIndices(selectorIndices)
            }
            setValueInternal(selectorIndices[mWheelMiddleItemIndex], true)
            if (!mWrapSelectorWheel && selectorIndices[mWheelMiddleItemIndex] < mMinValue) {
                mCurrentScrollOffset = mInitialScrollOffset
            }
        }
        while (mCurrentScrollOffset - mInitialScrollOffset < -gap) {
            mCurrentScrollOffset += mSelectorElementSize
            if (isAscendingOrder()) {
                incrementSelectorIndices(selectorIndices)
            } else {
                decrementSelectorIndices(selectorIndices)
            }
            setValueInternal(selectorIndices[mWheelMiddleItemIndex], true)
            if (!mWrapSelectorWheel && selectorIndices[mWheelMiddleItemIndex] > mMaxValue) {
                mCurrentScrollOffset = mInitialScrollOffset
            }
        }
        if (startScrollOffset != mCurrentScrollOffset) {
            if (isHorizontalMode()) {
                onScrollChanged(mCurrentScrollOffset, 0, startScrollOffset, 0)
            } else {
                onScrollChanged(0, mCurrentScrollOffset, 0, startScrollOffset)
            }
        }
    }

    private fun computeScrollOffset(isHorizontalMode: Boolean): Int {
        return if (isHorizontalMode) mCurrentScrollOffset else 0
    }

    private fun computeScrollRange(isHorizontalMode: Boolean): Int {
        return if (isHorizontalMode) (mMaxValue - mMinValue + 1) * mSelectorElementSize else 0
    }

    private fun computeScrollExtent(isHorizontalMode: Boolean): Int {
        return if (isHorizontalMode) width else height
    }

    override fun computeHorizontalScrollOffset(): Int {
        return computeScrollOffset(isHorizontalMode())
    }

    override fun computeHorizontalScrollRange(): Int {
        return computeScrollRange(isHorizontalMode())
    }

    override fun computeHorizontalScrollExtent(): Int {
        return computeScrollExtent(isHorizontalMode())
    }

    override fun computeVerticalScrollOffset(): Int {
        return computeScrollOffset(!isHorizontalMode())
    }

    override fun computeVerticalScrollRange(): Int {
        return computeScrollRange(!isHorizontalMode())
    }

    override fun computeVerticalScrollExtent(): Int {
        return computeScrollExtent(isHorizontalMode())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mNumberFormatter = NumberFormat.getInstance()
    }

    /**
     * Set listener to be notified on click of the current value.
     *
     * @param l The listener.
     */
    override fun setOnClickListener(l: OnClickListener?) {
        mOnClickListener = l
    }

    /**
     * Sets the listener to be notified on change of the current value.
     *
     * @param onValueChangedListener The listener.
     */
    fun setOnValueChangedListener(onValueChangedListener: OnValueChangeListener?) {
        mOnValueChangeListener = onValueChangedListener
    }

    /**
     * Set listener to be notified for scroll state changes.
     *
     * @param onScrollListener The listener.
     */
    fun setOnScrollListener(onScrollListener: OnScrollListener?) {
        mOnScrollListener = onScrollListener
    }

    private val maxTextSize: Float
        private get() = mTextSize.coerceAtLeast(mSelectedTextSize)

    private fun getPaintCenterY(fontMetrics: Paint.FontMetrics?): Float {
        return if (fontMetrics == null) {
            0f
        } else {
            abs(fontMetrics.top + fontMetrics.bottom) / 2
        }
    }

    /**
     * Computes the max width if no such specified as an attribute.
     */
    private fun tryComputeMaxWidth() {
        if (!mComputeMaxWidth) {
            return
        }
        mSelectorWheelPaint.textSize = maxTextSize
        var maxTextWidth = 0
        if (mDisplayedValues == null) {
            var maxDigitWidth = 0f
            for (i in 0..9) {
                val digitWidth = mSelectorWheelPaint.measureText(formatNumber(i))
                if (digitWidth > maxDigitWidth) {
                    maxDigitWidth = digitWidth
                }
            }
            var numberOfDigits = 0
            var current = mMaxValue
            while (current > 0) {
                numberOfDigits++
                current /= 10
            }
            maxTextWidth = (numberOfDigits * maxDigitWidth).toInt()
        } else {
            for (displayedValue: String in mDisplayedValues!!) {
                val textWidth = mSelectorWheelPaint.measureText(displayedValue)
                if (textWidth > maxTextWidth) {
                    maxTextWidth = textWidth.toInt()
                }
            }
        }
        maxTextWidth += mSelectedText.paddingLeft + mSelectedText.paddingRight
        if (mMaxWidth != maxTextWidth) {
            mMaxWidth = Math.max(maxTextWidth, mMinWidth)
            invalidate()
        }
    }

    /**
     * Gets whether the selector wheel wraps when reaching the min/max value.
     *
     * @return True if the selector wheel wraps.
     * @see .getMinValue
     * @see .getMaxValue
     */

    /**
     * Sets whether the selector wheel shown during flinging/scrolling should
     * wrap around the [NewPlayerTimePicker.getMinValue] and
     * [NewPlayerTimePicker.getMaxValue] values.
     *
     *
     * By default if the range (max - min) is more than the number of items shown
     * on the selector wheel the selector wheel wrapping is enabled.
     *
     *
     *
     * **Note:** If the number of items, i.e. the range (
     * [.getMaxValue] - [.getMinValue]) is less than
     * the number of items shown on the selector wheel, the selector wheel will
     * not wrap. Hence, in such a case calling this method is a NOP.
     *
     *
     * @param wrapSelectorWheel Whether to wrap.
     */
    fun setWrapSelectorWheel(wrapSelectorWheel: Boolean) {
        mWrapSelectorWheelPreferred = wrapSelectorWheel
        updateWrapSelectorWheel()
    }

    /**
     * Whether or not the selector wheel should be wrapped is determined by user choice and whether
     * the choice is allowed. The former comes from [.setWrapSelectorWheel], the
     * latter is calculated based on min & max value set vs selector's visual length. Therefore,
     * this method should be called any time any of the 3 values (i.e. user choice, min and max
     * value) gets updated.
     */
    private fun updateWrapSelectorWheel() {
        mWrapSelectorWheel = isWrappingAllowed() && mWrapSelectorWheelPreferred
    }

    private fun isWrappingAllowed(): Boolean {
        return mMaxValue - mMinValue >= mSelectorIndices.size - 1
    }

    /**
     * Sets the speed at which the numbers be incremented and decremented when
     * the up and down buttons are long pressed respectively.
     *
     *
     * The default value is 300 ms.
     *
     *
     * @param intervalMillis The speed (in milliseconds) at which the numbers
     * will be incremented and decremented.
     */
    fun setOnLongPressUpdateInterval(intervalMillis: Long) {
        mLongPressUpdateInterval = intervalMillis
    }
    /**
     * Returns the value of the picker.
     *
     * @return The value.
     */
    /**
     * Set the current value for the number picker.
     *
     *
     * If the argument is less than the [NewPlayerTimePicker.getMinValue] and
     * [NewPlayerTimePicker.getWrapSelectorWheel] is `false` the
     * current value is set to the [NewPlayerTimePicker.getMinValue] value.
     *
     *
     *
     * If the argument is less than the [NewPlayerTimePicker.getMinValue] and
     * [NewPlayerTimePicker.getWrapSelectorWheel] is `true` the
     * current value is set to the [NewPlayerTimePicker.getMaxValue] value.
     *
     *
     *
     * If the argument is less than the [NewPlayerTimePicker.getMaxValue] and
     * [NewPlayerTimePicker.getWrapSelectorWheel] is `false` the
     * current value is set to the [NewPlayerTimePicker.getMaxValue] value.
     *
     *
     *
     * If the argument is less than the [NewPlayerTimePicker.getMaxValue] and
     * [NewPlayerTimePicker.getWrapSelectorWheel] is `true` the
     * current value is set to the [NewPlayerTimePicker.getMinValue] value.
     *
     *
     * @param value The current value.
     * @see .setWrapSelectorWheel
     * @see .setMinValue
     * @see .setMaxValue
     */

    /**
     * Returns the min value of the picker.
     *
     * @return The min value
     */
    fun getMinValue(): Int {
        return mMinValue
    }

    /**
     * Sets the min value of the picker.
     *
     * @param minValue The min value inclusive.
     *
     * **Note:** The length of the displayed values array
     * set via [.setDisplayedValues] must be equal to the
     * range of selectable numbers which is equal to
     * [.getMaxValue] - [.getMinValue] + 1.
     */
    fun setMinValue(minValue: Int) {
//        if (minValue < 0) {
//            throw new IllegalArgumentException("minValue must be >= 0");
//        }
        mMinValue = minValue
        if (mMinValue > mCurrentValue) {
            setCurrentValue(mMinValue)
        }
        updateWrapSelectorWheel()
        initializeSelectorWheelIndices()
        updateInputTextView()
        tryComputeMaxWidth()
        invalidate()
    }

    /**
     * Returns the max value of the picker.
     *
     * @return The max value.
     */
    fun getMaxValue(): Int {
        return mMaxValue
    }

    /**
     * Sets the max value of the picker.
     *
     * @param maxValue The max value inclusive.
     *
     * **Note:** The length of the displayed values array
     * set via [.setDisplayedValues] must be equal to the
     * range of selectable numbers which is equal to
     * [.getMaxValue] - [.getMinValue] + 1.
     */
    fun setMaxValue(maxValue: Int) {
        if (maxValue < 0) {
            throw IllegalArgumentException("maxValue must be >= 0")
        }
        mMaxValue = maxValue
        if (mMaxValue < mCurrentValue) {
            setCurrentValue(mMaxValue)
        }
        updateWrapSelectorWheel()
        initializeSelectorWheelIndices()
        updateInputTextView()
        tryComputeMaxWidth()
        invalidate()
    }

    /**
     * Gets the values to be displayed instead of string values.
     *
     * @return The displayed values.
     */
    fun getDisplayedValues(): Array<String>? {
        return mDisplayedValues
    }

    /**
     * Sets the values to be displayed.
     *
     * @param displayedValues The displayed values.
     *
     * **Note:** The length of the displayed values array
     * must be equal to the range of selectable numbers which is equal to
     * [.getMaxValue] - [.getMinValue] + 1.
     */
    fun setDisplayedValues(displayedValues: Array<String>) {
        if (mDisplayedValues == displayedValues) {
            return
        }
        mDisplayedValues = displayedValues
        if (mDisplayedValues != null) {
            // Allow text entry rather than strictly numeric entry.
            mSelectedText.setRawInputType(
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            )
        } else {
            mSelectedText.setRawInputType(InputType.TYPE_CLASS_NUMBER)
        }
        updateInputTextView()
        initializeSelectorWheelIndices()
        tryComputeMaxWidth()
    }

    private fun getFadingEdgeStrength(isHorizontalMode: Boolean): Float {
        return if (isHorizontalMode && mFadingEdgeEnabled) mFadingEdgeStrength else 0f
    }

    override fun getTopFadingEdgeStrength(): Float {
        return getFadingEdgeStrength(!isHorizontalMode())
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return getFadingEdgeStrength(!isHorizontalMode())
    }

    override fun getLeftFadingEdgeStrength(): Float {
        return getFadingEdgeStrength(isHorizontalMode())
    }

    override fun getRightFadingEdgeStrength(): Float {
        return getFadingEdgeStrength(isHorizontalMode())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeAllCallbacks()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (((mDividerDrawable != null) && mDividerDrawable!!.isStateful
                && mDividerDrawable!!.setState(drawableState))
        ) {
            invalidateDrawable(mDividerDrawable!!)
        }
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        if (mDividerDrawable != null) {
            mDividerDrawable!!.jumpToCurrentState()
        }
    }

    override fun onDraw(canvas: Canvas) {
        // save canvas
        canvas.save()
        val showSelectorWheel = !mHideWheelUntilFocused || hasFocus()
        var x: Float
        var y: Float
        if (isHorizontalMode()) {
            x = mCurrentScrollOffset.toFloat()
            y = (mSelectedText.baseline + mSelectedText.top).toFloat()
            if (mRealWheelItemCount < DEFAULT_WHEEL_ITEM_COUNT) {
                canvas.clipRect(mLeftDividerLeft, 0, mRightDividerRight, bottom)
            }
        } else {
            x = (right - left) / 2f
            y = mCurrentScrollOffset.toFloat()
            if (mRealWheelItemCount < DEFAULT_WHEEL_ITEM_COUNT) {
                canvas.clipRect(0, mTopDividerTop, right, mBottomDividerBottom)
            }
        }

        // draw the selector wheel
        val selectorIndices = getSelectorIndices()
        for (i in selectorIndices.indices) {
            if (i == mWheelMiddleItemIndex) {
                mSelectorWheelPaint.textAlign = Paint.Align.values()[mSelectedTextAlign]
                mSelectorWheelPaint.textSize = mSelectedTextSize
                mSelectorWheelPaint.color = mSelectedTextColor
                mSelectorWheelPaint.isStrikeThruText = mSelectedTextStrikeThru
                mSelectorWheelPaint.isUnderlineText = mSelectedTextUnderline
            } else {
                mSelectorWheelPaint.textAlign = Paint.Align.values().get(mTextAlign)
                mSelectorWheelPaint.textSize = mTextSize
                mSelectorWheelPaint.color = mTextColor
                mSelectorWheelPaint.isStrikeThruText = mTextStrikeThru
                mSelectorWheelPaint.isUnderlineText = mTextUnderline
            }
            val selectorIndex =
                selectorIndices[if (isAscendingOrder()) i else selectorIndices.size - i - 1]
            val scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex) ?: continue
            // Do not draw the middle item if input is visible since the input
            // is shown only if the wheel is static and it covers the middle
            // item. Otherwise, if the user starts editing the text via the
            // IME he may see a dimmed version of the old value intermixed
            // with the new one.
            if (((showSelectorWheel && i != mWheelMiddleItemIndex)
                    || (i == mWheelMiddleItemIndex && mSelectedText.visibility != VISIBLE))
            ) {
                var textY = y
                if (!isHorizontalMode()) {
                    textY += getPaintCenterY(mSelectorWheelPaint.fontMetrics)
                }
                var xOffset = 0
                var yOffset = 0
                if (i != mWheelMiddleItemIndex && mItemSpacing != 0) {
                    if (isHorizontalMode()) {
                        if (i > mWheelMiddleItemIndex) {
                            xOffset = mItemSpacing
                        } else {
                            xOffset = -mItemSpacing
                        }
                    } else {
                        if (i > mWheelMiddleItemIndex) {
                            yOffset = mItemSpacing
                        } else {
                            yOffset = -mItemSpacing
                        }
                    }
                }
                drawText(
                    scrollSelectorValue,
                    x + xOffset,
                    textY + yOffset,
                    mSelectorWheelPaint,
                    canvas
                )
            }
            if (isHorizontalMode()) {
                x += mSelectorElementSize.toFloat()
            } else {
                y += mSelectorElementSize.toFloat()
            }
        }

        // restore canvas
        canvas.restore()

        // draw the dividers
        if (showSelectorWheel && mDividerDrawable != null) {
            if (isHorizontalMode()) drawHorizontalDividers(canvas) else drawVerticalDividers(canvas)
        }
    }

    private fun drawHorizontalDividers(canvas: Canvas) {
        when (mDividerType) {
            SIDE_LINES -> {
                val top: Int
                val bottom: Int
                if (mDividerLength in 1..mMaxHeight) {
                    top = (mMaxHeight - mDividerLength) / 2
                    bottom = top + mDividerLength
                } else {
                    top = 0
                    bottom = getBottom()
                }
                // draw the left divider
                val leftOfLeftDivider = mLeftDividerLeft
                val rightOfLeftDivider = leftOfLeftDivider + mDividerThickness
                mDividerDrawable?.setBounds(leftOfLeftDivider, top, rightOfLeftDivider, bottom)
                mDividerDrawable?.draw(canvas)
                // draw the right divider
                val rightOfRightDivider = mRightDividerRight
                val leftOfRightDivider = rightOfRightDivider - mDividerThickness
                mDividerDrawable?.setBounds(leftOfRightDivider, top, rightOfRightDivider, bottom)
                mDividerDrawable?.draw(canvas)
            }
            UNDERLINE -> {
                val left: Int
                val right: Int
                if (mDividerLength in 1..mMaxWidth) {
                    left = (mMaxWidth - mDividerLength) / 2
                    right = left + mDividerLength
                } else {
                    left = mLeftDividerLeft
                    right = mRightDividerRight
                }
                val bottomOfUnderlineDivider = mBottomDividerBottom
                val topOfUnderlineDivider = bottomOfUnderlineDivider - mDividerThickness
                mDividerDrawable?.setBounds(
                    left,
                    topOfUnderlineDivider,
                    right,
                    bottomOfUnderlineDivider
                )
                mDividerDrawable?.draw(canvas)
            }
        }
    }

    private fun drawVerticalDividers(canvas: Canvas) {
        val left: Int
        val right: Int
        if (mDividerLength in 1..mMaxWidth) {
            left = (mMaxWidth - mDividerLength) / 2
            right = left + mDividerLength
        } else {
            left = 0
            right = getRight()
        }
        when (mDividerType) {
            SIDE_LINES -> {
                // draw the top divider
                val topOfTopDivider = mTopDividerTop
                val bottomOfTopDivider = topOfTopDivider + mDividerThickness
                mDividerDrawable?.setBounds(left, topOfTopDivider, right, bottomOfTopDivider)
                mDividerDrawable?.draw(canvas)
                // draw the bottom divider
                val bottomOfBottomDivider = mBottomDividerBottom
                val topOfBottomDivider = bottomOfBottomDivider - mDividerThickness
                mDividerDrawable?.setBounds(
                    left,
                    topOfBottomDivider,
                    right,
                    bottomOfBottomDivider
                )
                mDividerDrawable?.draw(canvas)
            }
            UNDERLINE -> {
                val bottomOfUnderlineDivider = mBottomDividerBottom
                val topOfUnderlineDivider = bottomOfUnderlineDivider - mDividerThickness
                mDividerDrawable?.setBounds(
                    left,
                    topOfUnderlineDivider,
                    right,
                    bottomOfUnderlineDivider
                )
                mDividerDrawable?.draw(canvas)
            }
        }
    }

    private fun drawText(text: String, x: Float, y: Float, paint: Paint, canvas: Canvas) {
        var y = y
        if (text.contains("\n")) {
            val lines = text.split("\n".toRegex()).toTypedArray()
            val height = (Math.abs(paint.descent() + paint.ascent())
                * mLineSpacingMultiplier)
            val diff = (lines.size - 1) * height / 2
            y -= diff
            for (line: String in lines) {
                canvas.drawText(line, x, y, paint)
                y += height
            }
        } else {
            canvas.drawText(text, x, y, paint)
        }
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = PlayerTimePicker::class.java.name
        event.isScrollable = mScrollerEnabled
        val scroll = (mMinValue + mCurrentValue) * mSelectorElementSize
        val maxScroll = (mMaxValue - mMinValue) * mSelectorElementSize
        if (isHorizontalMode()) {
            event.scrollX = scroll
            event.maxScrollX = maxScroll
        } else {
            event.scrollY = scroll
            event.maxScrollY = maxScroll
        }
    }

    /**
     * Makes a measure spec that tries greedily to use the max value.
     *
     * @param measureSpec The measure spec.
     * @param maxSize     The max value for the size.
     * @return A measure spec greedily imposing the max size.
     */
    private fun makeMeasureSpec(measureSpec: Int, maxSize: Int): Int {
        if (maxSize == SIZE_UNSPECIFIED) {
            return measureSpec
        }
        val size = MeasureSpec.getSize(measureSpec)
        val mode = MeasureSpec.getMode(measureSpec)
        when (mode) {
            MeasureSpec.EXACTLY -> return measureSpec
            MeasureSpec.AT_MOST -> return MeasureSpec.makeMeasureSpec(
                Math.min(size, maxSize),
                MeasureSpec.EXACTLY
            )
            MeasureSpec.UNSPECIFIED -> return MeasureSpec.makeMeasureSpec(
                maxSize,
                MeasureSpec.EXACTLY
            )
            else -> throw IllegalArgumentException("Unknown measure mode: $mode")
        }
    }

    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec. Tries to respect the min size, unless a different size
     * is imposed by the constraints.
     *
     * @param minSize      The minimal desired size.
     * @param measuredSize The currently measured size.
     * @param measureSpec  The current measure spec.
     * @return The resolved size and state.
     */
    private fun resolveSizeAndStateRespectingMinSize(
        minSize: Int, measuredSize: Int,
        measureSpec: Int
    ): Int {
        if (minSize != SIZE_UNSPECIFIED) {
            val desiredWidth = Math.max(minSize, measuredSize)
            return resolveSizeAndState(desiredWidth, measureSpec, 0)
        } else {
            return measuredSize
        }
    }

    /**
     * Resets the selector indices and clear the cached string representation of
     * these indices.
     */
    private fun initializeSelectorWheelIndices() {
        mSelectorIndexToStringCache.clear()
        val selectorIndices = getSelectorIndices()
        val current = mCurrentValue
        for (i in selectorIndices.indices) {
            var selectorIndex = current + (i - mWheelMiddleItemIndex)
            if (mWrapSelectorWheel) {
                selectorIndex = getWrappedSelectorIndex(selectorIndex)
            }
            selectorIndices[i] = selectorIndex
            ensureCachedScrollSelectorValue(selectorIndices[i])
        }
    }

    private fun setValueInternal(current: Int, notifyChange: Boolean) {
        var current = current
        if (mCurrentValue == current) {
            return
        }
        // Wrap around the values if we go past the start or end
        if (mWrapSelectorWheel) {
            current = getWrappedSelectorIndex(current)
        } else {
            current = Math.max(current, mMinValue)
            current = Math.min(current, mMaxValue)
        }
        val previous = mCurrentValue
        setCurrentValue(current)
        // If we're flinging, we'll update the text view at the end when it becomes visible
        if (mScrollState != OnScrollListener.SCROLL_STATE_FLING) {
            updateInputTextView()
        }
        if (notifyChange) {
            notifyChange(previous, current)
        }
        initializeSelectorWheelIndices()
        updateAccessibilityDescription()
        invalidate()
    }

    /**
     * Updates the accessibility values of the view,
     * to the currently selected value
     */
    private fun updateAccessibilityDescription() {
        if (!mAccessibilityDescriptionEnabled) {
            return
        }
        this.contentDescription = mCurrentValue.toString()
    }

    /**
     * Changes the current value by one which is increment or
     * decrement based on the passes argument.
     * decrement the current value.
     *
     * @param increment True to increment, false to decrement.
     */
    private fun changeValueByOne(increment: Boolean) {
        if (!moveToFinalScrollerPosition(mFlingPlayerTimerPickerScroller)) {
            moveToFinalScrollerPosition(mAdjustPlayerTimerPickerScroller)
        }
        smoothScroll(increment, 1)
    }

    /**
     * Starts a smooth scroll to wheel position.
     *
     * @param position The wheel position to scroll to.
     */
    fun smoothScrollToPosition(position: Int) {
        val currentPosition = getSelectorIndices()[mWheelMiddleItemIndex]
        if (currentPosition == position) {
            return
        }
        smoothScroll(position > currentPosition, Math.abs(position - currentPosition))
    }

    /**
     * Starts a smooth scroll
     *
     * @param increment True to increment, false to decrement.
     * @param steps     The steps to scroll.
     */
    fun smoothScroll(increment: Boolean, steps: Int) {
        val diffSteps = (if (increment) -mSelectorElementSize else mSelectorElementSize) * steps
        if (isHorizontalMode()) {
            mPreviousScrollerX = 0
            mFlingPlayerTimerPickerScroller.startScroll(0, 0, diffSteps, 0, SNAP_SCROLL_DURATION)
        } else {
            mPreviousScrollerY = 0
            mFlingPlayerTimerPickerScroller.startScroll(0, 0, 0, diffSteps, SNAP_SCROLL_DURATION)
        }
        invalidate()
    }

    private fun initializeSelectorWheel() {
        initializeSelectorWheelIndices()
        val selectorIndices = getSelectorIndices()
        val totalTextSize = ((selectorIndices.size - 1) * mTextSize + mSelectedTextSize).toInt()
        val textGapCount = selectorIndices.size.toFloat()
        if (isHorizontalMode()) {
            val totalTextGapWidth = ((right - left) - totalTextSize).toFloat()
            mSelectorTextGapWidth = (totalTextGapWidth / textGapCount).toInt()
            mSelectorElementSize = maxTextSize.toInt() + mSelectorTextGapWidth
            mInitialScrollOffset =
                (mSelectedTextCenterX - mSelectorElementSize * mWheelMiddleItemIndex).toInt()
        } else {
            val totalTextGapHeight = ((bottom - top) - totalTextSize).toFloat()
            mSelectorTextGapHeight = (totalTextGapHeight / textGapCount).toInt()
            mSelectorElementSize = maxTextSize.toInt() + mSelectorTextGapHeight
            mInitialScrollOffset =
                (mSelectedTextCenterY - mSelectorElementSize * mWheelMiddleItemIndex).toInt()
        }
        mCurrentScrollOffset = mInitialScrollOffset
        updateInputTextView()
    }

    private fun initializeFadingEdges() {
        if (isHorizontalMode()) {
            isHorizontalFadingEdgeEnabled = true
            isVerticalFadingEdgeEnabled = false
            setFadingEdgeLength((right - left - mTextSize.toInt()) / 2)
        } else {
            isHorizontalFadingEdgeEnabled = false
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength((bottom - top - mTextSize.toInt()) / 2)
        }
    }

    /**
     * Callback invoked upon completion of a given `scroller`.
     */
    private fun onScrollerFinished(playerTimerPickerScroller: PlayerTimerPickerScroller) {
        if (playerTimerPickerScroller === mFlingPlayerTimerPickerScroller) {
            ensureScrollWheelAdjusted()
            updateInputTextView()
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE)
        } else if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            updateInputTextView()
        }
    }

    /**
     * Handles transition to a given `scrollState`
     */
    private fun onScrollStateChange(scrollState: Int) {
        if (mScrollState == scrollState) {
            return
        }
        mScrollState = scrollState
        if (mOnScrollListener != null) {
            mOnScrollListener!!.onScrollStateChange(this, scrollState)
        }
    }

    /**
     * Flings the selector with the given `velocity`.
     */
    private fun fling(velocity: Int) {
        if (isHorizontalMode()) {
            mPreviousScrollerX = 0
            if (velocity > 0) {
                mFlingPlayerTimerPickerScroller.fling(0, 0, velocity, 0, 0, Int.MAX_VALUE, 0, 0)
            } else {
                mFlingPlayerTimerPickerScroller.fling(
                    Int.MAX_VALUE,
                    0,
                    velocity,
                    0,
                    0,
                    Int.MAX_VALUE,
                    0,
                    0
                )
            }
        } else {
            mPreviousScrollerY = 0
            if (velocity > 0) {
                mFlingPlayerTimerPickerScroller.fling(0, 0, 0, velocity, 0, 0, 0, Int.MAX_VALUE)
            } else {
                mFlingPlayerTimerPickerScroller.fling(
                    0,
                    Int.MAX_VALUE,
                    0,
                    velocity,
                    0,
                    0,
                    0,
                    Int.MAX_VALUE
                )
            }
        }
        invalidate()
    }

    /**
     * @return The wrapped index `selectorIndex` value.
     */
    private fun getWrappedSelectorIndex(selectorIndex: Int): Int {
        if (selectorIndex > mMaxValue) {
            return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1
        } else if (selectorIndex < mMinValue) {
            return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1
        }
        return selectorIndex
    }

    private fun getSelectorIndices(): IntArray {
        return mSelectorIndices
    }

    /**
     * Increments the `selectorIndices` whose string representations
     * will be displayed in the selector.
     */
    private fun incrementSelectorIndices(selectorIndices: IntArray) {
        for (i in 0 until selectorIndices.size - 1) {
            selectorIndices[i] = selectorIndices[i + 1]
        }
        var nextScrollSelectorIndex = selectorIndices[selectorIndices.size - 2] + 1
        if (mWrapSelectorWheel && nextScrollSelectorIndex > mMaxValue) {
            nextScrollSelectorIndex = mMinValue
        }
        selectorIndices[selectorIndices.size - 1] = nextScrollSelectorIndex
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex)
    }

    /**
     * Decrements the `selectorIndices` whose string representations
     * will be displayed in the selector.
     */
    private fun decrementSelectorIndices(selectorIndices: IntArray) {
        for (i in selectorIndices.size - 1 downTo 1) {
            selectorIndices[i] = selectorIndices[i - 1]
        }
        var nextScrollSelectorIndex = selectorIndices[1] - 1
        if (mWrapSelectorWheel && nextScrollSelectorIndex < mMinValue) {
            nextScrollSelectorIndex = mMaxValue
        }
        selectorIndices[0] = nextScrollSelectorIndex
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex)
    }

    /**
     * Ensures we have a cached string representation of the given `
     * selectorIndex` to avoid multiple instantiations of the same string.
     */
    private fun ensureCachedScrollSelectorValue(selectorIndex: Int) {
        val cache = mSelectorIndexToStringCache
        var scrollSelectorValue = cache[selectorIndex]
        if (scrollSelectorValue != null) {
            return
        }
        if (selectorIndex < mMinValue || selectorIndex > mMaxValue) {
            scrollSelectorValue = ""
        } else {
            if (mDisplayedValues != null) {
                val displayedValueIndex = selectorIndex - mMinValue
                if (displayedValueIndex >= mDisplayedValues!!.size) {
                    cache.remove(selectorIndex)
                    return
                }
                scrollSelectorValue = mDisplayedValues!![displayedValueIndex]
            } else {
                scrollSelectorValue = formatNumber(selectorIndex)
            }
        }
        cache.put(selectorIndex, scrollSelectorValue)
    }

    private fun formatNumber(value: Int): String {
        return if ((mTimePickerFormatter != null)) mTimePickerFormatter!!.format(value) else formatNumberWithLocale(
            value
        )
    }

    /**
     * Updates the view of this NumberPicker. If displayValues were specified in
     * the string corresponding to the index specified by the current value will
     * be returned. Otherwise, the formatter specified in [.setFormatter]
     * will be used to format the number.
     */
    private fun updateInputTextView() {
        /*
         * If we don't have displayed values then use the current number else
         * find the correct value in the displayed values for the current
         * number.
         */
        val text =
            if ((mDisplayedValues == null)) formatNumber(mCurrentValue) else mDisplayedValues!![mCurrentValue - mMinValue]
        if (TextUtils.isEmpty(text)) {
            return
        }
        val beforeText: CharSequence = mSelectedText.text
        if ((text == beforeText.toString())) {
            return
        }
        mSelectedText.setText(text)
    }

    /**
     * Notifies the listener, if registered, of a change of the value of this
     * NumberPicker.
     */
    private fun notifyChange(previous: Int, current: Int) {
        if (mOnValueChangeListener != null) {
            mOnValueChangeListener!!.onValueChange(this, previous, current)
        }
    }
    /**
     * Posts a command for changing the current value by one.
     *
     * @param increment Whether to increment or decrement the value.
     */
    /**
     * Posts a command for changing the current value by one.
     *
     * @param increment Whether to increment or decrement the value.
     */
    private fun postChangeCurrentByOneFromLongPress(
        increment: Boolean,
        delayMillis: Long = ViewConfiguration.getLongPressTimeout().toLong()
    ) {
        if (mChangeCurrentByOneFromLongPressCommand == null) {
            mChangeCurrentByOneFromLongPressCommand = ChangeCurrentByOneFromLongPressCommand()
        } else {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand)
        }
        mChangeCurrentByOneFromLongPressCommand?.setStep(increment)
        postDelayed(mChangeCurrentByOneFromLongPressCommand, delayMillis)
    }

    /**
     * Removes the command for changing the current value by one.
     */
    private fun removeChangeCurrentByOneFromLongPress() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand)
        }
    }

    /**
     * Removes all pending callback from the message queue.
     */
    private fun removeAllCallbacks() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand)
        }
        if (mSetSelectionCommand != null) {
            mSetSelectionCommand!!.cancel()
        }
    }

    /**
     * @return The selected index given its displayed `value`.
     */
    private fun getSelectedPos(value: String): Int {
        var value = value
        if (mDisplayedValues == null) {
            try {
                return value.toInt()
            } catch (e: NumberFormatException) {
                // Ignore as if it's not a number we don't care
            }
        } else {
            for (i in mDisplayedValues!!.indices) {
                // Don't force the user to type in jan when ja will do
                value = value.toLowerCase()
                if (mDisplayedValues!![i].toLowerCase().startsWith(value)) {
                    return mMinValue + i
                }
            }

            /*
             * The user might have typed in a number into the month field i.e.
             * 10 instead of OCT so support that too.
             */try {
                return value.toInt()
            } catch (e: NumberFormatException) {
                // Ignore as if it's not a number we don't care
            }
        }
        return mMinValue
    }

    /**
     * Posts a [SetSelectionCommand] from the given
     * `selectionStart` to `selectionEnd`.
     */
    private fun postSetSelectionCommand(selectionStart: Int, selectionEnd: Int) {
        if (mSetSelectionCommand == null) {
            mSetSelectionCommand = SetSelectionCommand(mSelectedText)
        } else {
            mSetSelectionCommand!!.post(selectionStart, selectionEnd)
        }
    }

    /**
     * Filter for accepting only valid indices or prefixes of the string
     * representation of valid indices.
     */
    internal inner class InputTextFilter() : NumberKeyListener() {
        // XXX This doesn't allow for range limits when controlled by a soft input method!
        override fun getInputType(): Int {
            return InputType.TYPE_CLASS_TEXT
        }

        override fun getAcceptedChars(): CharArray {
            return DIGIT_CHARACTERS
        }

        override fun filter(
            source: CharSequence, start: Int, end: Int, dest: Spanned,
            dstart: Int, dend: Int
        ): CharSequence {
            // We don't know what the output will be, so always cancel any
            // pending set selection command.
            if (mSetSelectionCommand != null) {
                mSetSelectionCommand!!.cancel()
            }
            if (mDisplayedValues == null) {
                var filtered = super.filter(source, start, end, dest, dstart, dend)
                if (filtered == null) {
                    filtered = source.subSequence(start, end)
                }
                val result = (dest.subSequence(0, dstart).toString() + filtered
                    + dest.subSequence(dend, dest.length))
                if (("" == result)) {
                    return result
                }
                val value = getSelectedPos(result)

                /*
                 * Ensure the user can't type in a value greater than the max
                 * allowed. We have to allow less than min as the user might
                 * want to delete some numbers and then type a new number.
                 * And prevent multiple-"0" that exceeds the length of upper
                 * bound number.
                 */return if (value > mMaxValue || result.length > mMaxValue.toString().length) {
                    ""
                } else {
                    filtered
                }
            } else {
                val filtered: CharSequence = source.subSequence(start, end).toString()
                if (TextUtils.isEmpty(filtered)) {
                    return ""
                }
                val result = (dest.subSequence(0, dstart).toString() + filtered
                    + dest.subSequence(dend, dest.length))
                val str = result.toLowerCase()
                for (value: String in mDisplayedValues!!) {
                    val valLowerCase = value.toLowerCase()
                    if (valLowerCase.startsWith(str)) {
                        postSetSelectionCommand(result.length, value.length)
                        return value.subSequence(dstart, value.length)
                    }
                }
                return ""
            }
        }
    }

    /**
     * Ensures that the scroll wheel is adjusted i.e. there is no offset and the
     * middle element is in the middle of the widget.
     */
    private fun ensureScrollWheelAdjusted() {
        // adjust to the closest value
        var delta = mInitialScrollOffset - mCurrentScrollOffset
        if (delta == 0) {
            return
        }
        if (Math.abs(delta) > mSelectorElementSize / 2) {
            delta += if ((delta > 0)) -mSelectorElementSize else mSelectorElementSize
        }
        if (isHorizontalMode()) {
            mPreviousScrollerX = 0
            mAdjustPlayerTimerPickerScroller.startScroll(
                0,
                0,
                delta,
                0,
                SELECTOR_ADJUSTMENT_DURATION_MILLIS
            )
        } else {
            mPreviousScrollerY = 0
            mAdjustPlayerTimerPickerScroller.startScroll(
                0,
                0,
                0,
                delta,
                SELECTOR_ADJUSTMENT_DURATION_MILLIS
            )
        }
        invalidate()
    }

    /**
     * Command for setting the input text selection.
     */
    private class SetSelectionCommand internal constructor(private val mInputText: EditText) :
        Runnable {
        private var mSelectionStart = 0
        private var mSelectionEnd = 0

        /**
         * Whether this runnable is currently posted.
         */
        private var mPosted = false
        fun post(selectionStart: Int, selectionEnd: Int) {
            mSelectionStart = selectionStart
            mSelectionEnd = selectionEnd
            if (!mPosted) {
                mInputText.post(this)
                mPosted = true
            }
        }

        fun cancel() {
            if (mPosted) {
                mInputText.removeCallbacks(this)
                mPosted = false
            }
        }

        override fun run() {
            mPosted = false
            mInputText.setSelection(mSelectionStart, mSelectionEnd)
        }
    }

    /**
     * Command for changing the current value from a long press by one.
     */
    internal inner class ChangeCurrentByOneFromLongPressCommand() : Runnable {
        private var mIncrement = false
        fun setStep(increment: Boolean) {
            mIncrement = increment
        }

        override fun run() {
            changeValueByOne(mIncrement)
            postDelayed(this, mLongPressUpdateInterval)
        }
    }

    private fun formatNumberWithLocale(value: Int): String {
        return mNumberFormatter.format(value.toLong())
    }

    private fun dpToPx(dp: Float): Float {
        return DpUtils.dp2px(context, dp)
    }

    private fun pxToDp(px: Float): Float {
        return DpUtils.px2dp(context, px)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp,
            resources.displayMetrics
        )
    }

    private fun pxToSp(px: Float): Float {
        return px / resources.displayMetrics.scaledDensity
    }

    private fun String.toTimePickerFormatter(): TimePickerFormatter? {
        return if (isNullOrBlank()) {
            null
        } else {
            val s: String = this
            object : TimePickerFormatter {
                override fun format(i: Int): String {
                    return String.format(Locale.getDefault(), s, i)
                }
            }
        }
    }

    private fun setWidthAndHeight() {
        if (isHorizontalMode()) {
            mMinHeight = SIZE_UNSPECIFIED
            mMaxHeight = dpToPx(DEFAULT_MIN_WIDTH.toFloat()).toInt()
            mMinWidth = dpToPx(DEFAULT_MAX_HEIGHT.toFloat()).toInt()
            mMaxWidth = SIZE_UNSPECIFIED
        } else {
            mMinHeight = SIZE_UNSPECIFIED
            mMaxHeight = dpToPx(DEFAULT_MAX_HEIGHT.toFloat()).toInt()
            mMinWidth = dpToPx(DEFAULT_MIN_WIDTH.toFloat()).toInt()
            mMaxWidth = SIZE_UNSPECIFIED
        }
    }

    fun setDividerColor(color: Int) {
        mDividerColor = color
        mDividerDrawable = ColorDrawable(color)
    }

    fun setDividerColorResource(colorId: Int) {
        setDividerColor(ContextCompat.getColor(context, colorId))
    }

    fun setDividerDistance(distance: Int) {
        mDividerDistance = distance
    }

    fun setDividerDistanceResource(dimenId: Int) {
        setDividerDistance(resources.getDimensionPixelSize(dimenId))
    }

    fun setDividerType(dividerType: Int) {
        mDividerType = dividerType
        invalidate()
    }

    fun setDividerThickness(thickness: Int) {
        mDividerThickness = thickness
    }

    fun setDividerThicknessResource(dimenId: Int) {
        setDividerThickness(resources.getDimensionPixelSize(dimenId))
    }

    fun setOrder(order: Int) {
        mOrder = order
    }

    override fun setOrientation(orientation: Int) {
        mOrientation = orientation
        setWidthAndHeight()
        requestLayout()
    }

    fun setWheelItemCount(count: Int) {
        if (count < 1) {
            throw IllegalArgumentException("Wheel item count must be >= 1")
        }
        mRealWheelItemCount = count
        mWheelItemCount = count.coerceAtLeast(DEFAULT_WHEEL_ITEM_COUNT)
        mWheelMiddleItemIndex = mWheelItemCount / 2
        mSelectorIndices = IntArray(mWheelItemCount)
    }

    fun setFormatter(timePickerFormatter: TimePickerFormatter?) {
        if (timePickerFormatter === mTimePickerFormatter) {
            return
        }
        mTimePickerFormatter = timePickerFormatter
        initializeSelectorWheelIndices()
        updateInputTextView()
    }

    fun setFormatter(string: String?) {
        if (string.isNullOrBlank()) {
            return
        }
        val formatter = string.toTimePickerFormatter() ?: return
        setFormatter(formatter)
    }

    fun setFormatter(stringId: Int) {
        setFormatter(context.getString(stringId))
    }

    fun setFadingEdgeEnabled(fadingEdgeEnabled: Boolean) {
        mFadingEdgeEnabled = fadingEdgeEnabled
    }

    fun setSelectedTextColor(color: Int) {
        mSelectedTextColor = color
        mSelectedText.setTextColor(mSelectedTextColor)
    }

    fun setSelectedTextColorResource(colorId: Int) {
        setSelectedTextColor(ContextCompat.getColor(context, colorId))
    }

    fun setSelectedTextSize(textSize: Float) {
        val size = DpUtils.dp2px(context, textSize)
        mSelectedTextSize = size
        mSelectedText.textSize = pxToSp(size)
    }

    fun setTextColor(color: Int) {
        mTextColor = color
        mSelectorWheelPaint.color = mTextColor
    }

    fun setTextColorResource(colorId: Int) {
        setTextColor(ContextCompat.getColor(context, colorId))
    }

    fun setTextSize(textSize: Float) {
        val size = DpUtils.dp2px(context, textSize)
        mTextSize = size
        mSelectorWheelPaint.textSize = size
    }

    fun setMaxFlingVelocityCoefficient(coefficient: Int) {
        mMaxFlingVelocityCoefficient = coefficient
        mMaximumFlingVelocity = (mViewConfiguration.scaledMaximumFlingVelocity
            / mMaxFlingVelocityCoefficient)
    }

    fun isHorizontalMode(): Boolean {
        return orientation == HORIZONTAL
    }

    fun isAscendingOrder(): Boolean {
        return getOrder() == ASCENDING
    }

    fun getDividerColor(): Int {
        return mDividerColor
    }

    fun getDividerDistance(): Float {
        return pxToDp(mDividerDistance.toFloat())
    }

    fun getDividerThickness(): Float {
        return pxToDp(mDividerThickness.toFloat())
    }

    fun getOrder(): Int {
        return mOrder
    }

    override fun getOrientation(): Int {
        return mOrientation
    }

    fun getWheelItemCount(): Int {
        return mWheelItemCount
    }

    fun isFadingEdgeEnabled(): Boolean {
        return mFadingEdgeEnabled
    }

    fun getSelectedTextColor(): Int {
        return mSelectedTextColor
    }

    fun getSelectedTextSize(): Float {
        return mSelectedTextSize
    }

    fun getTextColor(): Int {
        return mTextColor
    }

    fun getTextSize(): Float {
        return spToPx(mTextSize)
    }

    fun getMaxFlingVelocityCoefficient(): Int {
        return mMaxFlingVelocityCoefficient
    }

    companion object {
        val VERTICAL = LinearLayout.VERTICAL
        val HORIZONTAL = LinearLayout.HORIZONTAL
        val ASCENDING = 0
        val DESCENDING = 1
        val RIGHT = 0
        val CENTER = 1
        val LEFT = 2
        val SIDE_LINES = 0
        val UNDERLINE = 1
        private val DEFAULT_LONG_PRESS_UPDATE_INTERVAL: Long = 300
        private val DEFAULT_MAX_FLING_VELOCITY_COEFFICIENT = 8
        private val SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800
        private val SNAP_SCROLL_DURATION = 300
        private val DEFAULT_FADING_EDGE_STRENGTH = 0.9f
        private val UNSCALED_DEFAULT_DIVIDER_THICKNESS = 2
        private val UNSCALED_DEFAULT_DIVIDER_DISTANCE = 48
        private val SIZE_UNSPECIFIED = -1
        private val DEFAULT_MAX_VALUE = 100
        private val DEFAULT_MIN_VALUE = 1
        private val DEFAULT_WHEEL_ITEM_COUNT = 7
        private val DEFAULT_MAX_HEIGHT = 180
        private val DEFAULT_MIN_WIDTH = 64
        private val DEFAULT_TEXT_ALIGN = CENTER
        private val DEFAULT_TEXT_SIZE = 14f
        private val DEFAULT_SELECTED_TEXT_SIZE = 16f
        private val DEFAULT_LINE_SPACING_MULTIPLIER = 1f

        fun resolveSizeAndState(size: Int, measureSpec: Int, childMeasuredState: Int): Int {
            var result = size
            val specMode = MeasureSpec.getMode(measureSpec)
            val specSize = MeasureSpec.getSize(measureSpec)
            when (specMode) {
                MeasureSpec.UNSPECIFIED -> result = size
                MeasureSpec.AT_MOST -> result = if (specSize < size) {
                    specSize or MEASURED_STATE_TOO_SMALL
                } else {
                    size
                }
                MeasureSpec.EXACTLY -> result = specSize
            }
            return result or (childMeasuredState and MEASURED_STATE_MASK)
        }

        private val DIGIT_CHARACTERS = charArrayOf(
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9'
        )
    }

    @SuppressLint("WrongConstant")
    private fun focusCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Should be focusable by default, as the text view whose visibility changes is focusable
            if (focusable == FOCUSABLE_AUTO) {
                focusable = FOCUSABLE
                isFocusableInTouchMode = true
            }
        }
    }

    private fun init() {
        mNumberFormatter = NumberFormat.getInstance()
    //    setDividerColor(mDividerColor)
        val displayMetrics = resources.displayMetrics
        val defDividerDistance = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            UNSCALED_DEFAULT_DIVIDER_DISTANCE.toFloat(), displayMetrics
        ).toInt()
        val defDividerThickness = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            UNSCALED_DEFAULT_DIVIDER_THICKNESS.toFloat(), displayMetrics
        ).toInt()
        mDividerDistance = defDividerDistance
        mDividerLength = 360
        mDividerThickness = defDividerThickness
        mDividerType = SIDE_LINES
        mOrder = ASCENDING
        mOrientation = VERTICAL
        setWidthAndHeight()
        setWillNotDraw(false)
        val inflater = context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater
        inflater.inflate(R.layout.bili_player_new_number_picker, this, true)

        // input text
        mSelectedText = findViewById(R.id.number_picker_editext)
        mSelectedText.isEnabled = false
        mSelectedText.isFocusable = false
        mSelectedText.imeOptions = EditorInfo.IME_ACTION_NONE
        val paint = Paint()
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        mSelectorWheelPaint = paint
        setSelectedTextColor(mSelectedTextColor)
        setTextColor(mTextColor)
        setTextSize(mTextSize)
        setSelectedTextSize(mSelectedTextSize)
        setFormatter(mTimePickerFormatter)
        updateInputTextView()
        setMaxValue(mMaxValue)
        setMinValue(mMinValue)
        setWheelItemCount(mWheelItemCount)

        setWrapSelectorWheel(mWrapSelectorWheel)
        mViewConfiguration = ViewConfiguration.get(context)
        mTouchSlop = mViewConfiguration.scaledTouchSlop
        mMinimumFlingVelocity = mViewConfiguration.scaledMinimumFlingVelocity
        mMaximumFlingVelocity = (mViewConfiguration.scaledMaximumFlingVelocity
            / mMaxFlingVelocityCoefficient)
        mFlingPlayerTimerPickerScroller = PlayerTimerPickerScroller(context, null)
        mAdjustPlayerTimerPickerScroller =
            PlayerTimerPickerScroller(context, DecelerateInterpolator(2.5f))
        if (importantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        focusCompat()
    }
}