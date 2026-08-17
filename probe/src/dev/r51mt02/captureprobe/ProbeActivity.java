package dev.r51mt02.captureprobe;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ProbeActivity extends Activity {
    private static final String TAG = "TclCaptureProbe";

    private static final int[][] ZONES = {
        {0, 0, 1920, 1080},
        {0, 540, 480, 540},
        {1440, 540, 480, 540},
        {480, 270, 960, 540},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        Thread worker = new Thread(() -> {
            executeProbe(context);
            runOnUiThread(this::finish);
        }, "tcl-capture-probe");
        worker.start();
    }

    static void executeProbe(Context context) {
        try {
            Class<?> managerClass = Class.forName("com.tcl.tvmanager.TAppManager");
            Method getInstance = managerClass.getMethod("getInstance", Context.class);
            Object manager = getInstance.invoke(null, context);
            Method getPixelInfo = managerClass.getMethod(
                    "getPixelInfo", int.class, int.class, int.class, int.class);

            report("TCL_PIXEL_PROBE_START manager=" + manager.getClass().getName());
            probeNativeStatus();
            probeFactoryPixels(context);
            probeScreenshot(context);
            for (int index = 0; index < ZONES.length; index++) {
                int[] zone = ZONES[index];
                Object info = getPixelInfo.invoke(
                        manager, zone[0], zone[1], zone[2], zone[3]);
                report(formatResult(index, zone, info));
                SystemClock.sleep(150);
            }
            report("TCL_PIXEL_PROBE_OK");
        } catch (Throwable error) {
            Throwable cause = unwrap(error);
            reportError("TCL_PIXEL_PROBE_FAILED " + cause, cause);
        }
    }

    private static void probeScreenshot(Context context) {
        try {
            Class<?> screenshotClass = Class.forName(
                    "com.tcl.app.screenshot.ScreenshotImpl");
            Object screenshot = screenshotClass
                    .getMethod("getInstance", Context.class)
                    .invoke(null, context);
            Method snapshotBuf = screenshotClass.getMethod(
                    "snapshotBuf", int.class, Rect.class,
                    int.class, int.class, int.class);
            Rect fullHd = new Rect(0, 0, 1920, 1080);
            for (int format = 0; format <= 2; format++) {
                byte[] data = (byte[]) snapshotBuf.invoke(
                        screenshot, 255, fullHd, format, 64, 36);
                report("TCL_SCREENSHOT format=" + format
                        + " length=" + (data == null ? -1 : data.length)
                        + " head=" + byteHead(data));
            }
        } catch (Throwable error) {
            Throwable cause = unwrap(error);
            reportError("TCL_SCREENSHOT_FAILED " + cause, cause);
        }
    }

    private static String byteHead(byte[] data) {
        if (data == null) {
            return "null";
        }
        StringBuilder result = new StringBuilder();
        int length = Math.min(data.length, 16);
        for (int index = 0; index < length; index++) {
            result.append(String.format("%02X", data[index] & 0xff));
        }
        return result.toString();
    }

    private static void probeFactoryPixels(Context context) {
        int[][] points = {
            {10, 10},
            {960, 540},
            {10, 700},
            {1900, 700},
        };
        try {
            Class<?> managerClass = Class.forName("tvos.tv.TManager");
            Object manager = managerClass
                    .getMethod("getInstance", Context.class)
                    .invoke(null, context);
            Object factoryManager = managerClass
                    .getMethod("getFactoryManager")
                    .invoke(manager);
            Method getPanelPixel = factoryManager.getClass().getMethod(
                    "getPanelPixel", int.class, int.class, int.class);

            for (int[] point : points) {
                int[] pixels = (int[]) getPanelPixel.invoke(
                        factoryManager, point[0], point[1], 8);
                report(formatFactoryPixels(point[0], point[1], pixels));
            }
        } catch (Throwable error) {
            Throwable cause = unwrap(error);
            reportError("TCL_FACTORY_PIXEL_FAILED " + cause, cause);
        }
    }

    private static String formatFactoryPixels(int x, int y, int[] pixels) {
        StringBuilder result = new StringBuilder();
        result.append("TCL_FACTORY_PIXEL x=").append(x).append(" y=").append(y);
        if (pixels == null) {
            return result.append(" result=null").toString();
        }
        result.append(" count=").append(pixels.length).append(" rgb=");
        for (int index = 0; index < pixels.length; index++) {
            if (index > 0) {
                result.append(',');
            }
            result.append(String.format("%06X", pixels[index] & 0xffffff));
        }
        return result.toString();
    }

    private static void probeNativeStatus() {
        try {
            Class<?> videoApiClass = Class.forName("com.tcl.tosapi.atv.TvVideoApi");
            Object videoApi = videoApiClass.getMethod("getInstance").invoke(null);
            report("TCL_AIPQ status="
                    + videoApiClass.getMethod("getVideoAIPQStatus").invoke(videoApi)
                    + " workmode="
                    + videoApiClass.getMethod("getVideoAIpqWorkmode").invoke(videoApi));
            Class<?> rectClass = Class.forName("com.tcl.tosapi.model.VideoWindowRect");
            Class<?> infoClass = Class.forName("com.tcl.tosapi.model.ScreenPixelInfo");
            Class<?> stageClass = Class.forName(
                    "com.tcl.tosapi.model.ScreenPixelInfo$EnTCLPixelRGBStage");
            Method nativeMethod = videoApiClass.getDeclaredMethod(
                    "getPixelInfo_native", rectClass, infoClass);
            nativeMethod.setAccessible(true);

            int[][] nativeZones = {
                {0, 0, 1920, 1080},
                {0, 0, 3840, 2160},
            };
            String[] stageNames = {
                "EN_TCL_PIXEL_STAGE_AFTER_DLC",
                "EN_TCL_PIXEL_STAGE_PRE_GAMMA",
                "EN_TCL_PIXEL_STAGE_AFTER_OSD",
            };
            for (int[] zone : nativeZones) {
                Object rect = rectClass
                        .getConstructor(int.class, int.class, int.class, int.class)
                        .newInstance(zone[0], zone[1], zone[2], zone[3]);
                for (String stageName : stageNames) {
                    Object info = infoClass.getConstructor().newInstance();
                    Object stage = stageClass.getField(stageName).get(null);
                    infoClass.getField("enStage").set(info, stage);
                    int stageValue = (Integer) stageClass
                            .getMethod("getValue").invoke(stage);
                    infoClass.getField("tmpStage").setInt(info, stageValue);
                    Object status = nativeMethod.invoke(videoApi, rect, info);
                    report("TCL_PIXEL_NATIVE requestedStage=" + stageName
                            + " status=" + status + " "
                            + formatResult(-1, zone, info));
                }
            }
        } catch (Throwable error) {
            Throwable cause = unwrap(error);
            reportError("TCL_PIXEL_NATIVE_FAILED " + cause, cause);
        }
    }

    private static String formatResult(int index, int[] zone, Object info)
            throws ReflectiveOperationException {
        if (info == null) {
            return "TCL_PIXEL_ZONE index=" + index + " result=null";
        }
        return "TCL_PIXEL_ZONE"
                + " index=" + index
                + " rect=" + zone[0] + "," + zone[1] + "," + zone[2] + "," + zone[3]
                + " stage=" + field(info, "enStage")
                + " rep=" + unsignedShort(field(info, "u16RepWinColor"))
                + " x=" + unsignedShort(field(info, "u16XStart"))
                + ".." + unsignedShort(field(info, "u16XEnd"))
                + " y=" + unsignedShort(field(info, "u16YStart"))
                + ".." + unsignedShort(field(info, "u16YEnd"))
                + " rMin=" + unsignedShort(field(info, "u16RCrMin"))
                + " rMax=" + unsignedShort(field(info, "u16RCrMax"))
                + " gMin=" + unsignedShort(field(info, "u16GYMin"))
                + " gMax=" + unsignedShort(field(info, "u16GYMax"))
                + " bMin=" + unsignedShort(field(info, "u16BCbMin"))
                + " bMax=" + unsignedShort(field(info, "u16BCbMax"))
                + " rSum=" + unsignedInt(field(info, "u32RCrSum"))
                + " gSum=" + unsignedInt(field(info, "u32GYSum"))
                + " bSum=" + unsignedInt(field(info, "u32BCbSum"));
    }

    private static Object field(Object object, String name)
            throws ReflectiveOperationException {
        Field field = object.getClass().getField(name);
        return field.get(object);
    }

    private static int unsignedShort(Object value) {
        return ((Number) value).shortValue() & 0xffff;
    }

    private static long unsignedInt(Object value) {
        return ((Number) value).intValue() & 0xffffffffL;
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof InvocationTargetException
                && ((InvocationTargetException) error).getCause() != null) {
            return ((InvocationTargetException) error).getCause();
        }
        return error;
    }

    private static void report(String message) {
        Log.i(TAG, message);
        System.out.println(message);
    }

    private static void reportError(String message, Throwable error) {
        Log.e(TAG, message, error);
        System.err.println(message);
        error.printStackTrace(System.err);
    }
}
