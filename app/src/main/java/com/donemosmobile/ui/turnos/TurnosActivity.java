package com.donemosmobile.ui.turnos;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.data.turnos.TurnoRepository;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
  private AppInput dniInput;
  private PacienteDto selectedPaciente;
  private TextView pacienteValue;
  private TextView dateValue;
  private TextView timeValue;
  private TextView pacienteError;
  private TextView fechaError;
  private TextView horaError;
  private String selectedFecha;
  private String selectedHora;

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

  private LinearLayout buildView() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    View header = buildHeader();
    root.addView(header, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);

    createPanel = buildCreateForm();
    createPanel.setVisibility(View.GONE);
    content.addView(createPanel);

    state = new StateView(this);
    content.addView(state);

    listContainer = new LinearLayout(this);
    listContainer.setOrientation(LinearLayout.VERTICAL);
    listContainer.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XL, getResources()));
    content.addView(listContainer);

    // Contenido scrolleable: deja la barra inferior siempre visible.
    ScrollView scroller = new ScrollView(this);
    scroller.setFillViewport(true);
    scroller.addView(content, new ScrollView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    root.addView(scroller, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

    root.addView(new SectionNavBar(this, Section.TURNOS), new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    return root;
  }

  private View buildHeader() {
    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);

    LinearLayout titles = new LinearLayout(this);
    titles.setOrientation(LinearLayout.VERTICAL);

    TextView title = new TextView(this);
    title.setText("Turnos de hoy");
    AppTypography.headline1(title);
    titles.addView(title);

    TextView subtitle = new TextView(this);
    subtitle.setText(hoyLegible());
    AppTypography.caption(subtitle);
    titles.addView(subtitle);

    header.addView(titles, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    TextView nuevo = new TextView(this);
    nuevo.setText("+ Nuevo");
    nuevo.setTextSize(15);
    nuevo.setTextColor(AppColors.PRIMARY);
    nuevo.setPadding(
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()));
    nuevo.setClickable(true);
    nuevo.setFocusable(true);
    nuevo.setOnClickListener(v -> showCreatePanel());
    header.addView(nuevo);

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
        state.showError(messageFor(type, message), TurnosActivity.this::loadTurnos);
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
        AppSpacing.dp(64, getResources()),
        ViewGroup.LayoutParams.WRAP_CONTENT));

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
    estado.setTextColor(estadoColor(turno.estado));
    estado.setPadding(
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()));
    content.addView(estado);

    AppCard card = new AppCard(this, content);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    card.setLayoutParams(params);
    return card;
  }

  private String metaLine(TurnoDto turno) {
    return turno.sedeLabel() + " · " + turno.tipoLabel();
  }

  private int estadoColor(String estado) {
    if (estado == null) {
      return AppColors.TEXT_SECONDARY;
    }
    switch (estado) {
      case "ATENDIDO":
        return AppColors.SUCCESS;
      case "CANCELADO":
        return AppColors.ERROR;
      case "PENDIENTE":
        return AppColors.WARNING;
      default:
        return AppColors.TEXT_SECONDARY;
    }
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
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);

    TextView title = new TextView(this);
    title.setText("Nuevo turno");
    AppTypography.headline2(title);
    root.addView(title);

    TextView help = new TextView(this);
    help.setText("Buscá al paciente por DNI y elegí fecha y hora.");
    AppTypography.caption(help);
    LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    helpParams.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    helpParams.bottomMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    root.addView(help, helpParams);

    dniInput = new AppInput(this, "DNI del paciente");
    dniInput.getEditText().setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    root.addView(dniInput);

    AppButton buscar = new AppButton(this, "Buscar paciente", AppButton.Style.OUTLINED,
        v -> buscarPaciente(dniInput.getText()));
    LinearLayout.LayoutParams buscarParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        AppSpacing.dp(44, getResources()));
    buscarParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    root.addView(buscar, buscarParams);

    pacienteValue = new TextView(this);
    pacienteValue.setText("Sin paciente seleccionado");
    pacienteValue.setTextSize(15);
    pacienteValue.setTextColor(AppColors.TEXT_SECONDARY);
    pacienteError = makeErrorText();
    addFieldBlock(root, makeField("Paciente", pacienteValue), pacienteError);

    dateValue = new TextView(this);
    LinearLayout dateField = makeField("Fecha", dateValue);
    dateField.setOnClickListener(v -> showDatePicker());
    fechaError = makeErrorText();
    addFieldBlock(root, dateField, fechaError);

    timeValue = new TextView(this);
    LinearLayout timeField = makeField("Hora", timeValue);
    timeField.setOnClickListener(v -> showTimePicker());
    horaError = makeErrorText();
    addFieldBlock(root, timeField, horaError);

    AppButton confirmar = new AppButton(this, "Confirmar turno", AppButton.Style.FILLED,
        v -> confirmar());
    LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        AppSpacing.dp(48, getResources()));
    confirmParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    root.addView(confirmar, confirmParams);

    AppButton cancelar = new AppButton(this, "Volver a la lista", AppButton.Style.TEXT,
        v -> loadTurnos());
    root.addView(cancelar);

    return root;
  }

  private void buscarPaciente(String dni) {
    dniInput.setError(null);
    if (dni.isEmpty()) {
      // Validación inline bajo el campo, no un Toast.
      dniInput.setError("Ingresá el DNI");
      return;
    }
    pacienteRepository.findByDni(dni, new PacienteRepository.Callback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        selectedPaciente = data;
        updatePacienteLabel(data.displayName());
        hideError(pacienteError);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        selectedPaciente = null;
        updatePacienteLabel("Sin paciente seleccionado");
        showError(pacienteError, messageFor(type, message));
      }
    });
  }

  private void updatePacienteLabel(String text) {
    if (pacienteValue == null) {
      return;
    }
    pacienteValue.setText(text);
    pacienteValue.setTextColor(
        text.startsWith("Sin ") ? AppColors.TEXT_SECONDARY : AppColors.TEXT_PRIMARY);
  }

  private void confirmar() {
    clearErrors();

    // Validación inline: cada error queda debajo de su campo, no en un Toast.
    boolean valid = true;
    if (selectedPaciente == null || selectedPaciente.id == null) {
      showError(pacienteError, "Seleccioná un paciente");
      valid = false;
    }
    if (selectedFecha == null) {
      showError(fechaError, "Elegí la fecha del turno");
      valid = false;
    }
    if (selectedHora == null) {
      showError(horaError, "Elegí la hora del turno");
      valid = false;
    }
    if (!valid) {
      return;
    }

    CreateTurnoRequest body = new CreateTurnoRequest(
        selectedPaciente.id, selectedFecha, selectedHora, null);
    turnoRepository.create(body, new TurnoRepository.Callback<TurnoDto>() {
      @Override
      public void onSuccess(TurnoDto data) {
        Toast.makeText(TurnosActivity.this, "Turno registrado", Toast.LENGTH_SHORT).show();
        resetForm();
        loadTurnos();
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        Toast.makeText(TurnosActivity.this, messageFor(type, message), Toast.LENGTH_LONG).show();
      }
    });
  }

  private void resetForm() {
    selectedPaciente = null;
    selectedFecha = null;
    selectedHora = null;
  }

  private void showDatePicker() {
    MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText("Fecha del turno")
        .build();
    picker.addOnPositiveButtonClickListener(selection -> {
      // Material devuelve millis en UTC: convertir con timezone UTC para no correr un día.
      SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
      fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
      dateValue.setText(fmt.format(new Date(selection)));
      dateValue.setTextColor(AppColors.TEXT_PRIMARY);
      hideError(fechaError);

      SimpleDateFormat api = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
      api.setTimeZone(TimeZone.getTimeZone("UTC"));
      selectedFecha = api.format(new Date(selection));
    });
    picker.show(getSupportFragmentManager(), "date_picker");
  }

  private void showTimePicker() {
    Calendar now = Calendar.getInstance();
    TimePickerDialog dialog = new TimePickerDialog(
        this,
        (view, hourOfDay, minute) -> {
          selectedHora = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
          timeValue.setText(selectedHora);
          timeValue.setTextColor(AppColors.TEXT_PRIMARY);
          hideError(horaError);
        },
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
        DateFormat.is24HourFormat(this));
    dialog.show();
  }

  // --- Helpers de UI ---

  private LinearLayout makeField(String label, TextView value) {
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

    value.setTextSize(15);
    value.setTextColor(AppColors.TEXT_SECONDARY);
    value.setTypeface(android.graphics.Typeface.create(
        "sans-serif-medium", android.graphics.Typeface.NORMAL));
    field.addView(value);

    return field;
  }

  /** TextView de error de campo, oculto hasta que haga falta. */
  private TextView makeErrorText() {
    TextView error = new TextView(this);
    error.setTextSize(13);
    error.setTextColor(AppColors.ERROR);
    error.setVisibility(View.GONE);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    params.leftMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    error.setLayoutParams(params);
    return error;
  }

  /** Apila un campo de 64dp con su mensaje de error debajo. */
  private void addFieldBlock(LinearLayout root, LinearLayout field, TextView error) {
    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    column.addView(field, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        AppSpacing.dp(64, getResources())));
    column.addView(error);

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    root.addView(column, params);
  }

  private void showError(TextView error, String message) {
    if (error == null) {
      return;
    }
    error.setText(message);
    error.setVisibility(View.VISIBLE);
  }

  private void hideError(TextView error) {
    if (error != null) {
      error.setVisibility(View.GONE);
    }
  }

  private void clearErrors() {
    if (dniInput != null) {
      dniInput.setError(null);
    }
    hideError(pacienteError);
    hideError(fechaError);
    hideError(horaError);
  }

  private android.graphics.drawable.GradientDrawable rounded(int color, float radiusDp) {
    android.graphics.drawable.GradientDrawable drawable =
        new android.graphics.drawable.GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(AppSpacing.dp((int) radiusDp, getResources()));
    return drawable;
  }

  /** Fecha de hoy en formato de API ({@code YYYY-MM-DD}). */
  private String hoy() {
    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
  }

  private String hoyLegible() {
    return new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "AR"))
        .format(new Date());
  }

  private String messageFor(ApiError.Type type, String message) {
    switch (type) {
      case UNAUTHORIZED:
        return "Sesión vencida, volvé a ingresar";
      case NETWORK:
        return "No se pudo conectar";
      case SERVER:
        return "Error del servidor, intentá de nuevo";
      case CLIENT:
        return message != null && !message.isEmpty()
            ? message : "No se pudo registrar el turno";
      default:
        return message != null && !message.isEmpty() ? message : "Ocurrió un error";
    }
  }
}
