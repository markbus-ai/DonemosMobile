package com.donemosmobile.ui.components;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.donemosmobile.ui.theme.AppColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Material confirmation for a state change, tinted with the app tokens. */
public final class ConfirmDialog {

  private ConfirmDialog() {}

  /**
   * Shows a two-button confirmation. {@code onConfirm} runs once; the confirm
   * button is disabled on the first tap so a double tap cannot repeat it.
   * A {@code null} confirm label shows a single acknowledgement button.
   */
  public static AlertDialog show(@NonNull Context context, @NonNull String title,
      @NonNull CharSequence message, @Nullable String confirmLabel, @NonNull String cancelLabel,
      @Nullable Runnable onConfirm) {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setMessage(message);
    if (confirmLabel != null) {
      builder.setNegativeButton(cancelLabel, null).setPositiveButton(confirmLabel, null);
    } else {
      builder.setPositiveButton(cancelLabel, null);
    }
    AlertDialog dialog = builder.create();
    dialog.setOnShowListener(d -> {
      Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
      Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
      positive.setTextColor(AppColors.TEXT_ACCENT);
      if (negative != null) {
        negative.setTextColor(AppColors.TEXT_SECONDARY);
      }
      positive.setOnClickListener(v -> {
        positive.setEnabled(false);
        dialog.dismiss();
        if (confirmLabel != null && onConfirm != null) {
          onConfirm.run();
        }
      });
    });
    dialog.show();
    return dialog;
  }
}
