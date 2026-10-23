package com.donemosmobile.ui;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * Accessibility helpers for the programmatic views. Clickable TextViews and
 * LinearLayouts are announced by TalkBack as plain text unless they get a
 * Button role, and they can end up smaller than the 48dp Material touch target.
 */
public final class A11y {

  /** Material minimum touch target, in dp. */
  public static final int MIN_TOUCH_DP = 48;

  private A11y() {}

  /**
   * Announces {@code view} as a button (and its selected state, read from
   * {@link View#isSelected()}), with an optional spoken label.
   */
  public static void asButton(@NonNull View view, @Nullable CharSequence description) {
    if (description != null) {
      view.setContentDescription(description);
    }
    ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
      @Override
      public void onInitializeAccessibilityNodeInfo(@NonNull View host,
          @NonNull AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        info.setClassName(Button.class.getName());
        info.setSelected(host.isSelected());
      }
    });
  }

  /** Guarantees a 48x48dp hit area without changing the visual padding. */
  public static void minTouchTarget(@NonNull View view) {
    int min = Ui.dp(view.getContext(), MIN_TOUCH_DP);
    view.setMinimumHeight(min);
    view.setMinimumWidth(min);
    if (view instanceof TextView) {
      ((TextView) view).setMinHeight(min);
      ((TextView) view).setMinWidth(min);
    }
  }

  /** Button role + 48dp target: the usual combo for a clickable text. */
  public static void touchableButton(@NonNull View view, @Nullable CharSequence description) {
    asButton(view, description);
    minTouchTarget(view);
  }
}
