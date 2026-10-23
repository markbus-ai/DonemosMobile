package com.donemosmobile.ui.components;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.donemosmobile.ui.theme.AppColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Material dialog around a custom input body, tinted like {@link ConfirmDialog}.
 * The positive button never dismisses on its own: {@link Submit} decides, so an
 * invalid input keeps the dialog open. Taps while the positive button is
 * disabled (a request in flight) are ignored.
 */
public final class InputDialog {

  /** Handles a tap on the positive button; the handler dismisses when done. */
  public interface Submit {
    void onSubmit(@NonNull AlertDialog dialog, @NonNull Button positive);
  }

  /** Receives the confirmed value exactly once. */
  public interface OnConfirm {
    void onConfirm();
  }

  private InputDialog() {}

  /** Shows the dialog; {@code submit} validates and dismisses when it is done. */
  public static AlertDialog show(@NonNull Context context, @NonNull String title,
      @Nullable String message, @NonNull View body, @NonNull String confirmLabel,
      @NonNull String cancelLabel, @NonNull Submit submit) {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setView(body)
        .setNegativeButton(cancelLabel, null)
        // Listener replaced in onShow so the handler controls dismissal.
        .setPositiveButton(confirmLabel, null);
    if (message != null) {
      builder.setMessage(message);
    }
    AlertDialog dialog = builder.create();
    dialog.setOnShowListener(d -> {
      Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
      Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
      positive.setTextColor(AppColors.TEXT_ACCENT);
      negative.setTextColor(AppColors.TEXT_SECONDARY);
      positive.setOnClickListener(v -> {
        if (positive.isEnabled()) {
          submit.onSubmit(dialog, positive);
        }
      });
    });
    dialog.show();
    return dialog;
  }

  /**
   * Shows the dialog and runs {@code onConfirm} once on the first positive tap:
   * the button is disabled before dismissing, so a double tap cannot repeat it.
   */
  public static AlertDialog showOnce(@NonNull Context context, @NonNull String title,
      @Nullable String message, @NonNull View body, @NonNull String confirmLabel,
      @NonNull String cancelLabel, @NonNull OnConfirm onConfirm) {
    return show(context, title, message, body, confirmLabel, cancelLabel, (dialog, positive) -> {
      positive.setEnabled(false);
      dialog.dismiss();
      onConfirm.onConfirm();
    });
  }
}
