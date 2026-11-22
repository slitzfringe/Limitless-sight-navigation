package app.limitlesssight.poc;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity implements SensorEventListener, TextureView.SurfaceTextureListener {
    private static final int CAMERA_REQUEST = 7;
    private static final long SAMPLE_MS = 4000;
    private static final int SAMPLE_W = 160, SAMPLE_H = 90;
    private static final int MODEL_SAMPLE_W = 512, MODEL_SAMPLE_H = 256;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Result> results = new LinkedHashMap<>();
    private final int[] pixels = new int[SAMPLE_W * SAMPLE_H];
    private final List<CameraPreview.Lens> lenses = new ArrayList<>();
    private Diagnostics diagnostics;
    private SensorManager sensors;
    private CameraManager manager;
    private CameraPreview camera;
    private TextureView texture;
    private ReplayView replay;
    private RoadOverlayView roadOverlay;
    private FfnetRoadSegmenter segmenter;
    private Spinner selector;
    private TextView status, report;
    private Button measure, rotate, live, mode, scenario, confidence;
    private Bitmap sample, modelSample;
    private boolean resumed, previewReady, demo, discoveryDone, liveVision, lastRoadVisible;
    private int selected, frames;
    private float roll = Float.NaN, pitch = Float.NaN;
    private long started, lastSample, lastVision, lastModelLog, firstFrame, lastFrame;
    private int snapshots, poseSamples;
    private double meanSharpness, meanBrightness, maxRoll, pitchSum;
    private final Runnable finish = this::finishCheck;
    private final Runnable previewTimeout = () -> {
        camera.stop();
        previewFailure("No preview frames within 6 seconds");
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        diagnostics = new Diagnostics(this);
        diagnostics.event("launch model=" + Build.MODEL + " sdk=" + Build.VERSION.SDK_INT);
        sensors = getSystemService(SensorManager.class);
        manager = getSystemService(CameraManager.class);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Color.rgb(17, 24, 34));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets edges = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(edges.left, edges.top, edges.right, edges.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        ScrollView controls = new ScrollView(this);
        root.addView(controls, new LinearLayout.LayoutParams(dp(260), -1));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(12), dp(6), dp(12), dp(6));
        controls.addView(column);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.limitless_sight_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setContentDescription("Limitless Sight");
        column.addView(logo, new LinearLayout.LayoutParams(-1, dp(74)));
        text(column, "STATIONARY BENCH TEST ONLY", 12).setTextColor(0xFFFFCF78);
        selector = new Spinner(this);
        column.addView(selector, new LinearLayout.LayoutParams(-1, dp(44)));
        selector.setContentDescription("Rear camera");
        measure = button(column, "Run 4-second lens check", this::checkOrRequest);
        rotate = button(column, "Rotate preview 90°", () -> {
            if (lenses.isEmpty()) return;
            int offset = camera.rotateClockwise(lenses.get(selected));
            status.setText("Preview rotation adjusted by " + offset + "°. Use the setting that keeps the horizon level.");
            diagnostics.event("preview_rotation lens=" + lenses.get(selected).key() + " offset_deg=" + offset);
        });
        live = button(column, "Start live road detection", this::toggleLiveVision);
        status = text(column, "", 14);
        report = text(column, "", 13);
        mode = button(column, "Open simulated route", this::toggleMode);
        scenario = button(column, "Next manoeuvre", () -> {
            replay.next();
            logReplay();
        });
        confidence = button(column, "Simulate low confidence", () -> {
            replay.toggleConfidence();
            confidence.setText(replay.lowConfidence() ? "Restore confidence" : "Simulate low confidence");
            logReplay();
        });
        scenario.setVisibility(View.GONE);
        confidence.setVisibility(View.GONE);

        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(Color.BLACK);
        root.addView(screen, new LinearLayout.LayoutParams(0, -1, 1));
        texture = new TextureView(this);
        texture.setSurfaceTextureListener(this);
        screen.addView(texture, new FrameLayout.LayoutParams(-1, -1));
        roadOverlay = new RoadOverlayView(this);
        roadOverlay.setVisibility(View.GONE);
        screen.addView(roadOverlay, new FrameLayout.LayoutParams(-1, -1));
        replay = new ReplayView(this);
        replay.setVisibility(View.GONE);
        screen.addView(replay, new FrameLayout.LayoutParams(-1, -1));
        int defaultRotation = "24053PY09I".equalsIgnoreCase(Build.MODEL) ? 270 : 0;
        camera = new CameraPreview(manager, texture, defaultRotation);
        diagnostics.event("preview_rotation_default offset_deg=" + defaultRotation);
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        roll = pitch = Float.NaN;
        Sensor gravity = sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravity != null) sensors.registerListener(this, gravity, SensorManager.SENSOR_DELAY_UI);
        if (demo) replay.setRunning(true);
        else openIfPossible();
        updateControls();
        diagnostics.event("foreground");
    }

    @Override protected void onPause() {
        resumed = false;
        sensors.unregisterListener(this);
        closePreview("background");
        replay.setRunning(false);
        diagnostics.event("background");
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (sample != null) sample.recycle();
        if (modelSample != null) modelSample.recycle();
        if (segmenter != null) segmenter.close();
        super.onDestroy();
    }

    private boolean permitted() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void openIfPossible() {
        if (!resumed || demo) return;
        if (!permitted()) {
            status.setText("Allow camera access to test lenses. The simulation does not need it.");
            return;
        }
        if (!discoveryDone) {
            try {
                lenses.addAll(CameraPreview.enumerate(manager, diagnostics));
            } catch (CameraAccessException error) {
                previewFailure("Camera discovery failed: " + error.getReason());
                return;
            }
            discoveryDone = true;
            ArrayAdapter<String> options = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
            options.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            for (int i = 0; i < lenses.size(); i++) {
                CameraPreview.Lens lens = lenses.get(i);
                String type = lens.zoomRatio < 0.99f ? String.format(Locale.US, "%.1f× ultra-wide", lens.zoomRatio)
                        : Double.isFinite(lens.fov) && lens.fov >= 90 ? "ultra-wide"
                        : Double.isFinite(lens.fov) && lens.fov >= 60 ? "main"
                        : "telephoto";
                options.add("Camera " + (i + 1) + " • " + type
                        + (lens.physicalId == null ? " · logical" : " · physical")
                        + (Double.isFinite(lens.fov) ? String.format(Locale.US, " · ~%.0f°", lens.fov) : ""));
            }
            double widestPhysicalFov = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < lenses.size(); i++) {
                CameraPreview.Lens lens = lenses.get(i);
                if (lens.physicalId != null && Double.isFinite(lens.fov) && lens.fov > widestPhysicalFov) {
                    widestPhysicalFov = lens.fov;
                    selected = i;
                }
            }
            selector.setAdapter(options);
            selector.setSelection(selected, false);
            selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == selected) return;
                    selected = position;
                    showResults();
                    openIfPossible();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        if (lenses.isEmpty()) {
            previewFailure("Android reported no compatible rear-camera preview streams");
            return;
        }
        if (!texture.isAvailable()) return;
        closePreview("opening");
        CameraPreview.Lens lens = lenses.get(selected);
        status.setText("Opening Camera " + (selected + 1) + "…");
        diagnostics.event("preview_open lens=" + lens.key());
        main.postDelayed(previewTimeout, 6000);
        camera.start(lens, new CameraPreview.Listener() {
            @Override public void ready() {
                diagnostics.event("preview_configured lens=" + lens.key());
                awaitingFrame = true;
            }
            @Override public void failed(String reason) { previewFailure(reason); }
        });
        updateControls();
        showResults();
    }

    private boolean awaitingFrame;

    private void previewFailure(String reason) {
        awaitingFrame = previewReady = false;
        main.removeCallbacks(previewTimeout);
        cancelCheck(reason);
        status.setText(reason + ". Retry or select another camera.");
        diagnostics.event("preview_error reason=" + reason);
        updateControls();
    }

    private void closePreview(String reason) {
        awaitingFrame = previewReady = false;
        main.removeCallbacks(previewTimeout);
        cancelCheck(reason);
        camera.stop();
    }

    private void toggleLiveVision() {
        if (!liveVision && segmenter == null) {
            try {
                segmenter = new FfnetRoadSegmenter(this, MODEL_SAMPLE_W, MODEL_SAMPLE_H,
                        new FfnetRoadSegmenter.Listener() {
                            @Override public void result(byte[] roadMask, int width, int height, long latencyMs) {
                                if (!liveVision) return;
                                roadOverlay.updateModel(roadMask, width, height);
                                long now = SystemClock.elapsedRealtime();
                                if (roadOverlay.roadVisible() != lastRoadVisible || now - lastModelLog >= 10000) {
                                    lastRoadVisible = roadOverlay.roadVisible();
                                    lastModelLog = now;
                                    diagnostics.event(String.format(Locale.US,
                                            "ffnet_result latency_ms=%d road_visible=%s confidence=%.2f",
                                            latencyMs, lastRoadVisible, roadOverlay.confidence()));
                                }
                            }
                            @Override public void failed(String reason) {
                                diagnostics.event("ffnet_error reason=" + reason);
                                liveVision = false;
                                roadOverlay.clear();
                                roadOverlay.setVisibility(View.GONE);
                                status.setText("FFNet inference failed: " + reason);
                                report.setText("Live road overlay disabled. No heuristic fallback was substituted.");
                                updateControls();
                            }
                        });
                diagnostics.event("ffnet_loaded model=FFNet-78S-LowRes precision=W8A8 runtime=LiteRT-1.4.2 backend="
                        + segmenter.backend());
            } catch (Exception error) {
                status.setText("Cannot load FFNet model: " + error.getClass().getSimpleName());
                report.setText("Live road overlay remains disabled.");
                diagnostics.event("ffnet_load_error reason=" + error.getClass().getSimpleName());
                return;
            }
        }
        liveVision = !liveVision;
        lastVision = 0;
        roadOverlay.clear();
        roadOverlay.setVisibility(liveVision ? View.VISIBLE : View.GONE);
        live.setText(liveVision ? "Stop live road detection" : "Start live road detection");
        status.setText(liveVision
                ? "Live FFNet road segmentation. Straight chevrons are simulated, not route navigation."
                : "Live road detection stopped.");
        report.setText(liveVision
                ? "FFNet-78S LowRes Cityscapes model. POC evaluation only; it must not be used while driving."
                : "");
        diagnostics.event("live_vision enabled=" + liveVision + (lenses.isEmpty() ? "" : " lens=" + lenses.get(selected).key()));
        updateControls();
    }

    private void checkOrRequest() {
        if (!permitted()) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else if (!previewReady) {
            if (lenses.isEmpty()) discoveryDone = false;
            openIfPossible();
        } else {
            if (sample == null) sample = Bitmap.createBitmap(SAMPLE_W, SAMPLE_H, Bitmap.Config.ARGB_8888);
            started = SystemClock.elapsedRealtime();
            frames = snapshots = poseSamples = 0;
            lastSample = firstFrame = lastFrame = 0;
            meanSharpness = meanBrightness = maxRoll = pitchSum = 0;
            status.setText("Measuring… keep the phone still and point at a detailed scene.");
            diagnostics.event("lens_check_start lens=" + lenses.get(selected).key());
            main.postDelayed(finish, SAMPLE_MS);
            updateControls();
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code != CAMERA_REQUEST) return;
        diagnostics.event("camera_permission granted=" + permitted());
        openIfPossible();
        updateControls();
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        openIfPossible();
    }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (!lenses.isEmpty()) camera.transform(lenses.get(selected));
    }
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        closePreview("surface_destroyed");
        updateControls();
        return true;
    }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (awaitingFrame) {
            awaitingFrame = false;
            main.removeCallbacks(previewTimeout);
            previewReady = true;
            status.setText("Live preview. Frame the same scene for each lens check.");
            diagnostics.event("preview_first_frame lens=" + lenses.get(selected).key());
            updateControls();
        }
        long now = SystemClock.elapsedRealtime();
        if (liveVision && segmenter != null && now - lastVision >= 1000) {
            if (modelSample == null) {
                modelSample = Bitmap.createBitmap(MODEL_SAMPLE_W, MODEL_SAMPLE_H, Bitmap.Config.ARGB_8888);
            }
            if (texture.getBitmap(modelSample) != null && segmenter.submit(modelSample)) {
                lastVision = now;
            }
        }
        if (started == 0) return;
        if (frames++ == 0) firstFrame = now;
        lastFrame = now;
        if (now - lastSample < 250) return;
        lastSample = now;
        if (texture.getBitmap(sample) == null) return;
        sample.getPixels(pixels, 0, SAMPLE_W, 0, 0, SAMPLE_W, SAMPLE_H);
        double brightness = 0, edge = 0;
        int count = 0;
        // Use the center patch so preview letterboxing does not inflate edge measurements.
        for (int y = SAMPLE_H / 4; y < SAMPLE_H * 3 / 4; y++) {
            for (int x = SAMPLE_W / 4; x < SAMPLE_W * 3 / 4; x++) {
                int p = y * SAMPLE_W + x;
                int light = luma(pixels[p]);
                brightness += light;
                edge += Math.abs(light - luma(pixels[p + 1])) + Math.abs(light - luma(pixels[p + SAMPLE_W]));
                count++;
            }
        }
        meanBrightness += brightness / count;
        meanSharpness += edge / (2 * count);
        snapshots++;
    }

    private static int luma(int p) {
        return (77 * Color.red(p) + 150 * Color.green(p) + 29 * Color.blue(p)) >> 8;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float x = event.values[0], y = event.values[1], z = event.values[2];
        roll = (float) Math.toDegrees(Math.atan2(Math.abs(y), Math.abs(x)));
        pitch = (float) Math.toDegrees(Math.atan2(z, Math.hypot(x, y)));
        if (started != 0 && Math.hypot(x, y) >= 3) {
            poseSamples++;
            maxRoll = Math.max(maxRoll, roll);
            pitchSum += pitch;
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void finishCheck() {
        long elapsed = SystemClock.elapsedRealtime() - started;
        started = 0;
        CameraPreview.Lens lens = lenses.get(selected);
        boolean valid = snapshots >= 4 && poseSamples >= 5 && frames >= 2 &&
                firstFrame <= lastFrame && lastFrame - firstFrame >= SAMPLE_MS - 500 &&
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (!valid) {
            results.remove(lens.key());
            status.setText("Incomplete measurement. Keep the rear preview running and hold the phone upright in landscape.");
            diagnostics.event("lens_check_incomplete lens=" + lens.key() + " frames=" + frames
                    + " samples=" + snapshots + " pose_samples=" + poseSamples + " elapsed_ms=" + elapsed);
        } else {
            Result result = new Result((frames - 1) * 1000d / (lastFrame - firstFrame),
                    meanSharpness / snapshots, meanBrightness / snapshots, maxRoll, pitchSum / poseSamples);
            results.put(lens.key(), result);
            status.setText(result.eligible() ? "Lens check recorded. Test another camera to compare."
                    : "Lens check recorded with warnings. Level the phone, improve the light or try another lens.");
            diagnostics.event("lens_check_complete lens=" + lens.key() + " " + result.metrics()
                    + " frames=" + frames + " samples=" + snapshots + " pose_samples=" + poseSamples
                    + " elapsed_ms=" + elapsed + " eligible=" + result.eligible());
        }
        showResults();
        updateControls();
    }

    private void cancelCheck(String reason) {
        main.removeCallbacks(finish);
        if (started != 0) {
            started = 0;
            diagnostics.event("lens_check_cancelled reason=" + reason);
        }
    }

    private void showResults() {
        if (demo || lenses.isEmpty()) return;
        StringBuilder text = new StringBuilder();
        Result best = null;
        int bestIndex = -1;
        for (int i = 0; i < lenses.size(); i++) {
            Result result = results.get(lenses.get(i).key());
            if (result == null) continue;
            text.append(String.format(Locale.US, "Camera %d: %.1f FPS · detail %.1f%n"
                    + "Light %.0f/255 · roll %.1f° · tilt %+.1f°%n",
                    i + 1, result.fps, result.sharpness, result.brightness, result.roll, result.pitch));
            if (result.eligible() && (best == null || result.sharpness > best.sharpness)) {
                best = result;
                bestIndex = i;
            }
        }
        if (bestIndex >= 0) text.append(results.size() > 1 ? "Best tested detail: Camera " : "Eligible so far: Camera ")
                .append(bestIndex + 1).append("\n");
        text.append("Bench metrics only; no road or mount calibration.");
        if (!diagnostics.available) text.append("\nLocal log unavailable; USB logcat only.");
        report.setText(text);
    }

    private void toggleMode() {
        demo = !demo;
        if (demo && liveVision) toggleLiveVision();
        if (demo) closePreview("simulation");
        texture.setVisibility(demo ? View.INVISIBLE : View.VISIBLE);
        replay.setVisibility(demo ? View.VISIBLE : View.GONE);
        replay.setRunning(demo && resumed);
        mode.setText(demo ? "Back to camera" : "Open simulated route");
        scenario.setVisibility(demo ? View.VISIBLE : View.GONE);
        confidence.setVisibility(demo ? View.VISIBLE : View.GONE);
        if (demo) {
            status.setText("Synthetic road, manoeuvre and speed. No camera, GPS or maps used.");
            report.setText("Try each manoeuvre, then inject low confidence to hide the road chevrons.");
            logReplay();
        } else {
            diagnostics.event("simulation_exit");
            report.setText("");
            openIfPossible();
        }
        updateControls();
    }

    private void logReplay() {
        diagnostics.event("simulation scenario=" + replay.scenario() + " road_overlay=" + !replay.lowConfidence());
    }

    private void updateControls() {
        selector.setVisibility(demo || lenses.isEmpty() ? View.GONE : View.VISIBLE);
        selector.setEnabled(started == 0);
        measure.setVisibility(demo ? View.GONE : View.VISIBLE);
        measure.setEnabled(resumed && started == 0);
        measure.setText(!permitted() ? "Enable camera" : previewReady ? "Run 4-second lens check" : "Retry preview");
        rotate.setVisibility(demo ? View.GONE : View.VISIBLE);
        rotate.setEnabled(previewReady && started == 0);
        live.setVisibility(demo ? View.GONE : View.VISIBLE);
        live.setEnabled(previewReady && started == 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(LinearLayout parent, String value, int size) {
        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setTextSize(size);
        text.setText(value);
        text.setPadding(0, dp(4), 0, dp(4));
        parent.addView(text);
        return text;
    }

    private Button button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private static final class Result {
        final double fps, sharpness, brightness, roll, pitch;
        Result(double fps, double sharpness, double brightness, double roll, double pitch) {
            this.fps = fps;
            this.sharpness = sharpness;
            this.brightness = brightness;
            this.roll = roll;
            this.pitch = pitch;
        }
        boolean eligible() {
            return fps >= 18 && brightness >= 28 && brightness <= 235 && roll <= 5 && Math.abs(pitch) <= 35;
        }
        String metrics() {
            return String.format(Locale.US, "fps=%.1f detail=%.2f brightness=%.1f roll_deg=%.1f tilt_deg=%.1f",
                    fps, sharpness, brightness, roll, pitch);
        }
    }
}
