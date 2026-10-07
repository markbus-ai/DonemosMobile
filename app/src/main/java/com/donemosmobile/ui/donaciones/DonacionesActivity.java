package com.donemosmobile.ui.donaciones;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.donemosmobile.AppActivity;
import com.donemosmobile.ui.components.ComingSoonView;
import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;

public class DonacionesActivity extends AppActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    root.addView(new ComingSoonView(this, "Donaciones",
        "El registro de donaciones va a estar disponible en una próxima "
            + "versión. Mientras tanto podés gestionar turnos y pacientes."),
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    root.addView(new SectionNavBar(this, Section.DONACIONES));

    setContentView(root);
  }
}
