package com.donemosmobile.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.aptitud.AptitudActivity;
import com.donemosmobile.ui.donaciones.DonacionesActivity;
import com.donemosmobile.ui.pacientes.PacientesActivity;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;
import com.donemosmobile.ui.turnos.TurnosActivity;

import java.util.Calendar;

public class HomeContent extends LinearLayout {

  public HomeContent(Context context) {
    super(context);

    setOrientation(VERTICAL);

    // Saludo dinámico
    TextView greeting = new TextView(context);
    greeting.setText(greetingForNow());
    AppTypography.headline1(greeting);
    greeting.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    addView(greeting);

    // Buscador — píldora gris estilo iOS
    EditText search = new EditText(context);
    search.setHint("Buscar paciente por nombre o DNI");
    search.setTextSize(15);
    search.setHintTextColor(AppColors.TEXT_SECONDARY);
    search.setTextColor(AppColors.TEXT_PRIMARY);
    search.setBackground(null);
    search.setMaxLines(1);

    LinearLayout searchPill = new LinearLayout(context);
    searchPill.setOrientation(HORIZONTAL);
    searchPill.setGravity(Gravity.CENTER_VERTICAL);
    searchPill.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()), 0,
        AppSpacing.dp(AppSpacing.MD, getResources()), 0);
    searchPill.setBackground(rounded(AppColors.SURFACE, 28));
    searchPill.addView(search, new LayoutParams(
        0, AppSpacing.dp(44, getResources()), 1));

    LinearLayout searchWrapper = new LinearLayout(context);
    searchWrapper.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    searchWrapper.addView(searchPill, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    addView(searchWrapper);

    // Acciones rápidas
    TextView actionsLabel = new TextView(context);
    actionsLabel.setText("Acciones rápidas");
    AppTypography.headline2(actionsLabel);
    actionsLabel.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    addView(actionsLabel);

    LinearLayout row1 = new LinearLayout(context);
    row1.setOrientation(HORIZONTAL);
    row1.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    row1.addView(makeTile(context, "Registrar donación", true,
        v -> navigate(context, DonacionesActivity.class)), gridParams());
    row1.addView(spacer());
    row1.addView(makeTile(context, "Nuevo turno", false,
        v -> navigateToCreate(context, TurnosActivity.class)), gridParams());
    addView(row1);

    LinearLayout row2 = new LinearLayout(context);
    row2.setOrientation(HORIZONTAL);
    row2.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    row2.addView(makeTile(context, "Nuevo paciente", false,
        v -> navigateToCreate(context, PacientesActivity.class)), gridParams());
    row2.addView(spacer());
    row2.addView(makeTile(context, "Evaluar aptitud", false,
        v -> navigate(context, AptitudActivity.class)), gridParams());
    addView(row2);

    // Actividad reciente
    TextView recent = new TextView(context);
    recent.setText("Actividad reciente");
    AppTypography.headline2(recent);
    recent.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XL, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    addView(recent);

    TextView empty = new TextView(context);
    empty.setText("Sin actividad registrada hoy");
    AppTypography.caption(empty);
    empty.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XXL, getResources()));
    addView(empty);
  }

  private String greetingForNow() {
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    if (hour < 13) {
      return "Buenos días";
    }
    if (hour < 20) {
      return "Buenas tardes";
    }
    return "Buenas noches";
  }

  private LinearLayout makeTile(Context context, String text, boolean primary, OnClickListener listener) {
    TextView label = new TextView(context);
    label.setText(text);
    label.setTextSize(15);
    label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    label.setTextColor(AppColors.TEXT_PRIMARY);
    label.setGravity(Gravity.CENTER);
    label.setPadding(AppSpacing.dp(AppSpacing.SM, getResources()), 0, AppSpacing.dp(AppSpacing.SM, getResources()), 0);

    LinearLayout tile = new LinearLayout(context);
    tile.setOrientation(VERTICAL);
    tile.setGravity(Gravity.CENTER);
    tile.setBackground(rounded(primary ? AppColors.PRIMARY_TINT : AppColors.SURFACE, 16));
    tile.setClickable(true);
    tile.setFocusable(true);
    tile.setOnClickListener(listener);

    // Ripple
    android.util.TypedValue outValue = new android.util.TypedValue();
    context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
    tile.setForeground(context.getDrawable(outValue.resourceId));

    tile.addView(label);
    tile.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        AppSpacing.dp(88, getResources())));

    return tile;
  }

  private GradientDrawable rounded(int color, float radiusDp) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(AppSpacing.dp((int) radiusDp, getResources()));
    return drawable;
  }

  private void navigate(Context context, Class<?> activityClass) {
    context.startActivity(new Intent(context, activityClass));
  }

  private void navigateToCreate(Context context, Class<?> activityClass) {
    Intent intent = new Intent(context, activityClass);
    intent.putExtra("mode", "create");
    context.startActivity(intent);
  }

  private LayoutParams gridParams() {
    return new LayoutParams(0, AppSpacing.dp(88, getResources()), 1);
  }

  private View spacer() {
    View s = new View(getContext());
    s.setLayoutParams(new LayoutParams(
        AppSpacing.dp(AppSpacing.SM, getResources()), 0));
    return s;
  }
}
