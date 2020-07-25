package com.heyanle.similarlayout

/**
 * Created by HeYanLe on 2020/7/22 0022 12:34.
 * https://github.com/heyanLE
 */
class SimilarListenerDelegate {

    var mOnScrollListener: ((Float, Boolean) -> Unit)? = null
    var mOnSimilarChangeListener: ((SimilarLayout.Location, Boolean) -> Unit)? = null

}