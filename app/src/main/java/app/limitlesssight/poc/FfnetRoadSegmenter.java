package app.limitlesssight.poc;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;

final class FfnetRoadSegmenter implements AutoCloseable {
    interface Listener {
        void result(byte[] roadMask, int width, int height, long latencyMs);
        void failed(String reason);
    }

    private static final int INPUT_W = 1024, INPUT_H = 512;
    private static final int OUTPUT_W = 256, OUTPUT_H = 128, CLASSES = 19;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final int sampleWidth, sampleHeight;
    private final int[] pixels;
    private final ByteBuffer input = ByteBuffer.allocateDirect(INPUT_W * INPUT_H * 3)
            .order(ByteOrder.nativeOrder());
    private final ByteBuffer output = ByteBuffer.allocateDirect(OUTPUT_W * OUTPUT_H * CLASSES)
            .order(ByteOrder.nativeOrder());
    private final byte[] roadMask = new byte[OUTPUT_W * OUTPUT_H];
    private final Interpreter interpreter;
    private final Listener listener;
    private final NnApiDelegate nnApiDelegate;
    private final String backend;

    FfnetRoadSegmenter(Context context, int sampleWidth, int sampleHeight, Listener listener) throws IOException {
        this.sampleWidth = sampleWidth;
        this.sampleHeight = sampleHeight;
        this.listener = listener;
        pixels = new int[sampleWidth * sampleHeight];
        MappedByteBuffer model = mapAsset(context, "models/ffnet_78s_lowres/ffnet_78s_lowres.tflite");
        Interpreter loaded;
        NnApiDelegate delegate = null;
        String selectedBackend;
        try {
            delegate = new NnApiDelegate();
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            options.addDelegate(delegate);
            loaded = new Interpreter(model, options);
            selectedBackend = "NNAPI";
        } catch (RuntimeException error) {
            if (delegate != null) delegate.close();
            delegate = null;
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            loaded = new Interpreter(model, options);
            selectedBackend = "CPU";
        }
        nnApiDelegate = delegate;
        interpreter = loaded;
        backend = selectedBackend;
        int[] inputShape = interpreter.getInputTensor(0).shape();
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        if (!shape(inputShape, 1, INPUT_H, INPUT_W, 3)
                || !shape(outputShape, 1, OUTPUT_H, OUTPUT_W, CLASSES)
                || interpreter.getInputTensor(0).dataType() != DataType.UINT8
                || interpreter.getOutputTensor(0).dataType() != DataType.UINT8) {
            interpreter.close();
            throw new IOException("Unexpected FFNet tensor shapes");
        }

    }

    String backend() {
        return backend;
    }

    boolean submit(Bitmap bitmap) {
        if (!busy.compareAndSet(false, true)) return false;
        bitmap.getPixels(pixels, 0, sampleWidth, 0, 0, sampleWidth, sampleHeight);
        worker.execute(this::run);
        return true;
    }

    private void run() {
        try {
            long started = System.nanoTime();
            input.rewind();
            for (int y = 0; y < INPUT_H; y++) {
                int sourceY = y * sampleHeight / INPUT_H;
                int row = sourceY * sampleWidth;
                for (int x = 0; x < INPUT_W; x++) {
                    int color = pixels[row + x * sampleWidth / INPUT_W];
                    input.put((byte) Color.red(color));
                    input.put((byte) Color.green(color));
                    input.put((byte) Color.blue(color));
                }
            }
            output.rewind();
            interpreter.run(input, output);
            output.rewind();
            for (int i = 0; i < roadMask.length; i++) {
                int bestClass = 0;
                int best = output.get() & 0xFF;
                for (int c = 1; c < CLASSES; c++) {
                    int value = output.get() & 0xFF;
                    if (value > best) {
                        best = value;
                        bestClass = c;
                    }
                }
                roadMask[i] = (byte) (bestClass == 0 ? 1 : 0);
            }
            long latencyMs = (System.nanoTime() - started) / 1_000_000;
            byte[] result = roadMask.clone();
            main.post(() -> listener.result(result, OUTPUT_W, OUTPUT_H, latencyMs));
        } catch (RuntimeException error) {
            main.post(() -> listener.failed(error.getClass().getSimpleName()));
        } finally {
            busy.set(false);
        }
    }

    private static MappedByteBuffer mapAsset(Context context, String path) throws IOException {
        try (AssetFileDescriptor descriptor = context.getAssets().openFd(path);
             FileInputStream stream = new FileInputStream(descriptor.getFileDescriptor());
             FileChannel channel = stream.getChannel()) {
            return channel.map(FileChannel.MapMode.READ_ONLY, descriptor.getStartOffset(), descriptor.getDeclaredLength());
        }
    }

    private static boolean shape(int[] actual, int... expected) {
        if (actual.length != expected.length) return false;
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != expected[i]) return false;
        }
        return true;
    }

    @Override public void close() {
        worker.shutdownNow();
        interpreter.close();
        if (nnApiDelegate != null) nnApiDelegate.close();
    }
}
