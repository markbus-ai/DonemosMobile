package com.donemosmobile.ui.aptitud;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
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
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.AptitudLabels;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.DateField;
import com.donemosmobile.ui.components.PatientPicker;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
import com.donemosmobile.ui.components.SelectGroup;
import com.donemosmobile.ui.components.SelectableRow;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.util.List;
import java.util.Map;

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

  private PatientPicker picker;
  private StateView state;

  private AppCard patientPanel;
  private TextView patientName;
  private TextView patientDni;
  private TextView currentAptitud;

  private LinearLayout actionPanel;
  private SelectGroup tipoGroup;
  private SelectGroup motivoGroup;
  private DateField desdeField;
  private DateField hastaField;
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

  private View buildView() {
    ScreenScaffold root = new ScreenScaffold(this, Section.APTITUD);
    root.setHeader(new ScreenHeader(this)
        .title("Aptitud")
        .subtitle("Buscá al paciente y definí su aptitud para donar."));

    LinearLayout content = root.content();
    content.setPadding(
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.XL));

    buildLookup(content);
    buildPatientPanel(content);
    buildActionPanel(content);

    return root;
  }

  // --- Paso 1: buscar paciente ---

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
        state.showError(
            ApiMessages.forType(type, message, "No se pudo actualizar la aptitud",
                "No se pudo completar la operación"),
            AptitudActivity.this::search);
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
    content.addView(currentAptitud, Ui.matchWrap(this, AppSpacing.SM));

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
    currentAptitud.setText("Aptitud actual: " + AptitudLabels.current(paciente));
    patientPanel.setVisibility(View.VISIBLE);
    actionPanel.setVisibility(View.VISIBLE);
  }

  // --- Paso 3: nueva aptitud ---

  private void buildActionPanel(LinearLayout root) {
    actionPanel = new LinearLayout(this);
    actionPanel.setOrientation(LinearLayout.VERTICAL);
    actionPanel.setVisibility(View.GONE);

    Ui.sectionTitle(actionPanel, this, "Nueva situación", 0);

    tipoGroup = new SelectGroup(this, "Elegí la aptitud", AppSpacing.MD);
    actionPanel.addView(tipoGroup);

    motivoGroup = new SelectGroup(this, "Motivo", AppSpacing.MD);
    motivoGroup.status().setOnClickListener(v -> loadMotivos());
    actionPanel.addView(motivoGroup);

    desdeField = new DateField(this, "Desde", false, this, "Fecha desde", "desde_picker");
    desdeField.setOnDateSelected((apiDate, displayDate) -> selectedDesde = apiDate);
    actionPanel.addView(desdeField);

    hastaField = new DateField(this, "Hasta (opcional)", false, this,
        "Fecha hasta", "hasta_picker");
    hastaField.setOnDateSelected((apiDate, displayDate) -> selectedHasta = apiDate);
    actionPanel.addView(hastaField);

    buildPermanenteRow();

    submitButton = new AppButton(this, "Guardar aptitud", AppButton.Style.FILLED, v -> submit());
    LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
    submitParams.topMargin = Ui.dp(this, AppSpacing.MD);
    actionPanel.addView(submitButton, submitParams);

    root.addView(actionPanel);
    renderTipo();
  }

  private void buildPermanenteRow() {
    permanenteRow = new LinearLayout(this);
    permanenteRow.setOrientation(LinearLayout.HORIZONTAL);
    permanenteRow.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = Ui.dp(this, AppSpacing.SM);

    permanenteCheck = new CheckBox(this);
    permanenteCheck.setText("Permanente (sin fecha de fin)");
    permanenteCheck.setTextColor(AppColors.TEXT_PRIMARY);
    permanenteCheck.setTextSize(14);
    permanenteCheck.setOnCheckedChangeListener((button, checked) -> {
      if (checked) {
        selectedHasta = null;
        hastaField.value().setText("Sin fecha de fin");
        hastaField.value().setTextColor(AppColors.TEXT_SECONDARY);
        hastaField.error().hide();
      } else if (selectedHasta == null) {
        hastaField.value().setText("");
        hastaField.value().setTextColor(AppColors.TEXT_SECONDARY);
      }
      hastaField.setFieldEnabled(!checked);
    });
    permanenteRow.addView(permanenteCheck);
    actionPanel.addView(permanenteRow, rowParams);
  }

  private void renderTipo() {
    if (tipoGroup == null) {
      return;
    }
    tipoGroup.options().removeAllViews();
    tipoGroup.options().addView(new SelectableRow(this, "Apto", TIPO_APTO.equals(selectedTipo),
        v -> selectTipo(TIPO_APTO)));
    tipoGroup.options().addView(new SelectableRow(this, "Apto con observación",
        TIPO_APTO_OBSERVACION.equals(selectedTipo), v -> selectTipo(TIPO_APTO_OBSERVACION)));
    tipoGroup.options().addView(new SelectableRow(this, "No apto",
        TIPO_NO_APTO.equals(selectedTipo), v -> selectTipo(TIPO_NO_APTO)));

    boolean nonApto = isNonApto(selectedTipo);
    motivoGroup.setVisibility(nonApto ? View.VISIBLE : View.GONE);
    desdeField.setVisibility(nonApto ? View.VISIBLE : View.GONE);
    hastaField.setVisibility(nonApto ? View.VISIBLE : View.GONE);
    permanenteRow.setVisibility(TIPO_NO_APTO.equals(selectedTipo) ? View.VISIBLE : View.GONE);
  }

  private boolean isNonApto(String tipo) {
    return TIPO_APTO_OBSERVACION.equals(tipo) || TIPO_NO_APTO.equals(tipo);
  }

  private void selectTipo(String tipo) {
    selectedTipo = tipo;
    tipoGroup.error().hide();
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
    if (motivoGroup == null) {
      return;
    }
    motivoGroup.options().removeAllViews();

    TextView status = motivoGroup.status();
    if (motivosLoading) {
      status.setText("Cargando motivos…");
      status.setTextColor(AppColors.TEXT_SECONDARY);
      status.setVisibility(View.VISIBLE);
      return;
    }
    if (motivos == null) {
      status.setText("No se pudieron cargar los motivos. Tocá para reintentar.");
      status.setTextColor(AppColors.ERROR);
      status.setVisibility(View.VISIBLE);
      return;
    }
    if (motivos.isEmpty()) {
      status.setText("No hay motivos cargados en el sistema.");
      status.setTextColor(AppColors.TEXT_SECONDARY);
      status.setVisibility(View.VISIBLE);
      return;
    }

    status.setVisibility(View.GONE);
    for (final MotivoDto motivo : motivos) {
      boolean selected = motivo.id != null && motivo.id.equals(selectedMotivoId);
      motivoGroup.options().addView(new SelectableRow(this, motivo.displayName(), selected,
          v -> selectMotivo(motivo)));
    }
  }

  private void selectMotivo(MotivoDto motivo) {
    selectedMotivoId = motivo.id;
    motivoGroup.error().hide();
    renderMotivos();
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
      tipoGroup.error().show("Elegí la nueva situación");
      return;
    }

    boolean nonApto = isNonApto(selectedTipo);
    if (nonApto) {
      if (selectedMotivoId == null) {
        motivoGroup.error().show("Elegí un motivo");
        return;
      }
      if (selectedDesde == null) {
        desdeField.error().show("Elegí la fecha desde");
        return;
      }
      if (selectedHasta != null && selectedHasta.compareTo(selectedDesde) < 0) {
        hastaField.error().show("La fecha hasta no puede ser anterior a desde");
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
            state.showError(
                ApiMessages.forType(type, message, "No se pudo actualizar la aptitud",
                    "No se pudo completar la operación"),
                AptitudActivity.this::submit);
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
      tipoGroup.error().show("Elegí una situación válida");
      handled = true;
    }
    if (errors.containsKey("motivo_id")) {
      motivoGroup.error().show("Elegí un motivo válido");
      handled = true;
    }
    if (errors.containsKey("desde")) {
      desdeField.error().show("Elegí una fecha desde válida");
      handled = true;
    }
    if (errors.containsKey("hasta")) {
      hastaField.error().show("La fecha hasta no puede ser anterior a desde");
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
    if (desdeField != null) {
      desdeField.resetValue();
    }
    if (hastaField != null) {
      hastaField.resetValue();
      hastaField.setFieldEnabled(true);
    }
    clearActionErrors();
    renderTipo();
    renderMotivos();
  }

  private void resetAll() {
    picker.dniInput().getEditText().setText("");
    picker.dniInput().setError(null);
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

  private void clearActionErrors() {
    tipoGroup.error().hide();
    motivoGroup.error().hide();
    desdeField.error().hide();
    hastaField.error().hide();
  }

  private void setSubmitting(boolean value) {
    submitting = value;
    if (submitButton != null) {
      submitButton.setEnabled(!value);
      submitButton.setText(value ? "Guardando…" : "Guardar aptitud");
    }
  }
}
