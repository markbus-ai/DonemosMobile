package com.donemosmobile.ui.donaciones;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.catalogo.CatalogoRepository;
import com.donemosmobile.data.donaciones.DonacionRepository;
import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.WarningCallback;
import com.donemosmobile.data.network.dto.AutoexclusionDto;
import com.donemosmobile.data.network.dto.DonacionDto;
import com.donemosmobile.data.network.dto.DonacionWarningResponse;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.TipoDonacionDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.AptitudLabels;
import com.donemosmobile.ui.StaleResults;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.BulletList;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.components.ForceDialog;
import com.donemosmobile.ui.components.InputDialog;
import com.donemosmobile.ui.components.PatientPicker;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
import com.donemosmobile.ui.components.SelectGroup;
import com.donemosmobile.ui.components.SelectableRow;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.components.StatusPanel;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.util.ArrayList;
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
 * <p>Container: wires the sections ({@link ComponentesSection},
 * {@link ClinicalSection}, {@link BagSection}, {@link CycleSection}) to the
 * repositories. The form state and its validation live in {@link DonacionForm};
 * the request body comes from {@link DonacionRequestMapper}.
 *
 * <p>La seccion Donaciones y el tile "Registrar donacion" de la Home apuntan
 * aca: una sola implementacion para ambos puntos de entrada.
 */
public class DonacionesActivity extends AppActivity {

  private PacienteRepository pacienteRepository;
  private CatalogoRepository catalogoRepository;
  private DonacionRepository donacionRepository;

  private final DonacionForm form = new DonacionForm();

  private PatientPicker picker;
  private StateView state;

  private AppCard patientPanel;
  private TextView patientName;
  private TextView patientDni;
  private TextView patientAptitud;
  private TextView patientCycle;

  private SelectGroup tipoGroup;
  private ComponentesSection componentes;
  private ClinicalSection clinical;
  private BagSection bag;
  private CycleSection cycle;

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

  private List<TipoDonacionDto> tipos;
  private boolean tiposLoading;
  private PacienteDto selectedPaciente;
  private TipoDonacionDto selectedTipo;
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

    componentes = new ComponentesSection(this);
    content.addView(componentes, Ui.matchWrap(this));
    clinical = new ClinicalSection(this);
    content.addView(clinical, Ui.matchWrap(this));
    bag = new BagSection(this);
    content.addView(bag, Ui.matchWrap(this));
    cycle = new CycleSection(this);
    content.addView(cycle, Ui.matchWrap(this));

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
    if (form.totalComponentes() == 0 && tipo.id != null
        && DonacionForm.isComponente(tipo.codigoUpper())) {
      form.cantidades.put(tipo.id, 1);
    }
    renderTipos();
    renderClinicalHint();
  }

  private void renderComponentes() {
    if (componentes != null) {
      componentes.render(tipos, form);
    }
  }

  private void renderClinicalHint() {
    if (clinical != null) {
      clinical.setAferesis(selectedTipo != null && selectedTipo.esAferesis);
    }
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
    successPanel.addAction("Registrar otra donación", AppButton.Style.TEXT, v -> resetAll(), 0);
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

    donacionRepository.create(submittedDni, DonacionRequestMapper.toRequest(form, forzar, motivo),
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

  /** Copies the selection and every input into {@link #form}. */
  private void snapshotForm() {
    form.pacienteId = selectedPaciente != null ? selectedPaciente.id : null;
    form.tipoId = selectedTipo != null ? selectedTipo.id : null;
    form.tipoAferesis = selectedTipo != null && selectedTipo.esAferesis;
    clinical.writeTo(form);
    bag.writeTo(form);
    cycle.writeTo(form);
  }

  /** Runs {@link DonacionForm#validate()} and paints every error it found. */
  private boolean validate() {
    snapshotForm();
    DonacionForm.Errors errors = form.validate();
    if (errors.missingPaciente) {
      state.showError("Buscá un paciente primero", this::search);
      return false;
    }
    if (errors.tipo != null) {
      tipoGroup.error().show(errors.tipo);
    }
    if (errors.componentes != null) {
      componentes.error().show(errors.componentes);
    }
    clinical.showFieldErrors(errors.fields);
    if (errors.clinical != null) {
      clinical.error().show(errors.clinical);
    }
    if (errors.cycle != null) {
      cycle.error().show(errors.cycle);
    }
    if (errors.form != null) {
      formError.show(errors.form);
    }
    return errors.ok();
  }

  // --- 409 / 201 ---

  private void showWarning(DonacionWarningResponse warnings) {
    warningList.removeAllViews();
    lastWarnings = warnings.messages();
    forzarButton.setVisibility(lastWarnings.isEmpty() ? View.GONE : View.VISIBLE);
    BulletList.addTo(warningList, lastWarnings);
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
    AppInput motivo = new AppInput(this, "Motivo (opcional)");
    motivo.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
    // Same cap as the API column (varchar 255).
    motivo.getEditText().setFilters(new InputFilter[] {new InputFilter.LengthFilter(255)});
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    int side = Ui.dp(this, AppSpacing.LG);
    body.setPadding(side, Ui.dp(this, AppSpacing.SM), side, 0);
    body.addView(motivo, Ui.matchWrap(this));
    InputDialog.showOnce(this, "Marcar autoexclusión",
        "Registro confidencial del donante. No se guarda su identidad.", body, "Confirmar",
        "Cancelar", () -> registerAutoexclusion(motivo.getText()));
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
    form.cantidades.clear();
    clinical.clearInputs();
    cycle.clearInputs();
    bag.reset();
    clearErrors();
    hideWarning();
    hidePanels();
    renderTipos();
    renderComponentes();
    renderClinicalHint();
    state.hide();
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
      componentes.error().show(errors.get("componentes"));
      handled = true;
    } else {
      for (String key : errors.keySet()) {
        if (key.startsWith("componentes.")) {
          componentes.error().show(errors.get(key));
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
      clinical.error().show(errors.get("signos_clinicos"));
      handled = true;
    }
    if (errors.containsKey("ciclo") || errors.containsKey("serie")
        || errors.containsKey("donacion")) {
      String message = errors.containsKey("ciclo") ? errors.get("ciclo")
          : errors.containsKey("serie") ? errors.get("serie") : errors.get("donacion");
      cycle.error().show(message);
      handled = true;
    }
    for (String key : new String[] {"tipo_bolsa", "anticoagulante", "lote",
        "tubuladura", "brazo", "dificultad", "doble_etiqueta"}) {
      if (errors.containsKey(key)) {
        bag.error().show(errors.get(key));
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
    clinical.clearFieldErrors();
    tipoGroup.error().hide();
    componentes.error().hide();
    clinical.error().hide();
    bag.error().hide();
    cycle.error().hide();
    formError.hide();
  }
}
