package com.donemosmobile.ui.turnos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.WarningCallback;
import com.donemosmobile.AppActivity;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.data.network.dto.TurnoWarningResponse;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.data.turnos.TurnoRepository;
import com.donemosmobile.ui.A11y;
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.ForceOverride;
import com.donemosmobile.ui.TurnoLabels;
import com.donemosmobile.ui.TurnoSchedule;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.ConfirmDialog;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.components.ForceDialog;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;

/**
 * Próximos turnos. Lista los turnos desde hoy ({@code desde}), deja atender o
 * cancelar los pendientes, y crear uno nuevo (paciente por DNI + fecha + hora).
 * Un 409 forzable pide confirmación con motivo; los límites duros bloquean.
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
  private ErrorText formError;
  private boolean submitting;
  /** Turno ids with a PATCH in flight: their actions stay disabled. */
  private final Set<Long> updating = new HashSet<>();
  /** Bumped per list request; older responses are dropped. */
  private int listGeneration;
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
        .title("Próximos turnos")
        .subtitle("Desde hoy, " + hoyLegible());

    TextView nuevo = new TextView(this);
    nuevo.setText("+ Nuevo");
    nuevo.setTextSize(15);
    nuevo.setTextColor(AppColors.TEXT_ACCENT);
    nuevo.setPadding(
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.SM));
    nuevo.setClickable(true);
    nuevo.setFocusable(true);
    nuevo.setOnClickListener(v -> showCreatePanel());
    nuevo.setGravity(Gravity.CENTER);
    A11y.touchableButton(nuevo, "Nuevo turno");
    header.action(nuevo);

    return header;
  }

  // --- Lista de turnos ---

  private void loadTurnos() {
    listContainer.removeAllViews();
    createPanel.setVisibility(View.GONE);
    final int generation = ++listGeneration;
    final String desde = TurnoSchedule.today();

    // Cache-first (stale-while-revalidate): si ya hay datos, pintalos al toque
    // y refresca en 2do plano sin mostrar el spinner.
    final List<TurnoDto> cached = turnoRepository.getCachedDesde(desde);
    final boolean hasCached = cached != null && !cached.isEmpty();
    if (hasCached) {
      state.hide();
      renderTurnos(cached);
    } else {
      state.showLoading();
    }

    turnoRepository.listDesde(desde, new ApiCallback<List<TurnoDto>>() {
      @Override
      public void onSuccess(List<TurnoDto> data) {
        if (isListStale(generation)) {
          return;
        }
        if (data == null || data.isEmpty()) {
          listContainer.removeAllViews();
          state.showEmpty("No hay turnos desde hoy");
          return;
        }
        state.hide();
        renderTurnos(data);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        // Con cache ya en pantalla no pisamos el contenido con un error.
        if (hasCached || isListStale(generation)) {
          return;
        }
        state.showError(
            ApiMessages.forType(type, message, "No se pudieron cargar los turnos",
                "Ocurrió un error"),
            TurnosActivity.this::loadTurnos);
      }
    });
  }

  /** True when the screen is gone, the form is open, or a newer list request won. */
  private boolean isListStale(int generation) {
    return !isAlive() || generation != listGeneration
        || createPanel.getVisibility() == View.VISIBLE;
  }

  private void renderTurnos(List<TurnoDto> turnos) {
    listContainer.removeAllViews();
    String currentDay = null;
    for (TurnoDto turno : turnos) {
      String day = turno.fecha != null ? turno.fecha : "";
      if (!day.equals(currentDay)) {
        currentDay = day;
        listContainer.addView(buildDayHeader(day));
      }
      listContainer.addView(buildTurnoCard(turno));
    }
  }

  private TextView buildDayHeader(String fecha) {
    TextView header = new TextView(this);
    header.setText(fecha.equals(TurnoSchedule.today()) ? "Hoy" : TurnoSchedule.display(fecha));
    AppTypography.caption(header);
    header.setTypeface(android.graphics.Typeface.create(
        "sans-serif-medium", android.graphics.Typeface.NORMAL));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = Ui.dp(this, AppSpacing.MD);
    params.bottomMargin = Ui.dp(this, AppSpacing.XS);
    header.setLayoutParams(params);
    return header;
  }

  private AppCard buildTurnoCard(TurnoDto turno) {
    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.HORIZONTAL);
    content.setGravity(Gravity.CENTER_VERTICAL);
    column.addView(content);

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
    estado.setText(TurnoLabels.estadoLabel(turno.estado));
    estado.setTextSize(13);
    estado.setTextColor(TurnoLabels.estadoColor(turno.estado));
    estado.setPadding(
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.XS),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.XS));
    content.addView(estado);

    if (TurnoLabels.PENDIENTE.equals(turno.estado) && turno.id != null) {
      column.addView(buildTurnoActions(turno));
    }

    AppCard card = new AppCard(this, column);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = Ui.dp(this, AppSpacing.SM);
    card.setLayoutParams(params);
    return card;
  }

  private String metaLine(TurnoDto turno) {
    return turno.sedeLabel() + " · " + turno.tipoLabel();
  }

  /** "Atender" (only for today or earlier) and "Cancelar" for a PENDIENTE turno. */
  private LinearLayout buildTurnoActions(TurnoDto turno) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.END);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = Ui.dp(this, AppSpacing.SM);
    row.setLayoutParams(rowParams);

    boolean busy = updating.contains(turno.id);
    String who = turno.pacienteLabel() + " · " + turno.horaCorta();

    AppButton cancelar = new AppButton(this, "Cancelar", AppButton.Style.TEXT,
        v -> confirmEstado(turno, TurnoLabels.CANCELADO));
    cancelar.setEnabled(!busy);
    cancelar.setContentDescription("Cancelar turno de " + who);
    row.addView(cancelar, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, A11y.MIN_TOUCH_DP)));

    // A future turno cannot have been attended yet.
    boolean canAttend = turno.fecha == null || turno.fecha.compareTo(TurnoSchedule.today()) <= 0;
    if (canAttend) {
      AppButton atender = new AppButton(this, busy ? "Guardando…" : "Atender",
          AppButton.Style.OUTLINED, v -> confirmEstado(turno, TurnoLabels.ATENDIDO));
      atender.setEnabled(!busy);
      atender.setContentDescription("Marcar como atendido el turno de " + who);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, A11y.MIN_TOUCH_DP));
      params.leftMargin = Ui.dp(this, AppSpacing.SM);
      row.addView(atender, params);
    }
    return row;
  }

  private void confirmEstado(TurnoDto turno, String estado) {
    if (turno.id == null || updating.contains(turno.id)) {
      return;
    }
    String when = (turno.fecha != null && !turno.fecha.equals(TurnoSchedule.today())
        ? TurnoSchedule.display(turno.fecha) + " " : "hoy ") + turno.horaCorta();
    boolean atender = TurnoLabels.ATENDIDO.equals(estado);
    String message = turno.pacienteLabel() + ", " + when + ".\n\n" + (atender
        ? "El turno queda como atendido. Si vas a registrar la donación, se marca solo."
        : "El turno queda cancelado y libera el horario. No se puede deshacer.");
    ConfirmDialog.show(this,
        atender ? "¿Marcar como atendido?" : "¿Cancelar el turno?",
        message,
        atender ? "Marcar atendido" : "Cancelar turno",
        "Volver",
        () -> updateEstado(turno, estado));
  }

  private void updateEstado(TurnoDto turno, String estado) {
    final long id = turno.id;
    if (!updating.add(id)) {
      return;
    }
    loadTurnosKeepingScroll();
    turnoRepository.updateEstado(id, estado, new ApiCallback<TurnoDto>() {
      @Override
      public void onSuccess(TurnoDto data) {
        updating.remove(id);
        if (!isAlive()) {
          return;
        }
        Toast.makeText(TurnosActivity.this, TurnoLabels.ATENDIDO.equals(estado)
            ? "Turno marcado como atendido" : "Turno cancelado", Toast.LENGTH_SHORT).show();
        loadTurnos();
      }

      @Override
      public void onValidationError(Map<String, String> fieldErrors) {
        updating.remove(id);
        if (!isAlive()) {
          return;
        }
        // 422 estado: the turno is no longer PENDIENTE (someone else changed it).
        String text = fieldErrors.containsKey("estado") ? fieldErrors.get("estado")
            : fieldErrors.values().iterator().next();
        Toast.makeText(TurnosActivity.this, text, Toast.LENGTH_LONG).show();
        loadTurnos();
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        updating.remove(id);
        if (!isAlive()) {
          return;
        }
        Toast.makeText(TurnosActivity.this, ApiMessages.forType(type, message,
            "No se pudo actualizar el turno", "No se pudo actualizar el turno"),
            Toast.LENGTH_LONG).show();
        loadTurnos();
      }
    });
  }

  /** Repaints the current list from cache so the in-flight card shows its busy state. */
  private void loadTurnosKeepingScroll() {
    List<TurnoDto> cached = turnoRepository.getCachedDesde(TurnoSchedule.today());
    if (cached != null && !cached.isEmpty() && createPanel.getVisibility() != View.VISIBLE) {
      renderTurnos(cached);
    }
  }

  // --- Alta de turno ---

  private void showCreatePanel() {
    listGeneration++;
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
    // Same window as the API: from today up to one year ahead.
    fechaField.setMaxYearsAhead(1);
    fechaField.setOnDateSelected((apiDate, displayDate) -> {
      selectedFecha = apiDate;
      checkHoraHoy();
    });
    root.addView(fechaField);

    horaField = new TimeField(this, "Hora", true);
    horaField.setOnTimeSelected(hora -> {
      selectedHora = hora;
      checkHoraHoy();
    });
    root.addView(horaField);

    formError = new ErrorText(this);
    root.addView(formError);

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
    pacienteRepository.findByDni(dni, new ApiCallback<PacienteDto>() {
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
    if (!isAlive()) {
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
    if (selectedFecha != null && selectedFecha.compareTo(TurnoSchedule.today()) < 0) {
      fechaField.error().show("La fecha del turno no puede ser anterior a hoy");
      valid = false;
    } else if (selectedFecha != null && TurnoSchedule.isBeyondMax(selectedFecha, TurnoSchedule.today())) {
      fechaField.error().show("La fecha del turno no puede ser posterior al "
          + TurnoSchedule.display(TurnoSchedule.maxFecha(TurnoSchedule.today())));
      valid = false;
    } else if (!checkHoraHoy()) {
      valid = false;
    }
    if (!valid) {
      return;
    }
    send(false, null);
  }

  /**
   * A time already past today stays blocked; the error suggests the next
   * valid slot. Returns {@code true} when the picked time is acceptable.
   */
  private boolean checkHoraHoy() {
    if (selectedFecha == null || selectedHora == null || !selectedFecha.equals(TurnoSchedule.today())) {
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

  /** Posts the turno; {@code forzar} resends after an accepted forcible 409. */
  private void send(boolean forzar, String motivo) {
    if (submitting || selectedPaciente == null || selectedPaciente.id == null
        || selectedFecha == null || selectedHora == null) {
      return;
    }
    formError.hide();
    setSubmitting(true);

    CreateTurnoRequest body = new CreateTurnoRequest(
        selectedPaciente.id, selectedFecha, selectedHora, null);
    if (forzar) {
      body.forzar = Boolean.TRUE;
      body.motivo = motivo;
    }
    turnoRepository.create(body, new WarningCallback<TurnoDto, TurnoWarningResponse>() {
      @Override
      public void onSuccess(TurnoDto data) {
        setSubmitting(false);
        if (!isAlive()) {
          return;
        }
        Toast.makeText(TurnosActivity.this, "Turno registrado", Toast.LENGTH_SHORT).show();
        resetForm();
        loadTurnos();
      }

      @Override
      public void onValidationError(Map<String, String> fieldErrors) {
        setSubmitting(false);
        if (!isAlive()) {
          return;
        }
        applyFieldErrors(fieldErrors);
      }

      @Override
      public void onWarning(TurnoWarningResponse warning) {
        setSubmitting(false);
        if (!isAlive()) {
          return;
        }
        handleWarning(warning);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        setSubmitting(false);
        if (!isAlive()) {
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

  /**
   * 409: forcible rules ask for an audited confirmation with motivo; hard
   * limits (slot taken, daily capacity) only explain why it cannot be booked.
   */
  private void handleWarning(TurnoWarningResponse warning) {
    List<String> messages = warning.messages();
    if (messages.isEmpty()) {
      messages.add("El turno no cumple las reglas de donación.");
    }
    if (!ForceOverride.allForcible(warning.codes())) {
      StringBuilder text = new StringBuilder();
      for (String message : messages) {
        text.append(text.length() > 0 ? "\n" : "").append(message);
      }
      formError.show(text);
      if (warning.codes().contains(ForceOverride.HORARIO_OCUPADO)) {
        horaField.error().show("Ese horario ya está ocupado. Elegí otra hora.");
      }
      ConfirmDialog.show(this, "No se puede agendar", text + "\n\nEste límite no se "
          + "puede forzar: elegí otro horario o fecha.", null, "Entendido", null);
      return;
    }
    ForceDialog.show(this, "¿Forzar el turno?", messages, "Forzar y agendar",
        motivo -> send(true, motivo));
  }

  /** Shows the backend's Spanish 422 message under each field. */
  private void applyFieldErrors(Map<String, String> errors) {
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
    if (formError != null) {
      formError.hide();
    }
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
