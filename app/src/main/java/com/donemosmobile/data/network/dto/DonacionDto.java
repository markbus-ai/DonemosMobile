package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Donacion devuelta por {@code POST /api/donaciones} (201). Lo que la pantalla
 * necesita es {@code numero_donacion} para confirmar y luego poder marcar la
 * autoexclusion; {@code ciclo}/{@code serie}/{@code donacion} traen la posicion
 * resuelta por el backend (calculada o el override cargado); {@code componentes}
 * viene embebido.
 */
public class DonacionDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("numero_donacion")
  public String numeroDonacion;

  @SerializedName("paciente_id")
  public Long pacienteId;

  @SerializedName("tipo_id")
  public Long tipoId;

  @SerializedName("fecha")
  public String fecha;

  /** Posicion en el ciclo resuelta por el backend para esta donacion. */
  @SerializedName("ciclo")
  public Integer ciclo;

  @SerializedName("serie")
  public Integer serie;

  @SerializedName("donacion")
  public Integer donacion;

  @SerializedName("componentes")
  public List<ComponenteDto> componentes;

  public static class ComponenteDto {
    @SerializedName("id")
    public Long id;

    @SerializedName("tipo_id")
    public Long tipoId;

    @SerializedName("vencimiento")
    public String vencimiento;

    @SerializedName("peso")
    public Double peso;

    @SerializedName("descartado")
    public boolean descartado;
  }
}
