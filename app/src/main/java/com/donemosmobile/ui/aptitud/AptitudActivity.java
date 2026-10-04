package com.donemosmobile.ui.aptitud;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.donemosmobile.AppActivity;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

public class AptitudActivity extends AppActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(AppColors.BACKGROUND);

    TextView title = new TextView(this);
    title.setText("Aptitud");
    AppTypography.headline1(title);
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, AppSpacing.dp(AppSpacing.XXL, getResources()), 0, 0);
    root.addView(title, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    setContentView(root);
  }
}
