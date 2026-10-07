package com.donemosmobile.ui.components;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.donemosmobile.ui.navigation.Section;

/**
 * Barra inferior consciente de la sección actual. Centraliza la navegación
 * entre secciones para que no se apilen Activities: cada sección reutiliza su
 * instancia existente (REORDER_TO_FRONT) y la Home vuelve a la raíz
 * (CLEAR_TOP), sin importar desde dónde se toque.
 *
 * <p>Pasar {@code null} como sección actual deja la barra sin ningún ítem
 * resaltado, útil para pantallas que no son de primer nivel.
 */
public class SectionNavBar extends AppNavBar {

  public SectionNavBar(Context context, @Nullable Section current) {
    super(context, Section.labels(), current != null ? current.index() : -1,
        position -> {
          Section[] sections = Section.values();
          if (position < 0 || position >= sections.length) {
            return;
          }
          if (current != null && position == current.index()) {
            return;
          }
          open(context, sections[position]);
        });
  }

  /** Abre otra sección reutilizando instancias para no apilar pantallas. */
  public static void open(Context context, Section target) {
    Intent intent = new Intent(context, target.activity);
    if (target == Section.HOME) {
      // La Home es la raíz: traerla al frente y limpiar lo que quedó encima.
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    } else {
      // Reutiliza la instancia de la sección si ya está en la tarea.
      intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }
    context.startActivity(intent);
  }
}
