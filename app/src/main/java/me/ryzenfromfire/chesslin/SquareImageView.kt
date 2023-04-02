package me.ryzenfromfire.chesslin

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

// Adapted from: https://stackoverflow.com/questions/16506275/imageview-be-a-square-with-dynamic-width
class SquareImageView : androidx.appcompat.widget.AppCompatImageView {

    // Based on this post, these constructors must be overridden to properly display the ImageView in Design Mode
    // https://stackoverflow.com/questions/16592965/android-studio-layout-editor-cannot-render-custom-views
    constructor(ctx: Context) : super (ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth
        setMeasuredDimension(width, width)
    }
}