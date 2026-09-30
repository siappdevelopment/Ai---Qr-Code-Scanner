package com.qrcode.scanner.launcher.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.interfaces.OnReminderDeleteListener;
import com.qrcode.scanner.launcher.models.ReminderModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ClReminderAdapter extends RecyclerView.Adapter<ClReminderAdapter.MyViewHolder> {
    private final Context context;
    private final ArrayList<ReminderModel> arrayListReminder;
    private final OnReminderDeleteListener onReminderDeleteListener;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);

    public ClReminderAdapter(Context context, ArrayList<ReminderModel> arrayListReminder, OnReminderDeleteListener onReminderDeleteListener) {
        this.context = context;
        this.arrayListReminder = arrayListReminder;
        this.onReminderDeleteListener = onReminderDeleteListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_cl_reminder, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ReminderModel reminderModel = arrayListReminder.get(position);

        holder.ivColor.setBackgroundTintList(ColorStateList.valueOf(reminderModel.getColor()));
        holder.tvTitle.setText(reminderModel.getTitle());

        long reminderTime = reminderModel.getTime();
        if (reminderTime > 0L) {
            Date date = new Date(reminderTime);
            holder.tvDate.setText(DATE_FORMAT.format(date));
            holder.tvTime.setText(TIME_FORMAT.format(date));
        } else {
            holder.tvDate.setText("");
            holder.tvTime.setText("");
        }

        holder.ivDeleteReminder.setOnClickListener(v -> {
            if (onReminderDeleteListener != null) {
                onReminderDeleteListener.onReminderDeleteListener(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayListReminder.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivColor, ivDeleteReminder;
        private final AppCompatTextView tvTitle, tvDate, tvTime;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivColor = itemView.findViewById(R.id.ivColor);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivDeleteReminder = itemView.findViewById(R.id.ivDeleteReminder);
        }
    }
}
