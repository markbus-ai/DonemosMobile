package com.donemosmobile.ui.components;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
  /** Years ahead of today that stay selectable; 0 means no upper bound. */
  private int maxYearsAhead;
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

  /** Disables every day after today + {@code years} (the API caps turnos at 1 year). */
  public void setMaxYearsAhead(int years) {
    this.maxYearsAhead = Math.max(0, years);
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
    if (futureOnly || maxYearsAhead > 0) {
      long today = todayUtcMidnight();
      CalendarConstraints.Builder constraints = new CalendarConstraints.Builder().setOpenAt(today);
      List<CalendarConstraints.DateValidator> validators = new ArrayList<>();
      if (futureOnly) {
        builder.setSelection(today);
        constraints.setStart(today);
        // from(today) keeps today selectable; now() would disable it.
        validators.add(DateValidatorPointForward.from(today));
      }
      if (maxYearsAhead > 0) {
        long last = yearsAheadUtcMidnight(today, maxYearsAhead);
        constraints.setEnd(last);
        // before(last) keeps the last day itself selectable.
        validators.add(DateValidatorPointBackward.before(last));
      }
      constraints.setValidator(validators.size() == 1
          ? validators.get(0) : CompositeDateValidator.allOf(validators));
      builder.setCalendarConstraints(constraints.build());
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

  private static long yearsAheadUtcMidnight(long todayUtc, int years) {
    Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    utc.setTimeInMillis(todayUtc);
    utc.add(Calendar.YEAR, years);
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
