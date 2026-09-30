package com.qrcode.scanner.launcher.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.ClEndActivity;

public class ClMoreOptionFragment extends Fragment {
    private LinearLayout llAddToContact, llSendMessage, llSendMail, llCalendar, llWeb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cl_more_option, container, false);
        findIDs(view);
        return view;
    }

    private void findIDs(View view) {
        llAddToContact = view.findViewById(R.id.llAddToContact);
        llSendMessage = view.findViewById(R.id.llSendMessage);
        llSendMail = view.findViewById(R.id.llSendMail);
        llCalendar = view.findViewById(R.id.llCalendar);
        llWeb = view.findViewById(R.id.llWeb);
        initialClicks();
    }

    private void initialClicks() {
        llAddToContact.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setType("vnd.android.cursor.dir/contact");
            startActivity(intent);
            ClEndActivity.finishAfterLaunch(this);
        });

        llSendMessage.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("smsto:"));
            startActivity(intent);
            ClEndActivity.finishAfterLaunch(this);
        });

        llSendMail.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("mailto:"));
            startActivity(intent);
            ClEndActivity.finishAfterLaunch(this);
        });

        llCalendar.setOnClickListener(view -> {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.google.android.calendar");
            if (intent != null) {
                startActivity(intent);
            } else {
                Intent calendarIntent = new Intent(Intent.ACTION_MAIN);
                calendarIntent.addCategory(Intent.CATEGORY_APP_CALENDAR);
                startActivity(calendarIntent);
            }
            ClEndActivity.finishAfterLaunch(this);
        });

        llWeb.setOnClickListener(view -> {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.google.android.googlequicksearchbox");
            if (intent != null) {
                startActivity(intent);
                ClEndActivity.finishAfterLaunch(this);
            }
        });
    }
}
