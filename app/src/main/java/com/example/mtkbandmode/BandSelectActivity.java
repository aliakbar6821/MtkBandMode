package com.example.mtkbandmode;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * BandSelectActivity – four tabs: GSM | UMTS | LTE | NR
 * Each tab is a BandTabFragment with checkboxes for that RAT's bands.
 *
 * Slot / SIM selection is passed via Intent extra "slot_id" (default 0).
 */
public class BandSelectActivity extends AppCompatActivity {

    public static final String EXTRA_SLOT = "slot_id";

    private static final String[] TAB_TITLES = {"GSM", "UMTS", "LTE", "NR"};
    private static final BandTabFragment.Rat[] RATS = {
            BandTabFragment.Rat.GSM,
            BandTabFragment.Rat.UMTS,
            BandTabFragment.Rat.LTE,
            BandTabFragment.Rat.NR
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_band_select);

        int slotId = getIntent().getIntExtra(EXTRA_SLOT, 0);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Band Mode – SIM " + (slotId + 1));
        }

        ViewPager2 pager = findViewById(R.id.viewpager);
        TabLayout  tabs  = findViewById(R.id.tab_layout);

        pager.setAdapter(new BandPagerAdapter(this, slotId));
        pager.setOffscreenPageLimit(3);

        new TabLayoutMediator(tabs, pager,
                (tab, pos) -> tab.setText(TAB_TITLES[pos])).attach();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ── Pager adapter ──────────────────────────────────────────────────────

    private static class BandPagerAdapter extends FragmentStateAdapter {
        private final int mSlotId;

        BandPagerAdapter(FragmentActivity fa, int slotId) {
            super(fa);
            mSlotId = slotId;
        }

        @Override public int getItemCount() { return RATS.length; }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return BandTabFragment.newInstance(RATS[position], mSlotId);
        }
    }
}
