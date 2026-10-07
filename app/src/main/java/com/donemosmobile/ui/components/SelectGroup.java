package com.donemosmobile.ui.components;

import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;

/**
 * Bloque selector reusable: etiqueta opcional, estado (cargando/error con
 * reintento), opciones y error. Centraliza el patron de los catalogos: tipo de
 * donacion, motivo, bolsa y brazo.
 *
 * <p>El estado y el error existen siempre pero arrancan ocultos, asi la
 * pantalla solo los muestra cuando corresponde.
 */
public class SelectGroup extends LinearLayout {

  private final LinearLayout options;
  private final TextView status;
  private final ErrorText error;

  public SelectGroup(Context context) {
    this(context, null, 0);
  }

  public SelectGroup(Context context, @Nullable String caption, int captionTopMarginDp) {
    super(context);
    setOrientation(VERTICAL);

    if (caption != null) {
      Ui.captionLabel(this, context, caption, captionTopMarginDp);
    }

    status = new TextView(context);
    status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    status.setTextColor(AppColors.TEXT_SECONDARY);
    status.setPadding(0, AppSpacing.dp(AppSpacing.XS, getResources()), 0, 0);
    status.setClickable(true);
    status.setFocusable(true);
    status.setVisibility(GONE);
    addView(status);

    options = new LinearLayout(context);
    options.setOrientation(VERTICAL);
    addView(options);

    error = new ErrorText(context);
    addView(error);
  }

  public LinearLayout options() {
    return options;
  }

  public TextView status() {
    return status;
  }

  public ErrorText error() {
    return error;
  }
}
