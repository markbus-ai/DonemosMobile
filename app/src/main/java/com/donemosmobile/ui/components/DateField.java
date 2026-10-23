package com.donemosmobile.ui.components;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
  private boolean futureOnly;
  private boolean pickerOpen;

  public DateField(Context context, String label, boolean mediumValue, FragmentActivity host,
      String pickerTitle, String pickerTag) {
    super(context, label, mediumValue);
    this.host = host;
    this.pickerTitle = pickerTitle;
    this.pickerTag = pickerTag;
    setFieldOnClickListener(v -> openPicker());
  }

  /** When true the picker opens on today and disables every past day. */
  public void setFutureOnly(boolean futureOnly) {
    this.futureOnly = futureOnly;
  }

  public void setOnDateSelected(OnDateSelected listener) {
    this.listener = listener;
  }

  private void openPicker() {
    // A fast double tap would otherwise stack two pickers.
    if (pickerOpen || host.isFinishing() || host.getSupportFragmentManager().isStateSaved()
        || host.getSupportFragmentManager().findFragmentByTag(pickerTag) != null) {
      return;
    }
    MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
        .setTitleText(pickerTitle);
    if (futureOnly) {
      long today = todayUtcMidnight();
      builder.setSelection(today)
          .setCalendarConstraints(new CalendarConstraints.Builder()
              .setStart(today)
              .setOpenAt(today)
              // from(today) keeps today selectable; now() would disable it.
              .setValidator(DateValidatorPointForward.from(today))
              .build());
    }
    MaterialDatePicker<Long> picker = builder.build();
    picker.addOnPositiveButtonClickListener(selection -> {
      String display = displayDate(selection);
      setValue(display);
      error().hide();
      if (listener != null) {
        listener.onDate(apiDate(selection), display);
      }
    });
    picker.addOnDismissListener(d -> pickerOpen = false);
    pickerOpen = true;
    picker.show(host.getSupportFragmentManager(), pickerTag);
  }

  /** Today (device-local calendar day) as the UTC midnight the picker uses. */
  public static long todayUtcMidnight() {
    Calendar local = Calendar.getInstance();
    Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    utc.clear();
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH),
        local.get(Calendar.DAY_OF_MONTH));
    return utc.getTimeInMillis();
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
