package com.qrcode.scanner.launcher.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.AppWidgetsActivity;
import com.qrcode.scanner.launcher.activities.ClEndActivity;
import com.qrcode.scanner.ui.screens.history.HistoryActivity;
import com.qrcode.scanner.ui.navigation.ComposeScanRequest;

public class ClContentFragment extends Fragment {
    private LinearLayout llScan, llHistory, llAppWidgets;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cl_content, container, false);
        findIDs(view);
        return view;
    }

    private void findIDs(View view) {
        llScan = view.findViewById(R.id.llScan);
        llHistory = view.findViewById(R.id.llHistory);
        llAppWidgets = view.findViewById(R.id.llAppWidgets);
        initialClicks();
    }

    private void initialClicks() {
        llScan.setOnClickListener(v -> {
            // Scan opens the app's Scan fragment, not the separate ScannerActivity.
            ComposeScanRequest.INSTANCE.openScan(requireContext());
            ClEndActivity.finishAfterLaunch(this);
        });
        llHistory.setOnClickListener(v -> openAndFinish(HistoryActivity.class));
        llAppWidgets.setOnClickListener(v -> openAndFinish(AppWidgetsActivity.class));
    }

    private void openAndFinish(Class<?> destination) {
        startActivity(new Intent(requireContext(), destination));
        ClEndActivity.finishAfterLaunch(this);
    }
}
