package com.heyanle.similarlayout

import android.view.InflateException
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by HeYanLe on 2020/7/22 0022 11:19.
 * https://github.com/heyanLE
 */

interface SimilarScrollable {

    abstract fun isScrollToTop():Boolean

    companion object{

        fun of(view:View):SimilarScrollable =
            when(view){
                is ScrollView -> object :SimilarScrollable{
                    override fun isScrollToTop(): Boolean =
                        (view.scrollY == 0)

                }
                is NestedScrollView -> object :SimilarScrollable{
                    override fun isScrollToTop(): Boolean =
                        (view.scrollY == 0)
                }
                is RecyclerView -> object :SimilarScrollable{
                    override fun isScrollToTop(): Boolean =
                        view.computeVerticalScrollOffset() == 0
                }
                is SimilarScrollable -> view
            else -> object :SimilarScrollable{
                override fun isScrollToTop(): Boolean = true

            }
        }


    }


}