package com.donemosmobile.ui.pacientes;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.AppActivity;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.CreatePacienteRequest;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.ui.A11y;
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.NumericBounds;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.FormActions;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
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

  private View buildView() {
    ScreenScaffold root = new ScreenScaffold(this, null);
    root.setHeader(new ScreenHeader(this).back(v -> finish()).title("Nuevo paciente"));

    LinearLayout content = root.content();
    content.setPadding(
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.SM),
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.XL));
    buildForm(content);
    return root;
  }

  private void buildForm(LinearLayout root) {
    TextView help = new TextView(this);
    help.setText("Completá los datos del paciente. El DNI no puede repetirse.");
    AppTypography.caption(help);
    root.addView(help, Ui.matchWrapBottom(this, AppSpacing.SM));

    dniInput = Ui.addInput(root, this, "DNI", InputType.TYPE_CLASS_NUMBER, AppSpacing.SM);
    nombreInput = Ui.addInput(root, this, "Nombre", InputType.TYPE_CLASS_TEXT, AppSpacing.SM);
    apellidoInput = Ui.addInput(root, this, "Apellido", InputType.TYPE_CLASS_TEXT, AppSpacing.SM);
    telefonoInput = Ui.addInput(root, this, "Teléfono", InputType.TYPE_CLASS_PHONE, AppSpacing.SM);
    // The API caps the column at 20 characters: never let the user type past it.
    telefonoInput.getEditText().setFilters(new android.text.InputFilter[] {
        new android.text.InputFilter.LengthFilter(NumericBounds.TELEFONO_MAX)});

    addSexoSelector(root);

    alturaInput = Ui.addInput(root, this, "Altura en cm (opcional)",
        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, AppSpacing.SM);

    state = new StateView(this);
    root.addView(state);

    successBanner = new TextView(this);
    successBanner.setTextColor(AppColors.SUCCESS_TEXT);
    successBanner.setTextSize(14);
    successBanner.setVisibility(View.GONE);
    root.addView(successBanner, Ui.matchWrap(this, AppSpacing.SM));

    FormActions actions = new FormActions(this);
    submitButton = actions.add("Crear paciente", AppButton.Style.FILLED, v -> submit(), 48,
        AppSpacing.MD);
    actions.add("Limpiar formulario", AppButton.Style.TEXT, v -> {
      clearForm();
      state.hide();
      successBanner.setVisibility(View.GONE);
    }, 48, 0);
    root.addView(actions);
  }

  private void addSexoSelector(LinearLayout root) {
    TextView label = new TextView(this);
    label.setText("Sexo (opcional)");
    AppTypography.caption(label);
    root.addView(label, Ui.matchWrap(this, AppSpacing.MD));

    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = Ui.dp(this, AppSpacing.XS);

    sexoMasculino = new AppButton(this, "Masculino", AppButton.Style.OUTLINED,
        v -> selectSexo("M"));
    sexoFemenino = new AppButton(this, "Femenino", AppButton.Style.OUTLINED,
        v -> selectSexo("F"));
    row.addView(sexoMasculino, new LinearLayout.LayoutParams(0, Ui.dp(this, A11y.MIN_TOUCH_DP), 1));
    View gap = new View(this);
    row.addView(gap, new LinearLayout.LayoutParams(Ui.dp(this, AppSpacing.SM), 1));
    row.addView(sexoFemenino, new LinearLayout.LayoutParams(0, Ui.dp(this, A11y.MIN_TOUCH_DP), 1));
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
        selected ? AppColors.TEXT_ACCENT : 0));
    button.setStrokeColor(ColorStateList.valueOf(AppColors.PRIMARY));
    button.setTextColor(selected ? AppColors.TEXT_ON_PRIMARY : AppColors.TEXT_ACCENT);
    // MaterialButton already has the Button role; expose the toggle state too.
    button.setSelected(selected);
    A11y.asButton(button, null);
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

    repository.create(body, new ApiCallback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        if (!isAlive()) {
          return;
        }
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
        if (!isAlive()) {
          return;
        }
        setSubmitting(false);
        state.showError(ApiMessages.forType(type, message, "No se pudo crear el paciente"),
            PacientesActivity.this::submit);
      }

      @Override
      public void onValidationError(Map<String, String> fieldErrors) {
        if (!isAlive()) {
          return;
        }
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
    } else if (!NumericBounds.isValidDni(dni)) {
      dniInput.setError("El DNI debe tener 7 u 8 dígitos y no empezar con 0");
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
    } else if (!NumericBounds.isValidTelefono(telefono)) {
      telefonoInput.setError("Teléfono inválido: hasta 20 caracteres entre dígitos, "
          + "espacios, ( ) o -, con + opcional al inicio");
      valid = false;
    }

    String alturaText = alturaInput.getText();
    if (!alturaText.isEmpty()) {
      // Accepts a decimal comma ("172,5") like the rest of the numeric fields.
      Double value = Ui.parseDoubleOrNull(alturaText);
      if (value == null) {
        alturaInput.setError("Altura inválida");
        valid = false;
      } else if (value <= 0 || value > 250) {
        alturaInput.setError("La altura debe ser mayor a 0 y hasta 250 cm");
        valid = false;
      }
    }

    return valid;
  }

  private Double parseAltura() {
    return Ui.parseDoubleOrNull(alturaInput.getText());
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

  /** Backend 422 message for the field (Spanish), with a per-field fallback. */
  private String fieldMessage(String field, String backendMessage) {
    String fallback;
    switch (field) {
      case "dni":
        fallback = "DNI inválido";
        break;
      case "nombre":
        fallback = "Nombre inválido";
        break;
      case "apellido":
        fallback = "Apellido inválido";
        break;
      case "telefono":
        fallback = "Teléfono inválido";
        break;
      case "altura":
        fallback = "La altura debe ser mayor a 0 y hasta 250 cm";
        break;
      default:
        fallback = "Dato inválido";
        break;
    }
    return ApiMessages.fieldOr(backendMessage, fallback);
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
}
