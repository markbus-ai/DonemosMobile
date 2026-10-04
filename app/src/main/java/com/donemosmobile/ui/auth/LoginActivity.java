package com.donemosmobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.AppActivity;
import com.donemosmobile.DonemosMobileActivity;
import com.donemosmobile.data.auth.AuthRepository;
import com.donemosmobile.data.network.dto.UserDto;
import com.donemosmobile.ui.components.AppButton;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/** Pantalla de ingreso del personal con las vistas programáticas del proyecto. */
public class LoginActivity extends AppActivity {

  private AppInput usernameInput;
  private AppInput passwordInput;
  private AppButton submitButton;
  private TextView errorView;
  private AuthRepository repository;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    repository = new AuthRepository(this);
    setContentView(buildView());
  }

  private LinearLayout buildView() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER_VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);
    int pad = AppSpacing.dp(AppSpacing.LG, getResources());
    root.setPadding(pad, pad, pad, pad);

    TextView title = new TextView(this);
    title.setText("Donemos");
    AppTypography.headline1(title);
    root.addView(title, params(0, AppSpacing.SM));

    TextView subtitle = new TextView(this);
    subtitle.setText("Ingresá con tu usuario del personal");
    AppTypography.subtitle(subtitle);
    root.addView(subtitle, params(0, AppSpacing.XL));

    usernameInput = new AppInput(this, "Usuario");
    usernameInput.getEditText().setInputType(
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
    root.addView(usernameInput, params(0, AppSpacing.MD));

    passwordInput = new AppInput(this, "Contraseña");
    passwordInput.getEditText().setInputType(
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    root.addView(passwordInput, params(0, AppSpacing.MD));

    errorView = new TextView(this);
    errorView.setTextColor(AppColors.ERROR);
    errorView.setTextSize(14);
    errorView.setVisibility(View.GONE);
    root.addView(errorView, params(0, AppSpacing.SM));

    submitButton = new AppButton(this, "Ingresar", AppButton.Style.FILLED, v -> submit());
    root.addView(submitButton, buttonParams(AppSpacing.LG));

    return root;
  }

  private void submit() {
    hideError();
    String username = usernameInput.getText();
    String password = passwordInput.getText();

    if (username.isEmpty()) {
      showError("Ingresá tu usuario");
      return;
    }
    if (password.isEmpty()) {
      showError("Ingresá tu contraseña");
      return;
    }

    setLoading(true);
    repository.login(username, password, new AuthRepository.Callback<UserDto>() {
      @Override
      public void onSuccess(UserDto data) {
        setLoading(false);
        goToHome();
      }

      @Override
      public void onError(AuthRepository.ErrorType type, String message) {
        setLoading(false);
        showError(messageFor(type, message));
      }
    });
  }

  private void goToHome() {
    Intent intent = new Intent(this, DonemosMobileActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }

  private String messageFor(AuthRepository.ErrorType type, String message) {
    switch (type) {
      case INVALID_CREDENTIALS:
        return "Credenciales inválidas";
      case TOO_MANY_ATTEMPTS:
        return "Demasiados intentos, esperá un momento";
      case NETWORK:
        return "No se pudo conectar";
      case SERVER:
        return "Error del servidor, intentá de nuevo";
      default:
        return message != null && !message.isEmpty() ? message : "Ocurrió un error";
    }
  }

  private void setLoading(boolean loading) {
    submitButton.setEnabled(!loading);
    submitButton.setText(loading ? "Ingresando…" : "Ingresar");
    usernameInput.setEnabled(!loading);
    passwordInput.setEnabled(!loading);
  }

  private void showError(String message) {
    errorView.setText(message);
    errorView.setVisibility(View.VISIBLE);
  }

  private void hideError() {
    errorView.setVisibility(View.GONE);
  }

  private LinearLayout.LayoutParams params(int topMarginDp, int bottomMarginDp) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(topMarginDp, getResources());
    params.bottomMargin = AppSpacing.dp(bottomMarginDp, getResources());
    return params;
  }

  private LinearLayout.LayoutParams buttonParams(int topMarginDp) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        AppSpacing.dp(48, getResources()));
    params.topMargin = AppSpacing.dp(topMarginDp, getResources());
    return params;
  }
}
