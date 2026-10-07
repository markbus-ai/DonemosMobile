package com.donemosmobile.ui.donaciones;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.catalogo.CatalogoRepository;
import com.donemosmobile.data.donaciones.DonacionRepository;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.CreateDonacionRequest;
import com.donemosmobile.data.network.dto.DonacionDto;
import com.donemosmobile.data.network.dto.DonacionWarningResponse;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.TipoDonacionDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.components.StateView;
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
 * bolsa -> CONFIRMAR. Valida inline, muestra los errores 422 por campo y los
 * warnings de reglas de negocio (409).
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

  private AppInput dniInput;
  private StateView state;

  private AppCard patientPanel;
  private TextView patientName;
  private TextView patientDni;
  private TextView patientAptitud;

  private LinearLayout tipoContainer;
  private TextView tipoStatus;
  private TextView tipoError;

  private LinearLayout compContainer;
  private TextView compHint;
  private TextView compError;
  private final Map<Long, Integer> cantidades = new LinkedHashMap<>();
  private final Map<Long, TextView> qtyViews = new LinkedHashMap<>();

  private LinearLayout clinicalHint;
  private AppInput hemoglobinaInput;
  private AppInput hematocritoInput;
  private AppInput plaquetasInput;
  private AppInput sistolicaInput;
  private AppInput diastolicaInput;
  private AppInput frecuenciaInput;
  private AppInput pesoInput;
  private TextView clinicalError;

  private LinearLayout bolsaContainer;
  private LinearLayout brazoContainer;
  private AppInput anticoagulanteInput;
  private AppInput loteInput;
  private AppInput tubuladuraInput;
  private AppInput dificultadInput;
  private CheckBox dobleEtiquetaCheck;
  private TextView bagError;

  private AppButton submitButton;
  private TextView formError;

  private LinearLayout warningPanel;
  private LinearLayout warningList;

  private LinearLayout successPanel;
  private TextView successNumber;
  private AppButton anotherButton;

  private List<TipoDonacionDto> tipos;
  private boolean tiposLoading;
  private PacienteDto selectedPaciente;
  private TipoDonacionDto selectedTipo;
  private String selectedTipoBolsa = "doble";
  private String selectedBrazo = "izquierdo";
  private boolean submitting;
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

  private LinearLayout buildView() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    root.addView(buildHeader(), new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XL, getResources()));

    buildLookup(content);
    buildPatientPanel(content);
    buildTipoSection(content);
    buildComponentesSection(content);
    buildClinicalSection(content);
    buildBagSection(content);
    buildActions(content);

    ScrollView scroller = new ScrollView(this);
    scroller.setFillViewport(true);
    scroller.addView(content, new ScrollView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    root.addView(scroller, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

    root.addView(new SectionNavBar(this, Section.DONACIONES), new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    return root;
  }

  private View buildHeader() {
    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.VERTICAL);
    header.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);

    TextView title = new TextView(this);
    title.setText("Registrar donación");
    AppTypography.headline1(title);
    header.addView(title);

    TextView subtitle = new TextView(this);
    subtitle.setText("Buscá al donante, elegí el tipo y confirmá la captura.");
    AppTypography.caption(subtitle);
    header.addView(subtitle);

    return header;
  }

  // --- Paso 1: paciente ---

  private void buildLookup(LinearLayout root) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.BOTTOM);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());

    dniInput = new AppInput(this, "DNI");
    dniInput.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    row.addView(dniInput, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    AppButton buscar = new AppButton(this, "Buscar", AppButton.Style.FILLED, v -> search());
    LinearLayout.LayoutParams buscarParams = new LinearLayout.LayoutParams(
        AppSpacing.dp(110, getResources()), AppSpacing.dp(56, getResources()));
    buscarParams.leftMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    row.addView(buscar, buscarParams);
    root.addView(row, rowParams);

    state = new StateView(this);
    root.addView(state);

    AppButton clear = new AppButton(this, "Limpiar", AppButton.Style.TEXT, v -> resetAll());
    root.addView(clear, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(40, getResources())));
  }

  private void search() {
    String dni = dniInput.getText();
    if (dni.isEmpty()) {
      dniInput.setError("Ingresá el DNI");
      return;
    }
    dniInput.setError(null);
    hidePanels();
    state.showLoading();

    pacienteRepository.findByDniWithAptitud(dni, new PacienteRepository.Callback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        state.hide();
        selectedPaciente = data;
        showPatient(data);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        selectedPaciente = null;
        if (type == ApiError.Type.NOT_FOUND) {
          state.showEmpty("No existe un paciente con ese DNI");
          return;
        }
        if (type == ApiError.Type.UNAUTHORIZED) {
          state.showError("Sesión vencida, volvé a ingresar", null);
          return;
        }
        state.showError(messageFor(type, message), DonacionesActivity.this::search);
      }
    });
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

    patientPanel = new AppCard(this, content);
    patientPanel.setVisibility(View.GONE);
    LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    panelParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    root.addView(patientPanel, panelParams);
  }

  private void showPatient(PacienteDto paciente) {
    if (paciente == null) {
      return;
    }
    patientName.setText(paciente.displayName());
    patientDni.setText("DNI " + safe(paciente.dni));
    patientAptitud.setText("Aptitud: " + aptitudLabel(paciente));
    patientPanel.setVisibility(View.VISIBLE);
  }

  // --- Paso 2: tipo de donacion ---

  private void buildTipoSection(LinearLayout root) {
    sectionTitle(root, "Tipo de donación");

    tipoStatus = new TextView(this);
    tipoStatus.setTextSize(14);
    tipoStatus.setTextColor(AppColors.TEXT_SECONDARY);
    tipoStatus.setPadding(0, AppSpacing.dp(AppSpacing.XS, getResources()), 0, 0);
    tipoStatus.setClickable(true);
    tipoStatus.setFocusable(true);
    tipoStatus.setOnClickListener(v -> loadTipos());
    root.addView(tipoStatus);

    tipoContainer = new LinearLayout(this);
    tipoContainer.setOrientation(LinearLayout.VERTICAL);
    root.addView(tipoContainer);

    tipoError = makeErrorText();
    root.addView(tipoError);
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
    catalogoRepository.listTiposDonacion(new CatalogoRepository.Callback<List<TipoDonacionDto>>() {
      @Override
      public void onSuccess(List<TipoDonacionDto> data) {
        tipos = data;
        tiposLoading = false;
        renderTipos();
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        tiposLoading = false;
        renderTipos();
      }
    });
  }

  private void renderTipos() {
    if (tipoContainer == null) {
      return;
    }
    tipoContainer.removeAllViews();
    if (tiposLoading) {
      tipoStatus.setText("Cargando tipos de donación…");
      tipoStatus.setTextColor(AppColors.TEXT_SECONDARY);
      tipoStatus.setVisibility(View.VISIBLE);
      return;
    }
    if (tipos == null) {
      tipoStatus.setText("No se pudieron cargar los tipos. Tocá para reintentar.");
      tipoStatus.setTextColor(AppColors.ERROR);
      tipoStatus.setVisibility(View.VISIBLE);
      return;
    }
    tipoStatus.setVisibility(View.GONE);
    for (final TipoDonacionDto tipo : tipos) {
      String label = tipo.displayName() + (tipo.esAferesis ? " · aféresis" : "");
      tipoContainer.addView(makeSelectableRow(label, isSelectedTipo(tipo),
          v -> selectTipo(tipo)));
    }
    renderComponentes();
  }

  private boolean isSelectedTipo(TipoDonacionDto tipo) {
    return selectedTipo != null && selectedTipo.id != null && selectedTipo.id.equals(tipo.id);
  }

  private void selectTipo(TipoDonacionDto tipo) {
    selectedTipo = tipo;
    hideError(tipoError);
    // Preselecciona el componente que corresponde si aun no hay unidades.
    if (totalComponentes() == 0 && tipo.id != null && MAXIMOS.containsKey(tipo.codigoUpper())) {
      cantidades.put(tipo.id, 1);
    }
    renderTipos();
    renderClinicalHint();
  }

  // --- Paso 3: componentes ---

  private void buildComponentesSection(LinearLayout root) {
    sectionTitle(root, "Componentes");

    compHint = new TextView(this);
    compHint.setText("SANGRE máx. 1 · PLASMA máx. 1 · PLAQUETAS máx. 3");
    AppTypography.caption(compHint);
    root.addView(compHint);

    compContainer = new LinearLayout(this);
    compContainer.setOrientation(LinearLayout.VERTICAL);
    root.addView(compContainer);

    compError = makeErrorText();
    root.addView(compError);
  }

  private void renderComponentes() {
    if (compContainer == null) {
      return;
    }
    compContainer.removeAllViews();
    qtyViews.clear();
    if (tipos == null) {
      return;
    }
    for (final TipoDonacionDto tipo : tipos) {
      if (tipo.id == null || !MAXIMOS.containsKey(tipo.codigoUpper())) {
        continue;
      }
      compContainer.addView(buildComponentRow(tipo));
    }
  }

  private LinearLayout buildComponentRow(final TipoDonacionDto tipo) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setBackground(rounded(AppColors.SURFACE, 12));
    row.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()));
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    row.setLayoutParams(rowParams);

    TextView label = new TextView(this);
    label.setText(tipo.displayName());
    label.setTextSize(15);
    label.setTextColor(AppColors.TEXT_PRIMARY);
    row.addView(label, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    AppButton minus = new AppButton(this, "\u2212", AppButton.Style.TEXT,
        v -> changeComponent(tipo, -1));
    minus.setLayoutParams(new LinearLayout.LayoutParams(
        AppSpacing.dp(44, getResources()), AppSpacing.dp(44, getResources())));
    row.addView(minus);

    TextView qty = new TextView(this);
    qty.setText(String.valueOf(cantidad(tipo)));
    qty.setTextSize(16);
    qty.setTextColor(AppColors.TEXT_PRIMARY);
    qty.setGravity(Gravity.CENTER);
    row.addView(qty, new LinearLayout.LayoutParams(
        AppSpacing.dp(36, getResources()), ViewGroup.LayoutParams.WRAP_CONTENT));
    qtyViews.put(tipo.id, qty);

    AppButton plus = new AppButton(this, "+", AppButton.Style.TEXT,
        v -> changeComponent(tipo, 1));
    plus.setLayoutParams(new LinearLayout.LayoutParams(
        AppSpacing.dp(44, getResources()), AppSpacing.dp(44, getResources())));
    row.addView(plus);

    return row;
  }

  private void changeComponent(TipoDonacionDto tipo, int delta) {
    if (tipo.id == null) {
      return;
    }
    int actual = cantidad(tipo);
    int max = MAXIMOS.getOrDefault(tipo.codigoUpper(), 1);
    int nuevo = Math.max(0, Math.min(max, actual + delta));
    cantidades.put(tipo.id, nuevo);
    TextView qty = qtyViews.get(tipo.id);
    if (qty != null) {
      qty.setText(String.valueOf(nuevo));
    }
    hideError(compError);
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
    sectionTitle(root, "Signos clínicos");

    clinicalHint = new LinearLayout(this);
    clinicalHint.setOrientation(LinearLayout.VERTICAL);
    TextView hint = new TextView(this);
    hint.setText("Donación por aféresis: plaquetas y hematocrito son obligatorios.");
    hint.setTextSize(13);
    hint.setTextColor(AppColors.WARNING);
    clinicalHint.addView(hint);
    clinicalHint.setVisibility(View.GONE);
    root.addView(clinicalHint);

    hemoglobinaInput = addInput(root, "Hemoglobina (g/dL)", decimalNumber());
    hematocritoInput = addInput(root, "Hematocrito (%)", decimalNumber());
    plaquetasInput = addInput(root, "Plaquetas (/mm³)", wholeNumber());
    sistolicaInput = addInput(root, "Presión sistólica", wholeNumber());
    diastolicaInput = addInput(root, "Presión diastólica", wholeNumber());
    frecuenciaInput = addInput(root, "Frecuencia cardíaca", wholeNumber());
    pesoInput = addInput(root, "Peso del donante (kg)", decimalNumber());

    clinicalError = makeErrorText();
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
    sectionTitle(root, "Datos de la bolsa");

    TextView bolsaLabel = new TextView(this);
    bolsaLabel.setText("Tipo de bolsa");
    AppTypography.caption(bolsaLabel);
    root.addView(bolsaLabel);

    bolsaContainer = new LinearLayout(this);
    bolsaContainer.setOrientation(LinearLayout.VERTICAL);
    root.addView(bolsaContainer);

    TextView brazoLabel = new TextView(this);
    brazoLabel.setText("Brazo");
    AppTypography.caption(brazoLabel);
    LinearLayout.LayoutParams brazoLabelParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    brazoLabelParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    root.addView(brazoLabel, brazoLabelParams);

    brazoContainer = new LinearLayout(this);
    brazoContainer.setOrientation(LinearLayout.VERTICAL);
    root.addView(brazoContainer);

    anticoagulanteInput = addInput(root, "Anticoagulante", InputType.TYPE_CLASS_TEXT);
    loteInput = addInput(root, "Lote", InputType.TYPE_CLASS_TEXT);
    tubuladuraInput = addInput(root, "Tubuladura", InputType.TYPE_CLASS_TEXT);
    dificultadInput = addInput(root, "Dificultad", InputType.TYPE_CLASS_TEXT);

    dobleEtiquetaCheck = new CheckBox(this);
    dobleEtiquetaCheck.setText("Doble etiqueta");
    dobleEtiquetaCheck.setTextColor(AppColors.TEXT_PRIMARY);
    dobleEtiquetaCheck.setTextSize(14);
    LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    checkParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    root.addView(dobleEtiquetaCheck, checkParams);

    bagError = makeErrorText();
    root.addView(bagError);

    renderBolsa();
    renderBrazo();
  }

  private void renderBolsa() {
    if (bolsaContainer == null) {
      return;
    }
    bolsaContainer.removeAllViews();
    for (final String value : new String[] {"doble", "triple", "cuadruple", "quintuple"}) {
      bolsaContainer.addView(makeSelectableRow(value, value.equals(selectedTipoBolsa),
          v -> {
            selectedTipoBolsa = value;
            hideError(bagError);
            renderBolsa();
          }));
    }
  }

  private void renderBrazo() {
    if (brazoContainer == null) {
      return;
    }
    brazoContainer.removeAllViews();
    for (final String value : new String[] {"izquierdo", "derecho"}) {
      brazoContainer.addView(makeSelectableRow(value, value.equals(selectedBrazo),
          v -> {
            selectedBrazo = value;
            hideError(bagError);
            renderBrazo();
          }));
    }
  }

  // --- Paso 6: confirmar + paneles de resultado ---

  private void buildActions(LinearLayout root) {
    formError = makeErrorText();
    root.addView(formError);

    warningPanel = new LinearLayout(this);
    warningPanel.setOrientation(LinearLayout.VERTICAL);
    warningPanel.setBackground(rounded(AppColors.PRIMARY_TINT, 12));
    warningPanel.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()));
    warningPanel.setVisibility(View.GONE);

    TextView warningTitle = new TextView(this);
    warningTitle.setText("La donación no está habilitada");
    AppTypography.bodyMedium(warningTitle);
    warningPanel.addView(warningTitle);

    warningList = new LinearLayout(this);
    warningList.setOrientation(LinearLayout.VERTICAL);
    warningPanel.addView(warningList);

    LinearLayout.LayoutParams warningParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    warningParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    root.addView(warningPanel, warningParams);

    successPanel = new LinearLayout(this);
    successPanel.setOrientation(LinearLayout.VERTICAL);
    successPanel.setBackground(rounded(AppColors.SURFACE, 12));
    successPanel.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()));
    successPanel.setVisibility(View.GONE);

    TextView successTitle = new TextView(this);
    successTitle.setText("Donación registrada");
    AppTypography.bodyMedium(successTitle);
    successPanel.addView(successTitle);

    successNumber = new TextView(this);
    successNumber.setTextSize(18);
    successNumber.setTextColor(AppColors.PRIMARY);
    successNumber.setTypeface(android.graphics.Typeface.create(
        "sans-serif-medium", android.graphics.Typeface.NORMAL));
    LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    numberParams.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    successPanel.addView(successNumber, numberParams);

    anotherButton = new AppButton(this, "Registrar otra donación",
        AppButton.Style.TEXT, v -> resetAll());
    successPanel.addView(anotherButton);

    LinearLayout.LayoutParams successParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    successParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    root.addView(successPanel, successParams);

    submitButton = new AppButton(this, "CONFIRMAR", AppButton.Style.FILLED, v -> submit(false));
    LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(52, getResources()));
    submitParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    root.addView(submitButton, submitParams);
  }

  // --- Submit ---

  private void submit(boolean forzar) {
    if (submitting) {
      return;
    }
    clearErrors();
    hideWarning();
    successPanel.setVisibility(View.GONE);
    if (!forzar && !validate()) {
      return;
    }
    setSubmitting(true);
    state.hide();

    donacionRepository.create(buildRequest(forzar), new DonacionRepository.Callback<DonacionDto>() {
      @Override
      public void onSuccess(DonacionDto data) {
        setSubmitting(false);
        createdDonacion = data;
        showSuccess(data);
      }

      @Override
      public void onWarning(DonacionWarningResponse warnings) {
        setSubmitting(false);
        showWarning(warnings);
      }

      @Override
      public void onValidationError(Map<String, String> fieldErrors) {
        setSubmitting(false);
        applyFieldErrors(fieldErrors);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        setSubmitting(false);
        showError(formError, messageFor(type, message));
      }
    });
  }

  private CreateDonacionRequest buildRequest(boolean forzar) {
    CreateDonacionRequest body = new CreateDonacionRequest();
    body.pacienteId = selectedPaciente != null ? selectedPaciente.id : null;
    body.tipoId = selectedTipo != null ? selectedTipo.id : null;
    body.tipoBolsa = selectedTipoBolsa;
    body.brazo = selectedBrazo;
    body.anticoagulante = blankToNull(anticoagulanteInput.getText());
    body.lote = blankToNull(loteInput.getText());
    body.tubuladura = blankToNull(tubuladuraInput.getText());
    body.dificultad = blankToNull(dificultadInput.getText());
    body.dobleEtiqueta = dobleEtiquetaCheck.isChecked();

    body.hemoglobina = parseDoubleOrNull(hemoglobinaInput.getText());
    body.hematocrito = parseDoubleOrNull(hematocritoInput.getText());
    body.plaquetas = parseIntOrNull(plaquetasInput.getText());
    body.presionSistolica = parseIntOrNull(sistolicaInput.getText());
    body.presionDiastolica = parseIntOrNull(diastolicaInput.getText());
    body.frecuenciaCardiaca = parseIntOrNull(frecuenciaInput.getText());
    body.pesoDonante = parseDoubleOrNull(pesoInput.getText());

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
    return body;
  }

  private boolean validate() {
    boolean ok = true;
    if (selectedPaciente == null || selectedPaciente.id == null) {
      state.showError("Buscá un paciente primero", this::search);
      return false;
    }
    if (selectedTipo == null || selectedTipo.id == null) {
      showError(tipoError, "Elegí el tipo de donación");
      ok = false;
    }
    if (totalComponentes() < 1) {
      showError(compError, "Agregá al menos un componente");
      ok = false;
    }
    if (!validNumeric(hemoglobinaInput, clinicalError)) {
      ok = false;
    }
    if (!validNumeric(hematocritoInput, clinicalError)) {
      ok = false;
    }
    if (!validNumeric(plaquetasInput, clinicalError)) {
      ok = false;
    }
    if (!validNumeric(sistolicaInput, clinicalError)) {
      ok = false;
    }
    if (!validNumeric(diastolicaInput, clinicalError)) {
      ok = false;
    }
    if (!validNumeric(frecuenciaInput, clinicalError)) {
      ok = false;
    }
    if (!validNumeric(pesoInput, clinicalError)) {
      ok = false;
    }

    boolean aferesis = selectedTipo != null && selectedTipo.esAferesis;
    if (aferesis) {
      if (parseDoubleOrNull(plaquetasInput.getText()) == null
          || parseDoubleOrNull(hematocritoInput.getText()) == null) {
        showError(clinicalError,
            "Aféresis: cargá plaquetas y hematocrito");
        ok = false;
      }
    }

    Integer sistolica = parseIntOrNull(sistolicaInput.getText());
    Integer diastolica = parseIntOrNull(diastolicaInput.getText());
    if (sistolica != null && diastolica != null && diastolica > sistolica) {
      showError(clinicalError, "La presión diastólica no puede superar la sistólica");
      ok = false;
    }

    if (!ok && clinicalError.getVisibility() != View.VISIBLE
        && compError.getVisibility() != View.VISIBLE
        && tipoError.getVisibility() != View.VISIBLE) {
      showError(formError, "Revisá los datos antes de confirmar");
    }
    return ok;
  }

  /** Valida que un input opcional, si vino, sea numerico. */
  private boolean validNumeric(AppInput input, TextView errorTarget) {
    String text = input.getText();
    if (text.isEmpty()) {
      return true;
    }
    try {
      Double.parseDouble(text);
      return true;
    } catch (NumberFormatException e) {
      showError(errorTarget, "Hay un valor numérico inválido");
      return false;
    }
  }

  // --- 409 / 201 ---

  private void showWarning(DonacionWarningResponse warnings) {
    warningList.removeAllViews();
    for (String message : warnings.messages()) {
      TextView item = new TextView(this);
      item.setText("• " + message);
      item.setTextSize(14);
      item.setTextColor(AppColors.TEXT_PRIMARY);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
      warningList.addView(item, params);
    }
    warningPanel.setVisibility(View.VISIBLE);
  }

  private void hideWarning() {
    warningPanel.setVisibility(View.GONE);
  }

  private void showSuccess(DonacionDto donacion) {
    String numero = donacion != null && donacion.numeroDonacion != null
        ? donacion.numeroDonacion : "—";
    successNumber.setText("Nº " + numero);
    successPanel.setVisibility(View.VISIBLE);
    submitButton.setVisibility(View.GONE);
    hideWarning();
    Toast.makeText(this, "Donación registrada", Toast.LENGTH_SHORT).show();
  }

  // --- Reset y helpers ---

  private void resetAll() {
    dniInput.getEditText().setText("");
    dniInput.setError(null);
    selectedPaciente = null;
    selectedTipo = null;
    createdDonacion = null;
    cantidades.clear();
    selectedTipoBolsa = "doble";
    selectedBrazo = "izquierdo";
    clearClinicalInputs();
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
      warningPanel.setVisibility(View.GONE);
    }
    if (successPanel != null) {
      successPanel.setVisibility(View.GONE);
    }
    if (submitButton != null) {
      submitButton.setVisibility(View.VISIBLE);
    }
  }

  private void setSubmitting(boolean value) {
    submitting = value;
    if (submitButton != null) {
      submitButton.setEnabled(!value);
      submitButton.setText(value ? "Guardando…" : "CONFIRMAR");
    }
  }

  private void applyFieldErrors(Map<String, String> errors) {
    boolean handled = false;
    if (errors.containsKey("paciente_id")) {
      state.showError("Elegí un paciente válido", this::search);
      handled = true;
    }
    if (errors.containsKey("tipo_id")) {
      showError(tipoError, "Elegí un tipo de donación válido");
      handled = true;
    }
    if (errors.containsKey("componentes")) {
      showError(compError, errors.get("componentes"));
      handled = true;
    } else {
      for (String key : errors.keySet()) {
        if (key.startsWith("componentes.")) {
          showError(compError, errors.get(key));
          handled = true;
          break;
        }
      }
    }
    if (errors.containsKey("signos_clinicos")) {
      showError(clinicalError, errors.get("signos_clinicos"));
      handled = true;
    }
    for (String key : new String[] {"tipo_bolsa", "anticoagulante", "lote",
        "tubuladura", "brazo", "dificultad", "doble_etiqueta"}) {
      if (errors.containsKey(key)) {
        showError(bagError, errors.get(key));
        handled = true;
        break;
      }
    }
    if (!handled) {
      String first = errors.values().iterator().hasNext()
          ? errors.values().iterator().next() : null;
      showError(formError, first != null && !first.isEmpty()
          ? first : "No se pudo registrar la donación");
    }
  }

  private void clearErrors() {
    hideError(tipoError);
    hideError(compError);
    hideError(clinicalError);
    hideError(bagError);
    hideError(formError);
  }

  // --- Helpers de UI ---

  /** Agrega un titulo de seccion y lo devuelve. */
  private TextView sectionTitle(LinearLayout root, String text) {
    TextView title = new TextView(this);
    title.setText(text);
    AppTypography.headline2(title);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.LG, getResources());
    root.addView(title, params);
    return title;
  }

  private AppInput addInput(LinearLayout root, String hint, int inputType) {
    AppInput input = new AppInput(this, hint);
    input.getEditText().setInputType(inputType);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    root.addView(input, params);
    return input;
  }

  private int decimalNumber() {
    return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
  }

  private int wholeNumber() {
    return InputType.TYPE_CLASS_NUMBER;
  }

  private TextView makeSelectableRow(String text, boolean selected, View.OnClickListener listener) {
    TextView row = new TextView(this);
    row.setText(text);
    row.setTextSize(15);
    row.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()));
    row.setBackground(rounded(selected ? AppColors.PRIMARY_TINT : AppColors.SURFACE, 12));
    row.setTextColor(selected ? AppColors.PRIMARY : AppColors.TEXT_PRIMARY);
    row.setClickable(true);
    row.setFocusable(true);
    row.setOnClickListener(listener);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    row.setLayoutParams(params);
    return row;
  }

  private TextView makeErrorText() {
    TextView error = new TextView(this);
    error.setTextSize(13);
    error.setTextColor(AppColors.ERROR);
    error.setVisibility(View.GONE);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    params.leftMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    error.setLayoutParams(params);
    return error;
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

  private GradientDrawable rounded(int color, float radiusDp) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(AppSpacing.dp((int) radiusDp, getResources()));
    return drawable;
  }

  private String blankToNull(String value) {
    return value != null && !value.trim().isEmpty() ? value.trim() : null;
  }

  private Double parseDoubleOrNull(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return Double.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer parseIntOrNull(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return (int) Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String aptitudLabel(PacienteDto paciente) {
    if (paciente.aptitud != null && paciente.aptitud.tipo != null) {
      switch (paciente.aptitud.tipo) {
        case "APTO":
          return "Apto";
        case "APTO_OBSERVACION":
          return "Apto con observación";
        case "NO_APTO":
          return "No apto";
        default:
          return paciente.aptitud.tipo;
      }
    }
    return "No disponible";
  }

  private String safe(String value) {
    return value != null && !value.isEmpty() ? value : "—";
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
            ? message : "No se pudo registrar la donación";
      default:
        return message != null && !message.isEmpty()
            ? message : "No se pudo completar la operación";
    }
  }
}
