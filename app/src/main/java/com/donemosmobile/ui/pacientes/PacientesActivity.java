package com.donemosmobile.ui.pacientes;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.AppActivity;
import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

public class PacientesActivity extends AppActivity {

  public static final String EXTRA_MODE = "mode";
  public static final String MODE_CREATE = "create";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    boolean isCreate = MODE_CREATE.equals(getIntent().getStringExtra(EXTRA_MODE));

    String title = isCreate ? "Nuevo paciente" : "Pacientes";
    String subtitle = isCreate
        ? "Formulario de alta (por construir)"
        : "Listado de pacientes (por construir)";

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    TextView titleTv = new TextView(this);
    titleTv.setText(title);
    AppTypography.headline1(titleTv);
    titleTv.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    root.addView(titleTv, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    TextView subtitleTv = new TextView(this);
    subtitleTv.setText(subtitle);
    AppTypography.caption(subtitleTv);
    subtitleTv.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    root.addView(subtitleTv, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    root.addView(new View(this), new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    // Pantalla de acceso rápido: sin sección resaltada, para poder saltar a
    // cualquier sección desde la barra.
    root.addView(new SectionNavBar(this, null));

    setContentView(root);
  }
}
