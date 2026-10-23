package com.donemosmobile.ui.mas;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.local.SessionStore;
import com.donemosmobile.data.network.dto.UserDto;
import com.donemosmobile.ui.RoleLabels;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.auth.SessionActions;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.FormActions;
import com.donemosmobile.ui.components.ScreenHeader;
import com.donemosmobile.ui.components.ScreenScaffold;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Sección "Más": muestra el personal logueado (usuario, rol y sede o zona) y permite
 * cerrar sesión. Si no hay usuario cacheado, muestra lo disponible e igual
 * ofrece la salida.
 */
public class MasActivity extends AppActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(buildView());
  }

  private View buildView() {
    ScreenScaffold root = new ScreenScaffold(this, Section.MAS, false);
    root.setHeader(new ScreenHeader(this).title("Más"));

    LinearLayout content = root.content();
    content.setPadding(
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.MD),
        Ui.dp(this, AppSpacing.MD),
        0);

    content.addView(buildUserCard());

    FormActions actions = new FormActions(this);
    actions.add("Cerrar sesión", AppButton.Style.FILLED,
        v -> SessionActions.logout(MasActivity.this), 48, AppSpacing.MD);
    content.addView(actions);

    return root;
  }

  private AppCard buildUserCard() {
    UserDto user = SessionStore.getInstance(this).getUser();

    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);

    if (user != null && !user.displayName().isEmpty()) {
      TextView name = new TextView(this);
      name.setText(user.displayName());
      AppTypography.headline2(name);
      column.addView(name, Ui.matchWrapBottom(this, AppSpacing.SM));
    }

    addLine(column, "Usuario", user != null ? user.username : null);
    addLine(column, "Rol", user != null ? RoleLabels.label(user.rol) : null);
    // An admin de zona works across sedes: show the zona instead of one sede.
    String rol = user != null ? user.rol : null;
    String zona = user != null ? user.zona : null;
    addLine(column, RoleLabels.scopeLabel(rol, zona),
        user != null ? RoleLabels.scopeValue(rol, user.sede, zona) : null);

    if (user == null) {
      TextView hint = new TextView(this);
      hint.setText("No hay datos de sesión en este dispositivo. Podés cerrar sesión y volver a ingresar.");
      AppTypography.caption(hint);
      column.addView(hint, Ui.matchWrap(this, AppSpacing.SM));
    }

    return new AppCard(this, column);
  }

  private void addLine(LinearLayout parent, String label, String value) {
    TextView line = new TextView(this);
    line.setText(label + ": " + Ui.safe(value));
    AppTypography.body(line);
    parent.addView(line);
  }
}
