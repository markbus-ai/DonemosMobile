package com.donemosmobile.ui.mas;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.AppActivity;
import com.donemosmobile.data.local.SessionStore;
import com.donemosmobile.data.network.dto.UserDto;
import com.donemosmobile.ui.auth.SessionActions;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppCard;
import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Sección "Más": muestra el personal logueado (usuario, rol y sede) y permite
 * cerrar sesión. Si no hay usuario cacheado, muestra lo disponible e igual
 * ofrece la salida.
 */
public class MasActivity extends AppActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(buildView());
  }

  private LinearLayout buildView() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    TextView title = new TextView(this);
    title.setText("Más");
    AppTypography.headline1(title);
    title.setPadding(dp(AppSpacing.MD), dp(AppSpacing.LG), dp(AppSpacing.MD), 0);
    root.addView(title);

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(AppSpacing.MD), dp(AppSpacing.MD), dp(AppSpacing.MD), 0);

    content.addView(buildUserCard());

    AppButton logout = new AppButton(this, "Cerrar sesión", AppButton.Style.FILLED,
        v -> SessionActions.logout(MasActivity.this));
    LinearLayout.LayoutParams logoutParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
    logoutParams.topMargin = dp(AppSpacing.MD);
    content.addView(logout, logoutParams);

    root.addView(content, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    root.addView(new SectionNavBar(this, Section.MAS));

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
      LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      nameParams.bottomMargin = dp(AppSpacing.SM);
      column.addView(name, nameParams);
    }

    addLine(column, "Usuario", user != null ? user.username : null);
    addLine(column, "Rol", user != null ? user.rol : null);
    addLine(column, "Sede", user != null ? user.sede : null);

    if (user == null) {
      TextView hint = new TextView(this);
      hint.setText("No hay datos de sesión en este dispositivo. Podés cerrar sesión y volver a ingresar.");
      AppTypography.caption(hint);
      LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      hintParams.topMargin = dp(AppSpacing.SM);
      column.addView(hint, hintParams);
    }

    return new AppCard(this, column);
  }

  private void addLine(LinearLayout parent, String label, String value) {
    TextView line = new TextView(this);
    line.setText(label + ": " + safe(value));
    AppTypography.body(line);
    parent.addView(line);
  }

  private String safe(String value) {
    return value != null && !value.isEmpty() ? value : "—";
  }

  private int dp(int value) {
    return AppSpacing.dp(value, getResources());
  }
}
