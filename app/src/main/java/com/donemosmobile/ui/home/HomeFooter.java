package com.donemosmobile.ui.home;

import android.content.Context;
import android.content.Intent;

import com.donemosmobile.ui.components.AppNavBar;
import com.donemosmobile.ui.pacientes.PacientesActivity;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.turnos.TurnosActivity;

public class HomeFooter extends AppNavBar {

  public HomeFooter(Context context) {
    super(context, new String[]{"Inicio", "Pacientes", "Turnos", "Más"}, position -> {
      switch (position) {
        case 1:
          navigate(context, PacientesActivity.class);
          break;
        case 2:
          navigate(context, TurnosActivity.class);
          break;
        default:
          // TODO: Más (ajustes, perfil, cerrar sesión)
          break;
      }
    });
  }

  private static void navigate(Context context, Class<?> activityClass) {
    context.startActivity(new Intent(context, activityClass));
  }
}
