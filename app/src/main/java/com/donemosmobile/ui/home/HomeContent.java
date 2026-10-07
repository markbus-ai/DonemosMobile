package com.donemosmobile.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.data.turnos.TurnoRepository;
import com.donemosmobile.ui.TurnoLabels;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.aptitud.AptitudActivity;
import com.donemosmobile.ui.donaciones.DonacionesActivity;
import com.donemosmobile.ui.pacientes.PacienteLookupActivity;
import com.donemosmobile.ui.pacientes.PacientesActivity;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;
import com.donemosmobile.ui.turnos.TurnosActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeContent extends LinearLayout {

  private static final int RECENT_LIMIT = 5;

  private final Context context;
  private final TurnoRepository turnoRepository;
  private LinearLayout recentContainer;

  public HomeContent(Context context) {
    super(context);
    this.context = context;
    this.turnoRepository = new TurnoRepository(context);

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
    search.setHint("Buscar paciente por DNI");
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
    searchPill.setBackground(Ui.rounded(context, AppColors.SURFACE, 28));
    searchPill.addView(search, new LayoutParams(
        0, AppSpacing.dp(44, getResources()), 1));

    // La píldora abre el buscador por DNI: no buscar en el teclado todavía.
    search.setFocusable(false);
    search.setClickable(true);
    search.setOnClickListener(v -> navigate(context, PacienteLookupActivity.class));
    searchPill.setClickable(true);
    searchPill.setFocusable(true);
    searchPill.setOnClickListener(v -> navigate(context, PacienteLookupActivity.class));

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
    row1.addView(makeTile(context, "Nuevo turno", true,
        v -> navigateToCreate(context, TurnosActivity.class)), gridParams());
    addView(row1);

    LinearLayout row2 = new LinearLayout(context);
    row2.setOrientation(HORIZONTAL);
    row2.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);
    row2.addView(makeTile(context, "Nuevo paciente", true,
        v -> navigate(context, PacientesActivity.class)), gridParams());
    row2.addView(spacer());
    row2.addView(makeTile(context, "Evaluar aptitud", true,
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

    recentContainer = new LinearLayout(context);
    recentContainer.setOrientation(VERTICAL);
    recentContainer.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XXL, getResources()));
    addView(recentContainer);
  }

  /** Relee los turnos de hoy y pinta la actividad reciente con datos reales. */
  public void refreshRecent() {
    if (recentContainer == null) {
      return;
    }
    final String hoy = hoy();
    List<TurnoDto> cached = turnoRepository.getCachedList(hoy, null);
    if (cached != null && !cached.isEmpty()) {
      renderRecent(cached);
    } else {
      renderRecentState("Buscando turnos de hoy…");
    }

    turnoRepository.list(hoy, null, new TurnoRepository.Callback<List<TurnoDto>>() {
      @Override
      public void onSuccess(List<TurnoDto> data) {
        renderRecent(data);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        // Con cache en pantalla no la pisamos; si no hay nada, estado honesto.
        if (turnoRepository.getCachedList(hoy, null) == null) {
          renderRecentState(messageFor(type, message));
        }
      }
    });
  }

  private void renderRecent(List<TurnoDto> turnos) {
    if (recentContainer == null) {
      return;
    }
    recentContainer.removeAllViews();
    if (turnos == null || turnos.isEmpty()) {
      addRecentText("Sin actividad registrada hoy");
      return;
    }
    int shown = 0;
    for (TurnoDto turno : turnos) {
      if (shown >= RECENT_LIMIT) {
        break;
      }
      recentContainer.addView(buildRecentRow(turno));
      shown++;
    }
  }

  private void renderRecentState(String text) {
    if (recentContainer == null) {
      return;
    }
    recentContainer.removeAllViews();
    addRecentText(text);
  }

  private void addRecentText(String text) {
    TextView empty = new TextView(context);
    empty.setText(text);
    AppTypography.caption(empty);
    recentContainer.addView(empty);
  }

  private LinearLayout buildRecentRow(TurnoDto turno) {
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, AppSpacing.dp(AppSpacing.SM, getResources()), 0,
        AppSpacing.dp(AppSpacing.SM, getResources()));

    TextView hora = new TextView(context);
    hora.setText(turno.horaCorta());
    hora.setTextSize(15);
    hora.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    hora.setTextColor(AppColors.TEXT_PRIMARY);
    row.addView(hora, new LinearLayout.LayoutParams(
        AppSpacing.dp(56, getResources()), ViewGroup.LayoutParams.WRAP_CONTENT));

    TextView label = new TextView(context);
    label.setText(turno.pacienteLabel());
    label.setTextSize(14);
    label.setTextColor(AppColors.TEXT_PRIMARY);
    label.setMaxLines(1);
    label.setEllipsize(android.text.TextUtils.TruncateAt.END);
    row.addView(label, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    TextView estado = new TextView(context);
    estado.setText(turno.estado != null ? turno.estado : "");
    estado.setTextSize(12);
    estado.setTextColor(TurnoLabels.estadoColor(turno.estado));
    row.addView(estado);

    return row;
  }

  private String messageFor(ApiError.Type type, String message) {
    switch (type) {
      case UNAUTHORIZED:
        return "Sesión vencida, volvé a ingresar";
      case NETWORK:
        return "No se pudieron cargar los turnos de hoy";
      case SERVER:
        return "Error del servidor al cargar la actividad";
      default:
        return message != null && !message.isEmpty()
            ? message : "No se pudo cargar la actividad";
    }
  }

  private String hoy() {
    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
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

  /**
   * Tile de acción rápida. {@code enabled} distingue lo que ya funciona (fondo
   * con acento) de lo que todavía no (fondo gris + "Próximamente"), así el
   * usuario lo sabe antes de tocar.
   */
  private LinearLayout makeTile(Context context, String text, boolean enabled, OnClickListener listener) {
    TextView label = new TextView(context);
    label.setText(text);
    label.setTextSize(15);
    label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    label.setTextColor(enabled ? AppColors.TEXT_PRIMARY : AppColors.TEXT_SECONDARY);
    label.setGravity(Gravity.CENTER);
    label.setPadding(AppSpacing.dp(AppSpacing.SM, getResources()), 0, AppSpacing.dp(AppSpacing.SM, getResources()), 0);

    LinearLayout tile = new LinearLayout(context);
    tile.setOrientation(VERTICAL);
    tile.setGravity(Gravity.CENTER);
    tile.setBackground(Ui.rounded(context, enabled ? AppColors.PRIMARY_TINT : AppColors.SURFACE, 16));
    tile.setClickable(true);
    tile.setFocusable(true);
    tile.setOnClickListener(listener);

    // Ripple
    android.util.TypedValue outValue = new android.util.TypedValue();
    context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
    tile.setForeground(context.getDrawable(outValue.resourceId));

    tile.addView(label);

    if (!enabled) {
      TextView comingSoon = new TextView(context);
      comingSoon.setText("Próximamente");
      comingSoon.setTextSize(11);
      comingSoon.setTextColor(AppColors.TEXT_SECONDARY);
      comingSoon.setGravity(Gravity.CENTER);
      LinearLayout.LayoutParams soonParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      soonParams.topMargin = AppSpacing.dp(2, getResources());
      tile.addView(comingSoon, soonParams);
    }

    tile.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        AppSpacing.dp(88, getResources())));

    return tile;
  }

  private void navigate(Context context, Class<?> activityClass) {
    Intent intent = new Intent(context, activityClass);
    // Reutiliza la pantalla si ya está abierta: nunca la apila dos veces.
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    context.startActivity(intent);
  }

  private void navigateToCreate(Context context, Class<?> activityClass) {
    Intent intent = new Intent(context, activityClass);
    intent.putExtra("mode", "create");
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
