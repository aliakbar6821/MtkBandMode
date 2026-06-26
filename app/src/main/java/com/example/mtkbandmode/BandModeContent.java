package com.example.mtkbandmode;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BandModeContent {

    private BandModeContent() {}

    // ── GSM bands ──────────────────────────────────────────────────────────
    public static final Map<String, Long> GSM_BANDS = new LinkedHashMap<>();
    static {
        GSM_BANDS.put("GSM 850",   0x00000080L);
        GSM_BANDS.put("GSM 900",   0x00000100L);
        GSM_BANDS.put("GSM 1800",  0x00000200L);
        GSM_BANDS.put("GSM 1900",  0x00000400L);
    }

    // ── UMTS / WCDMA bands ─────────────────────────────────────────────────
    public static final Map<String, Long> UMTS_BANDS = new LinkedHashMap<>();
    static {
        UMTS_BANDS.put("WCDMA B1 (2100)",    0x00000001L);
        UMTS_BANDS.put("WCDMA B2 (1900)",    0x00000002L);
        UMTS_BANDS.put("WCDMA B4 (1700)",    0x00000008L);
        UMTS_BANDS.put("WCDMA B5 (850)",     0x00000010L);
        UMTS_BANDS.put("WCDMA B6 (800)",     0x00000020L);
        UMTS_BANDS.put("WCDMA B8 (900)",     0x00000080L);
        UMTS_BANDS.put("WCDMA B19 (850 JP)", 0x00040000L);
    }

    // ── LTE bands (low = B1-B64, high = B65+) ─────────────────────────────
    public static final Map<String, Long> LTE_BANDS_LOW = new LinkedHashMap<>();
    static {
        LTE_BANDS_LOW.put("LTE B1 (2100)",   1L);
        LTE_BANDS_LOW.put("LTE B2 (1900)",   1L << 1);
        LTE_BANDS_LOW.put("LTE B3 (1800)",   1L << 2);
        LTE_BANDS_LOW.put("LTE B4 (1700)",   1L << 3);
        LTE_BANDS_LOW.put("LTE B5 (850)",    1L << 4);
        LTE_BANDS_LOW.put("LTE B7 (2600)",   1L << 6);
        LTE_BANDS_LOW.put("LTE B8 (900)",    1L << 7);
        LTE_BANDS_LOW.put("LTE B12 (700)",   1L << 11);
        LTE_BANDS_LOW.put("LTE B13 (700)",   1L << 12);
        LTE_BANDS_LOW.put("LTE B17 (700)",   1L << 16);
        LTE_BANDS_LOW.put("LTE B18 (850)",   1L << 17);
        LTE_BANDS_LOW.put("LTE B19 (850)",   1L << 18);
        LTE_BANDS_LOW.put("LTE B20 (800)",   1L << 19);
        LTE_BANDS_LOW.put("LTE B25 (1900)",  1L << 24);
        LTE_BANDS_LOW.put("LTE B26 (850)",   1L << 25);
        LTE_BANDS_LOW.put("LTE B28 (700)",   1L << 27);
        LTE_BANDS_LOW.put("LTE B34 (TDD)",   1L << 33);
        LTE_BANDS_LOW.put("LTE B38 (TDD)",   1L << 37);
        LTE_BANDS_LOW.put("LTE B39 (TDD)",   1L << 38);
        LTE_BANDS_LOW.put("LTE B40 (TDD)",   1L << 39);
        LTE_BANDS_LOW.put("LTE B41 (TDD)",   1L << 40);
    }

    public static final Map<String, Long> LTE_BANDS_HIGH = new LinkedHashMap<>();
    static {
        LTE_BANDS_HIGH.put("LTE B66 (1700)", 1L << 1);
        LTE_BANDS_HIGH.put("LTE B71 (600)",  1L << 6);
    }

    // ── NR (5G) bands ──────────────────────────────────────────────────────
    public static final Map<String, Long> NR_BANDS_LOW = new LinkedHashMap<>();
    static {
        NR_BANDS_LOW.put("NR n1 (2100)",   1L);
        NR_BANDS_LOW.put("NR n2 (1900)",   1L << 1);
        NR_BANDS_LOW.put("NR n3 (1800)",   1L << 2);
        NR_BANDS_LOW.put("NR n5 (850)",    1L << 4);
        NR_BANDS_LOW.put("NR n7 (2600)",   1L << 6);
        NR_BANDS_LOW.put("NR n8 (900)",    1L << 7);
        NR_BANDS_LOW.put("NR n12 (700)",   1L << 11);
        NR_BANDS_LOW.put("NR n20 (800)",   1L << 19);
        NR_BANDS_LOW.put("NR n25 (1900)",  1L << 24);
        NR_BANDS_LOW.put("NR n28 (700)",   1L << 27);
        NR_BANDS_LOW.put("NR n38 (TDD)",   1L << 37);
        NR_BANDS_LOW.put("NR n40 (TDD)",   1L << 39);
        NR_BANDS_LOW.put("NR n41 (TDD)",   1L << 40);
        NR_BANDS_LOW.put("NR n77 (TDD)",   1L << 62);
        NR_BANDS_LOW.put("NR n78 (TDD)",   1L << 63);
    }

    public static final Map<String, Long> NR_BANDS_HIGH = new LinkedHashMap<>();
    static {
        NR_BANDS_HIGH.put("NR n79 (TDD)", 1L);
    }

    // ── SharedPreferences base keys (slot id appended by BandSelectHelper) ─
    public static final String PREF_NAME = "band_mode";
    public static final String KEY_GSM   = "band_mode_gsm";
    public static final String KEY_UMTS  = "band_mode_umts";
    public static final String KEY_LTE   = "band_mode_lte";
    public static final String KEY_NR    = "band_mode_nr";
}
