package ticwear.design.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ticwear.design.R;
import ticwear.design.drawable.ArcDrawable;

/**
 * Created by wnzhang on 16-5-31.
 */
public class SwipeTodoView extends RelativeLayout {
    private static final String TAG = "SwipeTodoView";
    private static final int ANIMATION_TIME = 400;
    private static final float ICON_SCALE_DEFAULT = 1.0f;
    private static final float ICON_SCALE_DOWN = 1.16f;
    private float mCenterIvInitX;
    private TextView mContentTv;
    private ImageView mOuterCircleIv;
    private ImageView mMiddleCircleIv;
    private ImageView mInnerCircleIv;
    private ImageView mCenterIv;
    private ImageView mLeftIv;
    private ImageView mRightIv;
    private TextView mSubContentTv;
    private TextView mTipTv;
    private ArcDrawable mLeftBgDrawable;
    private ArcDrawable mRightBgDrawable;
    private ObjectAnimator mIconAnimator;
    private ObjectAnimator mLeftBtnShowAnimator;
    private ObjectAnimator mRightBtnShowAnimator;
    private ObjectAnimator mLeftBtnHideAnimator;
    private ObjectAnimator mRightBtnHideAnimator;
    private AnimatorSet mOuterAnimator = null;
    private AnimatorSet mMiddleAnimator = null;
    private AnimatorSet mInnerAnimator = null;
    private boolean mShowLeftButton = false;
    private boolean mShowRightButton = false;
    private boolean mHasCenterIcon = false;
    private boolean mIsStopAnimation = false;
    private OnSelectChangedListener mLeftListener = null;
    private OnSelectChangedListener mRightListener = null;
    private ActionListener mActionListener = null;
    private Handler mHandler = new Handler();

    public SwipeTodoView(Context context) {
        this(context, null);
    }

    public SwipeTodoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.swipe_todo_view_ticwear, this);
        initView(context, attrs);
        initButtonAnimator();
        addTouchListener();
        startRotationAnimation();
        startRippleAnimation();
    }

    private void initView(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.SwipeTodoView);
        Drawable centerIconBg = typedArray.getDrawable(R.styleable.SwipeTodoView_tic_centerBtnBg);
        int leftIconResId = typedArray.getResourceId(R.styleable.SwipeTodoView_tic_leftBtnImage, 0);
        int rightIconResId = typedArray.getResourceId(R.styleable
                .SwipeTodoView_tic_rightBtnImage, 0);
        mShowLeftButton = (0 != leftIconResId);
        mShowRightButton = (0 != rightIconResId);
        int centerIconResId = typedArray.getResourceId(R.styleable
                .SwipeTodoView_tic_centerBtnImage, 0);
        ColorStateList defaultColorList = ColorStateList.valueOf(Color.BLUE);
        ColorStateList leftColorStateList = typedArray.getColorStateList(R.styleable
                .SwipeTodoView_tic_leftBtnColor);
        if (null == leftColorStateList) {
            leftColorStateList = defaultColorList;
        }
        ColorStateList rightColorStateList = typedArray.getColorStateList(R.styleable
                .SwipeTodoView_tic_rightBtnColor);
        if (null == rightColorStateList) {
            rightColorStateList = defaultColorList;
        }
        ColorStateList leftBgColor = typedArray.getColorStateList(R.styleable
                .SwipeTodoView_tic_leftBtnBgColor);
        ColorStateList rightBgColor = typedArray.getColorStateList(R.styleable
                .SwipeTodoView_tic_rightBtnBgColor);
        mLeftBgDrawable = new ArcDrawable(Color.WHITE);
        mLeftBgDrawable.setTintList(leftBgColor);
        mLeftBgDrawable.setGravity(Gravity.LEFT);
        mLeftBgDrawable.setAlpha(0);
        mRightBgDrawable = new ArcDrawable(Color.WHITE);
        mRightBgDrawable.setGravity(Gravity.RIGHT);
        mRightBgDrawable.setTintList(rightBgColor);
        mRightBgDrawable.setAlpha(0);
        String content = typedArray.getString(R.styleable.SwipeTodoView_tic_content);
        String subContent = typedArray.getString(R.styleable.SwipeTodoView_tic_subContent);
        typedArray.recycle();

        mOuterCircleIv = (ImageView) findViewById(R.id.outer_circle_iv);
        mMiddleCircleIv = (ImageView) findViewById(R.id.middle_circle_iv);
        mInnerCircleIv = (ImageView) findViewById(R.id.inner_circle_iv);
        mContentTv = (TextView) findViewById(R.id.content_tv);
        mSubContentTv = (TextView) findViewById(R.id.sub_content_tv);
        mTipTv = (TextView) findViewById(R.id.tip_tv);
        mCenterIv = (ImageView) findViewById(R.id.center_iv);
        mLeftIv = (ImageView) findViewById(R.id.left_iv);
        mRightIv = (ImageView) findViewById(R.id.right_iv);

        mContentTv.setText(content);
        mSubContentTv.setText(subContent);
        mCenterIv.setBackground(centerIconBg);
        if (centerIconResId != 0) {
            mHasCenterIcon = true;
            mCenterIv.setImageResource(centerIconResId);
            mCenterIv.getDrawable().setAlpha(255);
        }
        mLeftIv.setImageResource(leftIconResId);
        mLeftIv.setImageTintList(leftColorStateList);
        mLeftIv.setBackground(mLeftBgDrawable);
        mRightIv.setImageResource(rightIconResId);
        mRightIv.setImageTintList(rightColorStateList);
        mRightIv.setBackground(mRightBgDrawable);
    }

    private void initButtonAnimator() {
        mLeftBtnShowAnimator = getButtonShowAnimator(mLeftIv);
        mRightBtnShowAnimator = getButtonShowAnimator(mRightIv);
        mLeftBtnHideAnimator = getButtonHideAnimator(mLeftIv);
        mRightBtnHideAnimator = getButtonHideAnimator(mRightIv);
    }

    private ObjectAnimator getButtonShowAnimator(ImageView iv) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(iv,
                PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1.0f),
                PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1.0f),
                PropertyValuesHolder.ofFloat("alpha", 0, 1.0f));
        animator.setDuration(320);
        return animator;
    }

    private ObjectAnimator getButtonHideAnimator(ImageView iv) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(iv,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.3f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.3f),
                PropertyValuesHolder.ofFloat("alpha", 1.0f, 0));
        animator.setDuration(320);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mActionListener != null) {
                    mActionListener.onActionUp();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return animator;
    }

    private void addTouchListener() {
        mCenterIv.setOnTouchListener(new OnTouchListener() {
            private int mRawX;
            private float mDeltaX;
            private float mInitX;

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setScaleX(ICON_SCALE_DOWN);
                        v.setScaleY(ICON_SCALE_DOWN);
                        mInitX = v.getX();
                        mCenterIvInitX = mInitX;
                        mDeltaX = mInitX - event.getRawX();
                        if (mHasCenterIcon) {
                            mCenterIv.getDrawable().setAlpha(0);
                        }
                        showButtons();
                        pauseAnimation();
                        if (mActionListener != null) {
                            mActionListener.onActionDown();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mRawX = (int) (event.getRawX());
                        if (mRawX < mCenterIv.getWidth() && mShowLeftButton) {
                            mLeftIv.setSelected(true);
                            mLeftBgDrawable.setAlpha(255);
                        } else if ((mRawX > getWidth() - mCenterIv.getWidth()) &&
                                mShowRightButton) {
                            mRightIv.setSelected(true);
                            mRightBgDrawable.setAlpha(255);
                        } else {
                            mLeftIv.setSelected(false);
                            mRightIv.setSelected(false);
                            mLeftBgDrawable.setAlpha(0);
                            mRightBgDrawable.setAlpha(0);
                        }
                        v.setX(event.getRawX() + mDeltaX);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mRawX = (int) (event.getRawX());
                        if (mRawX < mCenterIv.getWidth() && mShowLeftButton) {
                            if (null != mLeftListener) {
                                mLeftListener.onSelected();
                            }
                        } else if ((mRawX > getWidth() - mCenterIv.getWidth()) &&
                                mShowRightButton) {
                            if (null != mRightListener) {
                                mRightListener.onSelected();
                            }
                        } else {
                            resetState(v);
                        }
                        break;
                }
                return true;
            }

            private void resetState(View v) {
                v.animate().scaleX(ICON_SCALE_DEFAULT).scaleY(ICON_SCALE_DEFAULT).x(mInitX)
                        .setDuration(0).start();
                if (mHasCenterIcon) {
                    mCenterIv.getDrawable().setAlpha(255);
                }
                hideButtons();
                resumeAnimation();
            }
        });
    }

    private void startRotationAnimation() {
        if (!mHasCenterIcon) {
            return;
        }
        mIconAnimator = ObjectAnimator.ofFloat(mCenterIv, "rotation", 0f, 15f);
        mIconAnimator.setRepeatCount(Animation.INFINITE);
        mIconAnimator.setInterpolator(new CycleInterpolator(1));
        mIconAnimator.setDuration(2000);
        mIconAnimator.start();
    }

    private AnimatorSet initRippleAnimator(ImageView iv) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(iv,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 2.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 2.5f),
                PropertyValuesHolder.ofFloat("alpha", 0, 1.0f));
        animator.setDuration(500);
        animator.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator animator2 = ObjectAnimator.ofPropertyValuesHolder(iv,
                PropertyValuesHolder.ofFloat("scaleX", 2.5f, 4.0f),
                PropertyValuesHolder.ofFloat("scaleY", 2.5f, 4.0f),
                PropertyValuesHolder.ofFloat("alpha", 1.0f, 0));
        animator2.setDuration(500);
        animator2.setInterpolator(new DecelerateInterpolator());

        final AnimatorSet set = new AnimatorSet();
        set.play(animator).before(animator2);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!mIsStopAnimation) {
                    set.start();
                }
            }
        });
        return set;
    }

    private void startRippleAnimation() {
        mOuterAnimator = initRippleAnimator(mOuterCircleIv);
        mMiddleAnimator = initRippleAnimator(mMiddleCircleIv);
        mInnerAnimator = initRippleAnimator(mInnerCircleIv);
        mIsStopAnimation = false;
        mOuterAnimator.start();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mMiddleAnimator.start();
            }
        }, ANIMATION_TIME);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mInnerAnimator.start();
            }
        }, ANIMATION_TIME);
    }

    public void pauseAnimation() {
        if (null != mIconAnimator) {
            mIconAnimator.pause();
        }
        mOuterCircleIv.setVisibility(View.GONE);
        mMiddleCircleIv.setVisibility(View.GONE);
        mInnerCircleIv.setVisibility(View.GONE);
        mOuterAnimator.pause();
        mMiddleAnimator.pause();
        mInnerAnimator.pause();
    }

    public void resumeAnimation() {
        if (null != mIconAnimator) {
            mIconAnimator.resume();
        }
        mOuterCircleIv.setVisibility(View.VISIBLE);
        mMiddleCircleIv.setVisibility(View.VISIBLE);
        mInnerCircleIv.setVisibility(View.VISIBLE);
        mOuterAnimator.resume();
        mMiddleAnimator.resume();
        mInnerAnimator.resume();
    }

    private void endAnimation() {
        if (null != mIconAnimator) {
            mIconAnimator.end();
        }
        mIsStopAnimation = true;
        mOuterAnimator.end();
        mInnerAnimator.end();
        mMiddleAnimator.end();
    }

    private void showButtons() {
        if (mShowLeftButton) {
            mLeftBtnShowAnimator.start();
        }
        if (mShowRightButton) {
            mRightBtnShowAnimator.start();
        }
    }

    private void hideButtons() {
        if (mShowLeftButton) {
            mLeftBtnHideAnimator.start();
        }
        if (mShowRightButton) {
            mRightBtnHideAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        endAnimation();
        super.onDetachedFromWindow();
    }

    public void setContent(CharSequence str) {
        mContentTv.setText(str);
    }

    public void setSubContent(CharSequence str) {
        mSubContentTv.setText(str);
    }

    public void setLeftIcon(int resId) {
        mLeftIv.setImageResource(resId);
        mShowLeftButton = (0 != resId);
    }

    public void setLeftIconColor(ColorStateList color) {
        mLeftIv.setImageTintList(color);
    }

    public void setLeftBgColor(ColorStateList color) {
        mLeftBgDrawable.setTintList(color);
    }

    public void setRightIcon(int resId) {
        mRightIv.setImageResource(resId);
        mShowRightButton = (0 != resId);
    }

    public void setRightIconColor(ColorStateList color) {
        mRightIv.setImageTintList(color);
    }

    public void setRightBgColor(ColorStateList color) {
        mRightBgDrawable.setTintList(color);
    }

    public interface OnSelectChangedListener {
        void onSelected();
    }

    public void setLeftIconListener(OnSelectChangedListener listener) {
        mLeftListener = listener;
    }

    public void setRightIconListener(OnSelectChangedListener listener) {
        mRightListener = listener;
    }

    public void enableTipTv(String text) {
        mTipTv.setVisibility(View.VISIBLE);
        mTipTv.setText(text);
    }

    public void resetToInit() {
        mCenterIv.clearAnimation();
        mCenterIv.animate().scaleX(ICON_SCALE_DEFAULT).scaleY(ICON_SCALE_DEFAULT).x
                (mCenterIvInitX).setDuration(0).start();
        if (mHasCenterIcon) {
            mCenterIv.getDrawable().setAlpha(255);
        }
        hideButtons();
        resumeAnimation();
    }

    public interface ActionListener {
        void onActionDown();

        void onActionUp();
    }

    public void setActionListener(ActionListener listener) {
        mActionListener = listener;
    }
}
