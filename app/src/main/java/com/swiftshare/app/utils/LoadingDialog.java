package com.swiftshare.app.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.swiftshare.app.R;

public class LoadingDialog {

    private final Dialog dialog;
    private final TextView textTitle;
    private final TextView textSubtitle;

    public LoadingDialog(Context context) {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        dialog.setContentView(view);
        
        // Make background transparent so custom CardView corners show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Add a dimmed background effect
            dialog.getWindow().setDimAmount(0.6f);
        }

        dialog.setCancelable(false); // Prevent dismissing by tapping outside

        textTitle = view.findViewById(R.id.loadingText);
        textSubtitle = view.findViewById(R.id.loadingSubtext);
    }

    public void show() {
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    public void show(String title, String subtitle) {
        textTitle.setText(title);
        textSubtitle.setText(subtitle);
        
        if (subtitle == null || subtitle.isEmpty()) {
            textSubtitle.setVisibility(View.GONE);
        } else {
            textSubtitle.setVisibility(View.VISIBLE);
        }
        
        show();
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }
}
