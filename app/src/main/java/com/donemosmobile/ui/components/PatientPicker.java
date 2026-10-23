package com.donemosmobile.ui.components;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.donemosmobile.ui.theme.AppSpacing;

/**
 * Buscador de paciente por DNI: input y boton, con estado y "Limpiar"
 * opcionales. El resultado (card o campo) lo dibuja la pantalla que lo usa.
 *
 * <p>Distingue el layout en fila (Donaciones/Aptitud) del apilado (Turnos).
 */
public class PatientPicker extends LinearLayout {

  public enum Layout { ROW, STACKED }

  private final AppButton search;
  private final String buttonText;
  private boolean searching;
  private boolean locked;
  @Nullable private AppButton clear;

  public interface Listener {
    void onSearch(String dni);

    void onClear();
  }

  private final AppInput dniInput;
  private final StateView state;
  private Listener listener;

  public PatientPicker(Context context, Layout layout, String dniLabel, String buttonText,
      AppButton.Style buttonStyle, boolean showState, boolean showClear) {
    super(context);
    setOrientation(VERTICAL);

    dniInput = new AppInput(context, dniLabel);
    dniInput.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

    this.buttonText = buttonText;
    search = new AppButton(context, buttonText, buttonStyle, v -> triggerSearch());

    if (layout == Layout.ROW) {
      LinearLayout row = new LinearLayout(context);
      row.setOrientation(HORIZONTAL);
      row.setGravity(Gravity.BOTTOM);
      row.addView(dniInput, new LinearLayout.LayoutParams(
          0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
      LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
          AppSpacing.dp(110, getResources()), AppSpacing.dp(56, getResources()));
      searchParams.leftMargin = AppSpacing.dp(AppSpacing.SM, getResources());
      row.addView(search, searchParams);
      LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      rowParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
      addView(row, rowParams);
    } else {
      addView(dniInput, new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(44, getResources()));
      searchParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
      addView(search, searchParams);
    }

    if (showState) {
      state = new StateView(context);
      addView(state);
    } else {
      state = null;
    }

    if (showClear) {
      clear = new AppButton(context, "Limpiar", AppButton.Style.TEXT, v -> {
        if (listener != null) {
          listener.onClear();
        }
      });
      addView(clear, new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(40, getResources())));
    }
  }

  /** Disables "Buscar" while a lookup is in flight so taps cannot stack requests. */
  public void setSearching(boolean searching) {
    this.searching = searching;
    search.setEnabled(!searching && !locked);
    search.setText(searching ? "Buscando…" : buttonText);
  }

  /**
   * Freezes the selection while the screen submits for the selected patient:
   * the DNI, "Buscar" and "Limpiar" stay disabled so the result cannot land on
   * another patient.
   */
  public void setLocked(boolean locked) {
    this.locked = locked;
    dniInput.getEditText().setEnabled(!locked);
    search.setEnabled(!locked && !searching);
    if (clear != null) {
      clear.setEnabled(!locked);
    }
  }

  public AppInput dniInput() {
    return dniInput;
  }

  @Nullable
  public StateView state() {
    return state;
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  private void triggerSearch() {
    if (searching || locked) {
      return;
    }
    String dni = dniInput.getText();
    dniInput.setError(null);
    if (dni.isEmpty()) {
      dniInput.setError("Ingresá el DNI");
      return;
    }
    if (listener != null) {
      listener.onSearch(dni);
    }
  }
}
