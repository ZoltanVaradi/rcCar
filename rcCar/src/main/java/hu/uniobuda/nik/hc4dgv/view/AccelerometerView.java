package hu.uniobuda.nik.hc4dgv.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Zoltan Varadi on 2013.12.07..
 */
public class AccelerometerView extends View {

    private static final int MAX_VALUES = 50;
    float[] xy;
    private Path line;
    private Paint paint;
    private Paint paint2;

    public AccelerometerView(Context context) {
        super(context);
        init();
    }

    public AccelerometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccelerometerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {

        xy = new float[2];
        xy[0] = 0;
        xy[1] = 0;


        this.line = new Path();
        this.paint = new Paint();
        this.paint.setColor(Color.argb(255, 0, 153, 204));
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setAntiAlias(true);

        this.paint2 = new Paint();
        this.paint2.setColor(Color.RED);
        this.paint2.setStyle(Paint.Style.FILL);
        this.paint2.setStrokeWidth(2);
        this.paint2.setAntiAlias(true);

    }

    public void addValue(float[] f) {
        this.xy = f;

        //invalidate();
        postInvalidate();
    }


    //Canvas-ra rajzolas
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int sensor_max_value = 25;

        int width = getWidth();
        int height = getHeight();
        int middle_w = width >> 1;
        int middle_h = height >> 1;
        int radius = middle_h - 1;

        int step_w = middle_w / sensor_max_value;
        int step_h = middle_h / sensor_max_value;

        line.reset();
        line.addCircle(middle_w, middle_h, radius, Path.Direction.CW);
        paint.setStrokeWidth(3);
        canvas.drawPath(line, paint); // egy vastag karika


        while (radius > 0) {
            radius -= 20;
            line.addCircle(middle_w, middle_h, radius, Path.Direction.CW);
        }

        line.moveTo(middle_w, 0);
        line.lineTo(middle_w, height);
        line.moveTo(0, middle_h);
        line.lineTo(width, middle_h);
        paint.setStrokeWidth(1);
        canvas.drawPath(line, paint); //egyre kissebb karikak es egy kereszt

        line.reset();// a piros potty rajozlasa
        line.addCircle(middle_w + (xy[0] * step_w), middle_h + (xy[1] * step_h), 7, Path.Direction.CCW);
        canvas.drawPath(line, paint2);
    }
}