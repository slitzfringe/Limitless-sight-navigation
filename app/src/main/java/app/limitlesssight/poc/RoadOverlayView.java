package app.limitlesssight.poc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.View;
import java.util.Arrays;

final class RoadOverlayView extends View {
    private static final int W = 160, H = 90;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path road = new Path();
    private final Path chevron = new Path();
    private final boolean[] candidate = new boolean[W * H];
    private final boolean[] connected = new boolean[W * H];
    private final int[] queue = new int[W * H];
    private final int[] left = new int[H];
    private final int[] right = new int[H];
    private final int[] corridorLeft = new int[H];
    private final int[] corridorRight = new int[H];
    private float confidence;
    private boolean roadVisible;
    private int confidentFrames, uncertainFrames;
    private boolean modelDriven;

    RoadOverlayView(Context context) {
        super(context);
        setContentDescription("Experimental live road-region overlay with a conservative simulated ego-road corridor.");
    }

    float confidence() {
        return confidence;
    }

    boolean roadVisible() {
        return roadVisible;
    }

    void clear() {
        confidence = 0;
        roadVisible = false;
        confidentFrames = uncertainFrames = 0;
        invalidate();
    }

    void updateModel(byte[] mask, int maskWidth, int maskHeight) {
        modelDriven = true;
        Arrays.fill(candidate, false);
        Arrays.fill(connected, false);
        Arrays.fill(left, W);
        Arrays.fill(right, -1);
        for (int y = H / 3; y < H; y++) {
            int sourceY = y * maskHeight / H;
            for (int x = 0; x < W; x++) {
                int sourceX = x * maskWidth / W;
                candidate[y * W + x] = mask[sourceY * maskWidth + sourceX] != 0;
            }
        }
        connectAndMeasure();
    }

    void update(int[] pixels) {
        modelDriven = false;
        int seedR = 0, seedG = 0, seedB = 0, seedCount = 0;
        for (int y = H * 3 / 4; y < H - 2; y++) {
            for (int x = W * 2 / 5; x < W * 3 / 5; x++) {
                int color = pixels[y * W + x];
                int light = luma(color);
                if (light < 18 || light > 245) continue;
                seedR += Color.red(color);
                seedG += Color.green(color);
                seedB += Color.blue(color);
                seedCount++;
            }
        }
        if (seedCount < 12) {
            clear();
            return;
        }
        seedR /= seedCount;
        seedG /= seedCount;
        seedB /= seedCount;
        int seedLight = (77 * seedR + 150 * seedG + 29 * seedB) >> 8;

        Arrays.fill(candidate, false);
        Arrays.fill(connected, false);
        Arrays.fill(left, W);
        Arrays.fill(right, -1);
        for (int y = H / 3; y < H; y++) {
            int tolerance = 72 + (H - y) / 2;
            for (int x = 0; x < W; x++) {
                int color = pixels[y * W + x];
                int light = luma(color);
                if (light < 16 || light > 246) continue;
                int distance = Math.abs(Color.red(color) - seedR)
                        + Math.abs(Color.green(color) - seedG)
                        + Math.abs(Color.blue(color) - seedB);
                candidate[y * W + x] = distance <= tolerance && Math.abs(light - seedLight) <= 54;
            }
        }

        connectAndMeasure();
    }

    private void connectAndMeasure() {
        int head = 0, tail = 0;
        for (int y = H - 8; y < H; y++) {
            for (int x = W / 3; x < W * 2 / 3; x++) {
                int index = y * W + x;
                if (candidate[index] && !connected[index]) {
                    connected[index] = true;
                    queue[tail++] = index;
                }
            }
        }
        while (head < tail) {
            int index = queue[head++];
            int x = index % W, y = index / W;
            if (x > 0) tail = add(index - 1, tail);
            if (x + 1 < W) tail = add(index + 1, tail);
            if (y > H / 3) tail = add(index - W, tail);
            if (y + 1 < H) tail = add(index + W, tail);
        }

        int area = 0, rows = 0;
        for (int y = H / 3; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (!connected[y * W + x]) continue;
                left[y] = Math.min(left[y], x);
                right[y] = Math.max(right[y], x);
                area++;
            }
            if (right[y] - left[y] >= 5) rows++;
        }
        for (int y = H / 3; y < H; y++) {
            if (right[y] < left[y]) {
                corridorLeft[y] = W;
                corridorRight[y] = -1;
                continue;
            }
            float detectedCenter = (left[y] + right[y]) * .5f;
            float center = detectedCenter * .75f + (W - 1) * .5f * .25f;
            int halfWidth = Math.max(3, Math.round((right[y] - left[y] + 1) * .28f));
            corridorLeft[y] = Math.max(left[y], Math.round(center) - halfWidth);
            corridorRight[y] = Math.min(right[y], Math.round(center) + halfWidth);
        }
        float areaRatio = area / (float) (W * H * 2 / 3);
        float rowRatio = rows / (float) (H * 2 / 3);
        confidence = Math.min(1f, areaRatio * 2.4f) * Math.min(1f, rowRatio * 1.35f);
        boolean reachesBottom = right[H - 5] >= left[H - 5];
        if (confidence >= .28f && reachesBottom) {
            confidentFrames++;
            uncertainFrames = 0;
            if (confidentFrames >= 2) roadVisible = true;
        } else if (confidence < .16f || !reachesBottom) {
            uncertainFrames++;
            confidentFrames = 0;
            if (uncertainFrames >= 3) roadVisible = false;
        }
        invalidate();
    }

    private int add(int index, int tail) {
        if (!candidate[index] || connected[index]) return tail;
        connected[index] = true;
        queue[tail] = index;
        return tail + 1;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(0xFFFFCF78);
        canvas.drawText(modelDriven ? "LIVE FFNET ROAD ML · EGO CORRIDOR CANDIDATE"
                : "LIVE VISION POC · EGO CORRIDOR CANDIDATE", 18, 28, paint);
        if (!roadVisible) {
            paint.setColor(Color.WHITE);
            canvas.drawText("Road region uncertain — overlay hidden", 18, 56, paint);
            return;
        }

        float sx = getWidth() / (float) W, sy = getHeight() / (float) H;
        road.reset();
        boolean started = false;
        for (int y = H - 1; y >= H / 3; y--) {
            if (right[y] < left[y]) continue;
            float x = left[y] * sx, py = y * sy;
            if (!started) {
                road.moveTo(x, py);
                started = true;
            } else {
                road.lineTo(x, py);
            }
        }
        for (int y = H / 3; y < H; y++) {
            if (right[y] < left[y]) continue;
            road.lineTo((right[y] + 1) * sx, y * sy);
        }
        road.close();
        paint.setColor(0x356DF4EC);
        canvas.drawPath(road, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(0xB06DF4EC);
        canvas.drawPath(road, paint);
        paint.setStyle(Paint.Style.FILL);

        Path corridor = new Path();
        boolean corridorStarted = false;
        for (int y = H - 1; y >= H / 3; y--) {
            if (corridorRight[y] < corridorLeft[y]) continue;
            float x = corridorLeft[y] * sx, py = y * sy;
            if (!corridorStarted) {
                corridor.moveTo(x, py);
                corridorStarted = true;
            } else {
                corridor.lineTo(x, py);
            }
        }
        for (int y = H / 3; y < H; y++) {
            if (corridorRight[y] < corridorLeft[y]) continue;
            corridor.lineTo((corridorRight[y] + 1) * sx, y * sy);
        }
        corridor.close();
        paint.setColor(0x4058A6FF);
        canvas.drawPath(corridor, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(0xD058A6FF);
        canvas.drawPath(corridor, paint);
        paint.setStyle(Paint.Style.FILL);

        float phase = ((SystemClock.elapsedRealtime() % 1200) / 1200f);
        for (int i = 0; i < 5; i++) {
            int y = Math.min(H - 6, Math.max(H / 3, H - 13 - Math.round((i + phase) * 10)));
            while (y < H - 1 && corridorRight[y] < corridorLeft[y]) y++;
            if (corridorRight[y] < corridorLeft[y]) continue;
            float x = (corridorLeft[y] + corridorRight[y]) * .5f * sx;
            float radius = Math.max(7, (corridorRight[y] - corridorLeft[y]) * sx * .16f);
            drawChevron(canvas, x, y * sy, radius);
        }
        postInvalidateDelayed(33);
    }

    private void drawChevron(Canvas canvas, float x, float y, float radius) {
        chevron.reset();
        chevron.moveTo(x - radius, y + radius * .45f);
        chevron.lineTo(x, y - radius);
        chevron.lineTo(x + radius, y + radius * .45f);
        chevron.lineTo(x + radius, y + radius * .8f);
        chevron.lineTo(x, y - radius * .2f);
        chevron.lineTo(x - radius, y + radius * .8f);
        chevron.close();
        paint.setColor(0xD96DF4EC);
        canvas.drawPath(chevron, paint);
    }

    private static int luma(int color) {
        return (77 * Color.red(color) + 150 * Color.green(color) + 29 * Color.blue(color)) >> 8;
    }
}
