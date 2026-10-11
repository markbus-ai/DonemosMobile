package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/donaciones}. Gson omite los campos nulos, asi que
 * solo viajan los datos capturados; el backend resuelve sede y fecha por su
 * cuenta. {@code forzar} es {@code Boolean} para no mandarlo cuando no se pide
 * override (la autorizacion queda auditada del lado servidor).
 */
public class CreateDonacionRequest {

  @SerializedName("paciente_id")
  public Long pacienteId;

  @SerializedName("tipo_id")
  public Long tipoId;

  @SerializedName("sede_id")
  public Long sedeId;

  @SerializedName("componentes")
  public List<Componente> componentes;

  @SerializedName("tipo_bolsa")
  public String tipoBolsa;

  @SerializedName("anticoagulante")
  public String anticoagulante;

  @SerializedName("lote")
  public String lote;

  @SerializedName("tubuladura")
  public String tubuladura;

  @SerializedName("brazo")
  public String brazo;

  @SerializedName("dificultad")
  public String dificultad;

  @SerializedName("doble_etiqueta")
  public Boolean dobleEtiqueta;

  @SerializedName("hemoglobina")
  public Double hemoglobina;

  @SerializedName("hematocrito")
  public Double hematocrito;

  @SerializedName("plaquetas")
  public Integer plaquetas;

  @SerializedName("presion_sistolica")
  public Integer presionSistolica;

  @SerializedName("presion_diastolica")
  public Integer presionDiastolica;

  @SerializedName("frecuencia_cardiaca")
  public Integer frecuenciaCardiaca;

  @SerializedName("peso_donante")
  public Double pesoDonante;

  /**
   * Override opcional de la posicion en el ciclo: {@code ciclo > serie >
   * donacion}, donde {@code donacion} es el ordinal dentro de la serie. Si se
   * mandan los tres, el backend los usa tal cual; si van vacios (nulos, Gson
   * los omite), el backend calcula la posicion y la re-sincroniza en el
   * paciente. Mandar solo algunos NO cuenta como override: cae al calculo.
   */
  @SerializedName("ciclo")
  public Integer ciclo;

  @SerializedName("serie")
  public Integer serie;

  @SerializedName("donacion")
  public Integer donacion;

  @SerializedName("forzar")
  public Boolean forzar;

  /** Una unidad de componente. La cantidad es la cantidad de filas. */
  public static class Componente {

    @SerializedName("tipo_id")
    public Long tipoId;

    @SerializedName("vencimiento")
    public String vencimiento;

    @SerializedName("peso")
    public Double peso;

    public Componente() {}

    public Componente(Long tipoId) {
      this.tipoId = tipoId;
    }
  }
}
