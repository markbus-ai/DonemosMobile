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
import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.WarningCallback;
import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.data.network.dto.TurnoWarningResponse;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.data.turnos.TurnoRepository;
import com.donemosmobile.ui.A11y;
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.TurnoLabels;
import com.donemosmobile.ui.TurnoSchedule;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.ConfirmDialog;
import com.donemosmobile.ui.components.ForceDialog;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Próximos turnos. Lista los turnos desde hoy ({@code desde}), deja atender o
 * cancelar los pendientes, y crear uno nuevo (paciente por DNI + fecha + hora).
 * Un 409 forzable pide confirmación con motivo; los límites duros bloquean.
 *
 * <p>Container: the list cards are {@link TurnoCardView}, the form is
 * {@link NuevoTurnoPanel} and the 409 decision is {@link TurnoWarningPresenter};
 * this activity keeps the requests and their callbacks.
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
  private NuevoTurnoPanel createPanel;

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

    createPanel = new NuevoTurnoPanel(this, new NuevoTurnoPanel.Listener() {
      @Override
      public void onSearchPaciente(String dni) {
        buscarPaciente(dni);
      }

      @Override
      public void onConfirm() {
        confirmar();
      }

      @Override
      public void onBackToList() {
        loadTurnos();
      }
    });
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
      listContainer.addView(new TurnoCardView(this, turno, updating.contains(turno.id),
          this::confirmEstado));
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

  private void buscarPaciente(String dni) {
    pendingDni = dni;
    createPanel.setSearching(true);
    pacienteRepository.findByDni(dni, new ApiCallback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        if (isStale(dni)) {
          return;
        }
        createPanel.setSearching(false);
        createPanel.showPaciente(data);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        if (isStale(dni)) {
          return;
        }
        createPanel.setSearching(false);
        createPanel.showPacienteError(type == ApiError.Type.NOT_FOUND
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

  private void confirmar() {
    if (submitting) {
      return;
    }
    createPanel.clearErrors();
    if (!createPanel.validate()) {
      return;
    }
    send(false, null);
  }

  /** Posts the turno; {@code forzar} resends after an accepted forcible 409. */
  private void send(boolean forzar, String motivo) {
    if (submitting || !createPanel.isComplete()) {
      return;
    }
    createPanel.hideFormError();
    setSubmitting(true);

    CreateTurnoRequest body = new CreateTurnoRequest(createPanel.selectedPaciente().id,
        createPanel.selectedFecha(), createPanel.selectedHora(), null);
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
        createPanel.applyFieldErrors(fieldErrors);
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

  /** Renders the {@link TurnoWarningPresenter} decision for a 409. */
  private void handleWarning(TurnoWarningResponse warning) {
    TurnoWarningPresenter.Decision decision = TurnoWarningPresenter.decide(warning);
    if (!decision.forcible) {
      createPanel.showFormError(decision.text);
      if (decision.horarioOcupado) {
        createPanel.showHorarioOcupado();
      }
      ConfirmDialog.show(this, "No se puede agendar", decision.text + "\n\nEste límite no se "
          + "puede forzar: elegí otro horario o fecha.", null, "Entendido", null);
      return;
    }
    ForceDialog.show(this, "¿Forzar el turno?", decision.messages, "Forzar y agendar",
        motivo -> send(true, motivo));
  }

  private void setSubmitting(boolean value) {
    submitting = value;
    if (createPanel != null) {
      createPanel.setSubmitting(value);
    }
  }

  private void resetForm() {
    pendingDni = null;
    createPanel.reset();
  }

  private String hoyLegible() {
    return new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "AR"))
        .format(new Date());
  }
}
