package com.filemanager.util;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * Created by wuhao on 2015/7/1.
 */
public class FooterScrollHelper {

    private static final int STATE_ONSCREEN = 0;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;

    private ListView mListView;
    private int mState = STATE_ONSCREEN;
    private int mScrollY;
    private int mMinRawY = 0;
    private int mTargetHeight = 0;

    private boolean mListViewDraw = false;

    public void setTargetViewHeight(ListView listView, int height) {
        mListView = listView;
        mTargetHeight = height;
        mListView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mListViewDraw = true;
                    }
                });
    }

    public int getFooterTranslationY(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        if (mListView == null) {
            return 0;
        }

        mScrollY = 0;
        int translationY = 0;

        if (mListViewDraw) {
            mScrollY = getScrollY();
        } else {
            return 0;
        }

        int rawY = mScrollY;

        switch (mState) {
            case STATE_OFFSCREEN:
                if (rawY >= mMinRawY) {
                    mMinRawY = rawY;
                } else {
                    mState = STATE_RETURNING;
                }
                translationY = rawY;
                break;

            case STATE_ONSCREEN:
                if (rawY > mTargetHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                translationY = rawY;
                break;

            case STATE_RETURNING:

                translationY = (rawY - mMinRawY) + mTargetHeight;

                if (translationY < 0) {
                    translationY = 0;
                    mMinRawY = rawY + mTargetHeight;
                }

                if (rawY == 0) {
                    mState = STATE_ONSCREEN;
                    translationY = 0;
                }

                if (translationY > mTargetHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                break;
        }

        return translationY;
    }

    private int getScrollY() {
        View c = mListView.getChildAt(0);
        if (c == null) {
            return 0;
        }
        int top = c.getTop();
        return -top + mListView.getFirstVisiblePosition() * c.getHeight();
    }

}
