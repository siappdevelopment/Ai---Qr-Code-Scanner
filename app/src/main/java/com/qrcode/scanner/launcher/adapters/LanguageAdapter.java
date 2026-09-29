package com.qrcode.scanner.launcher.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.qrcode.scanner.R;

import java.util.ArrayList;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    public interface OnLanguageClickListener {
        void onLanguageClick(int position);
    }

    private final Context context;
    private final ArrayList<Integer> arrayListIcon;
    private final ArrayList<String> arrayListName;
    private final ArrayList<String> arrayListSubName;
    private final ArrayList<String> arrayListCode;
    private int selectedPosition = -1;
    private OnLanguageClickListener listener;

    public LanguageAdapter(Context context, ArrayList<Integer> arrayListIcon, ArrayList<String> arrayListName, ArrayList<String> arrayListSubName, ArrayList<String> arrayListCode) {
        this.context = context;
        this.arrayListIcon = arrayListIcon;
        this.arrayListName = arrayListName;
        this.arrayListSubName = arrayListSubName;
        this.arrayListCode = arrayListCode;
    }

    public void setOnLanguageClickListener(OnLanguageClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_language, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.ivLanguageIcon.setImageResource(arrayListIcon.get(position));
        holder.tvLanguageName.setText(arrayListName.get(position));
        holder.tvLanguageSubName.setText(arrayListSubName.get(position));
        holder.ivSelect.setImageResource(position == selectedPosition ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);
        holder.itemView.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onLanguageClick(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayListIcon.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivLanguageIcon;
        private final AppCompatImageView ivSelect;
        private final AppCompatTextView tvLanguageName;
        private final AppCompatTextView tvLanguageSubName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLanguageIcon = itemView.findViewById(R.id.ivLanguageIcon);
            ivSelect = itemView.findViewById(R.id.ivSelect);
            tvLanguageName = itemView.findViewById(R.id.tvLanguageName);
            tvLanguageSubName = itemView.findViewById(R.id.tvLanguageSubName);
        }
    }
}
