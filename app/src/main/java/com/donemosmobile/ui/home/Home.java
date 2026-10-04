package com.donemosmobile.ui.home;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.donemosmobile.ui.theme.AppColors;

public class Home extends LinearLayout {

  public Home(Context context) {
    super(context);

    setOrientation(VERTICAL);
    setBackgroundColor(AppColors.BACKGROUND);

    HomeHeader header = new HomeHeader(context);
    HomeContent content = new HomeContent(context);
    HomeFooter footer = new HomeFooter(context);

    addView(header, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    addView(content, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        0,
        1));

    addView(footer, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
  }
}
