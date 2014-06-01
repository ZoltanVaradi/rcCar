package hu.zoltan.varadi.rccar.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zoltan Varadi on 2013.12.07..
 */
public class BatteryView extends View {
    private static final int MAX_VALUES = 50;
    private List<Integer> valuesAndroid;
    private List<Integer> valuesRcCar;
    private Path line;
    private Paint paintAndroid;
    private Paint paintRcCar;

    public BatteryView(Context context) {
        super(context);
        init();
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.valuesAndroid = new ArrayList<Integer>();
        this.valuesRcCar = new ArrayList<Integer>();

        this.line = new Path();

        this.paintAndroid = new Paint();
        this.paintAndroid.setColor(Color.argb(255, 0, 153, 204));
        this.paintAndroid.setStyle(Paint.Style.STROKE);
        this.paintAndroid.setStrokeWidth(2);
        this.paintAndroid.setAntiAlias(true);
        this.paintAndroid.setTextAlign(Paint.Align.RIGHT);

        this.paintRcCar = new Paint();
        this.paintRcCar.setColor(Color.argb(255, 102, 153, 0));
        this.paintRcCar.setStyle(Paint.Style.STROKE);
        this.paintRcCar.setStrokeWidth(2);
        this.paintRcCar.setAntiAlias(true);
        this.paintRcCar.setTextAlign(Paint.Align.RIGHT);

        for (int i = 0; i < MAX_VALUES; i++) {
            valuesAndroid.add(0);
            valuesRcCar.add(2);
        }

    }

    public void addValueAndroid(int value) {
        this.valuesAndroid.remove(0);
        this.valuesAndroid.add(value);

        postInvalidate();
    }

    public void addValueRcCar(int value) {
        this.valuesRcCar.remove(0);
        this.valuesRcCar.add(value);

        postInvalidate();
    }

    //egy egyszeru grafikon es egy beleiras
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int width_step = width / valuesAndroid.size();
        int height_step = height / 100; //100%

        line.reset();
        line.moveTo(0, height - (valuesAndroid.get(0) * height_step));
        for (int i = 1; i < valuesAndroid.size(); i++) {
            line.lineTo((i * width_step), height - (valuesAndroid.get(i) * height_step));
        }
        canvas.drawPath(line, paintAndroid);

        line.reset();
        line.moveTo(0, height - (valuesRcCar.get(0) * height_step));
        for (int i = 1; i < valuesRcCar.size(); i++) {
            line.lineTo((i * width_step), height - (valuesRcCar.get(i) * height_step));
        }
        canvas.drawPath(line, paintRcCar);

        paintAndroid.setStrokeWidth(1);
        paintRcCar.setStrokeWidth(1);
        canvas.drawText("Android", width - 3, 10, paintAndroid);
        canvas.drawText("rcCar", width - 3, 25, paintRcCar);

        paintAndroid.setStrokeWidth(2);
        paintRcCar.setStrokeWidth(2);
    }
}
