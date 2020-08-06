/*
 * Copyright (C) 2020 Kevin Higgins
 * @author Kevin Higgins
 * I have added some debug logging for learning purposes, and changed code in two
 * places, both demarcated with comments, in order to make this class work with
 * a rectangular Path instead of a Circle as the cropping shape. Otherwise this is
 * all Adam Styrc's design. Original available at https://github.com/adamstyrc/cookie-cutter
 * - Kevin Higgins 05/08/20
 *
 * Copyright (C) 2016 Adam Styrc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adamstyrc.cookiecutter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by adamstyrc on 31/03/16.
 */
public class CookieCutterImageView extends AppCompatImageView {

    private CookieCutterParams cookieCutterParams;

    public CookieCutterImageView(Context context) {
        super(context);
        init();
    }

    public CookieCutterImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public CookieCutterImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    @TargetApi(value = 21)
    public CookieCutterImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        super(context, attrs, defStyleRes);

        init();
    }

    public void init() {
        setScaleType(ScaleType.MATRIX);

        cookieCutterParams = new CookieCutterParams();
        setDefaultRadius();
        /**
         * Modified this because I am adding the image "programatically" as they say on Stack Overflow
         * I could be totally wrong but it seems the original design only allows for image to
         * defined in the XML...
         */
        //if (getDrawable() != null) {
        if (true) {
            Log.d("CookieCutterImageView", "Hacked to always add listener ");
            /**
             * End modified code
             */
            ViewTreeObserver vto = getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    onImageLoaded();
                }
            });
        }
    }

    public void onImageLoaded() {
        cookieCutterParams.updateWithView(getWidth(), getHeight());
        setImageCentered();
        setOnTouchListener(new CookieCutterTouchListener(cookieCutterParams, getImageMatrix()));
    }

    private void setImageCentered() {
        Matrix matrix = getImageMatrix();
        Bitmap bitmap = getBitmap();
        Log.d("CookieCutterImageView", "In setImageCentered");
        if (bitmap != null && cookieCutterParams.getCircle() != null) {
            RectF drawableRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
            Log.d("CookieCutterImageView", "Kevin debug - width prop: " + getWidth() + ", height: " + getHeight());
            Circle circle = cookieCutterParams.getCircle();
            Log.d("CookieCutterImageView", "Kevin debug - circleDiam: " + circle.getDiameter());
            RectF viewRect;

            if (bitmap.getWidth() > bitmap.getHeight()) {
                float scale = (float) circle.getDiameter() / bitmap.getHeight();
                float scaledWidth = scale * bitmap.getWidth();
                float x = (scaledWidth - getWidth()) / 2;
                viewRect = new RectF(-x, circle.getTopBound(), getWidth() + x, circle.getBottomBound());
            } else {
                float scale = (float) circle.getDiameter() / bitmap.getWidth();
                float scaledHeight = scale * bitmap.getHeight();
                float y = (scaledHeight - getHeight()) / 2;
                viewRect = new RectF(circle.getLeftBound(), -y, circle.getRightBound(), getHeight() + y);
            }
            matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
            setImageMatrix(matrix);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Circle circle = cookieCutterParams.getCircle();
        if (circle == null) {
            return;
        }

        Paint paint;
        switch (cookieCutterParams.getShape()) {
            case CIRCLE:
                paint = cookieCutterParams.getCircleParams().paint;
                canvas.drawCircle(circle.getCx(), circle.getCy(), circle.getRadius(), paint);
                break;

            case HOLE:
                CookieCutterParams.HoleParams hole = cookieCutterParams.getHoleParams();
                paint = hole.paint;
                Path path = hole.path;
                canvas.drawPath(path, paint);
                break;

            case SQUARE:
                paint = cookieCutterParams.getSquareParams().paint;
                canvas.drawRect(circle.getLeftBound(), circle.getTopBound(), circle.getRightBound(), circle.getBottomBound(), paint);
                break;
        }
    }

    @Override
    public void setImageURI(Uri uri) {

            super.setImageURI(uri);
            BitmapDrawable bitmapDrawable = (BitmapDrawable) getDrawable();
            Bitmap bitmap = bitmapDrawable.getBitmap();
            setImageBitmap(bitmap);


    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        onImageLoaded();
    }

    public Bitmap getCroppedBitmap() {
        Matrix matrix = getImageMatrix();

        MatrixParams matrixParams = MatrixParams.fromMatrix(matrix);
        Bitmap bitmap = getBitmap();
        Circle circle = cookieCutterParams.getCircle();
        /**
         * @Kevin Higgins begin changed code
         */
        // recover path bounds
        RectF pathBounds = new RectF();
        cookieCutterParams.getHoleParams().path.computeBounds(pathBounds, true);
        Log.d("CookieCutterImageView", "Kevin debug Path bounds:" + pathBounds.toString());
        int sizeX = (int) ((pathBounds.right - pathBounds.left) / matrixParams.getScaleWidth());
        int sizeY = (int) ((pathBounds.bottom - pathBounds.top) / matrixParams.getScaleHeight());
        int y = getCropRectTop(matrixParams, pathBounds);
        int x = getCropRectLeft(matrixParams, pathBounds);

        Log.d("CookieCutterImageView", "Kevin debug x: " + x + " y: " + y + " sizeX: " + sizeX + ", sizeY: " + sizeY);
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap,
                x,
                y,
                sizeX,
                sizeY);

        /*
        @Kevin Higgins end changed code
         */
        return croppedBitmap;
    }

    public CookieCutterParams getParams() {
        return cookieCutterParams;
    }

    private Bitmap getBitmap() {
        return ((BitmapDrawable) getDrawable()).getBitmap();
    }

    private int getCropLeft(MatrixParams matrixParams, Circle circle) {
        float translationX = matrixParams.getX();
        float x = circle.getLeftBound() - translationX;
        x = Math.max(x, 0);
        x /= matrixParams.getScaleWidth();
        return (int) x;
    }

    private int getCropTop(MatrixParams matrixParams, Circle circle) {
        float translationY = matrixParams.getY();
        float y = circle.getTopBound() - translationY;
        y = Math.max(y, 0);
        y /= matrixParams.getScaleWidth();
        return (int) y;
    }

    /**
     * @Kevin Higgins making my own versions of getCropTop and getCropleft for my case of a centred
     * 600x800 rect, rather than the circle. This is a late fix - I didn't realise that
     * this class is focused on the circle case and doesn't seem to have functionality
     * for figuring out the bounds of other shapes. Hence the following two methods to replace
     * the ones using the circle.
     * Begin changed code:
     */
    private int getCropRectLeft(MatrixParams matrixParams, RectF pathBounds) {
        float translationX = matrixParams.getX();
        float x = pathBounds.left - translationX;
        x = Math.max(x, 0);
        x /= matrixParams.getScaleWidth();
        return (int) x;
    }

    private int getCropRectTop(MatrixParams matrixParams, RectF pathBounds) {
        float translationY = matrixParams.getY();
        float y = pathBounds.top - translationY;
        y = Math.max(y, 0);
        y /= matrixParams.getScaleWidth();
        return (int) y;
    }
    /*
    End changed
     */
    private void setDefaultRadius() {
        Point screenSize = ImageUtils.getScreenSize(getContext());
        int minScreenSize = Math.min(screenSize.x, screenSize.y);
        cookieCutterParams.setCircleRadius((int) (minScreenSize * 0.4f));
        Log.d("CookieCutterImageView", "Kevin debug - radius is now: " + cookieCutterParams.getCircleRadius());
    }
}
