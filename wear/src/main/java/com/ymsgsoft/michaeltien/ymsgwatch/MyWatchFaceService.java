/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ymsgsoft.michaeltien.ymsgwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFaceService extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        final String TAG = Engine.class.getSimpleName();
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + connectionResult);
            }
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {

        }

        @Override
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }

        }

        @Override
        public void onConnected(Bundle bundle) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + bundle);
            }
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
//            updateConfigDataItemAndUiOnStartup();
        }

        Paint mBackgroundPaint;
        Paint mHandPaint;
        Paint mTextPaint;

        boolean mAmbient;
        Time mTime;

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(MyWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        int mHandleColor;
        int mSecHandleColor;
        int mMarkerColor;
        int mMarkerAmbientColor;
        int mBackgroundColor;
        float mHandleWidth;
        float mSecondHandleWidth;
        float mHeavyMarkStroke;
        float mThinMarkStroke;
        float mInnerRadius;
        float mMinDelta;
        float mHrDelta;
        boolean mIsRound;
        int mScreenTextColor = Color.WHITE;
        float mXOffset, mYOffset;

        float mTextSpacingHeight;
        int mHighTemp = 103, mLowTemp = 98;
        String mDegree;

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            Resources resources = MyWatchFaceService.this.getResources();
            mIsRound = insets.isRound();
            mXOffset = resources.getDimension(
                    mIsRound ? R.dimen.x_offset_round : R.dimen.x_offset);

            float textSize = resources.getDimension(
                    mIsRound ? R.dimen.text_size_round : R.dimen.text_size);

            mTextSpacingHeight = resources.getDimension(R.dimen.text_size);
            mTextPaint.setTextSize(textSize);

        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = MyWatchFaceService.this.getResources();
            mHandleColor = resources.getColor(R.color.analog_hands);
            mSecHandleColor = resources.getColor(R.color.second_hands);
            mMarkerColor = resources.getColor(R.color.marker_color);
            mMarkerAmbientColor = resources.getColor(R.color.marker_ambient_color);

            mInnerRadius = resources.getDimension(R.dimen.inner_circle_radius);
            mHandleWidth = resources.getDimension(R.dimen.analog_hand_stroke);
            mSecondHandleWidth = resources.getDimension(R.dimen.second_hand_stroke);
            mHeavyMarkStroke = resources.getDimension(R.dimen.heavy_mark_stroke);
            mThinMarkStroke = resources.getDimension(R.dimen.thin_mark_stroke);
            mMinDelta = resources.getDimension(R.dimen.min_hand_delta);
            mHrDelta = resources.getDimension(R.dimen.hr_hand_delta);
            mDegree = resources.getString(R.string.format_temperature) + 'C';

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);

            mHandPaint = new Paint();
            mHandPaint.setColor(mHandleColor);

            mHandPaint.setStrokeWidth(mHandleWidth);
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mTextPaint = new Paint();
            mTextPaint.setColor(mScreenTextColor);
            mTextPaint.setTypeface(BOLD_TYPEFACE);
            mTextPaint.setAntiAlias(true);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }
        private void drawRoundMarkers(Canvas canvas, float centerX, float centerY, float radius){
//            float radius = secLength;
            float radius2 = radius - 15;
            mHandPaint.setStyle(Paint.Style.STROKE);
            mHandPaint.setStrokeWidth(mSecondHandleWidth);
            canvas.drawCircle(centerX, centerY, radius, mHandPaint);
            canvas.drawCircle(centerX, centerY, radius2, mHandPaint);
            // draw marks
            for (int i = 0; i < 12; i++) {
                float rot = ((float) i) / 6f * (float) Math.PI;
                float x1 = (float) Math.sin(rot) * radius + centerX;
                float y1 = (float) -Math.cos(rot) * radius + centerY;
                float x2 = (float) Math.sin(rot) * radius2 + centerX;
                float y2 = (float) -Math.cos(rot) * radius2 + centerY;
                if (i % 3 == 0) {
                    mHandPaint.setStrokeWidth(mHeavyMarkStroke);
                } else {
                    mHandPaint.setStrokeWidth(mThinMarkStroke);

                }
                canvas.drawLine(x1, y1, x2, y2, mHandPaint);
            }
            mHandPaint.setStyle(Paint.Style.FILL);
            mHandPaint.setStrokeWidth(mHandleWidth);
            mHandPaint.setColor(mHandleColor);
        }
        private void drawSquareMarkers(Canvas canvas, float centerX, float centerY, float width, float height) {
            float radius = (float) Math.sqrt(Math.pow(width / 2, 2) + Math.pow(height / 2, 2));
            mHandPaint.setStyle(Paint.Style.STROKE);
            mHandPaint.setStrokeWidth(mSecondHandleWidth);
//                canvas.drawCircle(centerX, centerY, radius, mHandPaint);
//                canvas.drawCircle(centerX, centerY, radius2, mHandPaint);
            // draw marks
            float dx = 10;
            float dy = 10;
            if (!mAmbient) {
                mHandPaint.setStrokeWidth(mThinMarkStroke);
                for (int i = 0; i < 60; i++) {
                    float rot = ((float) i) / 30f * (float) Math.PI;
                    float x1 = (float) Math.sin(rot) * radius + centerX;
                    float y1 = (float) -Math.cos(rot) * radius + centerY;
                    if (i % 5 == 0) {
                        mHandPaint.setStrokeWidth(mHeavyMarkStroke);
                    } else {
                        mHandPaint.setStrokeWidth(mThinMarkStroke);
                    }
                    canvas.drawLine(centerX, centerY, x1, y1, mHandPaint);
                }
                // draw background
                canvas.drawRect(dx, dx, width - dx, height - dy, mBackgroundPaint);
            }
            mHandPaint.setStrokeWidth(mHeavyMarkStroke);
            for (int i = 0; i < 12; i++) {
                float rot = ((float) i) / 6f * (float) Math.PI;
                float x1 = (float) Math.sin(rot) * radius + centerX;
                float y1 = (float) -Math.cos(rot) * radius + centerY;
                canvas.drawLine(centerX, centerY, x1, y1, mHandPaint);
            }
            // draw background
            dx = 20;
            dy = 20;
            canvas.drawRect(dx, dx, width - dx, height - dy, mBackgroundPaint);
            mHandPaint.setStrokeWidth(mHandleWidth);
            mHandPaint.setColor(mHandleColor);
        }
        private Bitmap toGrayscale(Bitmap bmpOriginal){
            int width, height;
            height = bmpOriginal.getHeight();
            width = bmpOriginal.getWidth();

            Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(bmpGrayscale);
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
            paint.setColorFilter(f);
            c.drawBitmap(bmpOriginal, 0, 0, paint);
            return bmpGrayscale;
        }
        private void drawBorderText(Canvas canvas, String text, float x, float y){
            int color = mTextPaint.getColor();
            canvas.drawText( text, x-1, y-1, mTextPaint);
            canvas.drawText( text, x-1, y+1, mTextPaint);
            canvas.drawText(text, x + 1, y + 1, mTextPaint);
            canvas.drawText(text, x + 1, y - 1, mTextPaint);
            mTextPaint.setColor(mBackgroundColor);
            canvas.drawText(text, x, y, mTextPaint);
            mTextPaint.setColor(color);
        }
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Resources resources = MyWatchFaceService.this.getResources();
            mBackgroundColor = resources.getColor(mAmbient? R.color.analog_ambient_background: R.color.analog_background);

            mTime.setToNow();
            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
            mBackgroundPaint.setColor(mBackgroundColor);
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
//            float minRot = minutes / 30f * (float) Math.PI;
//            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;
            float minRotDegree = minutes * 6f;
            float hrRotDegree = mTime.hour * 30f + minutes / 2f;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            // draw marker
            if (!mAmbient) {
                mHandPaint.setColor(mMarkerColor);
            } else {
                mHandPaint.setColor(mMarkerAmbientColor);
            }
            if ( mIsRound) {
                drawRoundMarkers(canvas, centerX, centerY, secLength);
            } else {
                drawSquareMarkers(canvas, centerX, centerY, width, height);
            }
            // draw temperature
//            float dx = resources.getDimension(R.dimen.degree_offset);
            float high_temp_width = mTextPaint.measureText(String.valueOf(mHighTemp));
            float low_temp_offset =
                    ( high_temp_width - mTextPaint.measureText(String.valueOf(mLowTemp))) / 2;

            mYOffset = centerY;
            if ( !mAmbient ) {
                canvas.drawText(
                        String.valueOf(mHighTemp),
                        mXOffset,
                        mYOffset - mHeavyMarkStroke,
                        mTextPaint);
                canvas.drawText(
                        String.valueOf(mLowTemp),
                        mXOffset + low_temp_offset,
                        mYOffset + mTextSpacingHeight,
                        mTextPaint);

                canvas.drawText(
                        mDegree,
                        mXOffset + high_temp_width+5,
                        mYOffset + mTextSpacingHeight / 2,
                        mTextPaint);
            } else {
                drawBorderText(canvas,
                        String.valueOf(mHighTemp),
                        mXOffset,
                        mYOffset - mHeavyMarkStroke);
                drawBorderText(canvas,
                        String.valueOf(mLowTemp),
                        mXOffset + low_temp_offset,
                        mYOffset + mTextSpacingHeight);
                drawBorderText( canvas,
                        mDegree,
                        mXOffset + high_temp_width+5,
                        mYOffset + mTextSpacingHeight / 2 );
            }
            mHandPaint.setStrokeWidth(2);
//            float dx = resources.getDimension(R.dimen.divider_offset);
            float dl = high_temp_width;
            canvas.drawLine(mXOffset, mYOffset, mXOffset + dl, mYOffset, mHandPaint);

            // draw logo
            Drawable drawable = resources.getDrawable(R.drawable.ic_watch_logo);
            if ( drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if ( mAmbient) bitmap = toGrayscale(bitmap);
                canvas.drawBitmap( bitmap,
                        centerX+resources.getDimension(R.dimen.watch_icon_offsetX),
                        centerY+resources.getDimension(R.dimen.watch_icon_offsetY), null );
            }
            // draw weather icon
            mHandPaint.setColor(mMarkerColor);
            if ( !mAmbient ) {
                mHandPaint.setStyle(Paint.Style.FILL);
            } else {
                mHandPaint.setStyle(Paint.Style.STROKE);
            }
            float wx = resources.getDimension(R.dimen.weather_icon_offSetX);
            float wy = resources.getDimension(R.dimen.weather_icon_offSetY);
            float wr = resources.getDimension(R.dimen.weather_icon_radius);
            canvas.drawCircle( centerX+wx+wr, centerY, wr, mHandPaint );
            mHandPaint.setColor(mHandleColor);

            drawable = resources.getDrawable(mAmbient? R.drawable.ic_rain1 : R.drawable.ic_rain);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                canvas.drawBitmap( bitmap, centerX+wx, centerY-wy, null );
            }

            mHandPaint.setStrokeWidth(mHandleWidth);

            // draw minute hand and hour hand
            if ( mAmbient) {
                mHandPaint.setStyle(Paint.Style.STROKE);
            } else {
                mHandPaint.setStyle(Paint.Style.FILL);
            }
            canvas.save();
            canvas.rotate(minRotDegree, centerX, centerY);
            canvas.drawRect(centerX - mInnerRadius, centerY - minLength, centerX + 2 * mInnerRadius, centerY - mMinDelta, mHandPaint);
            if ( !mAmbient) { // draw divider shadow
                mHandPaint.setStyle(Paint.Style.STROKE);
                mHandPaint.setColor(mBackgroundColor);
                canvas.drawRect(centerX - mInnerRadius, centerY - minLength, centerX + 2 * mInnerRadius, centerY - mMinDelta, mHandPaint);
                mHandPaint.setColor(mHandleColor);
                mHandPaint.setStyle(Paint.Style.FILL);
            }
            canvas.rotate(hrRotDegree - minRotDegree, centerX, centerY);
            canvas.drawRect(centerX - mInnerRadius, centerY - hrLength, centerX + 2 * mInnerRadius, centerY - mHrDelta, mHandPaint);
            canvas.drawRect(centerX - mInnerRadius, centerY-hrLength, centerX + 2 * mInnerRadius, centerY - mHrDelta, mHandPaint);
            if ( !mAmbient) { // draw divider shadow
                mHandPaint.setStyle(Paint.Style.STROKE);
                mHandPaint.setColor(mBackgroundColor);
                canvas.drawRect(centerX - mInnerRadius, centerY - hrLength, centerX + 2 * mInnerRadius, centerY - mHrDelta, mHandPaint);
                mHandPaint.setColor(mHandleColor);
            }
            canvas.restore();

            canvas.drawCircle(centerX, centerY, mInnerRadius * 2, mHandPaint);
            canvas.drawCircle(centerX, centerY, mInnerRadius, mBackgroundPaint);

            if (!mAmbient) {
                // draw sec hand
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                mHandPaint.setColor(mSecHandleColor);
                mHandPaint.setStrokeWidth(mSecondHandleWidth);
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mHandPaint);
                mHandPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerY, centerY, mInnerRadius*2, mHandPaint);
                mHandPaint.setColor(mHandleColor);
                mHandPaint.setStrokeWidth(mHandleWidth);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFaceService.Engine> mWeakReference;

        public EngineHandler(MyWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
