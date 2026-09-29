package com.qrcode.scanner.launcher.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.adapters.ClReminderAdapter;
import com.qrcode.scanner.launcher.helpers.ReminderAlarmHelper;
import com.qrcode.scanner.launcher.interfaces.OnReminderDeleteListener;
import com.qrcode.scanner.launcher.models.ReminderModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class ClReminderFragment extends Fragment implements OnReminderDeleteListener {
    private AppCompatTextView tvCreateReminder, tvTime, btnNo, btnYes;
    private RecyclerView rvReminder;
    private LinearLayout llAddReminderView;
    private AppCompatEditText etAbout;
    private AppCompatImageView ivRed, ivBlack, ivGray, ivBlue, ivGreen, ivPurple, ivYellow, ivOrange, ivSkyBlue;

    private final ArrayList<ReminderModel> arrayListReminder = new ArrayList<>();
    private ClReminderAdapter clReminderAdapter;
    private long selectedReminderTime = 0;
    private int selectedColor = Color.parseColor("#FF0000");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cl_reminder, container, false);

        findIDs(view);

        return view;
    }

    private void findIDs(View view) {
        tvCreateReminder = view.findViewById(R.id.tvCreateReminder);
        rvReminder = view.findViewById(R.id.rvReminder);

        llAddReminderView = view.findViewById(R.id.llAddReminderView);
        etAbout = view.findViewById(R.id.etAbout);
        tvTime = view.findViewById(R.id.tvTime);

        ivRed = view.findViewById(R.id.ivRed);
        ivBlack = view.findViewById(R.id.ivBlack);
        ivGray = view.findViewById(R.id.ivGray);
        ivBlue = view.findViewById(R.id.ivBlue);
        ivGreen = view.findViewById(R.id.ivGreen);
        ivPurple = view.findViewById(R.id.ivPurple);
        ivYellow = view.findViewById(R.id.ivYellow);
        ivOrange = view.findViewById(R.id.ivOrange);
        ivSkyBlue = view.findViewById(R.id.ivSkyBlue);

        btnNo = view.findViewById(R.id.btnNo);
        btnYes = view.findViewById(R.id.btnYes);

        initialClicks();
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    private void initialClicks() {
        clReminderAdapter = new ClReminderAdapter(getContext(), arrayListReminder, this);
        rvReminder.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReminder.setAdapter(clReminderAdapter);
        loadReminderList();

        AppCompatImageView selectedImage = ivRed;
        if (selectedColor == Color.parseColor("#000000")) {
            selectedImage = ivBlack;
        } else if (selectedColor == Color.parseColor("#858E97")) {
            selectedImage = ivGray;
        } else if (selectedColor == Color.parseColor("#1E5EFF")) {
            selectedImage = ivBlue;
        } else if (selectedColor == Color.parseColor("#00B140")) {
            selectedImage = ivGreen;
        } else if (selectedColor == Color.parseColor("#7A00D4")) {
            selectedImage = ivPurple;
        } else if (selectedColor == Color.parseColor("#F3CF3C")) {
            selectedImage = ivYellow;
        } else if (selectedColor == Color.parseColor("#FF6A1A")) {
            selectedImage = ivOrange;
        } else if (selectedColor == Color.parseColor("#12A4E8")) {
            selectedImage = ivSkyBlue;
        }

        updateSelectedColorBorder(selectedImage, selectedColor, ivRed, ivBlack, ivGray, ivBlue, ivGreen, ivPurple, ivYellow, ivOrange, ivSkyBlue);

        tvCreateReminder.setOnClickListener(view -> {
            tvCreateReminder.setVisibility(GONE);
            rvReminder.setVisibility(GONE);
            llAddReminderView.setVisibility(VISIBLE);
        });

        tvTime.setOnClickListener(view -> showDateTimePicker());

        View.OnClickListener colorSelectListener = v -> {
            int color = Color.parseColor("#FF0000");
            AppCompatImageView selectedColorImage = ivRed;
            if (v.getId() == R.id.ivBlack) {
                color = Color.parseColor("#000000");
                selectedColorImage = ivBlack;
            } else if (v.getId() == R.id.ivGray) {
                color = Color.parseColor("#858E97");
                selectedColorImage = ivGray;
            } else if (v.getId() == R.id.ivBlue) {
                color = Color.parseColor("#1E5EFF");
                selectedColorImage = ivBlue;
            } else if (v.getId() == R.id.ivGreen) {
                color = Color.parseColor("#00B140");
                selectedColorImage = ivGreen;
            } else if (v.getId() == R.id.ivPurple) {
                color = Color.parseColor("#7A00D4");
                selectedColorImage = ivPurple;
            } else if (v.getId() == R.id.ivYellow) {
                color = Color.parseColor("#F3CF3C");
                selectedColorImage = ivYellow;
            } else if (v.getId() == R.id.ivOrange) {
                color = Color.parseColor("#FF6A1A");
                selectedColorImage = ivOrange;
            } else if (v.getId() == R.id.ivSkyBlue) {
                color = Color.parseColor("#12A4E8");
                selectedColorImage = ivSkyBlue;
            }

            selectedColor = color;
            updateSelectedColorBorder(selectedColorImage, color, ivRed, ivBlack, ivGray, ivBlue, ivGreen, ivPurple, ivYellow, ivOrange, ivSkyBlue);
        };

        ivRed.setOnClickListener(colorSelectListener);
        ivBlack.setOnClickListener(colorSelectListener);
        ivGray.setOnClickListener(colorSelectListener);
        ivBlue.setOnClickListener(colorSelectListener);
        ivGreen.setOnClickListener(colorSelectListener);
        ivPurple.setOnClickListener(colorSelectListener);
        ivYellow.setOnClickListener(colorSelectListener);
        ivOrange.setOnClickListener(colorSelectListener);
        ivSkyBlue.setOnClickListener(colorSelectListener);

        btnNo.setOnClickListener(view -> {
            tvCreateReminder.setVisibility(VISIBLE);
            rvReminder.setVisibility(VISIBLE);
            llAddReminderView.setVisibility(GONE);
        });

        btnYes.setOnClickListener(view -> {
            String title = Objects.requireNonNull(etAbout.getText()).toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Please Enter Remind About", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedReminderTime == 0) {
                Toast.makeText(getContext(), "Please Select Date & Time", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedReminderTime <= System.currentTimeMillis()) {
                Toast.makeText(getContext(), "Please Select a Future Date & Time", Toast.LENGTH_SHORT).show();
                return;
            }

            int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            if (requestCode == 0) {
                requestCode = 1;
            }
            ReminderModel reminderModel = new ReminderModel(title, selectedReminderTime, selectedColor, requestCode);
            arrayListReminder.add(0, reminderModel);
            ReminderAlarmHelper.scheduleReminder(requireContext().getApplicationContext(), reminderModel);
            saveReminderList();
            clReminderAdapter.notifyItemInserted(0);
            rvReminder.scrollToPosition(0);

            etAbout.setText("");
            tvTime.setText("Select date & time");
            selectedReminderTime = 0;

            tvCreateReminder.setVisibility(VISIBLE);
            rvReminder.setVisibility(VISIBLE);
            llAddReminderView.setVisibility(GONE);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadReminderList() {
        arrayListReminder.clear();
        arrayListReminder.addAll(ReminderAlarmHelper.loadReminderList(requireContext()));
        if (!arrayListReminder.isEmpty()) {
            arrayListReminder.sort((a, b) -> Long.compare(b.getTime(), a.getTime()));
            clReminderAdapter.notifyDataSetChanged();
            ReminderAlarmHelper.rescheduleAll(requireContext().getApplicationContext());
        }
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (timePicker, hour, minute) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, day, hour, minute, 0);
                selectedReminderTime = selectedCalendar.getTimeInMillis();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.ENGLISH);
                tvTime.setText(simpleDateFormat.format(selectedCalendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateSelectedColorBorder(AppCompatImageView selectedView, int selectedColor, AppCompatImageView... views) {
        for (AppCompatImageView appCompatImageView : views) {
            ViewGroup.LayoutParams params = appCompatImageView.getLayoutParams();
            if (appCompatImageView == selectedView) {
                params.width = dpToPx(40);
                params.height = dpToPx(40);
                appCompatImageView.setPadding(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
                appCompatImageView.setBackgroundResource(R.drawable.custom_color_selected);
                appCompatImageView.setBackgroundTintList(null);
                ViewCompat.setBackgroundTintList(appCompatImageView, ColorStateList.valueOf(selectedColor));
            } else {
                params.width = dpToPx(30);
                params.height = dpToPx(30);
                appCompatImageView.setPadding(0, 0, 0, 0);
                appCompatImageView.setBackgroundResource(R.drawable.custom_circle);
            }

            appCompatImageView.setLayoutParams(params);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void saveReminderList() {
        ReminderAlarmHelper.saveReminderList(requireContext(), arrayListReminder);
    }

    @Override
    public void onReminderDeleteListener(int position) {
        if (position >= 0 && position < arrayListReminder.size()) {
            ReminderModel reminderModel = arrayListReminder.get(position);
            ReminderAlarmHelper.cancelReminder(requireContext().getApplicationContext(), reminderModel);

            arrayListReminder.remove(position);
            saveReminderList();
            clReminderAdapter.notifyItemRemoved(position);
        }
    }
}