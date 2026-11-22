package app.limitlesssight.poc;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CameraPreview {
    interface Listener {
        void ready();
        void failed(String reason);
    }

    static final class Lens {
        final String cameraId, physicalId;
        final int sensorAngle;
        final Size size;
        final double fov;
        final float zoomRatio;

        Lens(String parent, String physical, CameraCharacteristics info, StreamConfigurationMap fallback,
                float zoom) {
            cameraId = parent;
            physicalId = physical;
            zoomRatio = zoom;
            Integer angle = info.get(CameraCharacteristics.SENSOR_ORIENTATION);
            sensorAngle = angle == null ? 0 : angle;
            StreamConfigurationMap map = info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = (map == null ? fallback : map) == null ? null
                    : (map == null ? fallback : map).getOutputSizes(SurfaceTexture.class);
            size = sizes == null ? null : Arrays.stream(sizes)
                    .min(Comparator
                            .comparingDouble((Size s) -> Math.abs((double) s.getWidth() / s.getHeight() - 4d / 3d))
                            .thenComparingLong(s -> Math.abs((long) s.getWidth() * s.getHeight() - 1228800)))
                    .orElse(null);
            float[] focal = info.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            android.util.SizeF sensor = info.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            fov = focal == null || focal.length == 0 || focal[0] <= 0 || sensor == null
                    ? Double.NaN : Math.toDegrees(2 * Math.atan(sensor.getWidth() / (2 * focal[0] * zoomRatio)));
        }

        String key() {
            if (physicalId != null) return cameraId + "/" + physicalId;
            return zoomRatio == 1f ? cameraId : cameraId + "@" + zoomRatio + "x";
        }
    }

    static List<Lens> enumerate(CameraManager manager, Diagnostics diagnostic) throws CameraAccessException {
        List<Lens> result = new ArrayList<>();
        String[] publicIds = manager.getCameraIdList();
        Set<String> seen = new HashSet<>(Arrays.asList(publicIds));
        for (String id : publicIds) {
            CameraCharacteristics info = manager.getCameraCharacteristics(id);
            if (!Integer.valueOf(CameraCharacteristics.LENS_FACING_BACK)
                    .equals(info.get(CameraCharacteristics.LENS_FACING))) continue;
            Range<Float> zoomRange = Build.VERSION.SDK_INT >= 30
                    ? info.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE) : null;
            if (zoomRange != null && zoomRange.getLower() < 0.99f) {
                add(result, new Lens(id, null, info, null, zoomRange.getLower()), diagnostic);
            }
            if (!"24053PY09I".equalsIgnoreCase(Build.MODEL)
                    || zoomRange == null || zoomRange.getLower() >= 0.99f) {
                add(result, new Lens(id, null, info, null, 1f), diagnostic);
            }
            List<String> children = new ArrayList<>(info.getPhysicalCameraIds());
            Collections.sort(children);
            for (String physical : children) {
                if (!seen.add(physical)) continue;
                try {
                    CameraCharacteristics child = manager.getCameraCharacteristics(physical);
                    add(result, new Lens(id, physical, child,
                            info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP), 1f), diagnostic);
                } catch (CameraAccessException | IllegalArgumentException error) {
                    diagnostic.event("lens_metadata_unavailable id=" + id + "/" + physical
                            + " error=" + error.getClass().getSimpleName());
                }
            }
        }
        return result;
    }

    private static void add(List<Lens> result, Lens lens, Diagnostics diagnostic) {
        diagnostic.event("lens id=" + lens.key() + " preview=" + lens.size + " fov=" + lens.fov
                + " zoom=" + lens.zoomRatio);
        if (lens.size != null) result.add(lens);
    }

    private final CameraManager manager;
    private final TextureView texture;
    private final Handler main = new Handler(Looper.getMainLooper());
    private CameraDevice device;
    private CameraCaptureSession session;
    private Surface surface;
    private int generation;
    private int rotationOffset;

    CameraPreview(CameraManager manager, TextureView texture, int defaultRotationOffset) {
        this.manager = manager;
        this.texture = texture;
        rotationOffset = defaultRotationOffset;
    }

    @SuppressLint("MissingPermission")
    void start(Lens lens, Listener listener) {
        stop();
        final int token = generation;
        SurfaceTexture buffer = texture.getSurfaceTexture();
        if (buffer == null) {
            listener.failed("Preview surface unavailable");
            return;
        }
        buffer.setDefaultBufferSize(lens.size.getWidth(), lens.size.getHeight());
        transform(lens);
        try {
            manager.openCamera(lens.cameraId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice camera) {
                    if (token != generation) {
                        camera.close();
                        return;
                    }
                    device = camera;
                    surface = new Surface(buffer);
                    try {
                        OutputConfiguration output = new OutputConfiguration(surface);
                        if (lens.physicalId != null) output.setPhysicalCameraId(lens.physicalId);
                        camera.createCaptureSession(new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                                Collections.singletonList(output), main::post, new CameraCaptureSession.StateCallback() {
                            @Override public void onConfigured(CameraCaptureSession configured) {
                                if (token != generation) {
                                    configured.close();
                                    return;
                                }
                                session = configured;
                                try {
                                    CaptureRequest.Builder request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    request.addTarget(surface);
                                    if (Build.VERSION.SDK_INT >= 30 && lens.physicalId == null) {
                                        request.set(CaptureRequest.CONTROL_ZOOM_RATIO, lens.zoomRatio);
                                    }
                                    configured.setRepeatingRequest(request.build(), null, main);
                                    listener.ready();
                                } catch (CameraAccessException | IllegalArgumentException | IllegalStateException error) {
                                    fail(token, listener, "Preview request failed: " + error.getClass().getSimpleName());
                                }
                            }
                            @Override public void onConfigureFailed(CameraCaptureSession failed) {
                                failed.close();
                                fail(token, listener, "Device rejected this preview stream");
                            }
                        }));
                    } catch (CameraAccessException | IllegalArgumentException | IllegalStateException error) {
                        fail(token, listener, "Camera session failed: " + error.getClass().getSimpleName());
                    }
                }
                @Override public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    fail(token, listener, "Camera disconnected");
                }
                @Override public void onError(CameraDevice camera, int error) {
                    camera.close();
                    fail(token, listener, "Camera error " + error);
                }
            }, main);
        } catch (CameraAccessException | SecurityException | IllegalArgumentException error) {
            fail(token, listener, "Cannot open camera: " + error.getClass().getSimpleName());
        }
    }

    private void fail(int token, Listener listener, String reason) {
        if (token != generation) return;
        stop();
        listener.failed(reason);
    }

    void transform(Lens lens) {
        int width = texture.getWidth(), height = texture.getHeight();
        if (width <= 0 || height <= 0 || texture.getDisplay() == null) return;
        int rotation = texture.getDisplay().getRotation();
        int orientation = texture.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation % 2 == 0) {
            rotation = Surface.ROTATION_90;
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT && rotation % 2 != 0) {
            rotation = Surface.ROTATION_0;
        }
        int degrees = (lens.sensorAngle - 90 * rotation + rotationOffset + 360) % 360;
        int bufferWidth = lens.size.getWidth(), bufferHeight = lens.size.getHeight();
        boolean quarterTurn = degrees == 90 || degrees == 270;
        float scale = Math.min((float) width / (quarterTurn ? bufferHeight : bufferWidth),
                (float) height / (quarterTurn ? bufferWidth : bufferHeight));
        Matrix transform = new Matrix();
        transform.setScale(scale * bufferWidth / width, scale * bufferHeight / height);
        transform.postTranslate(-scale * bufferWidth / 2f, -scale * bufferHeight / 2f);
        transform.postRotate(degrees);
        transform.postTranslate(width / 2f, height / 2f);
        texture.setTransform(transform);
    }

    int rotateClockwise(Lens lens) {
        rotationOffset = (rotationOffset + 90) % 360;
        transform(lens);
        return rotationOffset;
    }

    void stop() {
        generation++;
        if (session != null) session.close();
        if (device != null) device.close();
        if (surface != null) surface.release();
        session = null;
        device = null;
        surface = null;
    }
}
