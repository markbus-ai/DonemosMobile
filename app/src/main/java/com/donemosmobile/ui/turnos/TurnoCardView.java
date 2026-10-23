package com.donemosmobile.ui.turnos;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.R;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.ui.A11y;
import com.donemosmobile.ui.TurnoLabels;
import com.donemosmobile.ui.TurnoSchedule;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * One turno of the list: hora, paciente, sede and tipo, estado, and for a
 * PENDIENTE turno the "Cancelar" / "Atender" actions.
 */
final class TurnoCardView extends AppCard {

  /** A tap on one of the estado actions of the card. */
  interface OnEstado {
    void onEstado(TurnoDto turno, String estado);
  }

  /** @param busy a PATCH is in flight for this turno: its actions stay disabled */
  TurnoCardView(Context context, TurnoDto turno, boolean busy, OnEstado onEstado) {
    super(context, buildContent(context, turno, busy, onEstado));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = Ui.dp(context, AppSpacing.SM);
    setLayoutParams(params);
  }

  private static LinearLayout buildContent(Context context, TurnoDto turno, boolean busy,
      OnEstado onEstado) {
    LinearLayout column = new LinearLayout(context);
    column.setOrientation(LinearLayout.VERTICAL);

    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.HORIZONTAL);
    content.setGravity(Gravity.CENTER_VERTICAL);
    column.addView(content);

    TextView hora = new TextView(context);
    hora.setText(turno.horaCorta());
    hora.setTextSize(20);
    hora.setTextColor(AppColors.TEXT_PRIMARY);
    hora.setTypeface(android.graphics.Typeface.create(
        "sans-serif-medium", android.graphics.Typeface.NORMAL));
    content.addView(hora, new LinearLayout.LayoutParams(
        Ui.dp(context, 64), ViewGroup.LayoutParams.WRAP_CONTENT));

    LinearLayout details = new LinearLayout(context);
    details.setOrientation(LinearLayout.VERTICAL);

    TextView paciente = new TextView(context);
    // El backend embebe la relación; si no vino, el label cae al id.
    paciente.setText(turno.pacienteLabel());
    AppTypography.bodyMedium(paciente);
    details.addView(paciente);

    TextView meta = new TextView(context);
    meta.setText(context.getString(R.string.dot_pair, turno.sedeLabel(), turno.tipoLabel()));
    AppTypography.caption(meta);
    details.addView(meta);

    content.addView(details, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    TextView estado = new TextView(context);
    estado.setText(TurnoLabels.estadoLabel(turno.estado));
    estado.setTextSize(13);
    estado.setTextColor(TurnoLabels.estadoColor(turno.estado));
    estado.setPadding(
        Ui.dp(context, AppSpacing.SM),
        Ui.dp(context, AppSpacing.XS),
        Ui.dp(context, AppSpacing.SM),
        Ui.dp(context, AppSpacing.XS));
    content.addView(estado);

    if (TurnoLabels.PENDIENTE.equals(turno.estado) && turno.id != null) {
      column.addView(buildActions(context, turno, busy, onEstado));
    }
    return column;
  }

  /** "Atender" (only for today or earlier) and "Cancelar" for a PENDIENTE turno. */
  private static LinearLayout buildActions(Context context, TurnoDto turno, boolean busy,
      OnEstado onEstado) {
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.END);
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = Ui.dp(context, AppSpacing.SM);
    row.setLayoutParams(rowParams);

    String who = turno.pacienteLabel() + " · " + turno.horaCorta();

    AppButton cancelar = new AppButton(context, "Cancelar", AppButton.Style.TEXT,
        v -> onEstado.onEstado(turno, TurnoLabels.CANCELADO));
    cancelar.setEnabled(!busy);
    cancelar.setContentDescription("Cancelar turno de " + who);
    row.addView(cancelar, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(context, A11y.MIN_TOUCH_DP)));

    // A future turno cannot have been attended yet.
    boolean canAttend = turno.fecha == null || turno.fecha.compareTo(TurnoSchedule.today()) <= 0;
    if (canAttend) {
      AppButton atender = new AppButton(context, busy ? "Guardando…" : "Atender",
          AppButton.Style.OUTLINED, v -> onEstado.onEstado(turno, TurnoLabels.ATENDIDO));
      atender.setEnabled(!busy);
      atender.setContentDescription("Marcar como atendido el turno de " + who);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(context, A11y.MIN_TOUCH_DP));
      params.leftMargin = Ui.dp(context, AppSpacing.SM);
      row.addView(atender, params);
    }
    return row;
  }
}
