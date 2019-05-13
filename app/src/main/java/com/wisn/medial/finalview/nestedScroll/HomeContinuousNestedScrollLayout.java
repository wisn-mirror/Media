/*
 * Tencent is pleased to support the open source community by making QMUI_Android available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wisn.medial.finalview.nestedScroll;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class HomeContinuousNestedScrollLayout extends CoordinatorLayout implements
        HomeContinuousNestedTopAreaBehavior.Callback {

    private HomeContinuousNestedTopView mTopView;
    private HomeContinuousNestedBottomView mBottomView;

    private HomeContinuousNestedTopAreaBehavior mTopAreaBehavior;
    private HomeContinuousNestedBottomAreaBehavior mBottomAreaBehavior;
    private List<OnScrollListener> mOnScrollListeners = new ArrayList<>();
    private Runnable mCheckLayoutAction = new Runnable() {
        @Override
        public void run() {
            checkLayout();
        }
    };
    private boolean mKeepBottomAreaStableWhenCheckLayout = false;

    public HomeContinuousNestedScrollLayout(@NonNull Context context) {
        super(context);
    }

    public HomeContinuousNestedScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HomeContinuousNestedScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addOnScrollListener(@NonNull OnScrollListener onScrollListener) {
        if (!mOnScrollListeners.contains(onScrollListener)) {
            mOnScrollListeners.add(onScrollListener);
        }
    }

    public void removeOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListeners.remove(onScrollListener);
    }

    public void setKeepBottomAreaStableWhenCheckLayout(boolean keepBottomAreaStableWhenCheckLayout) {
        mKeepBottomAreaStableWhenCheckLayout = keepBottomAreaStableWhenCheckLayout;
    }

    public boolean isKeepBottomAreaStableWhenCheckLayout() {
        return mKeepBottomAreaStableWhenCheckLayout;
    }

    public void setTopAreaView(View topView, @Nullable LayoutParams layoutParams) {
        if (!(topView instanceof HomeContinuousNestedTopView)) {
            throw new IllegalStateException("topView must implement from HomeContinuousNestedTopView");
        }
        if (mTopView != null) {
            removeView(((View) mTopView));
        }
        mTopView = (HomeContinuousNestedTopView) topView;
        mTopView.injectScrollNotifier(new HomeContinuousNestedScrollCommon.OnScrollNotifier() {
            @Override
            public void notify(int innerOffset, int innerRange) {
                int offsetCurrent = mTopAreaBehavior == null ? 0 : -mTopAreaBehavior.getTopAndBottomOffset();
                int bottomCurrent = mBottomView == null ? 0 : mBottomView.getCurrentScroll();
                int bottomRange = mBottomView == null ? 0 : mBottomView.getScrollOffsetRange();
                dispatchScroll(innerOffset, innerRange, offsetCurrent, getOffsetRange(), bottomCurrent, bottomRange);
            }

            @Override
            public void onScrollStateChange(View view, int newScrollState) {
                // not need this. top view scroll is driven by top behavior
            }
        });
        if (layoutParams == null) {
            layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Behavior behavior = layoutParams.getBehavior();
        if (behavior instanceof HomeContinuousNestedTopAreaBehavior) {
            mTopAreaBehavior = (HomeContinuousNestedTopAreaBehavior) behavior;
        } else {
            mTopAreaBehavior = new HomeContinuousNestedTopAreaBehavior(getContext());
            layoutParams.setBehavior(mTopAreaBehavior);
        }
        mTopAreaBehavior.setCallback(this);
        addView(topView, layoutParams);
    }

    public HomeContinuousNestedTopView getTopView() {
        return mTopView;
    }

    public HomeContinuousNestedBottomView getBottomView() {
        return mBottomView;
    }

    public HomeContinuousNestedTopAreaBehavior getTopAreaBehavior() {
        return mTopAreaBehavior;
    }

    public HomeContinuousNestedBottomAreaBehavior getBottomAreaBehavior() {
        return mBottomAreaBehavior;
    }

    public void setBottomAreaView(View bottomView, @Nullable LayoutParams layoutParams) {
        if (!(bottomView instanceof HomeContinuousNestedBottomView)) {
            throw new IllegalStateException("bottomView must implement from HomeContinuousNestedBottomView");
        }
        if (mBottomView != null) {
            removeView(((View) mBottomView));
        }
        mBottomView = (HomeContinuousNestedBottomView) bottomView;
        mBottomView.injectScrollNotifier(new HomeContinuousNestedBottomView.OnScrollNotifier() {
            @Override
            public void notify(int innerOffset, int innerRange) {
                int topCurrent = mTopView == null ? 0 : mTopView.getCurrentScroll();
                int topRange = mTopView == null ? 0 : mTopView.getScrollOffsetRange();
                int offsetCurrent = mTopAreaBehavior == null ? 0 : -mTopAreaBehavior.getTopAndBottomOffset();
                dispatchScroll(topCurrent, topRange, offsetCurrent, getOffsetRange(), innerOffset, innerRange);
            }

            @Override
            public void onScrollStateChange(View view, int newScrollState) {
                dispatchScrollStateChange(newScrollState, false);
            }
        });
        if (layoutParams == null) {
            layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        Behavior behavior = layoutParams.getBehavior();
        if (behavior instanceof HomeContinuousNestedBottomAreaBehavior) {
            mBottomAreaBehavior = (HomeContinuousNestedBottomAreaBehavior) behavior;
        } else {
            mBottomAreaBehavior = new HomeContinuousNestedBottomAreaBehavior();
            layoutParams.setBehavior(mBottomAreaBehavior);
        }
        addView(bottomView, layoutParams);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        postCheckLayout();
    }

    public void postCheckLayout() {
        removeCallbacks(mCheckLayoutAction);
        post(mCheckLayoutAction);
    }

    public void checkLayout() {
        if (mTopView == null || mBottomView == null) {
            return;
        }
        int topCurrent = mTopView.getCurrentScroll();
        int topRange = mTopView.getScrollOffsetRange();
        int offsetCurrent = -mTopAreaBehavior.getTopAndBottomOffset();
        int offsetRange = getOffsetRange();
        if (offsetCurrent >= offsetRange || (offsetCurrent > 0 && mKeepBottomAreaStableWhenCheckLayout)) {
            mTopView.consumeScroll(Integer.MAX_VALUE);
            return;
        }

        if (mBottomView.getCurrentScroll() > 0) {
            mBottomView.consumeScroll(Integer.MIN_VALUE);
        }

        if (topCurrent < topRange && offsetCurrent > 0) {
            int remain = topRange - topCurrent;
            if (offsetCurrent >= remain) {
                mTopView.consumeScroll(Integer.MAX_VALUE);
                mTopAreaBehavior.setTopAndBottomOffset(remain - offsetCurrent);
            } else {
                mTopView.consumeScroll(offsetCurrent);
                mTopAreaBehavior.setTopAndBottomOffset(0);
            }
        }
    }

    public void scrollBottomViewToTop() {
        if (mTopView != null) {
            mTopView.consumeScroll(Integer.MAX_VALUE);
        }

        if (mBottomView != null) {
            mBottomView.consumeScroll(Integer.MIN_VALUE);

            int contentHeight = mBottomView.getContentHeight();
            if (contentHeight != HomeContinuousNestedBottomView.HEIGHT_IS_ENOUGH_TO_SCROLL) {
                mTopAreaBehavior.setTopAndBottomOffset(
                        getHeight() - contentHeight - ((View) mTopView).getHeight());
            } else {
                mTopAreaBehavior.setTopAndBottomOffset(
                        getHeight() - ((View) mBottomView).getHeight() - ((View) mTopView).getHeight());
            }
        }
    }

    private void dispatchScroll(int topCurrent, int topRange,
                                int offsetCurrent, int offsetRange,
                                int bottomCurrent, int bottomRange) {
        for (OnScrollListener onScrollListener : mOnScrollListeners) {
            onScrollListener.onScroll(topCurrent, topRange, offsetCurrent, offsetRange,
                    bottomCurrent, bottomRange);
        }
    }

    private void dispatchScrollStateChange(int newScrollState, boolean fromTopBehavior) {
        for (OnScrollListener onScrollListener : mOnScrollListeners) {
            onScrollListener.onScrollStateChange(newScrollState, fromTopBehavior);
        }
    }

    public void scrollBy(int dy) {
        if (dy > 0 && mTopAreaBehavior != null) {
            mTopAreaBehavior.scroll(this, ((View) mTopView), dy);
        } else if (dy != 0 && mBottomView != null) {
            mBottomView.consumeScroll(dy);
        }
    }

    public void smoothScrollBy(int dy, int duration) {
        if (dy == 0) {
            return;
        }
        if (dy > 0 && mTopAreaBehavior != null) {
            mTopAreaBehavior.smoothScrollBy(this, ((View) mTopView), dy, duration);
        } else if (mBottomView != null) {
            mBottomView.smoothScrollYBy(dy, duration);
        }
    }

    public void stopScroll() {
        if (mBottomView != null) {
            mBottomView.stopScroll();
        }
        if (mTopAreaBehavior != null) {
            mTopAreaBehavior.stopFlingOrScroll();
        }
    }

    public void scrollToTop() {
        if (mBottomView != null) {
            mBottomView.consumeScroll(Integer.MIN_VALUE);
        }
        if (mTopView != null) {
            mTopAreaBehavior.setTopAndBottomOffset(0);
            mTopView.consumeScroll(Integer.MIN_VALUE);
        }
    }


    public void scrollToBottom() {
        if (mTopView != null) {
            // consume the max value
            mTopView.consumeScroll(Integer.MAX_VALUE);
            if (mBottomView != null) {
                int contentHeight = mBottomView.getContentHeight();
                if (contentHeight != HomeContinuousNestedBottomView.HEIGHT_IS_ENOUGH_TO_SCROLL) {
                    // bottomView can not scroll
                    View topView = (View) mTopView;
                    if (topView.getHeight() + contentHeight < getHeight()) {
                        mTopAreaBehavior.setTopAndBottomOffset(0);
                    } else {
                        mTopAreaBehavior.setTopAndBottomOffset(
                                getHeight() - contentHeight - ((View) mTopView).getHeight());
                    }
                } else {
                    mTopAreaBehavior.setTopAndBottomOffset(
                            getHeight() - ((View) mBottomView).getHeight() - ((View) mTopView).getHeight());
                }
            }
        }
        if (mBottomView != null) {
            mBottomView.consumeScroll(Integer.MAX_VALUE);
        }
    }

    public int getOffsetCurrent() {
        return mTopAreaBehavior == null ? 0 : -mTopAreaBehavior.getTopAndBottomOffset();
    }

    public int getOffsetRange() {
        if (mTopView == null || mBottomView == null) {
            return 0;
        }
        int contentHeight = mBottomView.getContentHeight();
        if (contentHeight != HomeContinuousNestedBottomView.HEIGHT_IS_ENOUGH_TO_SCROLL) {
            return ((View) mTopView).getHeight() - (getHeight() - contentHeight);
        }
        return ((View) mTopView).getHeight() - (getHeight() - ((View) mBottomView).getHeight());
    }

    @Override
    public void onTopAreaOffset(int offset) {
        int topCurrent = mTopView == null ? 0 : mTopView.getCurrentScroll();
        int topRange = mTopView == null ? 0 : mTopView.getScrollOffsetRange();
        int bottomCurrent = mBottomView == null ? 0 : mBottomView.getCurrentScroll();
        int bottomRange = mBottomView == null ? 0 : mBottomView.getScrollOffsetRange();
        dispatchScroll(topCurrent, topRange, -offset, getOffsetRange(), bottomCurrent, bottomRange);
    }

    @Override
    public void onTopBehaviorTouchBegin() {
        dispatchScrollStateChange(
                HomeContinuousNestedScrollCommon.SCROLL_STATE_DRAGGING, true);
    }

    @Override
    public void onTopBehaviorTouchEnd() {
        dispatchScrollStateChange(
                HomeContinuousNestedScrollCommon.SCROLL_STATE_IDLE, true);
    }

    @Override
    public void onTopBehaviorFlingOrScrollStart() {
        dispatchScrollStateChange(
                HomeContinuousNestedScrollCommon.SCROLL_STATE_SETTLING, true);
    }

    @Override
    public void onTopBehaviorFlingOrScrollEnd() {
        dispatchScrollStateChange(
                HomeContinuousNestedScrollCommon.SCROLL_STATE_IDLE, true);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            stopScroll();
        }
        return super.dispatchTouchEvent(ev);
    }

    public ScrollInfo saveScrollInfo() {
        Object topInfo = mTopView != null ? mTopView.saveScrollInfo() : null;
        Object bottomInfo = mBottomView != null ? mBottomView.saveScrollInfo() : null;
        return new ScrollInfo(topInfo, bottomInfo, getOffsetCurrent());
    }

    public void restoreScrollInfo(@Nullable ScrollInfo scrollInfo) {
        if (scrollInfo == null) {
            return;
        }
        if (mTopAreaBehavior != null) {
            mTopAreaBehavior.setTopAndBottomOffset(-scrollInfo.topBottomOffset);
        }
        if (mTopView != null) {
            mTopView.restoreScrollInfo(scrollInfo.topInfo);
        }

        if (mBottomView != null) {
            mBottomView.restoreScrollInfo(scrollInfo.bottomInfo);
        }
    }

    public interface OnScrollListener {

        void onScroll(int topCurrent, int topRange,
                      int offsetCurrent, int offsetRange,
                      int bottomCurrent, int bottomRange);

        void onScrollStateChange(int newScrollState, boolean fromTopBehavior);
    }

    public static class ScrollInfo {
        public Object topInfo;
        public Object bottomInfo;
        public int topBottomOffset;

        public ScrollInfo(Object topInfo, Object bottomInfo, int topBottomOffset) {
            this.topInfo = topInfo;
            this.bottomInfo = bottomInfo;
            this.topBottomOffset = topBottomOffset;
        }
    }
}
