package com.example.mtkbandmode;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Applies band selections via root shell ("su -c service call phone ...").
 *
 * WHY ROOT SHELL INSTEAD OF REFLECTION:
 *   TelephonyManager#setPreferredNetworkTypeBitmap and
 *   #setAllowedNetworkTypesForReasons both require MODIFY_PHONE_STATE.
 *   This permission is guarded at the BINDER level — the OS checks the
 *   *calling UID* before the method body even runs.  Reflection does NOT
 *   bypass that check; it just lets us call the method by name.
 *   As a priv-app the permission is whitelisted in XML, but KernelSU/Magisk
 *   modules that overlay /system don't re-sign the APK with the platform key,
 *   so Android still sees it as a 3rd-party app and denies the binder call.
 *
 *   The only reliable path without a platform signature is:
 *     su -c "service call phone <TXN> ..."
 *   This executes as UID 0 (root), which always passes the MODIFY_PHONE_STATE
 *   check, regardless of the app's own UID or signature.
 *
 * SERVICE CALL TRANSACTION CODES (AOSP ITelephony.aidl):
 *   setAllowedNetworkTypesForReasons  = 84   (Android 13+, args: subId, reason=1, mask)
 *   setPreferredNetworkType           = 80   (Android 9-12,  args: subId, networkType)
 *   getAllowedNetworkTypesForReasons   = 85   (Android 13+)
 *   getPreferredNetworkType           = 81   (Android 9-12)
 *
 * NOTE: Transaction codes can differ by OEM ROM.  If SET fails, check with:
 *   adb shell su -c "service call phone 84 i32 <subId> i32 1 i64 <mask>"
 * and adjust TXN_SET_ALLOWED / TXN_SET_PREFERRED below to match your ROM.
 */
public class BandSelectHelper {

    private static final String TAG = "BandSelectHelper";

    // ── Binder transaction codes ──────────────────────────────────────────
    // Android 13+ (setAllowedNetworkTypesForReasons / getAllowedNetworkTypesForReasons)
    private static final int TXN_SET_ALLOWED = 84;
    private static final int TXN_GET_ALLOWED = 85;
    // Android 9-12 fallback (setPreferredNetworkType / getPreferredNetworkType)
    private static final int TXN_SET_PREFERRED = 80;
    private static final int TXN_GET_PREFERRED = 81;

    public interface Callback {
        void onSuccess(String message);
        void onError(String message);
    }

    private final Context mContext;
    private final int mSlotId;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public BandSelectHelper(Context context, int slotId) {
        mContext = context.getApplicationContext();
        mSlotId  = slotId;
    }

    // ── SharedPreferences persistence ─────────────────────────────────────

    public void saveBandSelection(String ratKey, long lowMask, long highMask) {
        mContext.getSharedPreferences(BandModeContent.PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(ratKey + "_low_"  + mSlotId, lowMask)
                .putLong(ratKey + "_high_" + mSlotId, highMask)
                .apply();
    }

    public long loadLow(String ratKey) {
        return mContext.getSharedPreferences(BandModeContent.PREF_NAME, Context.MODE_PRIVATE)
                .getLong(ratKey + "_low_" + mSlotId, ~0L);
    }

    public long loadHigh(String ratKey) {
        return mContext.getSharedPreferences(BandModeContent.PREF_NAME, Context.MODE_PRIVATE)
                .getLong(ratKey + "_high_" + mSlotId, ~0L);
    }

    // ── Apply via root shell ──────────────────────────────────────────────

    public void applyNetworkTypeMask(long mask, Callback callback) {
        mExecutor.execute(() -> {
            int subId = getSubIdForSlot(mSlotId);
            String result;

            // Try Android 13+ path first
            result = runRootCommand(buildSetAllowedCmd(subId, mask));
            if (result == null) {
                // Fallback: Android 9-12 path (networkType bitmask → type int not needed,
                // setPreferredNetworkType takes an int type, not bitmask — use bitmap variant)
                result = runRootCommand(buildSetPreferredCmd(subId, mask));
            }

            final String finalResult = result;
            if (finalResult != null) {
                mMainHandler.post(() -> callback.onSuccess(
                        "Band mask applied: 0x" + Long.toHexString(mask)
                        + "\n" + finalResult));
            } else {
                mMainHandler.post(() -> callback.onError(
                        "Failed to apply mask via root shell.\n"
                        + "Make sure root (KernelSU/Magisk) is granted to this app."));
            }
        });
    }

    public void readCurrentMask(Callback callback) {
        mExecutor.execute(() -> {
            int subId = getSubIdForSlot(mSlotId);
            String result = runRootCommand(buildGetAllowedCmd(subId));
            if (result == null) {
                result = runRootCommand(buildGetPreferredCmd(subId));
            }
            final String r = result;
            if (r != null) {
                mMainHandler.post(() -> callback.onSuccess(r));
            } else {
                mMainHandler.post(() -> callback.onError("Failed to read current mask"));
            }
        });
    }

    // ── Command builders ──────────────────────────────────────────────────

    /** Android 13+: setAllowedNetworkTypesForReasons(subId, reason=USER(1), mask) */
    private String buildSetAllowedCmd(int subId, long mask) {
        return String.format(
                "service call phone %d i32 %d i32 1 i64 %d",
                TXN_SET_ALLOWED, subId, mask);
    }

    /** Android 9-12: setPreferredNetworkTypeBitmap(subId, mask) */
    private String buildSetPreferredCmd(int subId, long mask) {
        return String.format(
                "service call phone %d i32 %d i64 %d",
                TXN_SET_PREFERRED, subId, mask);
    }

    private String buildGetAllowedCmd(int subId) {
        return String.format(
                "service call phone %d i32 %d i32 1",
                TXN_GET_ALLOWED, subId);
    }

    private String buildGetPreferredCmd(int subId) {
        return String.format(
                "service call phone %d i32 %d",
                TXN_GET_PREFERRED, subId);
    }

    // ── Root shell executor ───────────────────────────────────────────────

    /**
     * Runs a command as root via "su -c".
     * Returns the stdout string on success, or null if the command failed
     * or root was not granted.
     */
    private String runRootCommand(String cmd) {
        try {
            Log.d(TAG, "su -c \"" + cmd + "\"");
            Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            // Read stderr too for logging
            StringBuilder err = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    err.append(line).append("\n");
                }
            }

            int exitCode = proc.waitFor();
            String out = sb.toString().trim();
            String errStr = err.toString().trim();

            if (!errStr.isEmpty()) Log.w(TAG, "stderr: " + errStr);

            // "service call" returns "Result: Parcel(...)" on success
            // and the exit code is 0.  A failed binder call still exits 0
            // but returns "Result: Parcel(00000000 ...)" with all zeros for
            // a void/false return — check for that if needed.
            if (exitCode == 0 && !out.isEmpty()) {
                Log.d(TAG, "Result: " + out);
                return out;
            } else {
                Log.e(TAG, "Command failed (exit=" + exitCode + "): " + errStr);
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "runRootCommand exception", e);
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int getSubIdForSlot(int slotId) {
    try {
        Class<?> smClass = Class.forName("android.telephony.SubscriptionManager");
        Method m = smClass.getDeclaredMethod("getSubId", int.class);
        m.setAccessible(true);
        int[] ids = (int[]) m.invoke(null, slotId);
        if (ids != null && ids.length > 0) return ids[0];
    } catch (Exception e) {
        Log.w(TAG, "getSubIdForSlot fallback to slotId=" + slotId, e);
    }
    return slotId;
}
}
