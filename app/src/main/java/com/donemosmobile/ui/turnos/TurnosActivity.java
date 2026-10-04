package com.donemosmobile.ui.turnos;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.donemosmobile.AppActivity;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TurnosActivity extends AppActivity {

  public static final String EXTRA_MODE = "mode";
  public static final String MODE_CREATE = "create";

  private TextView dateValue;
  private TextView timeValue;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    boolean isCreate = MODE_CREATE.equals(getIntent().getStringExtra(EXTRA_MODE));

    if (isCreate) {
      setContentView(buildCreateForm());
    } else {
      setContentView(buildAgendaPlaceholder());
    }
  }

  // --- Nuevo turno: formulario con calendario ---

  private LinearLayout buildCreateForm() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    TextView title = new TextView(this);
    title.setText("Nuevo turno");
    AppTypography.headline1(title);
    root.addView(title, fieldParams(
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources())));

    // Paciente — depende del buscador, todavía sin modelo de datos
    LinearLayout patientField = makeField("Paciente", "Seleccionar paciente");
    patientField.setOnClickListener(v ->
        Toast.makeText(this, "Requiere el buscador de pacientes (pendiente)", Toast.LENGTH_SHORT).show());
    root.addView(patientField, fieldParams(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources())));

    // Fecha — calendario
    dateValue = new TextView(this);
    LinearLayout dateField = makeField("Fecha", "Elegir fecha", dateValue);
    dateField.setOnClickListener(v -> showDatePicker());
    root.addView(dateField, fieldParams(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources())));

    // Hora — reloj
    timeValue = new TextView(this);
    LinearLayout timeField = makeField("Hora", "Elegir hora", timeValue);
    timeField.setOnClickListener(v -> showTimePicker());
    root.addView(timeField, fieldParams(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources())));

    // Confirmar
    LinearLayout buttonWrapper = new LinearLayout(this);
    buttonWrapper.setOrientation(LinearLayout.VERTICAL);
    buttonWrapper.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    buttonWrapper.addView(new AppButton(this, "Confirmar turno", AppButton.Style.FILLED, v -> confirm()));
    root.addView(buttonWrapper, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    return root;
  }

  private void showDatePicker() {
    MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText("Fecha del turno")
        .build();
    picker.addOnPositiveButtonClickListener(selection -> {
      // Material devuelve millis en UTC: convertir con timezone UTC para no correr un día
      SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
      fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
      dateValue.setText(fmt.format(new Date(selection)));
      dateValue.setTextColor(AppColors.TEXT_PRIMARY);
    });
    picker.show(getSupportFragmentManager(), "date_picker");
  }

  private void showTimePicker() {
    Calendar now = Calendar.getInstance();
    TimePickerDialog dialog = new TimePickerDialog(
        this,
        (view, hourOfDay, minute) -> {
          timeValue.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
          timeValue.setTextColor(AppColors.TEXT_PRIMARY);
        },
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
        DateFormat.is24HourFormat(this));
    dialog.show();
  }

  private void confirm() {
    boolean hasDate = dateValue.getText() != null && !dateValue.getText().equals("Elegir fecha");
    boolean hasTime = timeValue.getText() != null && !timeValue.getText().equals("Elegir hora");

    if (!hasDate || !hasTime) {
      Toast.makeText(this, "Falta elegir fecha y hora", Toast.LENGTH_SHORT).show();
      return;
    }

    // TODO: persistir cuando exista la capa de datos
    Toast.makeText(this, "Turno registrado (sin persistencia aún)", Toast.LENGTH_SHORT).show();
    finish();
  }

  // --- Campo tipo píldora: etiqueta + valor ---

  private LinearLayout makeField(String label, String placeholder) {
    return makeField(label, placeholder, null);
  }

  private LinearLayout makeField(String label, String placeholder, TextView valueView) {
    LinearLayout field = new LinearLayout(this);
    field.setOrientation(LinearLayout.VERTICAL);
    field.setGravity(Gravity.CENTER_VERTICAL);
    field.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()), 0,
        AppSpacing.dp(AppSpacing.MD, getResources()), 0);
    field.setBackground(rounded(AppColors.SURFACE, 16));
    field.setClickable(true);
    field.setFocusable(true);

    TextView labelTv = new TextView(this);
    labelTv.setText(label);
    AppTypography.caption(labelTv);
    field.addView(labelTv);

    TextView value;
    if (valueView != null) {
      value = valueView;
    } else {
      value = new TextView(this);
    }
    value.setText(placeholder);
    value.setTextSize(15);
    value.setTextColor(AppColors.TEXT_SECONDARY);
    value.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    field.addView(value);

    field.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        AppSpacing.dp(64, getResources())));

    return field;
  }

  private android.graphics.drawable.GradientDrawable rounded(int color, float radiusDp) {
    android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(AppSpacing.dp((int) radiusDp, getResources()));
    return drawable;
  }

  private LinearLayout.LayoutParams fieldParams(int topMargin, int leftRight) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = topMargin;
    params.leftMargin = leftRight;
    params.rightMargin = leftRight;
    return params;
  }

  // --- Agenda: placeholder ---

  private LinearLayout buildAgendaPlaceholder() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    TextView title = new TextView(this);
    title.setText("Turnos");
    AppTypography.headline1(title);
    title.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    root.addView(title, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    TextView subtitle = new TextView(this);
    subtitle.setText("Agenda de turnos (por construir)");
    AppTypography.caption(subtitle);
    subtitle.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    root.addView(subtitle, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    return root;
  }
}
