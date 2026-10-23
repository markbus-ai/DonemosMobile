package com.donemosmobile.ui.pacientes;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.NumericBounds;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.util.Map;

/**
 * Búsqueda rápida de un paciente por DNI: input, botón y card de resultado.
 * Pensada para el mostrador: un campo, una acción, un resultado.
 */
public class PacienteLookupActivity extends AppActivity {

  private AppInput dniInput;
  private StateView state;
  private LinearLayout resultContainer;
  private PacienteRepository repository;
  private AppButton searchButton;
  /** DNI of the latest lookup; older responses are ignored. */
  private String pendingDni;
  private boolean savingCiclo;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    repository = new PacienteRepository(this);
    setContentView(buildView());
  }

  private View buildView() {
    ScreenScaffold root = new ScreenScaffold(this, Section.PACIENTES);
    root.setHeader(new ScreenHeader(this)
        .title("Buscar paciente")
        .subtitle("Ingresá el DNI para ver la ficha", AppSpacing.SM));

    LinearLayout content = root.content();
    content.setPadding(
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.MD),
        0);

    dniInput = new AppInput(this, "DNI");
    dniInput.getEditText().setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    content.addView(dniInput, Ui.matchWrap(this));

    searchButton = new AppButton(this, "Buscar", AppButton.Style.FILLED, v -> search());
    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        Ui.dp(this, 48));
    buttonParams.topMargin = Ui.dp(this, AppSpacing.MD);
    content.addView(searchButton, buttonParams);

    state = new StateView(this);
    content.addView(state);

    resultContainer = new LinearLayout(this);
    resultContainer.setOrientation(LinearLayout.VERTICAL);
    resultContainer.setPadding(
        0,
        Ui.dp(this, AppSpacing.SM),
        0,
        Ui.dp(this, AppSpacing.XL));
    content.addView(resultContainer);

    return root;
  }

  private void setSearching(boolean searching) {
    searchButton.setEnabled(!searching);
    searchButton.setText(searching ? "Buscando…" : "Buscar");
  }

  /** True when the activity is gone or a newer search replaced this one. */
  private boolean isStale(String dni) {
    return isFinishing() || isDestroyed() || !dni.equals(pendingDni);
  }

  private void search() {
    final String dni = dniInput.getText();
    if (dni.isEmpty()) {
      state.showError("Ingresá un DNI", null);
      return;
    }

    // Cache-first: si ya se buscó este DNI en la sesión, mostramos la ficha al
    // toque y revalidamos en 2do plano.
    final PacienteDto cached = repository.getCachedByDni(dni);
    resultContainer.removeAllViews();
    if (cached != null) {
      state.hide();
      showResult(cached);
    } else {
      state.showLoading();
    }

    pendingDni = dni;
    setSearching(true);
    repository.findByDni(dni, new PacienteRepository.Callback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        if (isStale(dni)) {
          return;
        }
        setSearching(false);
        state.hide();
        showResult(data);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        if (isStale(dni)) {
          return;
        }
        setSearching(false);
        if (type == ApiError.Type.NOT_FOUND) {
          resultContainer.removeAllViews();
          state.showEmpty("No existe un paciente con ese DNI");
          return;
        }
        // Con cache ya en pantalla no pisamos el contenido con un error.
        if (cached != null) {
          return;
        }
        state.showError(ApiMessages.forType(type, message, "No se pudo buscar"),
            PacienteLookupActivity.this::search);
      }
    });
  }

  private void showResult(PacienteDto paciente) {
    resultContainer.removeAllViews();

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);

    TextView name = new TextView(this);
    name.setText(paciente.displayName());
    AppTypography.headline2(name);
    content.addView(name);

    addLine(content, "DNI", Ui.safe(paciente.dni));
    addLine(content, "Teléfono", Ui.safe(paciente.telefono));
    if (paciente.sexo != null && !paciente.sexo.isEmpty()) {
      addLine(content, "Sexo", paciente.sexo);
    }
    if (paciente.altura != null) {
      addLine(content, "Altura", paciente.altura + " cm");
    }

    // Posicion actual del ciclo: la mantiene el sistema, siempre visible con
    // guion si falta.
    addLine(content, "Ciclo", cycleValue(paciente.cicloActual));
    addLine(content, "Serie", cycleValue(paciente.serieActual));
    addLine(content, "Donación", cycleValue(paciente.donacionActual));

    AppCard card = new AppCard(this, content);
    resultContainer.addView(card);

    AppButton edit = new AppButton(this, "Editar ciclo", AppButton.Style.OUTLINED,
        v -> promptEditCiclo(paciente));
    LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 44));
    editParams.topMargin = Ui.dp(this, AppSpacing.SM);
    resultContainer.addView(edit, editParams);

    TextView note = new TextView(this);
    note.setText("La posición en el ciclo la calcula el sistema al registrar cada "
        + "donación; editala solo para fijarla manualmente.");
    AppTypography.caption(note);
    resultContainer.addView(note, Ui.matchWrap(this, AppSpacing.XS));
  }

  private String cycleValue(Integer value) {
    return value != null ? String.valueOf(value) : "—";
  }

  private void addLine(LinearLayout parent, String label, String value) {
    TextView line = new TextView(this);
    line.setText(label + ": " + value);
    AppTypography.body(line);
    parent.addView(line, Ui.matchWrap(this, AppSpacing.XS));
  }

  // --- Editar ciclo (manual, con reinicio) ---

  private void promptEditCiclo(PacienteDto paciente) {
    if (paciente == null || paciente.dni == null) {
      return;
    }

    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    int pad = Ui.dp(this, AppSpacing.MD);
    form.setPadding(pad, Ui.dp(this, AppSpacing.SM), pad, 0);

    TextView helper = new TextView(this);
    helper.setText("Carga manual. Completá los tres valores, o dejá los tres vacíos para reiniciar el ciclo.");
    AppTypography.caption(helper);
    form.addView(helper, Ui.matchWrapBottom(this, AppSpacing.SM));

    AppInput ciclo = cycleField(form, "Ciclo", paciente.cicloActual);
    AppInput serie = cycleField(form, "Serie", paciente.serieActual);
    AppInput donacion = cycleField(form, "Donación (orden dentro de la serie)",
        paciente.donacionActual);

    ErrorText error = new ErrorText(this);
    form.addView(error);

    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle("Editar ciclo de donaciones")
        .setView(form)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Guardar", null)
        .create();

    dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(v -> saveCiclo(paciente, dialog,
            dialog.getButton(AlertDialog.BUTTON_POSITIVE), ciclo, serie, donacion, error)));
    dialog.show();
  }

  private AppInput cycleField(LinearLayout parent, String hint, Integer current) {
    AppInput input = new AppInput(this, hint);
    input.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    if (current != null) {
      input.getEditText().setText(String.valueOf(current));
    }
    parent.addView(input, Ui.matchWrap(this, AppSpacing.SM));
    return input;
  }

  private void saveCiclo(PacienteDto paciente, AlertDialog dialog, Button guardar,
      AppInput ciclo, AppInput serie, AppInput donacion, ErrorText error) {
    if (savingCiclo) {
      return;
    }
    String invalid = NumericBounds.checkCycle(ciclo.getText(), serie.getText(),
        donacion.getText());
    if (invalid != null) {
      error.show(invalid);
      return;
    }
    error.hide();
    savingCiclo = true;
    guardar.setEnabled(false);
    guardar.setText("Guardando…");

    repository.updateCiclo(paciente.dni,
        Ui.parseIntOrNull(ciclo.getText()),
        Ui.parseIntOrNull(serie.getText()),
        Ui.parseIntOrNull(donacion.getText()),
        new PacienteRepository.Callback<PacienteDto>() {
          @Override
          public void onSuccess(PacienteDto data) {
            savingCiclo = false;
            // The user may have left the screen while the request was in flight.
            if (isFinishing() || isDestroyed()) {
              return;
            }
            if (dialog.isShowing()) {
              dialog.dismiss();
            }
            Toast.makeText(PacienteLookupActivity.this, "Ciclo actualizado",
                Toast.LENGTH_SHORT).show();
            if (data != null) {
              showResult(data);
            }
          }

          @Override
          public void onValidationError(Map<String, String> fieldErrors) {
            restoreSaveButton(guardar);
            String first = fieldErrors.values().iterator().hasNext()
                ? fieldErrors.values().iterator().next() : null;
            error.show(first != null && !first.isEmpty()
                ? first : "No se pudo actualizar el ciclo");
          }

          @Override
          public void onError(ApiError.Type type, String message) {
            restoreSaveButton(guardar);
            error.show(ApiMessages.forType(type, message, "No se pudo actualizar el ciclo"));
          }
        });
  }

  private void restoreSaveButton(Button guardar) {
    savingCiclo = false;
    if (isFinishing() || isDestroyed()) {
      return;
    }
    guardar.setEnabled(true);
    guardar.setText("Guardar");
  }
}
