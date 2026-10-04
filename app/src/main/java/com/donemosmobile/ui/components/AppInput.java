package com.donemosmobile.ui.components;

import android.content.Context;
import android.content.res.ColorStateList;

import com.donemosmobile.ui.theme.AppColors;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

public class AppInput extends TextInputLayout {

  private TextInputEditText editText;

  public AppInput(Context context, String hint) {
    super(context);

    setHint(hint);
    setBoxStrokeColor(AppColors.PRIMARY);
    setHintTextColor(ColorStateList.valueOf(AppColors.PRIMARY));

    editText = new TextInputEditText(context);
    addView(editText);
  }

  public TextInputEditText getEditText() {
    return editText;
  }

  public String getText() {
    return editText.getText() != null ? editText.getText().toString().trim() : "";
  }
}
