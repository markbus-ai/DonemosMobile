package com.donemosmobile.ui.turnos;

import androidx.annotation.NonNull;

import com.donemosmobile.data.network.dto.TurnoWarningResponse;
import com.donemosmobile.ui.ForceOverride;

import java.util.Collections;
import java.util.List;

/**
 * Decides what a 409 of {@code POST /api/turnos} means for the screen:
 * forcible rules ask for an audited confirmation with motivo; hard limits
 * (slot taken, daily capacity) only explain why the turno cannot be booked.
 */
final class TurnoWarningPresenter {

  static final String FALLBACK_MESSAGE = "El turno no cumple las reglas de donación.";

  private TurnoWarningPresenter() {}

  /** What to show for one 409 envelope. */
  static final class Decision {
    /** Every rule can be overridden: offer the audited force dialog. */
    final boolean forcible;
    /** Messages to restate, never empty. */
    final List<String> messages;
    /** The messages joined one per line. */
    final String text;
    /** The picked slot is taken: flag the hora field too. */
    final boolean horarioOcupado;

    Decision(boolean forcible, List<String> messages, String text, boolean horarioOcupado) {
      this.forcible = forcible;
      this.messages = Collections.unmodifiableList(messages);
      this.text = text;
      this.horarioOcupado = horarioOcupado;
    }
  }

  @NonNull
  static Decision decide(@NonNull TurnoWarningResponse warning) {
    List<String> messages = warning.messages();
    if (messages.isEmpty()) {
      messages.add(FALLBACK_MESSAGE);
    }
    List<String> codes = warning.codes();
    return new Decision(ForceOverride.allForcible(codes), messages, String.join("\n", messages),
        codes.contains(ForceOverride.HORARIO_OCUPADO));
  }
}
