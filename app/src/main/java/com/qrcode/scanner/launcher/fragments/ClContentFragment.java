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
import com.qrcode.scanner.ui.screens.create.CreateActivity;
import com.qrcode.scanner.ui.screens.history.HistoryActivity;

public class ClContentFragment extends Fragment {
    private LinearLayout llQRTemplates, llHistory, llAppWidgets;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cl_content, container, false);
        findIDs(view);
        return view;
    }

    private void findIDs(View view) {
        llQRTemplates = view.findViewById(R.id.llQRTemplates);
        llHistory = view.findViewById(R.id.llHistory);
        llAppWidgets = view.findViewById(R.id.llAppWidgets);
        initialClicks();
    }

    private void initialClicks() {
        llQRTemplates.setOnClickListener(v -> openAndFinish(CreateActivity.class));
        llHistory.setOnClickListener(v -> openAndFinish(HistoryActivity.class));
        llAppWidgets.setOnClickListener(v -> openAndFinish(AppWidgetsActivity.class));
    }

    private void openAndFinish(Class<?> destination) {
        startActivity(new Intent(requireContext(), destination));
        ClEndActivity.finishAfterLaunch(this);
    }
}
