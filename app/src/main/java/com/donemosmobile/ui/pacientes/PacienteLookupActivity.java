package com.donemosmobile.ui.pacientes;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.ui.ApiMessages;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Búsqueda rápida de un paciente por DNI: input, botón y card de resultado.
 * Pensada para el mostrador: un campo, una acción, un resultado.
 */
public class PacienteLookupActivity extends AppActivity {

  private AppInput dniInput;
  private StateView state;
  private LinearLayout resultContainer;
  private PacienteRepository repository;

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

    AppButton searchButton = new AppButton(this, "Buscar", AppButton.Style.FILLED, v -> search());
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

  private void search() {
    String dni = dniInput.getText();
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

    repository.findByDni(dni, new PacienteRepository.Callback<PacienteDto>() {
      @Override
      public void onSuccess(PacienteDto data) {
        state.hide();
        showResult(data);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
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

    AppCard card = new AppCard(this, content);
    resultContainer.addView(card);
  }

  private void addLine(LinearLayout parent, String label, String value) {
    TextView line = new TextView(this);
    line.setText(label + ": " + value);
    AppTypography.body(line);
    parent.addView(line, Ui.matchWrap(this, AppSpacing.XS));
  }
}
