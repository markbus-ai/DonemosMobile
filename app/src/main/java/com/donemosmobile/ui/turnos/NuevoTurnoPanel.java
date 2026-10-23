package com.donemosmobile.ui.turnos;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.ui.TurnoSchedule;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.DateField;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.components.FormActions;
import com.donemosmobile.ui.components.FormField;
import com.donemosmobile.ui.components.PatientPicker;
import com.donemosmobile.ui.components.TimeField;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * "Nuevo turno" form: paciente por DNI, fecha y hora, with its inline
 * validation and error display. The host does the network work through
 * {@link Listener} and reports results back.
 */
final class NuevoTurnoPanel extends LinearLayout {

  /** User intents the host turns into requests or navigation. */
  interface Listener {
    void onSearchPaciente(String dni);

    void onConfirm();

    void onBackToList();
  }

  private static final String SIN_PACIENTE = "Sin paciente seleccionado";

  private final PatientPicker pacientePicker;
  private final FormField pacienteField;
  private final DateField fechaField;
  private final TimeField horaField;
  private final ErrorText formError;
  private final AppButton confirmarButton;

  @Nullable private PacienteDto selectedPaciente;
  @Nullable private String selectedFecha;
  @Nullable private String selectedHora;

  NuevoTurnoPanel(FragmentActivity host, Listener listener) {
    super(host);
    setOrientation(VERTICAL);
    setPadding(
        Ui.dp(host, AppSpacing.MD),
        Ui.dp(host, AppSpacing.MD),
        Ui.dp(host, AppSpacing.MD),
        0);

    TextView title = new TextView(host);
    title.setText("Nuevo turno");
    AppTypography.headline2(title);
    addView(title);

    TextView help = new TextView(host);
    help.setText("Buscá al paciente por DNI y elegí fecha y hora.");
    AppTypography.caption(help);
    LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    helpParams.topMargin = Ui.dp(host, AppSpacing.XS);
    helpParams.bottomMargin = Ui.dp(host, AppSpacing.SM);
    addView(help, helpParams);

    pacientePicker = new PatientPicker(host, PatientPicker.Layout.STACKED,
        "DNI del paciente", "Buscar paciente", AppButton.Style.OUTLINED, false, false);
    pacientePicker.setListener(new PatientPicker.Listener() {
      @Override
      public void onSearch(String dni) {
        listener.onSearchPaciente(dni);
      }

      @Override
      public void onClear() {
        // El formulario de Turnos no tiene accion de limpiar.
      }
    });
    addView(pacientePicker);

    pacienteField = new FormField(host, "Paciente", true);
    addView(pacienteField);
    updatePacienteLabel(SIN_PACIENTE);

    fechaField = new DateField(host, "Fecha", true, host, "Fecha del turno", "date_picker");
    // Turnos cannot be booked in the past (the API rejects it with 422).
    fechaField.setFutureOnly(true);
    // Same window as the API: from today up to one year ahead.
    fechaField.setMaxYearsAhead(1);
    fechaField.setOnDateSelected((apiDate, displayDate) -> {
      selectedFecha = apiDate;
      checkHoraHoy();
    });
    addView(fechaField);

    horaField = new TimeField(host, "Hora", true);
    horaField.setOnTimeSelected(hora -> {
      selectedHora = hora;
      checkHoraHoy();
    });
    addView(horaField);

    formError = new ErrorText(host);
    addView(formError);

    FormActions actions = new FormActions(host);
    confirmarButton = actions.add("Confirmar turno", AppButton.Style.FILLED,
        v -> listener.onConfirm(), 48, AppSpacing.MD);
    actions.add("Volver a la lista", AppButton.Style.TEXT, v -> listener.onBackToList(), 48, 0);
    addView(actions);
  }

  @Nullable
  PacienteDto selectedPaciente() {
    return selectedPaciente;
  }

  @Nullable
  String selectedFecha() {
    return selectedFecha;
  }

  @Nullable
  String selectedHora() {
    return selectedHora;
  }

  /** True when paciente, fecha and hora are all set: the turno can be sent. */
  boolean isComplete() {
    return selectedPaciente != null && selectedPaciente.id != null
        && selectedFecha != null && selectedHora != null;
  }

  void setSearching(boolean searching) {
    pacientePicker.setSearching(searching);
  }

  void showPaciente(PacienteDto paciente) {
    selectedPaciente = paciente;
    updatePacienteLabel(paciente.displayName());
    pacienteField.error().hide();
  }

  void showPacienteError(String message) {
    selectedPaciente = null;
    updatePacienteLabel(SIN_PACIENTE);
    pacienteField.error().show(message);
  }

  /** Inline validation: each error stays under its field, not in a Toast. */
  boolean validate() {
    boolean valid = true;
    if (selectedPaciente == null || selectedPaciente.id == null) {
      pacienteField.error().show("Seleccioná un paciente");
      valid = false;
    }
    if (selectedFecha == null) {
      fechaField.error().show("Elegí la fecha del turno");
      valid = false;
    }
    if (selectedHora == null) {
      horaField.error().show("Elegí la hora del turno");
      valid = false;
    }
    String today = TurnoSchedule.today();
    if (selectedFecha != null && selectedFecha.compareTo(today) < 0) {
      fechaField.error().show("La fecha del turno no puede ser anterior a hoy");
      valid = false;
    } else if (selectedFecha != null && TurnoSchedule.isBeyondMax(selectedFecha, today)) {
      fechaField.error().show("La fecha del turno no puede ser posterior al "
          + TurnoSchedule.display(TurnoSchedule.maxFecha(today)));
      valid = false;
    } else if (!checkHoraHoy()) {
      valid = false;
    }
    return valid;
  }

  /**
   * A time already past today stays blocked; the error suggests the next
   * valid slot. Returns {@code true} when the picked time is acceptable.
   */
  private boolean checkHoraHoy() {
    if (selectedFecha == null || selectedHora == null
        || !selectedFecha.equals(TurnoSchedule.today())) {
      return true;
    }
    String now = horaActual();
    if (selectedHora.compareTo(now) >= 0) {
      horaField.error().hide();
      return true;
    }
    String next = TurnoSchedule.nextSlot(now);
    horaField.error().show(next != null
        ? "Esa hora ya pasó. El próximo horario disponible hoy es " + next + "."
        : "Ya no quedan horarios hoy. Elegí otra fecha.");
    return false;
  }

  void showFormError(CharSequence message) {
    formError.show(message);
  }

  void hideFormError() {
    formError.hide();
  }

  void showHorarioOcupado() {
    horaField.error().show("Ese horario ya está ocupado. Elegí otra hora.");
  }

  /** Shows the backend's Spanish 422 message under each field. */
  void applyFieldErrors(Map<String, String> errors) {
    boolean shown = false;
    if (errors.containsKey("paciente_id")) {
      pacienteField.error().show(errors.get("paciente_id"));
      shown = true;
    }
    if (errors.containsKey("fecha")) {
      fechaField.error().show(errors.get("fecha"));
      shown = true;
    }
    if (errors.containsKey("hora")) {
      horaField.error().show(errors.get("hora"));
      shown = true;
    }
    if (errors.containsKey("motivo")) {
      formError.show(errors.get("motivo"));
      shown = true;
    }
    if (!shown) {
      String first = errors.values().iterator().hasNext()
          ? errors.values().iterator().next() : null;
      formError.show(first != null && !first.isEmpty() ? first : "Revisá los datos del turno");
    }
  }

  void setSubmitting(boolean value) {
    confirmarButton.setEnabled(!value);
    confirmarButton.setText(value ? "Guardando…" : "Confirmar turno");
  }

  void reset() {
    selectedPaciente = null;
    selectedFecha = null;
    selectedHora = null;
    pacientePicker.setSearching(false);
    pacientePicker.dniInput().getEditText().setText("");
    pacientePicker.dniInput().setError(null);
    updatePacienteLabel(SIN_PACIENTE);
    fechaField.resetValue();
    horaField.resetValue();
    clearErrors();
  }

  void clearErrors() {
    pacientePicker.dniInput().setError(null);
    pacienteField.error().hide();
    fechaField.error().hide();
    horaField.error().hide();
    formError.hide();
  }

  private void updatePacienteLabel(String text) {
    pacienteField.value().setText(text);
    pacienteField.value().setTextColor(
        text.startsWith("Sin ") ? AppColors.TEXT_SECONDARY : AppColors.TEXT_PRIMARY);
  }

  /** Current local time as {@code HH:mm}, comparable with the picked hour. */
  private static String horaActual() {
    return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
  }
}
