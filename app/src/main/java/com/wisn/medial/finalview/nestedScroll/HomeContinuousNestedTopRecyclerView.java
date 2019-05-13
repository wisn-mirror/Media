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

public class HomeContinuousNestedTopRecyclerView extends RecyclerView implements HomeContinuousNestedTopView {

    private OnScrollNotifier mScrollNotifier;
    private final int[] mScrollConsumed = new int[2];

    public HomeContinuousNestedTopRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public HomeContinuousNestedTopRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeContinuousNestedTopRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public int consumeScroll(int dyUnconsumed) {
        if (dyUnconsumed == Integer.MIN_VALUE) {
            scrollToPosition(0);
            return Integer.MIN_VALUE;
        } else if (dyUnconsumed == Integer.MAX_VALUE) {
            Adapter adapter = getAdapter();
            if (adapter != null) {
                scrollToPosition(adapter.getItemCount() - 1);
            }
            return Integer.MAX_VALUE;
        }

        boolean reStartNestedScroll = false;
        if (!hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)) {
            // the scrollBy use ViewCompat.TYPE_TOUCH to handle nested scroll...
            reStartNestedScroll = true;
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

            // and scrollBy only call dispatchNestedScroll, not call dispatchNestedPreScroll
            mScrollConsumed[0] = 0;
            mScrollConsumed[1] = 0;
            dispatchNestedPreScroll(0, dyUnconsumed, mScrollConsumed, null, ViewCompat.TYPE_TOUCH);
            dyUnconsumed -= mScrollConsumed[1];
        }
        scrollBy(0, dyUnconsumed);
        if (reStartNestedScroll) {
            stopNestedScroll(ViewCompat.TYPE_TOUCH);
        }
        return 0;
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
    public void injectScrollNotifier(OnScrollNotifier notifier) {
        mScrollNotifier = notifier;
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        if(mScrollNotifier != null){
            mScrollNotifier.notify(getCurrentScroll(), getScrollOffsetRange());
        }
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
        if(mScrollNotifier != null){
            mScrollNotifier.notify(getCurrentScroll(), getScrollOffsetRange());
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
