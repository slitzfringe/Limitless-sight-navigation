package app.limitlesssight.poc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.View;

final class ReplayView extends View {
    private static final String[] LABELS = {"Straight", "Service road left", "Ramp right"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path road = new Path(), marks = new Path(), arrow = new Path();
    private int index;
    private boolean lowConfidence, running;
    private long epoch;

    ReplayView(Context context) {
        super(context);
        setContentDescription("Synthetic road and direction cue. Not live navigation.");
    }

    String scenario() { return LABELS[index]; }
    boolean lowConfidence() { return lowConfidence; }
    void toggleConfidence() { lowConfidence = !lowConfidence; invalidate(); }
    void next() { index = (index + 1) % LABELS.length; invalidate(); }
    void setRunning(boolean running) {
        this.running = running;
        epoch = SystemClock.elapsedRealtime();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFF192A3C);
        float scale = Math.min(getWidth() / 640f, getHeight() / 320f);
        if (scale <= 0) return;
        canvas.save();
        canvas.translate((getWidth() - 640 * scale) / 2, (getHeight() - 320 * scale) / 2);
        canvas.scale(scale, scale);
        paint.setColor(0xFF2C493E);
        canvas.drawRect(0, 107, 640, 320, paint);
        drawRoad(canvas, 0, 0xFF414C57);
        if (index != 0) drawRoad(canvas, index == 1 ? -1 : 1, 0xFF414C57);
        drawPath(canvas, 0, 0xFF87939B, false);
        if (index != 0) drawPath(canvas, index == 1 ? -1 : 1, 0xFF87939B, false);
        if (!lowConfidence) drawPath(canvas, index == 1 ? -1 : index == 2 ? 1 : 0, 0xC06DF4EC, true);
        label(canvas, "SIMULATION ONLY", 12, 24, 16, 0xFFFFCF78);
        label(canvas, "24 km/h  SIM", 515, 24, 15, Color.WHITE);
        canvas.save();
        canvas.translate(320, 52);
        canvas.rotate(index == 1 ? -48 : index == 2 ? 48 : 0);
        chevron(canvas, 12, 0xFF8BF7EE);
        canvas.restore();
        paint.setTextAlign(Paint.Align.CENTER);
        label(canvas, scenario() + " · 120 m", 320, 79, 19, Color.WHITE);
        if (lowConfidence) label(canvas, "Road overlay hidden · simulated low confidence", 320, 101, 14, 0xFFFFCF78);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.restore();
        if (running && !lowConfidence) postInvalidateDelayed(33);
    }

    private static float center(float t, int branch) {
        return 320 + branch * 147 * t * t;
    }

    private void drawRoad(Canvas canvas, int branch, int color) {
        road.reset();
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i <= 24; i++) {
                float t = (side == -1 ? i : 24 - i) / 24f;
                float x = center(t, branch) + side * (144 - 124 * t);
                float y = 320 - 210 * t;
                if (side == -1 && i == 0) road.moveTo(x, y);
                else road.lineTo(x, y);
            }
        }
        road.close();
        paint.setColor(color);
        canvas.drawPath(road, paint);
    }

    private void drawPath(Canvas canvas, int branch, int color, boolean guidance) {
        float phase = ((SystemClock.elapsedRealtime() - epoch) % 1200) / 1200f;
        for (int i = 0; i < 7; i++) {
            float t = .25f + (i + (guidance ? 1 - phase : 0)) * .096f;
            float x = center(t, branch);
            float y = 320 - 210 * t;
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate((float) Math.toDegrees(Math.atan(branch * 294 * t / 210)));
            if (guidance) chevron(canvas, 16 - 12 * t, color);
            else {
                paint.setColor(color);
                marks.reset();
                marks.addRect(-1.5f, -5, 1.5f, 5, Path.Direction.CW);
                canvas.drawPath(marks, paint);
            }
            canvas.restore();
        }
    }

    private void chevron(Canvas canvas, float radius, int color) {
        arrow.reset();
        arrow.moveTo(-radius, radius * .55f);
        arrow.lineTo(0, -radius);
        arrow.lineTo(radius, radius * .55f);
        arrow.lineTo(radius, radius);
        arrow.lineTo(0, -radius * .28f);
        arrow.lineTo(-radius, radius);
        arrow.close();
        paint.setColor(color);
        canvas.drawPath(arrow, paint);
    }

    private void label(Canvas canvas, String text, float x, float y, int size, int color) {
        paint.setColor(color);
        paint.setTextSize(size);
        canvas.drawText(text, x, y, paint);
    }
}
