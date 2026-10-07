package com.donemosmobile.ui.pacientes;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.CreatePacienteRequest;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.util.Map;

/**
 * Alta de paciente. Formulario de una sola tarea: valida inline, da de alta
 * contra {@code POST /api/pacientes} y refleja los errores 422 por campo
 * (por ejemplo DNI duplicado) debajo del campo que corresponde.
 */
public class PacientesActivity extends AppActivity {

  private PacienteRepository repository;

  private AppInput dniInput;
  private AppInput nombreInput;
  private AppInput apellidoInput;
  private AppInput telefonoInput;
  private AppInput alturaInput;
  private AppButton sexoMasculino;
  private AppButton sexoFemenino;
  private String selectedSexo;

  private StateView state;
  private AppButton submitButton;
  private TextView successBanner;

  private boolean submitting;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    repository = new PacienteRepository(this);
    setContentView(buildView());
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
    buildForm(content);

    ScrollView scroller = new ScrollView(this);
    scroller.setFillViewport(true);
    scroller.addView(content, new ScrollView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    root.addView(scroller, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

    root.addView(new SectionNavBar(this, null), new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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

    TextView back = new TextView(this);
    back.setText("\u2039 Volver");
    back.setTextSize(15);
    back.setTextColor(AppColors.PRIMARY);
    back.setPadding(0, AppSpacing.dp(AppSpacing.XS, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()), AppSpacing.dp(AppSpacing.XS, getResources()));
    back.setClickable(true);
    back.setFocusable(true);
    back.setOnClickListener(v -> finish());
    header.addView(back);

    TextView title = new TextView(this);
    title.setText("Nuevo paciente");
    AppTypography.headline1(title);
    header.addView(title);

    return header;
  }

  private void buildForm(LinearLayout root) {
    TextView help = new TextView(this);
    help.setText("Completá los datos del paciente. El DNI no puede repetirse.");
    AppTypography.caption(help);
    LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    helpParams.bottomMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    root.addView(help, helpParams);

    dniInput = addInput(root, "DNI", InputType.TYPE_CLASS_NUMBER);
    nombreInput = addInput(root, "Nombre", InputType.TYPE_CLASS_TEXT);
    apellidoInput = addInput(root, "Apellido", InputType.TYPE_CLASS_TEXT);
    telefonoInput = addInput(root, "Teléfono", InputType.TYPE_CLASS_PHONE);

    addSexoSelector(root);

    alturaInput = addInput(root, "Altura en cm (opcional)",
        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

    state = new StateView(this);
    root.addView(state);

    successBanner = new TextView(this);
    successBanner.setTextColor(AppColors.SUCCESS);
    successBanner.setTextSize(14);
    successBanner.setVisibility(View.GONE);
    LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    bannerParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    root.addView(successBanner, bannerParams);

    submitButton = new AppButton(this, "Crear paciente", AppButton.Style.FILLED, v -> submit());
    LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(48, getResources()));
    submitParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    root.addView(submitButton, submitParams);

    AppButton clear = new AppButton(this, "Limpiar formulario", AppButton.Style.TEXT,
        v -> {
          clearForm();
          state.hide();
          successBanner.setVisibility(View.GONE);
        });
    root.addView(clear);
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

  private void addSexoSelector(LinearLayout root) {
    TextView label = new TextView(this);
    label.setText("Sexo (opcional)");
    AppTypography.caption(label);
    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    labelParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    root.addView(label, labelParams);

    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());

    sexoMasculino = new AppButton(this, "Masculino", AppButton.Style.OUTLINED,
        v -> selectSexo("M"));
    sexoFemenino = new AppButton(this, "Femenino", AppButton.Style.OUTLINED,
        v -> selectSexo("F"));
    row.addView(sexoMasculino, new LinearLayout.LayoutParams(0, AppSpacing.dp(44, getResources()), 1));
    View gap = new View(this);
    row.addView(gap, new LinearLayout.LayoutParams(AppSpacing.dp(AppSpacing.SM, getResources()), 1));
    row.addView(sexoFemenino, new LinearLayout.LayoutParams(0, AppSpacing.dp(44, getResources()), 1));
    root.addView(row, rowParams);

    renderSexo();
  }

  private void selectSexo(String sexo) {
    // Tocar la opcion ya elegida la deselecciona (queda opcional).
    selectedSexo = sexo.equals(selectedSexo) ? null : sexo;
    renderSexo();
  }

  private void renderSexo() {
    if (sexoMasculino == null || sexoFemenino == null) {
      return;
    }
    applySexoState(sexoMasculino, "M".equals(selectedSexo));
    applySexoState(sexoFemenino, "F".equals(selectedSexo));
  }

  private void applySexoState(AppButton button, boolean selected) {
    button.setBackgroundTintList(ColorStateList.valueOf(
        selected ? AppColors.PRIMARY : 0));
    button.setStrokeColor(ColorStateList.valueOf(AppColors.PRIMARY));
    button.setTextColor(selected ? AppColors.TEXT_ON_PRIMARY : AppColors.PRIMARY);
  }

  private void submit() {
    if (submitting) {
      return;
    }
    clearErrors();
    successBanner.setVisibility(View.GONE);
    if (!validate()) {
      return;
    }

    Double altura = parseAltura();
    CreatePacienteRequest body = new CreatePacienteRequest(
        dniInput.getText(), nombreInput.getText(), apellidoInput.getText(),
        telefonoInput.getText(), selectedSexo, altura);

    setSubmitting(true);
    state.showLoading();

    repository.create(body, new PacienteRepository.Callback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        setSubmitting(false);
        state.hide();
        Toast.makeText(PacientesActivity.this, "Paciente creado", Toast.LENGTH_SHORT).show();
        String name = data != null ? data.displayName() : "";
        successBanner.setText(name.isEmpty()
            ? "Paciente creado correctamente."
            : "Paciente " + name + " creado correctamente.");
        successBanner.setVisibility(View.VISIBLE);
        clearForm();
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        setSubmitting(false);
        state.showError(messageFor(type, message), PacientesActivity.this::submit);
      }

      @Override
      public void onValidationError(Map<String, String> fieldErrors) {
        setSubmitting(false);
        state.hide();
        applyFieldErrors(fieldErrors);
      }
    });
  }

  private boolean validate() {
    boolean valid = true;

    String dni = dniInput.getText();
    if (dni.isEmpty()) {
      dniInput.setError("Ingresá el DNI");
      valid = false;
    } else if (!dni.matches("\\d+")) {
      dniInput.setError("El DNI debe ser numérico");
      valid = false;
    }

    if (nombreInput.getText().isEmpty()) {
      nombreInput.setError("Ingresá el nombre");
      valid = false;
    }
    if (apellidoInput.getText().isEmpty()) {
      apellidoInput.setError("Ingresá el apellido");
      valid = false;
    }

    String telefono = telefonoInput.getText();
    if (telefono.isEmpty()) {
      telefonoInput.setError("Ingresá el teléfono");
      valid = false;
    } else if (!telefono.matches("\\d+")) {
      telefonoInput.setError("El teléfono debe ser numérico");
      valid = false;
    }

    String alturaText = alturaInput.getText();
    if (!alturaText.isEmpty()) {
      try {
        double value = Double.parseDouble(alturaText);
        if (value <= 0 || value > 250) {
          alturaInput.setError("La altura debe ser mayor a 0 y hasta 250 cm");
          valid = false;
        }
      } catch (NumberFormatException e) {
        alturaInput.setError("Altura inválida");
        valid = false;
      }
    }

    return valid;
  }

  private Double parseAltura() {
    String text = alturaInput.getText();
    if (text.isEmpty()) {
      return null;
    }
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private void applyFieldErrors(Map<String, String> errors) {
    boolean inline = false;
    inline |= setIfPresent(dniInput, "dni", errors);
    inline |= setIfPresent(nombreInput, "nombre", errors);
    inline |= setIfPresent(apellidoInput, "apellido", errors);
    inline |= setIfPresent(telefonoInput, "telefono", errors);
    inline |= setIfPresent(alturaInput, "altura", errors);

    if (!inline) {
      // Error de validacion sin campo mapeado: estado honesto y reintento.
      String first = errors.values().iterator().hasNext()
          ? errors.values().iterator().next() : null;
      String message = first != null && !first.isEmpty()
          ? first : "No se pudo crear el paciente";
      state.showError(message, this::submit);
    }
  }

  private boolean setIfPresent(AppInput input, String field, Map<String, String> errors) {
    if (!errors.containsKey(field)) {
      return false;
    }
    input.setError(fieldMessage(field, errors.get(field)));
    return true;
  }

  /** Traduce el mensaje 422 (en ingles) a un copy espanol por campo. */
  private String fieldMessage(String field, String backendMessage) {
    boolean taken = backendMessage != null
        && backendMessage.toLowerCase(java.util.Locale.ROOT).contains("taken");
    switch (field) {
      case "dni":
        return taken ? "Ya existe un paciente con ese DNI" : "DNI inválido";
      case "nombre":
        return "Nombre inválido";
      case "apellido":
        return "Apellido inválido";
      case "telefono":
        return "Teléfono inválido";
      case "sexo":
        return "Sexo inválido";
      case "altura":
        return "La altura debe ser mayor a 0 y hasta 250 cm";
      default:
        return backendMessage != null && !backendMessage.isEmpty()
            ? backendMessage : "Dato inválido";
    }
  }

  private void clearForm() {
    dniInput.getEditText().setText("");
    nombreInput.getEditText().setText("");
    apellidoInput.getEditText().setText("");
    telefonoInput.getEditText().setText("");
    alturaInput.getEditText().setText("");
    selectedSexo = null;
    renderSexo();
    clearErrors();
  }

  private void clearErrors() {
    dniInput.setError(null);
    nombreInput.setError(null);
    apellidoInput.setError(null);
    telefonoInput.setError(null);
    alturaInput.setError(null);
  }

  private void setSubmitting(boolean value) {
    submitting = value;
    submitButton.setEnabled(!value);
    submitButton.setText(value ? "Guardando…" : "Crear paciente");
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
            ? message : "No se pudo crear el paciente";
      default:
        return message != null && !message.isEmpty()
            ? message : "No se pudo crear el paciente";
    }
  }
}
