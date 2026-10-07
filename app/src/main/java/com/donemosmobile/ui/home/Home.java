package com.donemosmobile.ui.home;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.donemosmobile.ui.components.SectionNavBar;
import com.donemosmobile.ui.navigation.Section;
import com.donemosmobile.ui.theme.AppColors;

public class Home extends LinearLayout {

  public Home(Context context) {
    super(context);

    setOrientation(VERTICAL);
    setBackgroundColor(AppColors.BACKGROUND);

    HomeHeader header = new HomeHeader(context);
    HomeContent content = new HomeContent(context);

    addView(header, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    // El contenido scrollea para que la barra inferior quede siempre visible,
    // aun cuando la actividad reciente crezca.
    ScrollView scroller = new ScrollView(context);
    scroller.setFillViewport(true);
    scroller.addView(content, new ScrollView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    addView(scroller, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        0,
        1));

    addView(new SectionNavBar(context, Section.HOME), new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
  }
}
