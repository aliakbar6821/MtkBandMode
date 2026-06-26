package com.example.mtkbandmode;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERM = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, READ_PHONE_STATE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{READ_PHONE_STATE}, REQ_PERM);
        } else {
            buildSimButtons();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);
        if (req == REQ_PERM && grants.length > 0 && grants[0] == PERMISSION_GRANTED) {
            buildSimButtons();
        } else {
            Toast.makeText(this, "READ_PHONE_STATE required", Toast.LENGTH_LONG).show();
        }
    }

    private void buildSimButtons() {
        LinearLayout container = findViewById(R.id.sim_container);
        container.removeAllViews();

        SubscriptionManager sm = (SubscriptionManager) getSystemService(SubscriptionManager.class);
        if (sm == null) {
            addSimButton(container, "SIM 1 (Slot 0)", 0);
            return;
        }

        List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
        if (subs == null || subs.isEmpty()) {
            addSimButton(container, "SIM 1 (Slot 0)", 0);
            return;
        }

        for (SubscriptionInfo info : subs) {
            int slot = info.getSimSlotIndex();
            String label = "SIM " + (slot + 1) + " – " + info.getDisplayName();
            addSimButton(container, label, slot);
        }
    }

    private void addSimButton(LinearLayout container, String label, int slotId) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setOnClickListener(v -> {
            Intent i = new Intent(this, BandSelectActivity.class);
            i.putExtra(BandSelectActivity.EXTRA_SLOT, slotId);
            startActivity(i);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(lp);
        container.addView(btn);
    }
}
