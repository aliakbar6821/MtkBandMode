package com.example.mtkbandmode;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps the hidden TelephonyManager#setPreferredNetworkTypeBitmap (Android 9+) and
 * #getAllowedNetworkTypesBitmask (Android 12+) to read/write band selections.
 *
 * On MTK devices the original EngineerMode communicated via:
 *   EmRadioAidl → vendor/mediatek/hardware/mtkradioex → AT+EPBSE
 *
 * Standard apps can't reach that AIDL service without platform signing.
 * The closest public/hidden API path is TelephonyManager methods called via reflection.
 * Root users can additionally push the AT command via /dev/ttyC0 if needed.
 */
public class BandSelectHelper {

    private static final String TAG = "BandSelectHelper";

    public interface Callback {
        void onSuccess(String message);
        void onError(String message);
    }

    private final Context mContext;
    private final int mSlotId;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler   = new Handler(Looper.getMainLooper());

    public BandSelectHelper(Context context, int slotId) {
        mContext = context.getApplicationContext();
        mSlotId  = slotId;
    }

    // ── Persist band selection to SharedPreferences ────────────────────────

    public void saveBandSelection(String ratKey, long lowMask, long highMask) {
        SharedPreferences prefs = mContext.getSharedPreferences(
                BandModeContent.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putLong(ratKey + "_low_" + mSlotId, lowMask)
             .putLong(ratKey + "_high_" + mSlotId, highMask)
             .apply();
    }

    public long loadLow(String ratKey) {
        SharedPreferences prefs = mContext.getSharedPreferences(
                BandModeContent.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(ratKey + "_low_" + mSlotId, ~0L);
    }

    public long loadHigh(String ratKey) {
        SharedPreferences prefs = mContext.getSharedPreferences(
                BandModeContent.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(ratKey + "_high_" + mSlotId, ~0L);
    }

    // ── Apply via TelephonyManager hidden API ──────────────────────────────

    /**
     * Calls setAllowedNetworkTypesForReasons(int reason, long networkTypeBitmask)
     * or falls back to setPreferredNetworkTypeBitmap.
     * Both require MODIFY_PHONE_STATE – granted on MTK engineering builds or root.
     */
    public void applyNetworkTypeMask(long mask, Callback callback) {
        mExecutor.execute(() -> {
            try {
                TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                        Context.TELEPHONY_SERVICE);

                // createForSubscriptionId so we target the right SIM slot
                TelephonyManager tmSub = tm.createForSubscriptionId(
                        getSubIdForSlot(mSlotId));

                boolean applied = false;

                // Android 13+ path
                try {
                    Method m = TelephonyManager.class.getDeclaredMethod(
                            "setAllowedNetworkTypesForReasons", int.class, long.class);
                    m.setAccessible(true);
                    // reason = 1 (USER)
                    m.invoke(tmSub, 1, mask);
                    applied = true;
                    Log.d(TAG, "setAllowedNetworkTypesForReasons ok, mask=" + Long.toHexString(mask));
                } catch (NoSuchMethodException ignored) {}

                // Android 9-12 fallback
                if (!applied) {
                    Method m = TelephonyManager.class.getDeclaredMethod(
                            "setPreferredNetworkTypeBitmap", long.class);
                    m.setAccessible(true);
                    m.invoke(tmSub, mask);
                    Log.d(TAG, "setPreferredNetworkTypeBitmap ok, mask=" + Long.toHexString(mask));
                }

                mMainHandler.post(() -> callback.onSuccess("Band mask applied: 0x" + Long.toHexString(mask)));

            } catch (Exception e) {
                Log.e(TAG, "applyNetworkTypeMask failed", e);
                mMainHandler.post(() -> callback.onError("Failed: " + e.getMessage()
                        + "\n\nNote: MODIFY_PHONE_STATE requires platform signature or root."));
            }
        });
    }

    public void readCurrentMask(Callback callback) {
        mExecutor.execute(() -> {
            try {
                TelephonyManager tm = ((TelephonyManager) mContext.getSystemService(
                        Context.TELEPHONY_SERVICE))
                        .createForSubscriptionId(getSubIdForSlot(mSlotId));

                long mask = -1L;
                try {
                    Method m = TelephonyManager.class.getDeclaredMethod(
                            "getAllowedNetworkTypesForReasons", int.class);
                    m.setAccessible(true);
                    mask = (long) m.invoke(tm, 1);
                } catch (NoSuchMethodException e) {
                    Method m = TelephonyManager.class.getDeclaredMethod(
                            "getPreferredNetworkTypeBitmap");
                    m.setAccessible(true);
                    mask = (long) m.invoke(tm);
                }
                final long finalMask = mask;
                mMainHandler.post(() -> callback.onSuccess("0x" + Long.toHexString(finalMask)));
            } catch (Exception e) {
                mMainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private int getSubIdForSlot(int slotId) {
        try {
            Class<?> smClass = Class.forName("android.telephony.SubscriptionManager");
            Method m = smClass.getDeclaredMethod("getSubId", int.class);
            m.setAccessible(true);
            int[] ids = (int[]) m.invoke(null, slotId);
            if (ids != null && ids.length > 0) return ids[0];
        } catch (Exception e) {
            Log.w(TAG, "getSubIdForSlot fallback", e);
        }
        return slotId; // best-effort
    }
}
