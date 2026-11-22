package app.limitlesssight.poc;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class Diagnostics {
    private static final String TAG = "LimitlessSight";
    private final File active;
    private final File previous;
    boolean available = true;

    Diagnostics(Context context) {
        active = new File(context.getFilesDir(), "diagnostics.log");
        previous = new File(context.getFilesDir(), "diagnostics.previous.log");
    }

    void event(String message) {
        Log.i(TAG, message);
        try {
            if (active.length() >= 128 * 1024) {
                if (previous.exists() && !previous.delete()) throw new IOException("Cannot remove old log");
                if (!active.renameTo(previous)) throw new IOException("Cannot rotate log");
            }
            try (FileOutputStream stream = new FileOutputStream(active, true)) {
                stream.write((System.currentTimeMillis() + " " + message + "\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
            available = true;
        } catch (IOException error) {
            available = false;
            Log.e(TAG, "Local diagnostic write failed", error);
        }
    }
}
