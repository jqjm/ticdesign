package ticwear.design.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import ticwear.design.R;

/**
 * 实现类似progressbar功能，
 * 点击左右有两个按钮可以调节progressbar的值，
 * 也可以点击progressbar本身去调节值
 * Created by louxiaodan on 16/4/27.
 */
public class VolumeBar extends FrameLayout {
    private ProgressBarButton mMinButton;
    private ProgressBarButton mMaxButton;

    // 当前值
    private int mProgress = 50;
    private int mProgressStart;
    // 点击按钮时变化的值
    private int mProgressStep = 10;
    // 当数值小于（大于）10时隐藏减号（加号）
    private Paint mPaint;
    // bar中图片image的半径（乘2的值为宽和高）
    private int mDrawableRadius;
    // thumb背景
    private Drawable mVolumeDrawable;
    // 无声时thumb背景
    private Drawable mNoVolumeDrawable;
    // 减号
    private Drawable mMinButtonDrawable;
    // 加号
    private Drawable mMaxButtonDrawable;
    // 背景色
    private int mBgColor;
    // 数值颜色
    private int mValueColor;
    // 前后左右的padding
    private int mTouchPadding;

    // 数值改变时回到监听器的onVolumeChanged()
    private OnVolumeChangedListener mListener;

    private SeekBar mSeekbar;

    private int mMinLimit = 0;
    private int mMaxLimit = 100;

    public VolumeBar(Context context) {
        this(context, null);
    }

    public VolumeBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumeBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_Ticwear_VolumeBar);
    }

    public VolumeBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setWillNotDraw(false);
        inflater.inflate(R.layout.volume_bar_ticwear, this, true);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.VolumeBar, defStyleAttr, defStyleRes);
        mDrawableRadius = (a.getDimensionPixelSize(R.styleable.VolumeBar_tic_vb_btnImageSize, 32)) / 2;
        mBgColor = a.getColor(R.styleable.VolumeBar_tic_vb_bgColor, Color.RED);
        mValueColor = a.getColor(R.styleable.VolumeBar_tic_vb_valueColor, Color.GREEN);

        mTouchPadding = a.getDimensionPixelSize(R.styleable.VolumeBar_tic_vb_touchPadding, 0);
        int thumbImageId = a.getResourceId(R.styleable.VolumeBar_tic_vb_thumbImage, 0);
        int thumbLeftImageId = a.getResourceId(R.styleable.VolumeBar_tic_vb_thumbLeftImage, 0);
        a.recycle();
        mPaint = new Paint();
        mPaint.setDither(true);                    // set the dither to true
        mPaint.setStyle(Paint.Style.STROKE);       // set to STOKE
        mPaint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        mPaint.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
        mPaint.setAntiAlias(true);

        // 读取需要的背景图
        Resources.Theme t = context.getApplicationContext().getTheme();
        if (thumbImageId != 0) {
            mVolumeDrawable = getResources().getDrawable(thumbImageId, t);
            mNoVolumeDrawable = getResources().getDrawable(thumbLeftImageId, t);
        }
        mMinButtonDrawable = getResources().getDrawable(R.drawable.tic_ic_minus_32px, t);
        mMaxButtonDrawable = getResources().getDrawable(R.drawable.tic_ic_plus_32px, t);

        // 设定各按钮监听器
        mMinButton = (ProgressBarButton) findViewById(R.id.min);
        mMinButton.setDefaultImageSize(mDrawableRadius * 2);
        mMinButton.setTouchListener(mMinButtonListener);

        mMaxButton = (ProgressBarButton) findViewById(R.id.max);
        mMaxButton.setTouchListener(mMaxButtonListener);

        mSeekbar = (SeekBar) findViewById(R.id.seekbar);
        mSeekbar.setProgress(mProgress);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(mTouchPadding, 0, mTouchPadding, 0);
        mSeekbar.setLayoutParams(lp);

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    progress = validateProgress(progress);
                    if (progress != mProgress) {
                        adjustVolume(progress - mProgress, true);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public interface OnVolumeChangedListener {
        void onVolumeChanged(VolumeBar volumeBar, int progress, boolean fromUser);
    }

    public void setOnVolumeChangedListetener(OnVolumeChangedListener listener) {
        mListener = listener;
    }

    /**
     * 设定当前值
     *
     * @param progress 当前值
     */
    public void setProgress(int progress) {
        mProgress = validateProgress(progress);
        mSeekbar.setProgress(mProgress);
        if (mListener != null) {
            mListener.onVolumeChanged(this, mProgress, false);
        }
        invalidate();
    }

    /**
     * 设置progress的增加值
     * 若设置后的progress超出最大（最小限度）
     * 则progress为最大（最小）限度
     *
     * @param dif 增加值（可为负）
     */
    public void setProgressDif(int dif) {
        mProgress += dif;
        mProgress = validateProgress(mProgress);
        mSeekbar.setProgress(mProgress);
        if (mListener != null) {
            mListener.onVolumeChanged(this, mProgress, false);
        }
        invalidate();
    }

    /**
     * 得到但前值
     *
     * @return 当前值
     */
    public int getProgress() {
        return mProgress;
    }

    /**
     * 设定按下按钮时改变的大小
     *
     * @param step 改变的大小
     */
    public void setStep(int step) {
        mProgressStep = step;
    }

    /**
     * 设定可以选取的最小值
     *
     * @param minLimit 最小值
     */
    public void setMinLimit(int minLimit) {
        if (minLimit > mMaxLimit) {
            minLimit = mMaxLimit;
        }
        mMinLimit = minLimit;
        // 若当前值小于最小值，则当前值设为最小值
        if (mProgress < mMinLimit) {
            mProgress = mMinLimit;
            mSeekbar.setProgress(mProgress);
            if (mListener != null) {
                mListener.onVolumeChanged(this, mProgress, false);
            }
            invalidate();
        }
    }

    /**
     * 设定可以选取的最大值
     *
     * @param maxLimit 最大值
     */
    public void setMaxLimit(int maxLimit) {
        if (maxLimit < mMinLimit) {
            maxLimit = mMinLimit;
        }
        mMaxLimit = maxLimit;
        // 若当前值大于最大值，则当前值设为最大值
        if (mProgress > mMaxLimit) {
            mProgress = mMaxLimit;
            mSeekbar.setProgress(mProgress);
            if (mListener != null) {
                mListener.onVolumeChanged(this, mProgress, false);
            }
            invalidate();
        }
    }

    private ProgressBarButton.TouchListener mMinButtonListener = new ProgressBarButton.TouchListener() {
        @Override
        public void onDown() {
            mProgressStart = mProgress;
        }

        @Override
        public void onUp() {
            if (mProgressStart - mProgress < mProgressStep) {
                int det = Math.min(mProgressStep, mProgressStep - (mProgressStart - mProgress));
                adjustVolume(-det, false);
            }
        }

        @Override
        public void onLongPress() {
            adjustVolume(-1, false);
        }
    };

    private ProgressBarButton.TouchListener mMaxButtonListener = new ProgressBarButton.TouchListener() {
        @Override
        public void onDown() {
            mProgressStart = mProgress;
        }

        @Override
        public void onUp() {
            if (mProgress - mProgressStart < mProgressStep) {
                int det = Math.min(mProgressStep, mProgressStep - (mProgress - mProgressStart));
                adjustVolume(det, false);
            }
        }

        @Override
        public void onLongPress() {
            adjustVolume(1, false);
        }
    };

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (visibility == View.VISIBLE) {
            mMinButton.setTouchListener(mMinButtonListener);
            mMaxButton.setTouchListener(mMaxButtonListener);
        } else {
            mMinButton.removeTouchListener();
            mMaxButton.removeTouchListener();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        int radius = getHeight() / 2 - mTouchPadding;
        int radiusWithPadding = radius + mTouchPadding;

        mPaint.setStrokeWidth(2 * radius);
//        mPaint.setPathEffect(new CornerPathEffect(radius));
        mPaint.setColor(mBgColor);

        // 背景线
        canvas.drawLine(radiusWithPadding, radiusWithPadding, getWidth() - radiusWithPadding, radiusWithPadding, mPaint);

        mPaint.setColor(mValueColor);

        // 取值线
        canvas.drawLine(radiusWithPadding, radiusWithPadding, radiusWithPadding + mProgress / 100.0f * (getWidth() - 2 * radiusWithPadding), radiusWithPadding, mPaint);

        // 判断是否隐藏减号
        float thumbleft = mTouchPadding + mProgress / 100.0f * (getWidth() - 2 * radiusWithPadding);
        if (thumbleft < radiusWithPadding) {
            mMinButton.setImageDrawable(null);
        } else {
            mMinButton.setImageDrawable(mMinButtonDrawable);
        }

        // 判断是否隐藏加号
        float thumbRight = mTouchPadding + 2 * radius + mProgress / 100.0f * (getWidth() - 2 * radiusWithPadding);
        float buttonLeft = getWidth() - radiusWithPadding;

        if (thumbRight > buttonLeft) {
            mMaxButton.setImageDrawable(null);
        } else {
            mMaxButton.setImageDrawable(mMaxButtonDrawable);
        }

        // 设定thumb图片
        Drawable thumbBg;
        if (mProgress == 0) {
            thumbBg = mNoVolumeDrawable;
        } else {
            thumbBg = mVolumeDrawable;
        }
        if (thumbBg != null) {
            thumbBg.setBounds((int) (radiusWithPadding + mProgress / 100.0f * (getWidth() - 2 * radiusWithPadding) - mDrawableRadius),
                    radiusWithPadding - mDrawableRadius,
                    (int) (radiusWithPadding + mProgress / 100.0f * (getWidth() - 2 * radiusWithPadding) + mDrawableRadius),
                    radiusWithPadding + mDrawableRadius);
            thumbBg.draw(canvas);
        }
        super.onDraw(canvas);
    }

    private void adjustVolume(int det, boolean fromSeekbar) {
        mProgress += det;
        mProgress = validateProgress(mProgress);
        if (!fromSeekbar) {
            mSeekbar.setProgress(mProgress);
        }
        if (mListener != null) {
            mListener.onVolumeChanged(this, mProgress, true);
        }
        invalidate();
    }

    private int validateProgress(int progress) {
        if (progress > mMaxLimit) {
            progress = mMaxLimit;
        } else if (progress < mMinLimit) {
            progress = mMinLimit;
        }
        return progress;
    }
}
