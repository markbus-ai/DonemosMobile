package com.donemosmobile.ui.aptitud;

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
import com.donemosmobile.data.aptitud.AptitudRepository;
import com.donemosmobile.data.catalogo.CatalogoRepository;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.MotivoDto;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.UpdateAptitudRequest;
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
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Evaluacion de aptitud: busca un paciente por DNI, muestra su situacion
 * actual y permite asignar una nueva (APTO / APTO CON OBSERVACION / NO APTO)
 * contra {@code PATCH /api/pacientes/{dni}/aptitud}. Los tipos no APTO piden
 * motivo y fecha desde (hasta y permanente opcionales).
 */
public class AptitudActivity extends AppActivity {

  private static final String TIPO_APTO = "APTO";
  private static final String TIPO_APTO_OBSERVACION = "APTO_OBSERVACION";
  private static final String TIPO_NO_APTO = "NO_APTO";

  private PacienteRepository pacienteRepository;
  private AptitudRepository aptitudRepository;
  private CatalogoRepository catalogoRepository;

  private AppInput dniInput;
  private StateView state;

  private AppCard patientPanel;
  private TextView patientName;
  private TextView patientDni;
  private TextView currentAptitud;

  private LinearLayout actionPanel;
  private LinearLayout tipoContainer;
  private TextView tipoError;

  private LinearLayout motivoSection;
  private LinearLayout motivoContainer;
  private TextView motivoStatus;
  private TextView motivoError;

  private LinearLayout desdeBlock;
  private TextView desdeValue;
  private TextView desdeError;
  private LinearLayout hastaBlock;
  private LinearLayout hastaField;
  private TextView hastaValue;
  private TextView hastaError;
  private LinearLayout permanenteRow;
  private CheckBox permanenteCheck;

  private AppButton submitButton;

  private PacienteDto selectedPaciente;
  private String selectedTipo;
  private Long selectedMotivoId;
  private String selectedDesde;
  private String selectedHasta;
  private List<MotivoDto> motivos;
  private boolean motivosLoading;
  private boolean submitting;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    pacienteRepository = new PacienteRepository(this);
    aptitudRepository = new AptitudRepository(this);
    catalogoRepository = new CatalogoRepository(this);
    setContentView(buildView());
    loadMotivos();
  }

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
    buildActionPanel(content);

    ScrollView scroller = new ScrollView(this);
    scroller.setFillViewport(true);
    scroller.addView(content, new ScrollView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    root.addView(scroller, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

    root.addView(new SectionNavBar(this, Section.APTITUD), new LinearLayout.LayoutParams(
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
    title.setText("Aptitud");
    AppTypography.headline1(title);
    header.addView(title);

    TextView subtitle = new TextView(this);
    subtitle.setText("Buscá al paciente y definí su aptitud para donar.");
    AppTypography.caption(subtitle);
    header.addView(subtitle);

    return header;
  }

  // --- Paso 1: buscar paciente ---

  private void buildLookup(LinearLayout root) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.BOTTOM);

    dniInput = new AppInput(this, "DNI");
    dniInput.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    row.addView(dniInput, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    AppButton buscar = new AppButton(this, "Buscar", AppButton.Style.FILLED, v -> search());
    LinearLayout.LayoutParams buscarParams = new LinearLayout.LayoutParams(
        AppSpacing.dp(110, getResources()), AppSpacing.dp(56, getResources()));
    buscarParams.leftMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    row.addView(buscar, buscarParams);

    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    root.addView(row, rowParams);

    state = new StateView(this);
    root.addView(state);

    AppButton clear = new AppButton(this, "Limpiar", AppButton.Style.TEXT, v -> resetAll());
    LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(40, getResources()));
    root.addView(clear, clearParams);
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
        resetAction();
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
        state.showError(messageFor(type, message), AptitudActivity.this::search);
      }
    });
  }

  // --- Paso 2: paciente + aptitud actual ---

  private void buildPatientPanel(LinearLayout root) {
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);

    patientName = new TextView(this);
    AppTypography.headline2(patientName);
    content.addView(patientName);

    patientDni = new TextView(this);
    AppTypography.caption(patientDni);
    content.addView(patientDni);

    currentAptitud = new TextView(this);
    currentAptitud.setTextSize(15);
    currentAptitud.setTextColor(AppColors.TEXT_PRIMARY);
    LinearLayout.LayoutParams aptitudParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    aptitudParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    content.addView(currentAptitud, aptitudParams);

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
    currentAptitud.setText("Aptitud actual: " + currentAptitudLabel(paciente));
    patientPanel.setVisibility(View.VISIBLE);
    actionPanel.setVisibility(View.VISIBLE);
  }

  private String currentAptitudLabel(PacienteDto paciente) {
    if (paciente.aptitud != null && paciente.aptitud.tipo != null) {
      return tipoLabel(paciente.aptitud.tipo);
    }
    return "No disponible";
  }

  private String tipoLabel(String tipo) {
    if (TIPO_APTO.equals(tipo)) {
      return "Apto";
    }
    if (TIPO_APTO_OBSERVACION.equals(tipo)) {
      return "Apto con observación";
    }
    if (TIPO_NO_APTO.equals(tipo)) {
      return "No apto";
    }
    return tipo;
  }

  // --- Paso 3: nueva aptitud ---

  private void buildActionPanel(LinearLayout root) {
    actionPanel = new LinearLayout(this);
    actionPanel.setOrientation(LinearLayout.VERTICAL);
    actionPanel.setVisibility(View.GONE);

    TextView title = new TextView(this);
    title.setText("Nueva situación");
    AppTypography.headline2(title);
    actionPanel.addView(title);

    TextView tipoLabel = new TextView(this);
    tipoLabel.setText("Elegí la aptitud");
    AppTypography.caption(tipoLabel);
    LinearLayout.LayoutParams tipoLabelParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    tipoLabelParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    actionPanel.addView(tipoLabel, tipoLabelParams);

    tipoContainer = new LinearLayout(this);
    tipoContainer.setOrientation(LinearLayout.VERTICAL);
    actionPanel.addView(tipoContainer);

    tipoError = makeErrorText();
    actionPanel.addView(tipoError);

    buildMotivoSection();

    desdeValue = new TextView(this);
    desdeBlock = makeFieldBlock("Desde", desdeValue);
    LinearLayout desdeField = (LinearLayout) desdeBlock.getChildAt(0);
    desdeField.setOnClickListener(v -> showDatePicker(true));
    actionPanel.addView(desdeBlock);
    desdeError = (TextView) desdeBlock.getChildAt(1);

    hastaValue = new TextView(this);
    hastaBlock = makeFieldBlock("Hasta (opcional)", hastaValue);
    hastaField = (LinearLayout) hastaBlock.getChildAt(0);
    hastaField.setOnClickListener(v -> showDatePicker(false));
    actionPanel.addView(hastaBlock);
    hastaError = (TextView) hastaBlock.getChildAt(1);

    buildPermanenteRow();

    submitButton = new AppButton(this, "Guardar aptitud", AppButton.Style.FILLED, v -> submit());
    LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(48, getResources()));
    submitParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    actionPanel.addView(submitButton, submitParams);

    root.addView(actionPanel);
    renderTipo();
  }

  private void buildMotivoSection() {
    motivoSection = new LinearLayout(this);
    motivoSection.setOrientation(LinearLayout.VERTICAL);

    TextView label = new TextView(this);
    label.setText("Motivo");
    AppTypography.caption(label);
    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    labelParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    motivoSection.addView(label, labelParams);

    motivoStatus = new TextView(this);
    motivoStatus.setTextSize(14);
    motivoStatus.setTextColor(AppColors.TEXT_SECONDARY);
    motivoStatus.setPadding(0, AppSpacing.dp(AppSpacing.XS, getResources()), 0, 0);
    motivoStatus.setClickable(true);
    motivoStatus.setFocusable(true);
    motivoStatus.setOnClickListener(v -> loadMotivos());
    motivoSection.addView(motivoStatus);

    motivoContainer = new LinearLayout(this);
    motivoContainer.setOrientation(LinearLayout.VERTICAL);
    motivoSection.addView(motivoContainer);

    motivoError = makeErrorText();
    motivoSection.addView(motivoError);

    actionPanel.addView(motivoSection);
  }

  private void buildPermanenteRow() {
    permanenteRow = new LinearLayout(this);
    permanenteRow.setOrientation(LinearLayout.HORIZONTAL);
    permanenteRow.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());

    permanenteCheck = new CheckBox(this);
    permanenteCheck.setText("Permanente (sin fecha de fin)");
    permanenteCheck.setTextColor(AppColors.TEXT_PRIMARY);
    permanenteCheck.setTextSize(14);
    permanenteCheck.setOnCheckedChangeListener((button, checked) -> {
      if (checked) {
        selectedHasta = null;
        hastaValue.setText("Sin fecha de fin");
        hastaValue.setTextColor(AppColors.TEXT_SECONDARY);
        hideError(hastaError);
      } else if (selectedHasta == null) {
        hastaValue.setText("");
        hastaValue.setTextColor(AppColors.TEXT_SECONDARY);
      }
      hastaField.setEnabled(!checked);
      hastaField.setAlpha(checked ? 0.5f : 1f);
    });
    permanenteRow.addView(permanenteCheck);
    actionPanel.addView(permanenteRow, rowParams);
  }

  private void renderTipo() {
    if (tipoContainer == null) {
      return;
    }
    tipoContainer.removeAllViews();
    tipoContainer.addView(makeSelectableRow("Apto", TIPO_APTO.equals(selectedTipo),
        v -> selectTipo(TIPO_APTO)));
    tipoContainer.addView(makeSelectableRow("Apto con observación",
        TIPO_APTO_OBSERVACION.equals(selectedTipo), v -> selectTipo(TIPO_APTO_OBSERVACION)));
    tipoContainer.addView(makeSelectableRow("No apto", TIPO_NO_APTO.equals(selectedTipo),
        v -> selectTipo(TIPO_NO_APTO)));

    boolean nonApto = isNonApto(selectedTipo);
    motivoSection.setVisibility(nonApto ? View.VISIBLE : View.GONE);
    desdeBlock.setVisibility(nonApto ? View.VISIBLE : View.GONE);
    hastaBlock.setVisibility(nonApto ? View.VISIBLE : View.GONE);
    permanenteRow.setVisibility(TIPO_NO_APTO.equals(selectedTipo) ? View.VISIBLE : View.GONE);
  }

  private boolean isNonApto(String tipo) {
    return TIPO_APTO_OBSERVACION.equals(tipo) || TIPO_NO_APTO.equals(tipo);
  }

  private void selectTipo(String tipo) {
    selectedTipo = tipo;
    hideError(tipoError);
    renderTipo();
  }

  private void loadMotivos() {
    if (motivosLoading) {
      return;
    }
    List<MotivoDto> cached = catalogoRepository.getCachedMotivos();
    if (cached != null) {
      motivos = cached;
      motivosLoading = false;
      renderMotivos();
      return;
    }
    motivos = null;
    motivosLoading = true;
    renderMotivos();

    catalogoRepository.listMotivos(new CatalogoRepository.Callback<List<MotivoDto>>() {
      @Override
      public void onSuccess(List<MotivoDto> data) {
        motivos = data;
        motivosLoading = false;
        renderMotivos();
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        motivosLoading = false;
        if (motivos == null) {
          motivos = null;
          renderMotivos();
        }
      }
    });
  }

  private void renderMotivos() {
    if (motivoContainer == null) {
      return;
    }
    motivoContainer.removeAllViews();

    if (motivosLoading) {
      motivoStatus.setText("Cargando motivos…");
      motivoStatus.setTextColor(AppColors.TEXT_SECONDARY);
      motivoStatus.setVisibility(View.VISIBLE);
      return;
    }
    if (motivos == null) {
      motivoStatus.setText("No se pudieron cargar los motivos. Tocá para reintentar.");
      motivoStatus.setTextColor(AppColors.ERROR);
      motivoStatus.setVisibility(View.VISIBLE);
      return;
    }
    if (motivos.isEmpty()) {
      motivoStatus.setText("No hay motivos cargados en el sistema.");
      motivoStatus.setTextColor(AppColors.TEXT_SECONDARY);
      motivoStatus.setVisibility(View.VISIBLE);
      return;
    }

    motivoStatus.setVisibility(View.GONE);
    for (final MotivoDto motivo : motivos) {
      boolean selected = motivo.id != null && motivo.id.equals(selectedMotivoId);
      motivoContainer.addView(makeSelectableRow(motivo.displayName(), selected,
          v -> selectMotivo(motivo)));
    }
  }

  private void selectMotivo(MotivoDto motivo) {
    selectedMotivoId = motivo.id;
    hideError(motivoError);
    renderMotivos();
  }

  private void showDatePicker(final boolean desde) {
    String title = desde ? "Fecha desde" : "Fecha hasta";
    MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText(title)
        .build();
    picker.addOnPositiveButtonClickListener(selection -> {
      if (desde) {
        selectedDesde = apiDate(selection);
        desdeValue.setText(displayDate(selection));
        desdeValue.setTextColor(AppColors.TEXT_PRIMARY);
        hideError(desdeError);
      } else {
        selectedHasta = apiDate(selection);
        hastaValue.setText(displayDate(selection));
        hastaValue.setTextColor(AppColors.TEXT_PRIMARY);
        hideError(hastaError);
      }
    });
    picker.show(getSupportFragmentManager(), desde ? "desde_picker" : "hasta_picker");
  }

  // --- Submit ---

  private void submit() {
    if (submitting) {
      return;
    }
    clearActionErrors();
    if (selectedPaciente == null || selectedPaciente.dni == null) {
      state.showError("Buscá un paciente primero", this::search);
      return;
    }
    if (selectedTipo == null) {
      showError(tipoError, "Elegí la nueva situación");
      return;
    }

    boolean nonApto = isNonApto(selectedTipo);
    if (nonApto) {
      if (selectedMotivoId == null) {
        showError(motivoError, "Elegí un motivo");
        return;
      }
      if (selectedDesde == null) {
        showError(desdeError, "Elegí la fecha desde");
        return;
      }
      if (selectedHasta != null && selectedHasta.compareTo(selectedDesde) < 0) {
        showError(hastaError, "La fecha hasta no puede ser anterior a desde");
        return;
      }
    }

    Boolean permanente = TIPO_NO_APTO.equals(selectedTipo)
        ? permanenteCheck.isChecked() : null;
    UpdateAptitudRequest body = new UpdateAptitudRequest(
        selectedTipo,
        nonApto ? selectedMotivoId : null,
        nonApto ? selectedDesde : null,
        nonApto ? selectedHasta : null,
        permanente);

    setSubmitting(true);
    state.showLoading();

    aptitudRepository.update(selectedPaciente.dni, body,
        new AptitudRepository.Callback<PacienteDto>() {
          @Override
          public void onSuccess(PacienteDto data) {
            setSubmitting(false);
            state.hide();
            Toast.makeText(AptitudActivity.this, "Aptitud actualizada",
                Toast.LENGTH_SHORT).show();
            if (data != null) {
              selectedPaciente = data;
            }
            resetAction();
            showPatient(selectedPaciente);
          }

          @Override
          public void onError(ApiError.Type type, String message) {
            setSubmitting(false);
            state.showError(messageFor(type, message), AptitudActivity.this::submit);
          }

          @Override
          public void onValidationError(Map<String, String> fieldErrors) {
            setSubmitting(false);
            state.hide();
            applyFieldErrors(fieldErrors);
          }
        });
  }

  private void applyFieldErrors(Map<String, String> errors) {
    boolean handled = false;
    if (errors.containsKey("tipo")) {
      showError(tipoError, "Elegí una situación válida");
      handled = true;
    }
    if (errors.containsKey("motivo_id")) {
      showError(motivoError, "Elegí un motivo válido");
      handled = true;
    }
    if (errors.containsKey("desde")) {
      showError(desdeError, "Elegí una fecha desde válida");
      handled = true;
    }
    if (errors.containsKey("hasta")) {
      showError(hastaError, "La fecha hasta no puede ser anterior a desde");
      handled = true;
    }
    if (!handled) {
      String first = errors.values().iterator().hasNext()
          ? errors.values().iterator().next() : null;
      state.showError(first != null && !first.isEmpty()
          ? first : "No se pudo actualizar la aptitud", this::submit);
    }
  }

  private void resetAction() {
    selectedTipo = null;
    selectedMotivoId = null;
    selectedDesde = null;
    selectedHasta = null;
    if (permanenteCheck != null) {
      permanenteCheck.setChecked(false);
    }
    if (desdeValue != null) {
      desdeValue.setText("");
    }
    if (hastaValue != null) {
      hastaValue.setText("");
    }
    if (hastaField != null) {
      hastaField.setEnabled(true);
      hastaField.setAlpha(1f);
    }
    clearActionErrors();
    renderTipo();
    renderMotivos();
  }

  private void resetAll() {
    dniInput.getEditText().setText("");
    dniInput.setError(null);
    selectedPaciente = null;
    state.hide();
    hidePanels();
    resetAction();
  }

  private void hidePanels() {
    if (patientPanel != null) {
      patientPanel.setVisibility(View.GONE);
    }
    if (actionPanel != null) {
      actionPanel.setVisibility(View.GONE);
    }
  }

  // --- Helpers de UI ---

  /** Fila seleccionable reutilizada por el selector de aptitud y de motivo. */
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

  /**
   * Campo clickable estilo Turnos. Devuelve la columna [campo 64dp, error] para
   * poder mostrar/ocultar el bloque completo.
   */
  private LinearLayout makeFieldBlock(String label, TextView value) {
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
    field.addView(value);

    TextView error = makeErrorText();

    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    column.addView(field, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(64, getResources())));
    column.addView(error);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    column.setLayoutParams(params);
    return column;
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

  private void clearActionErrors() {
    hideError(tipoError);
    hideError(motivoError);
    hideError(desdeError);
    hideError(hastaError);
  }

  private GradientDrawable rounded(int color, float radiusDp) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(AppSpacing.dp((int) radiusDp, getResources()));
    return drawable;
  }

  private void setSubmitting(boolean value) {
    submitting = value;
    if (submitButton != null) {
      submitButton.setEnabled(!value);
      submitButton.setText(value ? "Guardando…" : "Guardar aptitud");
    }
  }

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
            ? message : "No se pudo actualizar la aptitud";
      default:
        return message != null && !message.isEmpty()
            ? message : "No se pudo completar la operación";
    }
  }
}
