package com.donemosmobile.ui.components;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.donemosmobile.ui.ForceOverride;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/**
 * Confirmation for an audited override: restates every warning and requires a
 * written motivo before the forced request is sent. The confirm button only
 * closes the dialog when the motivo passes {@link ForceOverride#checkMotivo}.
 */
public final class ForceDialog {

  public interface OnConfirm {
    void onConfirm(@NonNull String motivo);
  }

  private ForceDialog() {}

  public static AlertDialog show(@NonNull Context context, @NonNull String title,
      @NonNull List<String> warnings, @NonNull String confirmLabel, @NonNull OnConfirm onConfirm) {
    LinearLayout body = new LinearLayout(context);
    body.setOrientation(LinearLayout.VERTICAL);
    int side = Ui.dp(context, AppSpacing.LG);
    body.setPadding(side, Ui.dp(context, AppSpacing.SM), side, 0);

    TextView intro = new TextView(context);
    intro.setText("Se va a registrar igual, aunque no cumple estas reglas:");
    intro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    intro.setTextColor(AppColors.TEXT_SECONDARY);
    body.addView(intro);

    BulletList.addTo(body, warnings);

    TextView note = new TextView(context);
    note.setText("La autorización queda auditada con tu usuario y este motivo.");
    note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    note.setTextColor(AppColors.WARNING_TEXT);
    body.addView(note, Ui.matchWrap(context, AppSpacing.SM));

    TextInputLayout motivoLayout = new TextInputLayout(context);
    motivoLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
    motivoLayout.setBoxStrokeColor(AppColors.TEXT_ACCENT);
    motivoLayout.setHintTextColor(ColorStateList.valueOf(AppColors.TEXT_ACCENT));
    motivoLayout.setHint("Motivo de la autorización");
    motivoLayout.setCounterEnabled(true);
    motivoLayout.setCounterMaxLength(ForceOverride.MOTIVO_MAX);
    motivoLayout.setHelperText("Obligatorio, entre " + ForceOverride.MOTIVO_MIN + " y "
        + ForceOverride.MOTIVO_MAX + " caracteres.");

    TextInputEditText motivo = new TextInputEditText(motivoLayout.getContext());
    motivo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    motivo.setMinLines(2);
    motivo.setMaxLines(5);
    motivo.setTextColor(AppColors.TEXT_PRIMARY);
    // Hard cap at the API maximum: pasting a longer text cannot slip through.
    motivo.setFilters(new InputFilter[] {new InputFilter.LengthFilter(ForceOverride.MOTIVO_MAX)});
    motivoLayout.addView(motivo);
    body.addView(motivoLayout, Ui.matchWrap(context, AppSpacing.MD));

    AlertDialog dialog = new MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setView(body)
        .setNegativeButton("Cancelar", null)
        // Listener replaced in onShow so an invalid motivo keeps the dialog open.
        .setPositiveButton(confirmLabel, null)
        .create();

    dialog.setOnShowListener(d -> {
      Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
      Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
      positive.setTextColor(AppColors.TEXT_ACCENT);
      negative.setTextColor(AppColors.TEXT_SECONDARY);
      positive.setOnClickListener(v -> {
        String text = motivo.getText() != null ? motivo.getText().toString() : "";
        String error = ForceOverride.checkMotivo(text);
        if (error != null) {
          motivoLayout.setError(error);
          motivo.requestFocus();
          return;
        }
        // Disable before dismissing: a double tap cannot fire two requests.
        positive.setEnabled(false);
        dialog.dismiss();
        onConfirm.onConfirm(text.trim());
      });
    });

    motivo.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        motivoLayout.setError(null);
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    dialog.show();
    return dialog;
  }
}
