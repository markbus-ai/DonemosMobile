package com.donemosmobile.ui.pacientes;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.pacientes.PacienteRepository;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.components.StateView;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;
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

  private LinearLayout buildView() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    TextView title = new TextView(this);
    title.setText("Buscar paciente");
    AppTypography.headline1(title);
    title.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    root.addView(title);

    TextView subtitle = new TextView(this);
    subtitle.setText("Ingresá el DNI para ver la ficha");
    AppTypography.caption(subtitle);
    subtitle.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    root.addView(subtitle);

    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);

    dniInput = new AppInput(this, "DNI");
    dniInput.getEditText().setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER);
    form.addView(dniInput);

    com.donemosmobile.ui.components.AppButton searchButton =
        new com.donemosmobile.ui.components.AppButton(
            this, "Buscar", com.donemosmobile.ui.components.AppButton.Style.FILLED,
            v -> search());
    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        AppSpacing.dp(48, getResources()));
    buttonParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    form.addView(searchButton, buttonParams);

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.addView(form);

    state = new StateView(this);
    content.addView(state);

    resultContainer = new LinearLayout(this);
    resultContainer.setOrientation(LinearLayout.VERTICAL);
    resultContainer.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XL, getResources()));
    content.addView(resultContainer);

    // Contenido scrolleable: deja la barra inferior siempre visible.
    ScrollView scroller = new ScrollView(this);
    scroller.setFillViewport(true);
    scroller.addView(content, new ScrollView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    root.addView(scroller, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

    root.addView(new SectionNavBar(this, Section.PACIENTES), new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

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
        state.showError(messageFor(type, message), PacienteLookupActivity.this::search);
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

    addLine(content, "DNI", safe(paciente.dni));
    addLine(content, "Teléfono", safe(paciente.telefono));
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
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    parent.addView(line, params);
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
      default:
        return message != null && !message.isEmpty() ? message : "No se pudo buscar";
    }
  }
}
