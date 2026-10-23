package com.donemosmobile.ui.donaciones;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.WarningCallback;
import com.donemosmobile.AppActivity;
import com.donemosmobile.data.catalogo.CatalogoRepository;
import com.donemosmobile.data.donaciones.DonacionRepository;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.AutoexclusionDto;
import com.donemosmobile.data.network.dto.CreateDonacionRequest;
import com.donemosmobile.data.network.dto.DonacionDto;
import com.donemosmobile.data.network.dto.DonacionWarningResponse;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.TipoDonacionDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.NumericBounds;
import com.donemosmobile.ui.StaleResults;
import com.donemosmobile.ui.AptitudLabels;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.ForceDialog;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.components.PatientPicker;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
import com.donemosmobile.ui.components.SelectGroup;
import com.donemosmobile.ui.components.SelectableRow;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.components.StatusPanel;
import com.donemosmobile.ui.components.StepperField;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de una donacion. Una sola pantalla, en pasos: paciente por DNI ->
 * tipo de donacion -> componentes (cantidad) -> signos clinicos -> datos de la
 * bolsa -> ciclo manual (opcional) -> CONFIRMAR. Valida inline, muestra los
 * errores 422 por campo. Ante un 409 deja forzar con autorizacion auditada; al
 * confirmar (201) muestra el numero de donacion y permite marcar la
 * autoexclusion confidencial.
 *
 * <p>La posicion en el ciclo (ciclo / serie / donacion) la calcula el backend
 * al registrar la donacion; los tres campos son un override opcional para
 * casos especificos en los que el personal necesite fijarla a mano.
 *
 * <p>La seccion Donaciones y el tile "Registrar donacion" de la Home apuntan
 * aca: una sola implementacion para ambos puntos de entrada.
 */
public class DonacionesActivity extends AppActivity {

  /** Maximo de unidades por codigo de componente (ComponentRules del backend). */
  private static final Map<String, Integer> MAXIMOS = new LinkedHashMap<>();
  static {
    MAXIMOS.put("SANGRE", 1);
    MAXIMOS.put("PLASMA", 1);
    MAXIMOS.put("PLAQUETAS", 3);
  }

  private PacienteRepository pacienteRepository;
  private CatalogoRepository catalogoRepository;
  private DonacionRepository donacionRepository;

  private PatientPicker picker;
  private StateView state;

  private AppCard patientPanel;
  private TextView patientName;
  private TextView patientDni;
  private TextView patientAptitud;
  private TextView patientCycle;

  private SelectGroup tipoGroup;

  private LinearLayout compContainer;
  private TextView compHint;
  private ErrorText compError;
  private final Map<Long, Integer> cantidades = new LinkedHashMap<>();

  private LinearLayout clinicalHint;
  private AppInput hemoglobinaInput;
  private AppInput hematocritoInput;
  private AppInput plaquetasInput;
  private AppInput sistolicaInput;
  private AppInput diastolicaInput;
  private AppInput frecuenciaInput;
  private AppInput pesoInput;
  private ErrorText clinicalError;

  private SelectGroup bolsaGroup;
  private SelectGroup brazoGroup;
  private AppInput anticoagulanteInput;
  private AppInput loteInput;
  private AppInput tubuladuraInput;
  private AppInput dificultadInput;
  private CheckBox dobleEtiquetaCheck;
  private ErrorText bagError;

  private AppInput cicloInput;
  private AppInput serieInput;
  private AppInput donacionInput;
  private ErrorText cycleError;

  private AppButton submitButton;
  private ErrorText formError;

  private StatusPanel warningPanel;
  private LinearLayout warningList;
  private AppButton forzarButton;

  private StatusPanel successPanel;
  private TextView successNumber;
  private TextView successPosition;
  private TextView successNote;
  private AppButton autoexclusionButton;
  private AppButton anotherButton;

  private List<TipoDonacionDto> tipos;
  private boolean tiposLoading;
  private PacienteDto selectedPaciente;
  private TipoDonacionDto selectedTipo;
  private String selectedTipoBolsa = "doble";
  private String selectedBrazo = "izquierdo";
  private boolean submitting;
  /** Messages of the last 409, restated in the override confirmation. */
  private List<String> lastWarnings = new ArrayList<>();
  private static final String FORZAR_LABEL = "Forzar con autorización…";
  /** DNI of the latest lookup; responses for any other DNI are ignored. */
  private String pendingDni;
  private boolean autoexclusionInFlight;
  private boolean autoexclusionRegistered;
  private DonacionDto createdDonacion;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    pacienteRepository = new PacienteRepository(this);
    catalogoRepository = new CatalogoRepository(this);
    donacionRepository = new DonacionRepository(this);
    setContentView(buildView());
    loadTipos();
  }

  // --- Layout ---

  private View buildView() {
    ScreenScaffold root = new ScreenScaffold(this, Section.DONACIONES);
    root.setHeader(new ScreenHeader(this)
        .title("Registrar donación")
        .subtitle("Buscá al donante, elegí el tipo y confirmá la captura."));

    LinearLayout content = root.content();
    content.setPadding(
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.XL));

    buildLookup(content);
    buildPatientPanel(content);
    buildTipoSection(content);
    buildComponentesSection(content);
    buildClinicalSection(content);
    buildBagSection(content);
    buildCicloSection(content);
    buildActions(content);

    return root;
  }

  // --- Paso 1: paciente ---

  private void buildLookup(LinearLayout root) {
    picker = new PatientPicker(this, PatientPicker.Layout.ROW, "DNI", "Buscar",
        AppButton.Style.FILLED, true, true);
    picker.setListener(new PatientPicker.Listener() {
      @Override
      public void onSearch(String dni) {
        search(dni);
      }

      @Override
      public void onClear() {
        resetAll();
      }
    });
    root.addView(picker);

    state = picker.state();
  }

  /** Reintento desde el estado de error: relee el DNI y valida. */
  private void search() {
    String dni = picker.dniInput().getText();
    picker.dniInput().setError(null);
    if (dni.isEmpty()) {
      picker.dniInput().setError("Ingresá el DNI");
      return;
    }
    search(dni);
  }

  private void search(String dni) {
    hidePanels();
    state.showLoading();
    pendingDni = dni;
    picker.setSearching(true);

    pacienteRepository.findByDniWithAptitud(dni, new ApiCallback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        if (isStale(dni)) {
          return;
        }
        picker.setSearching(false);
        state.hide();
        selectedPaciente = data;
        showPatient(data);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        if (isStale(dni)) {
          return;
        }
        picker.setSearching(false);
        selectedPaciente = null;
        if (type == ApiError.Type.NOT_FOUND) {
          state.showEmpty("No existe un paciente con ese DNI");
          return;
        }
        if (type == ApiError.Type.UNAUTHORIZED) {
          state.showError("Sesión vencida, volvé a ingresar", null);
          return;
        }
        state.showError(
            ApiMessages.forType(type, message, "No se pudo buscar al donante.",
                "No se pudo completar la operación"),
            DonacionesActivity.this::search);
      }
    });
  }

  /** True when the activity is gone or a newer search replaced this one. */
  private boolean isStale(String dni) {
    return StaleResults.isStale(!isAlive(), dni, pendingDni);
  }

  /** True when the activity is gone or the selected patient is no longer {@code dni}. */
  private boolean isStaleSubmit(String dni) {
    return StaleResults.isStale(!isAlive(), dni,
        selectedPaciente != null ? selectedPaciente.dni : null);
  }

  private void buildPatientPanel(LinearLayout root) {
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);

    patientName = new TextView(this);
    AppTypography.headline2(patientName);
    content.addView(patientName);

    patientDni = new TextView(this);
    AppTypography.caption(patientDni);
    content.addView(patientDni);

    patientAptitud = new TextView(this);
    patientAptitud.setTextSize(14);
    patientAptitud.setTextColor(AppColors.TEXT_SECONDARY);
    content.addView(patientAptitud);

    patientCycle = new TextView(this);
    patientCycle.setTextSize(14);
    patientCycle.setTextColor(AppColors.TEXT_PRIMARY);
    content.addView(patientCycle, Ui.matchWrap(this, AppSpacing.XS));

    patientPanel = new AppCard(this, content);
    patientPanel.setVisibility(View.GONE);
    root.addView(patientPanel, Ui.matchWrap(this, AppSpacing.MD));
  }

  private void showPatient(PacienteDto paciente) {
    if (paciente == null) {
      return;
    }
    patientName.setText(paciente.displayName());
    patientDni.setText("DNI " + Ui.safe(paciente.dni));
    patientAptitud.setText("Aptitud: " + AptitudLabels.current(paciente));
    patientCycle.setText(currentPositionText(
        paciente.cicloActual, paciente.serieActual, paciente.donacionActual));
    patientPanel.setVisibility(View.VISIBLE);
  }

  /**
   * Posicion actual del donante para el panel del paciente. Contadores nulos
   * significan que no hay posición de ciclo registrada, no que no haya
   * donaciones: pueden existir donaciones previas sin ciclo.
   */
  private String currentPositionText(Integer ciclo, Integer serie, Integer donacion) {
    if (ciclo == null && serie == null && donacion == null) {
      return "Sin posición de ciclo registrada (la próxima será 1·1·1)";
    }
    return "Ciclo actual: " + cycleValue(ciclo) + " · Serie " + cycleValue(serie)
        + " · Donación " + cycleValue(donacion);
  }

  /** Posicion resultante devuelta por el 201, para el panel de exito. */
  private String resultPositionText(Integer ciclo, Integer serie, Integer donacion) {
    if (ciclo == null && serie == null && donacion == null) {
      return null;
    }
    return "Ciclo " + cycleValue(ciclo) + " · Serie " + cycleValue(serie)
        + " · Donación " + cycleValue(donacion);
  }

  private String cycleValue(Integer value) {
    return value != null ? String.valueOf(value) : "—";
  }

  // --- Paso 2: tipo de donacion ---

  private void buildTipoSection(LinearLayout root) {
    Ui.sectionTitle(root, this, "Tipo de donación", AppSpacing.LG);

    tipoGroup = new SelectGroup(this);
    tipoGroup.status().setOnClickListener(v -> loadTipos());
    root.addView(tipoGroup);
  }

  private void loadTipos() {
    if (tiposLoading) {
      return;
    }
    List<TipoDonacionDto> cached = catalogoRepository.getCachedTiposDonacion();
    if (cached != null) {
      tipos = cached;
      tiposLoading = false;
      renderTipos();
      return;
    }
    tipos = null;
    tiposLoading = true;
    renderTipos();
    catalogoRepository.listTiposDonacion(new ApiCallback<List<TipoDonacionDto>>() {
      @Override
      public void onSuccess(List<TipoDonacionDto> data) {
        if (!isAlive()) {
          return;
        }
        tipos = data;
        tiposLoading = false;
        renderTipos();
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        if (!isAlive()) {
          return;
        }
        tiposLoading = false;
        renderTipos();
      }
    });
  }

  private void renderTipos() {
    if (tipoGroup == null) {
      return;
    }
    tipoGroup.options().removeAllViews();
    TextView status = tipoGroup.status();
    if (tiposLoading) {
      status.setText("Cargando tipos de donación…");
      status.setTextColor(AppColors.TEXT_SECONDARY);
      status.setVisibility(View.VISIBLE);
      return;
    }
    if (tipos == null) {
      status.setText("No se pudieron cargar los tipos. Tocá para reintentar.");
      status.setTextColor(AppColors.ERROR);
      status.setVisibility(View.VISIBLE);
      return;
    }
    status.setVisibility(View.GONE);
    for (final TipoDonacionDto tipo : tipos) {
      String label = tipo.displayName() + (tipo.esAferesis ? " · aféresis" : "");
      tipoGroup.options().addView(new SelectableRow(this, label, isSelectedTipo(tipo),
          v -> selectTipo(tipo)));
    }
    renderComponentes();
  }

  private boolean isSelectedTipo(TipoDonacionDto tipo) {
    return selectedTipo != null && selectedTipo.id != null && selectedTipo.id.equals(tipo.id);
  }

  private void selectTipo(TipoDonacionDto tipo) {
    selectedTipo = tipo;
    tipoGroup.error().hide();
    // Preselecciona el componente que corresponde si aun no hay unidades.
    if (totalComponentes() == 0 && tipo.id != null && MAXIMOS.containsKey(tipo.codigoUpper())) {
      cantidades.put(tipo.id, 1);
    }
    renderTipos();
    renderClinicalHint();
  }

  // --- Paso 3: componentes ---

  private void buildComponentesSection(LinearLayout root) {
    Ui.sectionTitle(root, this, "Componentes", AppSpacing.LG);

    compHint = new TextView(this);
    compHint.setText("SANGRE máx. 1 · PLASMA máx. 1 · PLAQUETAS máx. 3");
    AppTypography.caption(compHint);
    root.addView(compHint);

    compContainer = new LinearLayout(this);
    compContainer.setOrientation(LinearLayout.VERTICAL);
    root.addView(compContainer);

    compError = new ErrorText(this);
    root.addView(compError);
  }

  private void renderComponentes() {
    if (compContainer == null) {
      return;
    }
    compContainer.removeAllViews();
    if (tipos == null) {
      return;
    }
    for (final TipoDonacionDto tipo : tipos) {
      if (tipo.id == null || !MAXIMOS.containsKey(tipo.codigoUpper())) {
        continue;
      }
      StepperField stepper = new StepperField(this, tipo.displayName(), cantidad(tipo),
          MAXIMOS.getOrDefault(tipo.codigoUpper(), 1));
      stepper.setOnChange(value -> {
        cantidades.put(tipo.id, value);
        compError.hide();
      });
      compContainer.addView(stepper);
    }
  }

  private int cantidad(TipoDonacionDto tipo) {
    Integer value = tipo.id != null ? cantidades.get(tipo.id) : null;
    return value != null ? value : 0;
  }

  private int totalComponentes() {
    int total = 0;
    for (Integer value : cantidades.values()) {
      if (value != null) {
        total += value;
      }
    }
    return total;
  }

  // --- Paso 4: signos clinicos ---

  private void buildClinicalSection(LinearLayout root) {
    Ui.sectionTitle(root, this, "Signos clínicos", AppSpacing.LG);

    clinicalHint = new LinearLayout(this);
    clinicalHint.setOrientation(LinearLayout.VERTICAL);
    TextView hint = new TextView(this);
    hint.setText("Donación por aféresis: plaquetas y hematocrito son obligatorios.");
    hint.setTextSize(13);
    hint.setTextColor(AppColors.WARNING_TEXT);
    clinicalHint.addView(hint);
    clinicalHint.setVisibility(View.GONE);
    root.addView(clinicalHint);

    hemoglobinaInput = Ui.addInput(root, this, "Hemoglobina (g/dL)", Ui.decimalNumber(),
        AppSpacing.SM);
    hematocritoInput = Ui.addInput(root, this, "Hematocrito (%)", Ui.decimalNumber(),
        AppSpacing.SM);
    plaquetasInput = Ui.addInput(root, this, "Plaquetas (/mm³)", Ui.wholeNumber(), AppSpacing.SM);
    sistolicaInput = Ui.addInput(root, this, "Presión sistólica", Ui.wholeNumber(), AppSpacing.SM);
    diastolicaInput = Ui.addInput(root, this, "Presión diastólica", Ui.wholeNumber(),
        AppSpacing.SM);
    frecuenciaInput = Ui.addInput(root, this, "Frecuencia cardíaca", Ui.wholeNumber(),
        AppSpacing.SM);
    pesoInput = Ui.addInput(root, this, "Peso del donante (kg)", Ui.decimalNumber(), AppSpacing.SM);

    clinicalError = new ErrorText(this);
    root.addView(clinicalError);
  }

  private void renderClinicalHint() {
    if (clinicalHint == null) {
      return;
    }
    clinicalHint.setVisibility(
        selectedTipo != null && selectedTipo.esAferesis ? View.VISIBLE : View.GONE);
  }

  // --- Paso 5: datos de la bolsa ---

  private void buildBagSection(LinearLayout root) {
    Ui.sectionTitle(root, this, "Datos de la bolsa", AppSpacing.LG);

    bolsaGroup = new SelectGroup(this, "Tipo de bolsa", 0);
    root.addView(bolsaGroup);

    brazoGroup = new SelectGroup(this, "Brazo", AppSpacing.SM);
    root.addView(brazoGroup);

    anticoagulanteInput = Ui.addInput(root, this, "Anticoagulante", InputType.TYPE_CLASS_TEXT,
        AppSpacing.SM);
    loteInput = Ui.addInput(root, this, "Lote", InputType.TYPE_CLASS_TEXT, AppSpacing.SM);
    tubuladuraInput = Ui.addInput(root, this, "Tubuladura", InputType.TYPE_CLASS_TEXT,
        AppSpacing.SM);
    dificultadInput = Ui.addInput(root, this, "Dificultad", InputType.TYPE_CLASS_TEXT,
        AppSpacing.SM);

    dobleEtiquetaCheck = new CheckBox(this);
    dobleEtiquetaCheck.setText("Doble etiqueta");
    dobleEtiquetaCheck.setTextColor(AppColors.TEXT_PRIMARY);
    dobleEtiquetaCheck.setTextSize(14);
    root.addView(dobleEtiquetaCheck, Ui.matchWrap(this, AppSpacing.SM));

    bagError = new ErrorText(this);
    root.addView(bagError);

    renderBolsa();
    renderBrazo();
  }

  private void renderBolsa() {
    if (bolsaGroup == null) {
      return;
    }
    bolsaGroup.options().removeAllViews();
    for (final String value : new String[] {"doble", "triple", "cuadruple", "quintuple"}) {
      bolsaGroup.options().addView(new SelectableRow(this, value, value.equals(selectedTipoBolsa),
          v -> {
            selectedTipoBolsa = value;
            bagError.hide();
            renderBolsa();
          }));
    }
  }

  private void renderBrazo() {
    if (brazoGroup == null) {
      return;
    }
    brazoGroup.options().removeAllViews();
    for (final String value : new String[] {"izquierdo", "derecho"}) {
      brazoGroup.options().addView(new SelectableRow(this, value, value.equals(selectedBrazo),
          v -> {
            selectedBrazo = value;
            bagError.hide();
            renderBrazo();
          }));
    }
  }

  // --- Paso 6: ciclo (calculado, con override opcional) ---

  private void buildCicloSection(LinearLayout root) {
    Ui.sectionTitle(root, this, "Ciclo de donaciones", AppSpacing.LG);

    TextView helper = new TextView(this);
    helper.setText("La posición en el ciclo (ciclo > serie > donación) se calcula "
        + "automáticamente al registrar la donación: 5 donaciones por serie y 3 series "
        + "por ciclo. Dejá los campos vacíos para que la calcule el sistema; completalos "
        + "solo si necesitás fijar la posición manualmente.");
    AppTypography.caption(helper);
    root.addView(helper);

    cicloInput = Ui.addInput(root, this, "Ciclo", Ui.wholeNumber(), AppSpacing.SM);
    serieInput = Ui.addInput(root, this, "Serie", Ui.wholeNumber(), AppSpacing.SM);
    donacionInput = Ui.addInput(root, this, "Donación (orden dentro de la serie)",
        Ui.wholeNumber(), AppSpacing.SM);

    cycleError = new ErrorText(this);
    root.addView(cycleError);
  }

  // --- Paso 7: confirmar + paneles de resultado ---

  private void buildActions(LinearLayout root) {
    formError = new ErrorText(this);
    root.addView(formError);

    warningPanel = new StatusPanel(this, "La donación no está habilitada",
        AppColors.PRIMARY_TINT);
    warningList = new LinearLayout(this);
    warningList.setOrientation(LinearLayout.VERTICAL);
    warningPanel.body().addView(warningList);

    TextView warningNote = new TextView(this);
    warningNote.setText("Forzar deja registrada una autorización extraordinaria "
        + "auditada con tu usuario y un motivo obligatorio.");
    warningNote.setTextSize(12);
    warningNote.setTextColor(AppColors.TEXT_SECONDARY);
    warningPanel.body().addView(warningNote, Ui.matchWrap(this, AppSpacing.SM));

    // Outlined, not filled: overriding the rules is the exception, not the next step.
    forzarButton = warningPanel.addAction(FORZAR_LABEL, AppButton.Style.OUTLINED,
        v -> confirmForce(), AppSpacing.SM);
    root.addView(warningPanel, Ui.matchWrap(this, AppSpacing.MD));

    successPanel = new StatusPanel(this, "Donación registrada", AppColors.SURFACE);

    successNumber = new TextView(this);
    successNumber.setTextSize(18);
    successNumber.setTextColor(AppColors.TEXT_ACCENT);
    successNumber.setTypeface(android.graphics.Typeface.create(
        "sans-serif-medium", android.graphics.Typeface.NORMAL));
    successPanel.body().addView(successNumber, Ui.matchWrap(this, AppSpacing.XS));

    successPosition = new TextView(this);
    successPosition.setTextSize(14);
    successPosition.setTextColor(AppColors.TEXT_PRIMARY);
    successPanel.body().addView(successPosition, Ui.matchWrap(this, AppSpacing.XS));

    successNote = new TextView(this);
    successNote.setTextSize(13);
    successNote.setTextColor(AppColors.SUCCESS_TEXT);
    successNote.setVisibility(View.GONE);
    successPanel.body().addView(successNote);

    autoexclusionButton = successPanel.addAction("Marcar autoexclusión", AppButton.Style.OUTLINED,
        v -> promptAutoexclusion(), AppSpacing.SM);
    anotherButton = successPanel.addAction("Registrar otra donación", AppButton.Style.TEXT,
        v -> resetAll(), 0);
    root.addView(successPanel, Ui.matchWrap(this, AppSpacing.MD));

    submitButton = new AppButton(this, "CONFIRMAR", AppButton.Style.FILLED, v -> submit(false));
    LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
    submitParams.topMargin = Ui.dp(this, AppSpacing.MD);
    root.addView(submitButton, submitParams);
  }

  // --- Submit ---

  private void submit(boolean forzar) {
    submit(forzar, null);
  }

  /** Opens the audited override confirmation for the last 409 warnings. */
  private void confirmForce() {
    if (submitting || lastWarnings.isEmpty()) {
      return;
    }
    // Same input checks as a normal submit: forcing never skips validate().
    clearErrors();
    if (!validate()) {
      return;
    }
    ForceDialog.show(this, "¿Forzar la donación?", lastWarnings, "Forzar y registrar",
        motivo -> submit(true, motivo));
  }

  private void submit(boolean forzar, String motivo) {
    if (submitting) {
      return;
    }
    clearErrors();
    hideWarning();
    successPanel.hide();
    // Forcing only skips the business warnings, never the input validation.
    if (!validate()) {
      return;
    }
    setSubmitting(true);
    state.hide();
    // Bind the result to the patient it was sent for.
    final String submittedDni = selectedPaciente.dni;

    donacionRepository.create(submittedDni, buildRequest(forzar, motivo),
        new WarningCallback<DonacionDto, DonacionWarningResponse>() {
      @Override
      public void onSuccess(DonacionDto data) {
        setSubmitting(false);
        if (isStaleSubmit(submittedDni)) {
          return;
        }
        createdDonacion = data;
        showSuccess(data);
      }

      @Override
      public void onWarning(DonacionWarningResponse warnings) {
        setSubmitting(false);
        if (isStaleSubmit(submittedDni)) {
          return;
        }
        showWarning(warnings);
      }

      @Override
      public void onValidationError(Map<String, String> fieldErrors) {
        setSubmitting(false);
        if (isStaleSubmit(submittedDni)) {
          return;
        }
        applyFieldErrors(fieldErrors);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        setSubmitting(false);
        if (isStaleSubmit(submittedDni)) {
          return;
        }
        formError.show(
            ApiMessages.forType(type, message, "No se pudo registrar la donación",
                "No se pudo completar la operación"));
      }
    });
  }

  private CreateDonacionRequest buildRequest(boolean forzar, String motivo) {
    CreateDonacionRequest body = new CreateDonacionRequest();
    body.pacienteId = selectedPaciente != null ? selectedPaciente.id : null;
    body.tipoId = selectedTipo != null ? selectedTipo.id : null;
    body.tipoBolsa = selectedTipoBolsa;
    body.brazo = selectedBrazo;
    body.anticoagulante = Ui.blankToNull(anticoagulanteInput.getText());
    body.lote = Ui.blankToNull(loteInput.getText());
    body.tubuladura = Ui.blankToNull(tubuladuraInput.getText());
    body.dificultad = Ui.blankToNull(dificultadInput.getText());
    body.dobleEtiqueta = dobleEtiquetaCheck.isChecked();

    body.hemoglobina = Ui.parseDoubleOrNull(hemoglobinaInput.getText());
    body.hematocrito = Ui.parseDoubleOrNull(hematocritoInput.getText());
    body.plaquetas = Ui.parseIntOrNull(plaquetasInput.getText());
    body.presionSistolica = Ui.parseIntOrNull(sistolicaInput.getText());
    body.presionDiastolica = Ui.parseIntOrNull(diastolicaInput.getText());
    body.frecuenciaCardiaca = Ui.parseIntOrNull(frecuenciaInput.getText());
    body.pesoDonante = Ui.parseDoubleOrNull(pesoInput.getText());

    // Override opcional de la posicion: nulo cuando el campo queda vacío (Gson
    // lo omite). Solo los tres juntos cuentan como override; si falta alguno, el
    // backend calcula la posicion.
    body.ciclo = Ui.parseIntOrNull(cicloInput.getText());
    body.serie = Ui.parseIntOrNull(serieInput.getText());
    body.donacion = Ui.parseIntOrNull(donacionInput.getText());

    body.componentes = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
      Integer qty = entry.getValue();
      if (qty == null || qty <= 0 || entry.getKey() == null) {
        continue;
      }
      for (int i = 0; i < qty; i++) {
        body.componentes.add(new CreateDonacionRequest.Componente(entry.getKey()));
      }
    }

    body.forzar = forzar ? Boolean.TRUE : null;
    // The API requires motivo (5..500) whenever forzar is true; it lands in the audit.
    body.motivo = forzar ? motivo : null;
    return body;
  }

  private boolean validate() {
    boolean ok = true;
    if (selectedPaciente == null || selectedPaciente.id == null) {
      state.showError("Buscá un paciente primero", this::search);
      return false;
    }
    if (selectedTipo == null || selectedTipo.id == null) {
      tipoGroup.error().show("Elegí el tipo de donación");
      ok = false;
    }
    if (totalComponentes() < 1) {
      compError.show("Agregá al menos un componente");
      ok = false;
    }
    boolean clinicalOk = checkBounds(hemoglobinaInput, NumericBounds.HEMOGLOBINA);
    clinicalOk &= checkBounds(hematocritoInput, NumericBounds.HEMATOCRITO);
    clinicalOk &= checkBounds(plaquetasInput, NumericBounds.PLAQUETAS);
    clinicalOk &= checkBounds(sistolicaInput, NumericBounds.SISTOLICA);
    clinicalOk &= checkBounds(diastolicaInput, NumericBounds.DIASTOLICA);
    clinicalOk &= checkBounds(frecuenciaInput, NumericBounds.FRECUENCIA);
    clinicalOk &= checkBounds(pesoInput, NumericBounds.PESO);
    if (!clinicalOk) {
      clinicalError.show("Revisá los valores clínicos marcados");
      ok = false;
    }

    boolean aferesis = selectedTipo != null && selectedTipo.esAferesis;
    if (aferesis) {
      if (Ui.parseDoubleOrNull(plaquetasInput.getText()) == null
          || Ui.parseDoubleOrNull(hematocritoInput.getText()) == null) {
        clinicalError.show("Aféresis: cargá plaquetas y hematocrito");
        ok = false;
      }
    }

    Integer sistolica = Ui.parseIntOrNull(sistolicaInput.getText());
    Integer diastolica = Ui.parseIntOrNull(diastolicaInput.getText());
    String presionError = NumericBounds.checkPresion(sistolica, diastolica);
    if (presionError != null) {
      diastolicaInput.setError(presionError);
      clinicalError.show(presionError);
      ok = false;
    }

    String cycleProblem = NumericBounds.checkCycle(cicloInput.getText(), serieInput.getText(),
        donacionInput.getText());
    if (cycleProblem != null) {
      cycleError.show(cycleProblem);
      ok = false;
    }

    if (!ok && clinicalError.getVisibility() != View.VISIBLE
        && compError.getVisibility() != View.VISIBLE
        && cycleError.getVisibility() != View.VISIBLE
        && tipoGroup.error().getVisibility() != View.VISIBLE) {
      formError.show("Revisá los datos antes de confirmar");
    }
    return ok;
  }

  /** Shows the Spanish bounds error on the field itself; true when valid. */
  private boolean checkBounds(AppInput input, NumericBounds.Range range) {
    String error = NumericBounds.check(range, input.getText());
    input.setError(error);
    return error == null;
  }

  // --- 409 / 201 ---

  private void showWarning(DonacionWarningResponse warnings) {
    warningList.removeAllViews();
    lastWarnings = warnings.messages();
    forzarButton.setVisibility(lastWarnings.isEmpty() ? View.GONE : View.VISIBLE);
    for (String message : lastWarnings) {
      TextView item = new TextView(this);
      item.setText("• " + message);
      item.setTextSize(14);
      item.setTextColor(AppColors.TEXT_PRIMARY);
      warningList.addView(item, Ui.matchWrap(this, AppSpacing.XS));
    }
    warningPanel.show();
  }

  private void hideWarning() {
    warningPanel.hide();
  }

  private void showSuccess(DonacionDto donacion) {
    autoexclusionRegistered = false;
    String numero = donacion != null && donacion.numeroDonacion != null
        ? donacion.numeroDonacion : "—";
    successNumber.setText("Nº " + numero);

    Integer ciclo = donacion != null ? donacion.ciclo : null;
    Integer serie = donacion != null ? donacion.serie : null;
    Integer donacionNum = donacion != null ? donacion.donacion : null;
    String position = resultPositionText(ciclo, serie, donacionNum);
    if (position != null) {
      successPosition.setText(position);
      successPosition.setVisibility(View.VISIBLE);
      // La posicion ya quedo sincronizada en el paciente: reflejarla arriba.
      if (patientCycle != null) {
        patientCycle.setText(currentPositionText(ciclo, serie, donacionNum));
      }
    } else {
      successPosition.setVisibility(View.GONE);
    }

    successNote.setVisibility(View.GONE);
    autoexclusionButton.setEnabled(true);
    autoexclusionButton.setVisibility(View.VISIBLE);
    successPanel.show();
    submitButton.setVisibility(View.GONE);
    hideWarning();
    Toast.makeText(this, "Donación registrada", Toast.LENGTH_SHORT).show();
  }

  private void promptAutoexclusion() {
    if (createdDonacion == null || createdDonacion.numeroDonacion == null) {
      return;
    }
    final EditText motivo = new EditText(this);
    motivo.setHint("Motivo (opcional)");
    motivo.setInputType(InputType.TYPE_CLASS_TEXT);
    // Same cap as the API column (varchar 255).
    motivo.setFilters(new InputFilter[] {new InputFilter.LengthFilter(255)});
    new AlertDialog.Builder(this)
        .setTitle("Marcar autoexclusión")
        .setMessage("Registro confidencial del donante. No se guarda su identidad.")
        .setView(motivo)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Confirmar", (dialog, which) ->
            registerAutoexclusion(motivo.getText().toString()))
        .show();
  }

  private void registerAutoexclusion(String motivo) {
    if (autoexclusionRegistered || autoexclusionInFlight) {
      return;
    }
    autoexclusionInFlight = true;
    autoexclusionButton.setEnabled(false);
    donacionRepository.marcarAutoexclusion(createdDonacion.numeroDonacion, motivo,
        new ApiCallback<AutoexclusionDto>() {
          @Override
          public void onSuccess(AutoexclusionDto data) {
            autoexclusionInFlight = false;
            autoexclusionRegistered = true;
            if (!isAlive()) {
              return;
            }
            successNote.setText("Autoexclusión registrada y confidencial.");
            successNote.setVisibility(View.VISIBLE);
            Toast.makeText(DonacionesActivity.this, "Autoexclusión registrada",
                Toast.LENGTH_SHORT).show();
          }

          @Override
          public void onValidationError(Map<String, String> fieldErrors) {
            autoexclusionInFlight = false;
            if (!isAlive()) {
              return;
            }
            autoexclusionButton.setEnabled(true);
            formError.show(autoexclusionFieldMessage(fieldErrors));
          }

          @Override
          public void onError(ApiError.Type type, String message) {
            autoexclusionInFlight = false;
            if (!isAlive()) {
              return;
            }
            autoexclusionButton.setEnabled(true);
            formError.show(
                ApiMessages.forType(type, message, "No se pudo registrar la autoexclusión.",
                    "No se pudo completar la operación"));
          }
        });
  }

  /** Spanish copy for the autoexclusion 422 keys; Laravel text may be English. */
  private String autoexclusionFieldMessage(Map<String, String> fieldErrors) {
    if (fieldErrors.containsKey("motivo")) {
      return "El motivo no puede superar los 255 caracteres";
    }
    if (fieldErrors.containsKey("numero") || fieldErrors.containsKey("donacion")
        || fieldErrors.containsKey("autoexclusion")) {
      return "La donación ya tiene una autoexclusión registrada";
    }
    String first = fieldErrors.values().iterator().hasNext()
        ? fieldErrors.values().iterator().next() : null;
    return first != null && !first.isEmpty() ? first : "No se pudo registrar la autoexclusión";
  }

  // --- Reset y helpers ---

  private void resetAll() {
    // Drop any lookup still in flight so its late response cannot repaint.
    pendingDni = null;
    picker.setSearching(false);
    picker.dniInput().getEditText().setText("");
    picker.dniInput().setError(null);
    selectedPaciente = null;
    selectedTipo = null;
    createdDonacion = null;
    autoexclusionRegistered = false;
    cantidades.clear();
    selectedTipoBolsa = "doble";
    selectedBrazo = "izquierdo";
    clearClinicalInputs();
    clearCycleInputs();
    if (dobleEtiquetaCheck != null) {
      dobleEtiquetaCheck.setChecked(false);
    }
    clearErrors();
    hideWarning();
    hidePanels();
    renderTipos();
    renderComponentes();
    renderClinicalHint();
    renderBolsa();
    renderBrazo();
    state.hide();
  }

  private void clearClinicalInputs() {
    clearInput(hemoglobinaInput);
    clearInput(hematocritoInput);
    clearInput(plaquetasInput);
    clearInput(sistolicaInput);
    clearInput(diastolicaInput);
    clearInput(frecuenciaInput);
    clearInput(pesoInput);
  }

  private void clearCycleInputs() {
    clearInput(cicloInput);
    clearInput(serieInput);
    clearInput(donacionInput);
  }

  private void clearInput(AppInput input) {
    if (input != null) {
      input.getEditText().setText("");
      input.setError(null);
    }
  }

  private void hidePanels() {
    if (patientPanel != null) {
      patientPanel.setVisibility(View.GONE);
    }
    if (warningPanel != null) {
      warningPanel.hide();
    }
    if (successPanel != null) {
      successPanel.hide();
    }
    if (submitButton != null) {
      submitButton.setVisibility(View.VISIBLE);
    }
  }

  private void setSubmitting(boolean value) {
    submitting = value;
    if (picker != null) {
      picker.setLocked(value);
    }
    if (submitButton != null) {
      submitButton.setEnabled(!value);
      submitButton.setText(value ? "Guardando…" : "CONFIRMAR");
    }
    if (forzarButton != null) {
      forzarButton.setEnabled(!value);
      forzarButton.setText(value ? "Guardando…" : FORZAR_LABEL);
    }
  }

  private void applyFieldErrors(Map<String, String> errors) {
    boolean handled = false;
    if (errors.containsKey("paciente_id")) {
      state.showError("Elegí un paciente válido", this::search);
      handled = true;
    }
    if (errors.containsKey("tipo_id")) {
      tipoGroup.error().show("Elegí un tipo de donación válido");
      handled = true;
    }
    if (errors.containsKey("componentes")) {
      compError.show(errors.get("componentes"));
      handled = true;
    } else {
      for (String key : errors.keySet()) {
        if (key.startsWith("componentes.")) {
          compError.show(errors.get(key));
          handled = true;
          break;
        }
      }
    }
    if (errors.containsKey("motivo")) {
      // Backend copy for the audited override motivo (Spanish, 5..500).
      formError.show(errors.get("motivo"));
      handled = true;
    }
    if (errors.containsKey("signos_clinicos")) {
      clinicalError.show(errors.get("signos_clinicos"));
      handled = true;
    }
    if (errors.containsKey("ciclo") || errors.containsKey("serie")
        || errors.containsKey("donacion")) {
      String message = errors.containsKey("ciclo") ? errors.get("ciclo")
          : errors.containsKey("serie") ? errors.get("serie") : errors.get("donacion");
      cycleError.show(message);
      handled = true;
    }
    for (String key : new String[] {"tipo_bolsa", "anticoagulante", "lote",
        "tubuladura", "brazo", "dificultad", "doble_etiqueta"}) {
      if (errors.containsKey(key)) {
        bagError.show(errors.get(key));
        handled = true;
        break;
      }
    }
    if (!handled) {
      String first = errors.values().iterator().hasNext()
          ? errors.values().iterator().next() : null;
      formError.show(first != null && !first.isEmpty()
          ? first : "No se pudo registrar la donación");
    }
  }

  private void clearErrors() {
    for (AppInput input : new AppInput[] {hemoglobinaInput, hematocritoInput, plaquetasInput,
        sistolicaInput, diastolicaInput, frecuenciaInput, pesoInput}) {
      if (input != null) {
        input.setError(null);
      }
    }
    tipoGroup.error().hide();
    compError.hide();
    clinicalError.hide();
    bagError.hide();
    cycleError.hide();
    formError.hide();
  }
}
