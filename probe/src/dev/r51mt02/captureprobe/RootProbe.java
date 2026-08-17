package dev.r51mt02.captureprobe;

import android.content.Context;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import java.io.File;
import java.lang.reflect.Method;

/** Entry point for a one-shot, post-root app_process test. */
public final class RootProbe {
    private RootProbe() {
    }

    public static void main(String[] args) {
        try {
            Context context = createSystemContext();
            System.out.println("TCL_ROOT_PROBE_START uid=" + Process.myUid()
                    + " context=" + context.getClass().getName());
            if (args.length > 0 && "aipq".equals(args[0])) {
                probeWithTemporaryAipq(context);
            } else {
                ProbeActivity.executeProbe(context);
                probeHdmiCapture();
            }
            System.out.println("TCL_ROOT_PROBE_FINISHED");
            System.exit(0);
        } catch (Throwable error) {
            System.err.println("TCL_ROOT_PROBE_FAILED " + error);
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void probeWithTemporaryAipq(Context context) throws Throwable {
        Class<?> managerClass = Class.forName("com.tcl.tvmanager.TAppManager");
        managerClass.getMethod("getInstance", Context.class).invoke(null, context);
        Class<?> videoApiClass = Class.forName("com.tcl.tosapi.atv.TvVideoApi");
        Object videoApi = videoApiClass.getMethod("getInstance").invoke(null);
        Method getStatus = videoApiClass.getMethod("getVideoAIPQStatus");
        Method getWorkmode = videoApiClass.getMethod("getVideoAIpqWorkmode");
        Method setWorkmode = videoApiClass.getMethod(
                "setVideoAIpqWorkmode", int.class, int.class);
        int originalWorkmode = ((Number) getWorkmode.invoke(videoApi)).intValue();

        System.out.println("TCL_AIPQ_TEMP_BEFORE status="
                + getStatus.invoke(videoApi) + " workmode=" + originalWorkmode);
        try {
            // ON=1 and EXEC=0 request a runtime-only change; nothing is saved.
            Object result = setWorkmode.invoke(videoApi, 1, 0);
            System.out.println("TCL_AIPQ_TEMP_SET result=" + result);
            SystemClock.sleep(2500);
            System.out.println("TCL_AIPQ_TEMP_ACTIVE status="
                    + getStatus.invoke(videoApi) + " workmode="
                    + getWorkmode.invoke(videoApi));
            ProbeActivity.executeProbe(context);
            probeHdmiCapture();
        } finally {
            Object restoreResult = setWorkmode.invoke(videoApi, originalWorkmode, 0);
            SystemClock.sleep(500);
            System.out.println("TCL_AIPQ_TEMP_RESTORED result=" + restoreResult
                    + " status=" + getStatus.invoke(videoApi)
                    + " workmode=" + getWorkmode.invoke(videoApi));
        }
    }

    private static void probeHdmiCapture() {
        final File target = new File("/data/local/tmp/tcl-hdmi-capture-probe.ts");
        Object capture = null;
        Method stop = null;
        Method delete = null;
        boolean initialized = false;
        boolean started = false;

        try {
            target.delete();
            Class<?> captureClass = Class.forName(
                    "com.tcl.tosapi.capture.HDMICaptureApi");
            capture = captureClass.getMethod("getInstance").invoke(null);
            Method getState = captureClass.getMethod("getState");
            Method init = captureClass.getMethod("init", String.class, int.class);
            Method start = captureClass.getMethod("start");
            stop = captureClass.getMethod("stop");
            delete = captureClass.getMethod("delete");

            System.out.println("TCL_HDMI_CAPTURE_BEFORE state="
                    + getState.invoke(capture));
            initialized = (Boolean) init.invoke(capture, target.getPath(), 1);
            System.out.println("TCL_HDMI_CAPTURE_INIT ok=" + initialized
                    + " state=" + getState.invoke(capture));
            if (!initialized) {
                return;
            }

            started = (Boolean) start.invoke(capture);
            System.out.println("TCL_HDMI_CAPTURE_START ok=" + started
                    + " state=" + getState.invoke(capture));
            if (started) {
                SystemClock.sleep(2000);
            }
            System.out.println("TCL_HDMI_CAPTURE_FILE exists=" + target.exists()
                    + " size=" + (target.exists() ? target.length() : -1));
        } catch (Throwable error) {
            System.err.println("TCL_HDMI_CAPTURE_FAILED " + error);
            error.printStackTrace(System.err);
        } finally {
            try {
                if (started && stop != null) {
                    System.out.println("TCL_HDMI_CAPTURE_STOP ok="
                            + stop.invoke(capture));
                }
            } catch (Throwable error) {
                System.err.println("TCL_HDMI_CAPTURE_STOP_FAILED " + error);
            }
            try {
                if (initialized && delete != null) {
                    System.out.println("TCL_HDMI_CAPTURE_DELETE ok="
                            + delete.invoke(capture));
                }
            } catch (Throwable error) {
                System.err.println("TCL_HDMI_CAPTURE_DELETE_FAILED " + error);
            }
            if (target.exists() && !target.delete()) {
                System.err.println("TCL_HDMI_CAPTURE_CLEANUP_FAILED " + target);
            }
        }
    }

    private static Context createSystemContext() throws ReflectiveOperationException {
        if (Looper.myLooper() == null) {
            Looper.prepareMainLooper();
        }
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method systemMain = activityThreadClass.getDeclaredMethod("systemMain");
        systemMain.setAccessible(true);
        Object activityThread = systemMain.invoke(null);

        Method getSystemContext = activityThreadClass.getDeclaredMethod("getSystemContext");
        getSystemContext.setAccessible(true);
        return (Context) getSystemContext.invoke(activityThread);
    }
}
