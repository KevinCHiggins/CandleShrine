/*
 * Copyright (C) 2020 Kevin Higgins
 * @author Kevin Higgins
 * I changed the Path option to be a 600x800 rectangle, commenting my changes. Otherwise this is
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

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Created by adamstyrc on 04/04/16.
 */
public class CookieCutterParams {

    private int circleRadius = 400;
    private Circle circle;
    private float maxZoom = 4;
    private int minImageSize = 200;
    private CookieCutterShape shape = CookieCutterShape.HOLE;

    private int width;
    private int height;

    private HoleParams holeParams;
    private CircleParams circleParams;
    private SquareParams squareParams;


    Circle getCircle() {
        return circle;
    }

    public void setCircleRadius(int circleRadius) {
        this.circleRadius = circleRadius;
    }

    public int getCircleRadius() {
        return circleRadius;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
    }

    public int getMinImageSize() {
        return minImageSize;
    }

    public void setMinImageSize(int minImageSize) {
        this.minImageSize = minImageSize;
    }

    public void setShape(CookieCutterShape shape) {
        this.shape = shape;
    }

    public CookieCutterShape getShape() {
        return shape;
    }

    void updateWithView(int width, int height) {
        this.width = width;
        this.height = height;

        circle = new Circle(width / 2, height / 2, circleRadius);

        holeParams = new HoleParams();
        circleParams = new CircleParams();
        squareParams = new SquareParams();
    }





    public HoleParams getHoleParams() {
        return holeParams;
    }

    public CircleParams getCircleParams() {
        return circleParams;
    }

    public SquareParams getSquareParams() {
        return squareParams;
    }

    public class HoleParams {
        Path path;
        Paint paint;

        public HoleParams() {
            setPath();

            paint = new Paint();
            paint.setColor(Color.parseColor("#AA000000"));
        }

        private void setPath() {
            path = new Path();
            path.setFillType(Path.FillType.EVEN_ODD);
            /**
             * @author Kevin Higgins 27/07/20
             * @return
             */
            /*
            path.addRect(0, 0, width, height, Path.Direction.CW);
            path.addCircle(circle.getCx(), circle.getCy(), circle.getRadius(), Path.Direction.CW);
            */


            path.addRect(new RectF((width - 504) / 2, (height - 707) / 2, 504 + ((width - 504) / 2), 707 + ((height - 707) / 2)), Path.Direction.CW);
        }
    }

    public class CircleParams {
        Paint paint;

        public CircleParams() {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);
        }

        public void setStrokeWidth(float strokeWidth) {
            paint.setStrokeWidth(strokeWidth);
        }

        public void setColor(int color) {
            paint.setColor(color);
        }
    }

    public class SquareParams {
        Paint paint;

        public SquareParams() {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);
        }

        public void setStrokeWidth(float strokeWidth) {
            paint.setStrokeWidth(strokeWidth);
        }

        public void setColor(int color) {
            paint.setColor(color);
        }
    }
}
