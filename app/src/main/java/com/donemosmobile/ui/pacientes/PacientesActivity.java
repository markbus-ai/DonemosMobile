package com.donemosmobile.ui.pacientes;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.donemosmobile.AppActivity;
import com.donemosmobile.ui.components.ComingSoonView;
import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.theme.AppColors;

/**
 * Destino del acceso rápido "Nuevo paciente". El alta todavía no está
 * construida, así que mostramos un estado "Próximamente" honesto con la barra
 * de navegación para no dejar al usuario atrapado.
 */
public class PacientesActivity extends AppActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    root.addView(new ComingSoonView(this, "Nuevo paciente",
        "El alta de pacientes va a estar disponible en una próxima versión. "
            + "Por ahora podés buscar por DNI desde la sección Pacientes."),
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    // Pantalla de acceso rápido: sin sección resaltada, para poder saltar a
    // cualquier sección desde la barra.
    root.addView(new SectionNavBar(this, null));

    setContentView(root);
  }
}
