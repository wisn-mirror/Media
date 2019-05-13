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
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;


public class HomeContinuousNestedBottomRecyclerView extends RecyclerView
        implements HomeContinuousNestedBottomView {

    private HomeContinuousNestedBottomView.OnScrollNotifier mOnScrollNotifier;
    private final int[] mScrollConsumed = new int[2];

    public HomeContinuousNestedBottomRecyclerView(@NonNull Context context) {
        super(context);
        init();
    }

    public HomeContinuousNestedBottomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HomeContinuousNestedBottomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (mOnScrollNotifier != null) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        mOnScrollNotifier.onScrollStateChange(recyclerView,
                                HomeContinuousNestedScrollCommon.SCROLL_STATE_IDLE);
                    } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                        mOnScrollNotifier.onScrollStateChange(recyclerView,
                                HomeContinuousNestedScrollCommon.SCROLL_STATE_SETTLING);
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        mOnScrollNotifier.onScrollStateChange(recyclerView,
                                HomeContinuousNestedScrollCommon.SCROLL_STATE_DRAGGING);
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (mOnScrollNotifier != null) {
                    mOnScrollNotifier.notify(
                            recyclerView.computeVerticalScrollOffset(),
                            Math.max(0, recyclerView.computeVerticalScrollRange() - recyclerView.getHeight()));
                }
            }
        });
    }

    @Override
    public void consumeScroll(int yUnconsumed) {
        if (yUnconsumed == Integer.MIN_VALUE) {
            scrollToPosition(0);
        } else if (yUnconsumed == Integer.MAX_VALUE) {
            Adapter adapter = getAdapter();
            if (adapter != null) {
                scrollToPosition(adapter.getItemCount() - 1);
            }
        } else {
            boolean reStartNestedScroll = false;
            if (!hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)) {
                // the scrollBy use ViewCompat.TYPE_TOUCH to handle nested scroll...
                reStartNestedScroll = true;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

                // and scrollBy only call dispatchNestedScroll, not call dispatchNestedPreScroll
                mScrollConsumed[0] = 0;
                mScrollConsumed[1] = 0;
                dispatchNestedPreScroll(0, yUnconsumed, mScrollConsumed, null, ViewCompat.TYPE_TOUCH);
                yUnconsumed -= mScrollConsumed[1];
            }
            scrollBy(0, yUnconsumed);
            if (reStartNestedScroll) {
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
            }
        }
    }

    @Override
    public int getContentHeight() {
        Adapter adapter = getAdapter();
        if (adapter == null) {
            return 0;
        }
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return 0;
        }
        final int scrollRange = this.computeVerticalScrollRange();
        if (scrollRange > getHeight()) {
            return HEIGHT_IS_ENOUGH_TO_SCROLL;
        }
        return scrollRange;
    }

    @Override
    public void injectScrollNotifier(OnScrollNotifier notifier) {
        mOnScrollNotifier = notifier;
    }

    @Override
    public int getCurrentScroll() {
        return computeVerticalScrollOffset();
    }

    @Override
    public int getScrollOffsetRange() {
        return Math.max(0, computeVerticalScrollRange() - getHeight());
    }

    @Override
    public void smoothScrollYBy(int dy, int duration) {
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        smoothScrollBy(0, dy, null);
    }

    @Override
    public Object saveScrollInfo() {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager lm = (LinearLayoutManager) layoutManager;
            if (lm.getOrientation() == LinearLayoutManager.HORIZONTAL) {
                return null;
            }
            int pos = lm.findFirstVisibleItemPosition();
            View firstView = lm.findViewByPosition(pos);
            int offset = firstView == null ? 0 : firstView.getTop();
            return new ScrollInfo(pos, offset);
        }
        return null;
    }

    @Override
    public void restoreScrollInfo(Object scrollInfo) {
        if (!(scrollInfo instanceof ScrollInfo)) {
            return;
        }
        ScrollInfo sc = (ScrollInfo) scrollInfo;
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(sc.scrollPosition, sc.scrollOffset);
        }
    }

    public static class ScrollInfo {
        public int scrollPosition;
        public int scrollOffset;

        public ScrollInfo(int scrollPosition, int scrollOffset) {
            this.scrollPosition = scrollPosition;
            this.scrollOffset = scrollOffset;
        }
    }
}
