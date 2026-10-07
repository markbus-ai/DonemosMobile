package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Turno crudo devuelto por {@code GET /api/turnos} / {@code POST /api/turnos}.
 *
 * <p>El backend serializa el modelo Eloquent con las relaciones embebidas
 * ({@code paciente}, {@code sede}, {@code tipo}) además de los ids crudos. Las
 * relaciones pueden venir {@code null} en filas legacy, así que se mapea
 * defensivo: los accesores caen a un fallback con el id.
 */
public class TurnoDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("paciente_id")
  public Long pacienteId;

  @SerializedName("sede_id")
  public Long sedeId;

  @SerializedName("tipo_id")
  public Long tipoId;

  /** Formato {@code YYYY-MM-DD}. */
  @SerializedName("fecha")
  public String fecha;

  /** Formato {@code H:i}. */
  @SerializedName("hora")
  public String hora;

  /** PENDIENTE | ATENDIDO | CANCELADO. */
  @SerializedName("estado")
  public String estado;

  /** Relación embebida; {@code null} en filas legacy. */
  @SerializedName("paciente")
  public Paciente paciente;

  /** Relación embebida; {@code null} en filas legacy. */
  @SerializedName("sede")
  public Sede sede;

  /** Relación embebida; {@code null} en filas legacy. */
  @SerializedName("tipo")
  public Tipo tipo;

  /**
   * Hora recortada a {@code HH:mm} para mostrar; vacío si no vino. Si el
   * backend manda un datetime ISO legacy, se queda con la parte de hora para
   * no mostrar el año truncado.
   */
  public String horaCorta() {
    if (hora == null || hora.isEmpty()) {
      return "";
    }
    String value = hora;
    int sep = value.indexOf('T');
    if (sep < 0) {
      sep = value.indexOf(' ');
    }
    if (sep >= 0 && sep + 1 < value.length()) {
      value = value.substring(sep + 1);
    }
    return value.length() >= 5 ? value.substring(0, 5) : value;
  }

  /** Nombre del paciente listo para mostrar; cae al id si la relación no vino. */
  public String pacienteLabel() {
    if (paciente != null) {
      String first = paciente.nombre != null ? paciente.nombre : "";
      String last = paciente.apellido != null ? paciente.apellido : "";
      String full = (first + " " + last).trim();
      if (!full.isEmpty()) {
        return full;
      }
    }
    return pacienteId != null ? "Paciente #" + pacienteId : "Paciente sin asignar";
  }

  /** Nombre de la sede; cae al id si la relación no vino. */
  public String sedeLabel() {
    if (sede != null && sede.nombre != null && !sede.nombre.isEmpty()) {
      return sede.nombre;
    }
    return sedeId != null ? "Sede #" + sedeId : "Sede —";
  }

  /** Nombre del tipo de donación; cae al id si la relación no vino. */
  public String tipoLabel() {
    if (tipo != null) {
      String label = tipo.nombre != null && !tipo.nombre.isEmpty()
          ? tipo.nombre : tipo.codigo;
      if (label != null && !label.isEmpty()) {
        return label;
      }
    }
    return tipoId != null ? "Tipo #" + tipoId : "Tipo —";
  }

  /** Paciente embebido en el turno. */
  public static class Paciente {
    @SerializedName("id")
    public Long id;

    @SerializedName("dni")
    public String dni;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("apellido")
    public String apellido;
  }

  /** Sede embebida en el turno. */
  public static class Sede {
    @SerializedName("id")
    public Long id;

    @SerializedName("nombre")
    public String nombre;
  }

  /** Tipo de donación embebido en el turno. */
  public static class Tipo {
    @SerializedName("id")
    public Long id;

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;
  }
}
