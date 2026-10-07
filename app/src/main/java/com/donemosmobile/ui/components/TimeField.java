package com.donemosmobile.ui.components;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

/** Campo de formulario que abre el time picker nativo. */
public class TimeField extends FormField {

  public interface OnTimeSelected {
    void onTime(String hhmm);
  }

  private OnTimeSelected listener;

  public TimeField(Context context, String label, boolean mediumValue) {
    super(context, label, mediumValue);
    setFieldOnClickListener(v -> openPicker());
  }

  public void setOnTimeSelected(OnTimeSelected listener) {
    this.listener = listener;
  }

  private void openPicker() {
    Calendar now = Calendar.getInstance();
    TimePickerDialog dialog = new TimePickerDialog(
        getContext(),
        (view, hourOfDay, minute) -> {
          String value = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
          setValue(value);
          error().hide();
          if (listener != null) {
            listener.onTime(value);
          }
        },
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
        DateFormat.is24HourFormat(getContext()));
    dialog.show();
  }
}
