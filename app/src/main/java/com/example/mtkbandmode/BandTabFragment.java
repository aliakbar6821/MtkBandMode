package com.example.mtkbandmode;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic fragment that renders a list of band checkboxes for one RAT tab.
 * Instantiated with a Bundle containing the RAT type and slot id.
 */
public class BandTabFragment extends Fragment {

    public static final String ARG_RAT    = "rat";
    public static final String ARG_SLOT   = "slot";

    public enum Rat { GSM, UMTS, LTE, NR }

    private Rat mRat;
    private int mSlotId;
    private BandSelectHelper mHelper;

    // CheckBoxes mapped to their bitmask contributions
    private final Map<CheckBox, Long> mLowBoxes  = new HashMap<>();
    private final Map<CheckBox, Long> mHighBoxes = new HashMap<>();

    public static BandTabFragment newInstance(Rat rat, int slotId) {
        BandTabFragment f = new BandTabFragment();
        Bundle b = new Bundle();
        b.putString(ARG_RAT,  rat.name());
        b.putInt(ARG_SLOT, slotId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        mRat    = Rat.valueOf(args.getString(ARG_RAT));
        mSlotId = args.getInt(ARG_SLOT, 0);
        mHelper = new BandSelectHelper(requireContext(), mSlotId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_band_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout container = view.findViewById(R.id.band_list_container);

        switch (mRat) {
            case GSM:
                addBands(container, BandModeContent.GSM_BANDS, true, BandModeContent.KEY_GSM_LOW);
                break;
            case UMTS:
                addBands(container, BandModeContent.UMTS_BANDS, true, BandModeContent.KEY_UMTS_LOW);
                break;
            case LTE:
                addBands(container, BandModeContent.LTE_BANDS_LOW,  true,  BandModeContent.KEY_LTE_LOW);
                addBands(container, BandModeContent.LTE_BANDS_HIGH, false, BandModeContent.KEY_LTE_HIGH);
                break;
            case NR:
                addBands(container, BandModeContent.NR_BANDS_LOW,  true,  BandModeContent.KEY_NR_LOW);
                addBands(container, BandModeContent.NR_BANDS_HIGH, false, BandModeContent.KEY_NR_HIGH);
                break;
        }

        view.findViewById(R.id.btn_set).setOnClickListener(v -> applySelection());
        view.findViewById(R.id.btn_select_all).setOnClickListener(v -> setAll(true));
        view.findViewById(R.id.btn_clear_all).setOnClickListener(v -> setAll(false));
    }

    private void addBands(LinearLayout container, Map<String, Long> bands,
                          boolean isLow, String prefKey) {
        long saved = isLow ? mHelper.loadLow(prefKey) : mHelper.loadHigh(prefKey);
        for (Map.Entry<String, Long> entry : bands.entrySet()) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(entry.getKey());
            cb.setChecked((saved & entry.getValue()) != 0);
            container.addView(cb);
            if (isLow) mLowBoxes.put(cb, entry.getValue());
            else       mHighBoxes.put(cb, entry.getValue());
        }
    }

    private void applySelection() {
        long lowMask  = 0L;
        long highMask = 0L;
        for (Map.Entry<CheckBox, Long> e : mLowBoxes.entrySet())
            if (e.getKey().isChecked()) lowMask |= e.getValue();
        for (Map.Entry<CheckBox, Long> e : mHighBoxes.entrySet())
            if (e.getKey().isChecked()) highMask |= e.getValue();

        String ratKey = ratPrefKey();
        mHelper.saveBandSelection(ratKey, lowMask, highMask);

        // Build a combined mask for TelephonyManager (low bits are enough for GSM/UMTS/LTE)
        mHelper.applyNetworkTypeMask(lowMask | (highMask << 32), new BandSelectHelper.Callback() {
            @Override public void onSuccess(String message) { showStatus(message); }
            @Override public void onError(String message)   { showStatus("⚠ " + message); }
        });
    }

    private void setAll(boolean checked) {
        for (CheckBox cb : mLowBoxes.keySet())  cb.setChecked(checked);
        for (CheckBox cb : mHighBoxes.keySet()) cb.setChecked(checked);
    }

    private void showStatus(String msg) {
        View v = getView();
        if (v == null) return;
        TextView tv = v.findViewById(R.id.tv_status);
        tv.setText(msg);
    }

    private String ratPrefKey() {
        switch (mRat) {
            case GSM:  return "band_mode_gsm";
            case UMTS: return "band_mode_umts";
            case LTE:  return "band_mode_lte";
            case NR:   return "band_mode_nr";
            default:   return "band_mode_unknown";
        }
    }
}
