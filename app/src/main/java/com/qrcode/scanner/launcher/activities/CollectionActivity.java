package com.qrcode.scanner.launcher.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.ScreenFlowNavigation;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

public class CollectionActivity extends AppCompatActivity {
    private AppCompatTextView btnAgreeContinue;
    private AppCompatImageView ivSelect;
    private static final String KEY_AGREED = "key_agreed";
    private boolean isAgreed;
    private boolean navigatedAway;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        if (savedInstanceState != null) {
            isAgreed = savedInstanceState.getBoolean(KEY_AGREED, false);
        }
        AppCompatTextView tvFooter = findViewById(R.id.tvFooter);
        ivSelect = findViewById(R.id.ivSelect);
        btnAgreeContinue = findViewById(R.id.btnAgreeContinue);
        setupFooterLinks(tvFooter);
        updateAgreeUI();
        ivSelect.setOnClickListener(v -> {
            isAgreed = !isAgreed;
            updateAgreeUI();
        });
        // Back runs the same Agree & Continue action below.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                btnAgreeContinue.performClick();
            }
        });
        btnAgreeContinue.setOnClickListener(v -> {
            if (navigatedAway || isFinishing()) {
                return;
            }
            if (!isAgreed) {
                Toast.makeText(this, getString(R.string.collection_agree_toast), Toast.LENGTH_SHORT).show();
                return;
            }
            navigatedAway = true;
            ScreenFlowNavigation.continueAfter(this, ScreenFlowConfig.SCREEN_COLLECTION);
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_AGREED, isAgreed);
    }

    private void setupFooterLinks(AppCompatTextView tvFooter) {
        String fullText = getString(R.string.collection_footer);
        String privacy = getString(R.string.privacy_policy);
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf(privacy);
        if (start >= 0) {
            int linkColor = ContextCompat.getColor(this, R.color.primary);
            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    AppUtils.openPrivacyPolicy(CollectionActivity.this);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint textPaint) {
                    super.updateDrawState(textPaint);
                    textPaint.setColor(linkColor);
                    textPaint.setUnderlineText(true);
                }
            }, start, start + privacy.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvFooter.setText(spannable);
        tvFooter.setMovementMethod(LinkMovementMethod.getInstance());
        tvFooter.setHighlightColor(Color.TRANSPARENT);
    }

    private void updateAgreeUI() {
        ivSelect.setImageResource(isAgreed ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);
        btnAgreeContinue.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, isAgreed ? R.color.primary : R.color.text_secondary_dark)));
    }
}
