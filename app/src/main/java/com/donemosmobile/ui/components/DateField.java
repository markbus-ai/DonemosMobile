package com.donemosmobile.ui.components;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Campo de formulario que abre el date picker Material en espanol. */
public class DateField extends FormField {

  public interface OnDateSelected {
    void onDate(String apiDate, String displayDate);
  }

  private final FragmentActivity host;
  private final String pickerTitle;
  private final String pickerTag;
  private OnDateSelected listener;

  public DateField(Context context, String label, boolean mediumValue, FragmentActivity host,
      String pickerTitle, String pickerTag) {
    super(context, label, mediumValue);
    this.host = host;
    this.pickerTitle = pickerTitle;
    this.pickerTag = pickerTag;
    setFieldOnClickListener(v -> openPicker());
  }

  public void setOnDateSelected(OnDateSelected listener) {
    this.listener = listener;
  }

  private void openPicker() {
    MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText(pickerTitle)
        .build();
    picker.addOnPositiveButtonClickListener(selection -> {
      String display = displayDate(selection);
      setValue(display);
      error().hide();
      if (listener != null) {
        listener.onDate(apiDate(selection), display);
      }
    });
    picker.show(host.getSupportFragmentManager(), pickerTag);
  }

  /** Fecha de API en UTC ({@code yyyy-MM-dd}). */
  private String apiDate(long millis) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    return fmt.format(new Date(millis));
  }

  private String displayDate(long millis) {
    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    return fmt.format(new Date(millis));
  }
}
