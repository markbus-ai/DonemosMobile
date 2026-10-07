package com.donemosmobile.ui.components;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;

/**
 * Estructura base de una pantalla de primer nivel: encabezado, contenido
 * scrolleable (o fijo) y la barra de seccion persistente. Centraliza el armado
 * que antes repetia cada Activity.
 */
public class ScreenScaffold extends LinearLayout {

  private final LinearLayout content;

  public ScreenScaffold(Context context, @Nullable Section section) {
    this(context, section, true);
  }

  public ScreenScaffold(Context context, @Nullable Section section, boolean scrollable) {
    super(context);
    setOrientation(VERTICAL);
    setBackgroundColor(AppColors.BACKGROUND);

    content = new LinearLayout(context);
    content.setOrientation(VERTICAL);

    if (scrollable) {
      ScrollView scroller = new ScrollView(context);
      scroller.setFillViewport(true);
      scroller.addView(content, new ScrollView.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      addView(scroller, new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    } else {
      addView(content, new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    addView(new SectionNavBar(context, section), new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
  }

  /** Inserta el encabezado arriba de todo, antes del contenido. */
  public ScreenScaffold setHeader(View header) {
    addView(header, 0, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    return this;
  }

  public LinearLayout content() {
    return content;
  }
}
