package com.xyczero.customswipelistview;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;
import android.widget.Scroller;

import com.xyczero.customlistview.R;

/**
 * 
 * @author xyczero
 * 
 *         welcome to www.xyczero.com
 * 
 */
public class CustomSwipeListView extends ListView {
	private static final String TAG = "com.xyczeo.customswipelistview";
	public static final String ITEMSWIPE_LAYOUT_TAG = "com.xyczeo.customswipelistview.itemswipelayout";

	private final int MIN_VELOCITY = 500;// dip per second

	private final int TOUCH_SIWPE_RIGHT = 1;
	private final int TOUCH_SIWPE_LEFT = 2;
	private final int TOUCH_SIWPE_AUTO = 3;
	private final int TOUCH_SIWPE_NONE = 4;

	private final int MINIMUM_SWIPEITEM_TRIGGER_DELTAX = 5;

	private int mTouchSwipeMode = TOUCH_SIWPE_NONE;

	private Scroller mScroller;

	private VelocityTracker mVelocityTracker;
	private int mMinimumVelocity;
	private int mMaximumVelocity;

	private View mItemMainView;

	private View mItemSwipeView;

	private View mOldItemMainView;

	private View mOldItemSwipeView;

	private Rect mTouchFrame;

	private boolean isItemSwipeViewVisible;

	private boolean isClickItemSwipeView;

	private boolean isSwiping;

	private int mTouchSlop;

	private int mSelectedPosition = INVALID_POSITION;

	private float mDownMotionX;

	private float mDownMotionY;

	private int mScreenWidth;

	private final int DEFAULT_DURATION = 250;
	private int mAnimationLeftDuration = DEFAULT_DURATION;
	private int mAnimationRightDuration = DEFAULT_DURATION;

	private boolean mEnableSiwpeItemRight = true;

	private boolean mEnableSiwpeItemLeft = true;

	private int mSiwpeItemLeftTriggerDeltaX;

	private int mSwipeItemRightTriggerDeltaX;

	private RemoveItemCustomSwipeListener mRemoveItemCustomSwipeListener;

	public CustomSwipeListView(Context context) {
		super(context);
		initCustomSwipeListView();
	}

	public CustomSwipeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initCustomSwipeListView();
	}

	public CustomSwipeListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initCustomSwipeListView();
	}

	private void initCustomSwipeListView() {
		final Context context = getContext();
		final ViewConfiguration configuration = ViewConfiguration.get(context);

		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		mMinimumVelocity = CustomSwipeUtils.convertDiptoPixel(context,
				MIN_VELOCITY);
		mScreenWidth = CustomSwipeUtils.getScreenWidth(context);
		mScroller = new Scroller(context);
		initSiwpeItemTriggerDeltaX();
	}

	private void initSiwpeItemTriggerDeltaX() {
		mSiwpeItemLeftTriggerDeltaX = mScreenWidth / 3;
		mSwipeItemRightTriggerDeltaX = -mScreenWidth / 3;
	}

	private int getItemSwipeViewWidth(View itemSwipeView) {
		if (itemSwipeView != null)
			return mItemSwipeView.getLayoutParams().width;
		else
			return Integer.MAX_VALUE;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mDownMotionX = ev.getX();
			mDownMotionY = ev.getY();
			mSelectedPosition = INVALID_POSITION;
			mSelectedPosition = pointToPosition((int) mDownMotionX,
					(int) mDownMotionY);
			Log.d(TAG, "selectedPosition:" + mSelectedPosition);
			if (mSelectedPosition != INVALID_POSITION) {
				mItemMainView = getChildAt(
						mSelectedPosition - getFirstVisiblePosition())
						.findViewById(R.id.doc_layout);
				mItemSwipeView = getChildAt(
						mSelectedPosition - getFirstVisiblePosition())
						.findViewWithTag(ITEMSWIPE_LAYOUT_TAG);
				isClickItemSwipeView = isInSwipePosition((int) mDownMotionX,
						(int) mDownMotionY);
			}

			Log.d(TAG, "onInterceptTouchEvent:ACTION_DOWN" + "--"
					+ isClickItemSwipeView);
			break;
		case MotionEvent.ACTION_UP:
			if (isClickItemSwipeView) {
				mItemSwipeView.setVisibility(GONE);
				mItemMainView.scrollTo(0, 0);
				isItemSwipeViewVisible = false;
			}
			recycleVelocityTracker();
			Log.d(TAG, "onInterceptTouchEvent:ACTION_UP" + "--"
					+ isClickItemSwipeView);
			break;
		case MotionEvent.ACTION_CANCEL:
			recycleVelocityTracker();
			Log.d(TAG, "onInterceptTouchEvent:ACTION_CANCEL" + "--"
					+ isClickItemSwipeView);
			break;
		default:
			return false;
		}
		return !isClickItemSwipeView;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction() & MotionEvent.ACTION_MASK;
		final int x = (int) ev.getX();
		if (action == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
			ev.setAction(MotionEvent.ACTION_CANCEL);
			return super.onTouchEvent(ev);
		}

		if (!mScroller.isFinished()) {
			return false;
		}

		if (mSelectedPosition != INVALID_POSITION) {
			addVelocityTrackerMotionEvent(ev);
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG, "onTouchEvent:ACTION_DOWN");
				if (isItemSwipeViewVisible) {
					if (!isClickItemSwipeView && mOldItemMainView != null
							&& mOldItemSwipeView != null) {
						mOldItemSwipeView.setVisibility(GONE);
						mOldItemMainView.scrollTo(0, 0);
					}
					isItemSwipeViewVisible = false;
					ev.setAction(MotionEvent.ACTION_CANCEL);
					return super.onTouchEvent(ev);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				Log.d(TAG, "onTouchEvent:ACTION_MOVE");
				if (Math.abs(getScrollXVelocity()) > mMinimumVelocity
						|| (Math.abs(ev.getX() - mDownMotionX) > mTouchSlop && Math
								.abs(ev.getY() - mDownMotionY) < mTouchSlop)) {
					isSwiping = true;
				}
				if (isSwiping) {
					int deltaX = (int) mDownMotionX - x;
					if (deltaX > 0 && mEnableSiwpeItemLeft || deltaX < 0
							&& mEnableSiwpeItemRight) {
						mDownMotionX = x;
						mItemMainView.scrollBy(deltaX, 0);
					}
					// 如果默认调用super.onTouchEvent(),可能会导致在滑动过程中Item失焦。
					return true;
				}
				break;
			case MotionEvent.ACTION_UP:
				Log.d(TAG, "onTouchEvent:ACTION_UP");
				if (isSwiping) {
					final int velocityX = getScrollXVelocity();
					if (velocityX > mMinimumVelocity) {
						scrollByTouchSwipeMode(TOUCH_SIWPE_RIGHT, -mScreenWidth);
					} else if (velocityX < -mMinimumVelocity) {
						scrollByTouchSwipeMode(TOUCH_SIWPE_LEFT,
								getItemSwipeViewWidth(mItemSwipeView));
					} else {
						scrollByTouchSwipeMode(TOUCH_SIWPE_AUTO,
								Integer.MIN_VALUE);
					}

					recycleVelocityTracker();
					// 此处添加是因为scrollTo(0,0)没有走computeScroll，所以要附加一下
					if (mScroller.isFinished()) {
						isSwiping = false;
					}
					// 防止因横向滑动过慢或距离过小导致出发OnItemClick事件
					ev.setAction(MotionEvent.ACTION_CANCEL);
					return super.onTouchEvent(ev);
				}
				break;
			default:
				break;
			}
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void computeScroll() {
		if (isSwiping && mSelectedPosition != INVALID_POSITION) {
			if (mScroller.computeScrollOffset()) {
				mItemMainView.scrollTo(mScroller.getCurrX(),
						mScroller.getCurrY());
				postInvalidate();

				if (mScroller.isFinished()) {
					isSwiping = false;
					switch (mTouchSwipeMode) {
					case TOUCH_SIWPE_LEFT:
						mItemSwipeView.setVisibility(VISIBLE);
						isItemSwipeViewVisible = true;
						mOldItemMainView = mItemMainView;
						mOldItemSwipeView = mItemSwipeView;
						break;
					case TOUCH_SIWPE_RIGHT:
						if (mRemoveItemCustomSwipeListener == null) {
							throw new NullPointerException(
									"RemoveItemCustomSwipeListener is null, we should called setRemoveItemCustomSwipeListener()");
						}

						mItemMainView.scrollTo(0, 0);
						mRemoveItemCustomSwipeListener
								.onRemoveItemListener(mSelectedPosition);
						break;
					default:
						break;
					}
				}
			}
		}
		super.computeScroll();
	}

	private boolean isInSwipePosition(int x, int y) {
		Rect frame = mTouchFrame;
		if (frame == null) {
			mTouchFrame = new Rect();
			frame = mTouchFrame;
		}
		if (isItemSwipeViewVisible) {
			frame.set(mItemSwipeView.getLeft(),
					getChildAt(mSelectedPosition - getFirstVisiblePosition())
							.getTop(), mItemSwipeView.getRight(),
					getChildAt(mSelectedPosition - getFirstVisiblePosition())
							.getBottom());
			if (frame.contains(x, y)) {
				return true;
			}
		}
		return false;
	}

	private void addVelocityTrackerMotionEvent(MotionEvent ev) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);
	}

	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	private int getScrollXVelocity() {
		mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
		int velocity = (int) mVelocityTracker.getXVelocity();
		return velocity;
	}

	private void scrollByTouchSwipeMode(int touchSiwpeMode, int targetDelta) {
		mTouchSwipeMode = touchSiwpeMode;
		switch (touchSiwpeMode) {
		case TOUCH_SIWPE_RIGHT:
			scrollByTartgetDelta(targetDelta, mAnimationRightDuration);
		case TOUCH_SIWPE_LEFT:
			scrollByTartgetDelta(targetDelta, mAnimationLeftDuration);
			break;
		case TOUCH_SIWPE_AUTO:
			scrollByAuto();
			break;
		default:
			break;
		}
	}

	private void scrollByTartgetDelta(final int targetDelta,
			int animationDuration) {
		final int itemMainScrollX = mItemMainView.getScrollX();
		final int actualDelta = (targetDelta - itemMainScrollX);
		mScroller.startScroll(itemMainScrollX, 0, actualDelta, 0,
				animationDuration);
		postInvalidate();
	}

	private void scrollByAuto() {
		final int itemMainScrollX = mItemMainView.getScrollX();
		if (itemMainScrollX >= mSiwpeItemLeftTriggerDeltaX) {
			scrollByTouchSwipeMode(TOUCH_SIWPE_LEFT,
					getItemSwipeViewWidth(mItemSwipeView));
		} else if (itemMainScrollX <= mSwipeItemRightTriggerDeltaX) {
			scrollByTouchSwipeMode(TOUCH_SIWPE_RIGHT, -mScreenWidth);
		} else {
			// 滚回到原始位置,为了偷下懒这里是直接调用scrollTo滚动
			mItemMainView.scrollTo(0, 0);
			mItemSwipeView.setVisibility(GONE);
			isItemSwipeViewVisible = false;
		}
	}

	public void setAnimationLeftDuration(int duration) {
		mAnimationRightDuration = duration;
	}

	public void setAnimationRightDuration(int duration) {
		mAnimationLeftDuration = duration;
	}

	public void setSiwpeItemLeftEnable(boolean enable) {
		mEnableSiwpeItemLeft = enable;
	}

	public void setSiwpeItemRightEnable(boolean enable) {
		mEnableSiwpeItemRight = enable;
	}

	public void setSwipeItemRightTriggerDeltaX(int dipDeltaX) {
		if (dipDeltaX < MINIMUM_SWIPEITEM_TRIGGER_DELTAX)
			return;
		final int pxDeltaX = CustomSwipeUtils.convertDiptoPixel(getContext(),
				dipDeltaX);
		setSwipeItemTriggerDeltaX(TOUCH_SIWPE_RIGHT, pxDeltaX);
	}

	public void setSwipeItemLeftTriggerDeltaX(int dipDeltaX) {
		if (dipDeltaX < MINIMUM_SWIPEITEM_TRIGGER_DELTAX)
			return;
		final int pxDeltaX = CustomSwipeUtils.convertDiptoPixel(getContext(),
				dipDeltaX);
		setSwipeItemTriggerDeltaX(TOUCH_SIWPE_LEFT, pxDeltaX);
	}

	private void setSwipeItemTriggerDeltaX(int touchMode, int pxDeltaX) {
		switch (touchMode) {
		case TOUCH_SIWPE_RIGHT:
			mSwipeItemRightTriggerDeltaX = pxDeltaX <= mScreenWidth ? -pxDeltaX
					: mScreenWidth;
			break;
		case TOUCH_SIWPE_LEFT:
			mSwipeItemRightTriggerDeltaX = pxDeltaX <= mScreenWidth ? pxDeltaX
					: mScreenWidth;
			break;
		default:
			break;
		}
	}

	public void setRemoveItemCustomSwipeListener(
			RemoveItemCustomSwipeListener removeItemCustomSwipeListener) {
		mRemoveItemCustomSwipeListener = removeItemCustomSwipeListener;
	}

	public interface RemoveItemCustomSwipeListener {
		void onRemoveItemListener(int selectedPostion);
	}
}
