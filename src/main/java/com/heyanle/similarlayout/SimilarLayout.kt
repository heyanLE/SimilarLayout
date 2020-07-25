package com.heyanle.similarlayout

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.FrameLayout
import androidx.core.animation.addListener
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by HeYanLe on 2020/7/22 0022 11:04.
 * https://github.com/heyanLE
 */

class SimilarLayout : FrameLayout{


    /*
    Similar 主体 View
    Scrollable 对象，滑动判断是否到顶部
     */
    private var mSimilarView: View? = null
    private var mSimilarScrollable:SimilarScrollable = object : SimilarScrollable{
        override fun isScrollToTop(): Boolean = true
    }

    /*
    Similar 主体 Id
    Scrollable 滑动 View Id ，判断是否滑动到顶部
     */
    private var mSimilarViewId = 0
    private var mSimilarScrollableId = 0

    /*
    Similar 中间位和底部位距离整个 Layout 底部的距离
     */
    private var mSimilarBottomTranslateYAttr = 0.0f
    private var mSimilarCenterTranslateYAttr = -1f

    /*
    同上，但是对应着 TranslateY
     */
    private var mSimilarBottomTranslateY = 0.0f
    private var mSimilarCenterTranslateY = -1f

    /*
    是否在动画
     */
    private var mIsAnimator = false

    /*
    Similar 位置标识
     */

    private var mLocation = Location.BOTTOM

    enum class Location {
        TOP,CENTER,BOTTOM
    }

    private val mVelocityTracker:VelocityTracker by lazy {
        VelocityTracker.obtain()
    }

    private val mSimilarListenerDelegate = SimilarListenerDelegate()


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context,attrs)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        init(context,attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?){
        val array = context.obtainStyledAttributes(attrs, R.styleable.SimilarLayout)
        mSimilarViewId = array.getResourceId(R.styleable.SimilarLayout_similar_view_id,0)
        mSimilarScrollableId = array.getResourceId(R.styleable.SimilarLayout_similar_scrollable_id, 0)
        mSimilarBottomTranslateYAttr = array.getDimension(R.styleable.SimilarLayout_similar_bottom_y, 0f)
        mSimilarCenterTranslateYAttr = array.getDimension(R.styleable.SimilarLayout_similar_center_y, -10000f)
        array.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (mSimilarViewId != 0){
            mSimilarView = findViewById(mSimilarViewId)
        }
        if(mSimilarScrollableId != 0){
            mSimilarScrollable = SimilarScrollable.of(findViewById(mSimilarScrollableId))
        }

        mSimilarView?.translationY = mSimilarBottomTranslateY
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.getSize(heightMeasureSpec)
        mSimilarBottomTranslateY = height-mSimilarBottomTranslateYAttr
        mSimilarCenterTranslateY = height-mSimilarCenterTranslateYAttr
        mSimilarView?.translationY = mSimilarBottomTranslateY
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private var mLastY  = 0f
    private var mDownY = 0f
    private var isSimilarFocus = false
    private var mActivePointId = 0

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (mIsAnimator){
            return true
        }
        mSimilarView?.let {
            when(ev.action){
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.y - mLastY
                    if (dy > 0 && it.translationY == 0f) {
                        if (mSimilarScrollable.isScrollToTop()) {
                            requestDisallowInterceptTouchEvent(false) //父View向子View拦截分发事件
                            return super.dispatchTouchEvent(ev)
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mIsAnimator){
            return true
        }
        mSimilarView?.let {
            when(ev.action){
                MotionEvent.ACTION_DOWN -> {
                    mLastY = ev.y
                    mDownY = ev.y
                    isSimilarFocus = (it.translationY == 0f)
                }
                MotionEvent.ACTION_MOVE ->{
                    val dy = ev.y - mLastY
                    mLastY = ev.y
                    if (mDownY < it.translationY){
                        return false
                    }else{
                        if (dy<0){
                            return  !isSimilarFocus
                        }else if(dy >0){
                            return !isSimilarFocus || mSimilarScrollable.isScrollToTop()
                        }
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (mIsAnimator){
            return true
        }
        mVelocityTracker.addMovement(ev)
        mSimilarView?.let {
            when(ev.action){
                MotionEvent.ACTION_DOWN ->{
                    mActivePointId = ev.getPointerId(ev.actionIndex)
                    mLastY = ev.y
                    mDownY = ev.y
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    mActivePointId = ev.getPointerId(ev.actionIndex)
                    if (mActivePointId == 0){
                        mLastY = ev.getY(mActivePointId)
                    }
                    mActivePointId
                }
                MotionEvent.ACTION_MOVE ->{
                    val dy = ev.y - mLastY
                    mLastY = ev.y
                    var translation = it.translationY+dy

                    translation = max(translation,0f)
                    translation = min(translation, mSimilarBottomTranslateY)

                    it.translationY = translation
                    mSimilarListenerDelegate.mOnScrollListener?.let {listener ->
                        listener(it.translationY, true)
                    }

                }
                MotionEvent.ACTION_UP -> {
                    //similarHoldPosition()
                    mVelocityTracker.computeCurrentVelocity(1000,500f)
                    Log.i("SimilarLayout","mVelocityTracker.yVelocity -> ${mVelocityTracker.yVelocity}")
                    Log.i("SimilarLayout","mLocation -> ${mLocation.name}")
                    when (mVelocityTracker.yVelocity) {
                        500f -> {
                            when(mLocation){
                                Location.TOP -> moveSimilar(Location.CENTER,true)
                                else -> moveSimilar(Location.BOTTOM,true)
                            }
                        }
                        -500f -> {
                            when(mLocation){
                                Location.BOTTOM -> moveSimilar(Location.CENTER,true)
                                else -> moveSimilar(Location.TOP,true)
                            }
                        }
                        else -> {
                            similarHoldPosition()
                        }
                    }
                }
                else -> return true
            }
        }
        return true
    }


    private fun similarHoldPosition(){

        mSimilarView?.let { view ->
            val deltaTop = abs(view.translationY - 0)
            val deltaCenter = abs(view.translationY - mSimilarCenterTranslateY)
            val deltaBottom = abs(view.translationY - mSimilarBottomTranslateY)

            when{
                deltaCenter < deltaTop && deltaCenter < deltaBottom ->
                    moveSimilar(Location.CENTER,true)
                deltaBottom < deltaCenter && deltaBottom < deltaTop ->
                    moveSimilar(Location.BOTTOM,true)
                else -> moveSimilar(Location.TOP,true)
            }
        }

    }

    private fun moveSimilar(location:Location, isUser:Boolean){
        if (mIsAnimator){
            return
        }
        mSimilarView?.let { view ->
            val targetTranslationY:Float = when(location){
                Location.BOTTOM -> mSimilarBottomTranslateY
                Location.CENTER -> mSimilarCenterTranslateY
                else -> 0f
            }
            val animator = ValueAnimator.ofFloat(view.translationY, targetTranslationY)
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                view.translationY = value
                mSimilarListenerDelegate.mOnScrollListener?.let {listener ->
                    listener(value, false)
                }
            }
            animator.addListener (
                onStart = {
                    mIsAnimator = true
                },
                onEnd = {
                    mIsAnimator = false
                    mLocation = location
                    mSimilarListenerDelegate.mOnSimilarChangeListener?.let {listener->
                        listener(mLocation,isUser)
                    }
                }
            )
            animator.start()
        }

    }

    fun moveToTop(){
        moveSimilar(Location.TOP,false)
    }
    fun moveToCenter(){
        moveSimilar(Location.CENTER,false)
    }
    fun moveToBottom(){
        moveSimilar(Location.BOTTOM, false)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mVelocityTracker.recycle()
    }



}