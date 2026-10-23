package com.donemosmobile.ui.turnos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.data.turnos.TurnoRepository;
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.TurnoLabels;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.DateField;
import com.donemosmobile.ui.components.FormActions;
import com.donemosmobile.ui.components.FormField;
import com.donemosmobile.ui.components.PatientPicker;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.components.TimeField;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turnos del día. Lista los turnos de hoy desde la API, permite crear uno
 * nuevo (paciente por DNI + fecha + hora + tipo opcional) y refrescar.
 *
 * <p>Mobile-first: una tarea por pantalla, botones grandes, sin tablas largas.
 */
public class TurnosActivity extends AppActivity {

  public static final String EXTRA_MODE = "mode";
  public static final String MODE_CREATE = "create";

  private TurnoRepository turnoRepository;
  private PacienteRepository pacienteRepository;

  private StateView state;
  private LinearLayout listContainer;
  private LinearLayout createPanel;

  // Formulario de alta.
  private PatientPicker pacientePicker;
  private PacienteDto selectedPaciente;
  private FormField pacienteField;
  private DateField fechaField;
  private TimeField horaField;
  private String selectedFecha;
  private String selectedHora;
  private AppButton confirmarButton;
  private boolean submitting;
  /** DNI of the lookup in flight; responses for any other DNI are stale. */
  private String pendingDni;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    turnoRepository = new TurnoRepository(this);
    pacienteRepository = new PacienteRepository(this);

    boolean isCreate = MODE_CREATE.equals(getIntent().getStringExtra(EXTRA_MODE));
    setContentView(buildView());

    if (isCreate) {
      showCreatePanel();
    } else {
      loadTurnos();
    }
  }

  /**
   * Cuando la sección ya está abierta y se la vuelve a invocar (por ejemplo,
   * desde el acceso "Nuevo turno" de la Home), reutilizamos la instancia y
   * mostramos el formulario o la lista según el modo pedido.
   */
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);

    if (MODE_CREATE.equals(intent.getStringExtra(EXTRA_MODE))) {
      showCreatePanel();
    } else {
      loadTurnos();
    }
  }

  private View buildView() {
    ScreenScaffold root = new ScreenScaffold(this, Section.TURNOS);
    root.setHeader(buildHeader());

    LinearLayout content = root.content();

    createPanel = buildCreateForm();
    createPanel.setVisibility(View.GONE);
    content.addView(createPanel);

    state = new StateView(this);
    content.addView(state);

    listContainer = new LinearLayout(this);
    listContainer.setOrientation(LinearLayout.VERTICAL);
    listContainer.setPadding(
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.XL));
    content.addView(listContainer);

    return root;
  }

  private ScreenHeader buildHeader() {
    ScreenHeader header = new ScreenHeader(this)
        .title("Turnos de hoy")
        .subtitle(hoyLegible());

    TextView nuevo = new TextView(this);
    nuevo.setText("+ Nuevo");
    nuevo.setTextSize(15);
    nuevo.setTextColor(AppColors.PRIMARY);
    nuevo.setPadding(
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.SM));
    nuevo.setClickable(true);
    nuevo.setFocusable(true);
    nuevo.setOnClickListener(v -> showCreatePanel());
    header.action(nuevo);

    return header;
  }

  // --- Lista de turnos ---

  private void loadTurnos() {
    listContainer.removeAllViews();
    createPanel.setVisibility(View.GONE);

    // Cache-first (stale-while-revalidate): si ya hay datos, pintalos al toque
    // y refresca en 2do plano sin mostrar el spinner.
    final List<TurnoDto> cached = turnoRepository.getCachedList(hoy(), null);
    final boolean hasCached = cached != null && !cached.isEmpty();
    if (hasCached) {
      state.hide();
      renderTurnos(cached);
    } else {
      state.showLoading();
    }

    turnoRepository.list(hoy(), null, new TurnoRepository.Callback<List<TurnoDto>>() {
      @Override
      public void onSuccess(List<TurnoDto> data) {
        if (data == null || data.isEmpty()) {
          listContainer.removeAllViews();
          state.showEmpty("No hay turnos para hoy");
          return;
        }
        state.hide();
        renderTurnos(data);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        // Con cache ya en pantalla no pisamos el contenido con un error.
        if (hasCached) {
          return;
        }
        state.showError(
            ApiMessages.forType(type, message, "No se pudo registrar el turno", "Ocurrió un error"),
            TurnosActivity.this::loadTurnos);
      }
    });
  }

  private void renderTurnos(List<TurnoDto> turnos) {
    listContainer.removeAllViews();
    for (TurnoDto turno : turnos) {
      listContainer.addView(buildTurnoCard(turno));
    }
  }

  private AppCard buildTurnoCard(TurnoDto turno) {
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.HORIZONTAL);
    content.setGravity(Gravity.CENTER_VERTICAL);

    TextView hora = new TextView(this);
    hora.setText(turno.horaCorta());
    hora.setTextSize(20);
    hora.setTextColor(AppColors.TEXT_PRIMARY);
    hora.setTypeface(android.graphics.Typeface.create(
        "sans-serif-medium", android.graphics.Typeface.NORMAL));
    content.addView(hora, new LinearLayout.LayoutParams(
        Ui.dp(this, 64), ViewGroup.LayoutParams.WRAP_CONTENT));

    LinearLayout details = new LinearLayout(this);
    details.setOrientation(LinearLayout.VERTICAL);

    TextView paciente = new TextView(this);
    // El backend embebe la relación; si no vino, el label cae al id.
    paciente.setText(turno.pacienteLabel());
    AppTypography.bodyMedium(paciente);
    details.addView(paciente);

    TextView meta = new TextView(this);
    meta.setText(metaLine(turno));
    AppTypography.caption(meta);
    details.addView(meta);

    content.addView(details, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    TextView estado = new TextView(this);
    estado.setText(turno.estado != null ? turno.estado : "");
    estado.setTextSize(12);
    estado.setTextColor(TurnoLabels.estadoColor(turno.estado));
    estado.setPadding(
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.XS),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.XS));
    content.addView(estado);

    AppCard card = new AppCard(this, content);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = Ui.dp(this, AppSpacing.SM);
    card.setLayoutParams(params);
    return card;
  }

  private String metaLine(TurnoDto turno) {
    return turno.sedeLabel() + " · " + turno.tipoLabel();
  }

  // --- Alta de turno ---

  private void showCreatePanel() {
    listContainer.removeAllViews();
    state.hide();
    createPanel.setVisibility(View.VISIBLE);
  }

  private LinearLayout buildCreateForm() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.MD),
        0);

    TextView title = new TextView(this);
    title.setText("Nuevo turno");
    AppTypography.headline2(title);
    root.addView(title);

    TextView help = new TextView(this);
    help.setText("Buscá al paciente por DNI y elegí fecha y hora.");
    AppTypography.caption(help);
    LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    helpParams.topMargin = Ui.dp(this, AppSpacing.XS);
    helpParams.bottomMargin = Ui.dp(this, AppSpacing.SM);
    root.addView(help, helpParams);

    pacientePicker = new PatientPicker(this, PatientPicker.Layout.STACKED,
        "DNI del paciente", "Buscar paciente", AppButton.Style.OUTLINED, false, false);
    pacientePicker.setListener(new PatientPicker.Listener() {
      @Override
      public void onSearch(String dni) {
        buscarPaciente(dni);
      }

      @Override
      public void onClear() {
        // El formulario de Turnos no tiene accion de limpiar.
      }
    });
    root.addView(pacientePicker);

    pacienteField = new FormField(this, "Paciente", true);
    root.addView(pacienteField);
    updatePacienteLabel("Sin paciente seleccionado");

    fechaField = new DateField(this, "Fecha", true, this, "Fecha del turno", "date_picker");
    // Turnos cannot be booked in the past (the API rejects it with 422).
    fechaField.setFutureOnly(true);
    fechaField.setOnDateSelected((apiDate, displayDate) -> selectedFecha = apiDate);
    root.addView(fechaField);

    horaField = new TimeField(this, "Hora", true);
    horaField.setOnTimeSelected(hora -> selectedHora = hora);
    root.addView(horaField);

    FormActions actions = new FormActions(this);
    confirmarButton = actions.add("Confirmar turno", AppButton.Style.FILLED, v -> confirmar(), 48,
        AppSpacing.MD);
    actions.add("Volver a la lista", AppButton.Style.TEXT, v -> loadTurnos(), 48, 0);
    root.addView(actions);

    return root;
  }

  private void buscarPaciente(String dni) {
    pendingDni = dni;
    pacientePicker.setSearching(true);
    pacienteRepository.findByDni(dni, new PacienteRepository.Callback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        if (isStale(dni)) {
          return;
        }
        pacientePicker.setSearching(false);
        selectedPaciente = data;
        updatePacienteLabel(data.displayName());
        pacienteField.error().hide();
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        if (isStale(dni)) {
          return;
        }
        pacientePicker.setSearching(false);
        selectedPaciente = null;
        updatePacienteLabel("Sin paciente seleccionado");
        pacienteField.error().show(type == ApiError.Type.NOT_FOUND
            ? "No existe un paciente con ese DNI"
            // Never surface the raw backend text of a lookup (may be English).
            : ApiMessages.forType(type, null, "No se pudo buscar el paciente",
                "No se pudo buscar el paciente"));
      }
    });
  }

  /** True when the activity is gone or a newer search replaced this one. */
  private boolean isStale(String dni) {
    if (isFinishing() || isDestroyed()) {
      return true;
    }
    return !dni.equals(pendingDni);
  }

  private void updatePacienteLabel(String text) {
    if (pacienteField == null) {
      return;
    }
    pacienteField.value().setText(text);
    pacienteField.value().setTextColor(
        text.startsWith("Sin ") ? AppColors.TEXT_SECONDARY : AppColors.TEXT_PRIMARY);
  }

  private void confirmar() {
    if (submitting) {
      return;
    }
    clearErrors();

    // Validación inline: cada error queda debajo de su campo, no en un Toast.
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
    if (selectedFecha != null && selectedFecha.compareTo(hoy()) < 0) {
      fechaField.error().show("La fecha del turno no puede ser anterior a hoy");
      valid = false;
    } else if (selectedFecha != null && selectedHora != null && selectedFecha.equals(hoy())
        && selectedHora.compareTo(horaActual()) < 0) {
      horaField.error().show("La hora del turno ya pasó");
      valid = false;
    }
    if (!valid) {
      return;
    }

    setSubmitting(true);

    CreateTurnoRequest body = new CreateTurnoRequest(
        selectedPaciente.id, selectedFecha, selectedHora, null);
    turnoRepository.create(body, new TurnoRepository.Callback<TurnoDto>() {
      @Override
      public void onSuccess(TurnoDto data) {
        setSubmitting(false);
        if (isFinishing() || isDestroyed()) {
          return;
        }
        Toast.makeText(TurnosActivity.this, "Turno registrado", Toast.LENGTH_SHORT).show();
        resetForm();
        loadTurnos();
      }

      @Override
      public void onValidationError(Map<String, String> fieldErrors) {
        setSubmitting(false);
        applyFieldErrors(fieldErrors);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        setSubmitting(false);
        if (isFinishing() || isDestroyed()) {
          return;
        }
        String text;
        if (type == ApiError.Type.NOT_FOUND) {
          text = "No existe un paciente con ese DNI";
        } else if (type == ApiError.Type.CLIENT && message != null && !message.isEmpty()) {
          // 409 business rules (capacity, interval) come with a Spanish message.
          text = message;
        } else {
          text = ApiMessages.forType(type, null, "No se pudo registrar el turno",
              "No se pudo registrar el turno");
        }
        Toast.makeText(TurnosActivity.this, text, Toast.LENGTH_LONG).show();
      }
    });
  }

  /** Maps Laravel 422 keys to fixed Spanish copy under each field. */
  private void applyFieldErrors(Map<String, String> errors) {
    boolean shown = false;
    if (errors.containsKey("paciente_id")) {
      pacienteField.error().show("Seleccioná un paciente válido");
      shown = true;
    }
    if (errors.containsKey("fecha")) {
      fechaField.error().show("La fecha del turno no puede ser anterior a hoy");
      shown = true;
    }
    if (errors.containsKey("hora")) {
      horaField.error().show("Elegí una hora válida");
      shown = true;
    }
    if (!shown) {
      Toast.makeText(this, "Revisá los datos del turno", Toast.LENGTH_LONG).show();
    }
  }

  private void setSubmitting(boolean value) {
    submitting = value;
    if (confirmarButton != null) {
      confirmarButton.setEnabled(!value);
      confirmarButton.setText(value ? "Guardando…" : "Confirmar turno");
    }
  }

  private void resetForm() {
    selectedPaciente = null;
    selectedFecha = null;
    selectedHora = null;
    pendingDni = null;
    if (pacientePicker != null) {
      pacientePicker.setSearching(false);
      pacientePicker.dniInput().getEditText().setText("");
      pacientePicker.dniInput().setError(null);
    }
    updatePacienteLabel("Sin paciente seleccionado");
    fechaField.resetValue();
    horaField.resetValue();
    clearErrors();
  }

  private void clearErrors() {
    if (pacientePicker != null) {
      pacientePicker.dniInput().setError(null);
    }
    pacienteField.error().hide();
    fechaField.error().hide();
    horaField.error().hide();
  }

  /** Fecha de hoy en formato de API ({@code YYYY-MM-DD}). */
  private String hoy() {
    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
  }

  /** Current local time as {@code HH:mm}, comparable with the picked hour. */
  private String horaActual() {
    return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
  }

  private String hoyLegible() {
    return new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "AR"))
        .format(new Date());
  }
}
