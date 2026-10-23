package com.donemosmobile.data.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JVM tests for {@link ApiError#parse(String)}: one read of the error body
 * must yield both the message and the per-field errors.
 */
public class ApiErrorTest {

  @Test
  public void parsesMessageAndFieldErrorsFromSingleBody() {
    ApiError.Body body = ApiError.parse(
        "{\"message\":\"Datos inválidos\",\"errors\":{\"hemoglobina\":[\"Fuera de rango\","
            + "\"otro\"],\"motivo\":[\"Muy largo\"]}}");
    assertEquals("Datos inválidos", body.message);
    assertEquals(2, body.fieldErrors.size());
    assertEquals("Fuera de rango", body.fieldErrors.get("hemoglobina"));
    assertEquals("Muy largo", body.fieldErrors.get("motivo"));
  }

  @Test
  public void messageOnlyBodyHasNoFieldErrors() {
    ApiError.Body body = ApiError.parse("{\"message\":\"Unauthenticated.\"}");
    assertEquals("Unauthenticated.", body.message);
    assertTrue(body.fieldErrors.isEmpty());
  }

  @Test
  public void nonJsonOrEmptyBodyIsSafe() {
    ApiError.Body html = ApiError.parse("<html>502 Bad Gateway</html>");
    assertNull(html.message);
    assertTrue(html.fieldErrors.isEmpty());
    assertEquals("<html>502 Bad Gateway</html>", html.raw);

    ApiError.Body empty = ApiError.parse(null);
    assertNull(empty.message);
    assertTrue(empty.fieldErrors.isEmpty());

    ApiError.Body text = ApiError.parse("\"just a string\"");
    assertNull(text.message);
  }

  @Test
  public void statusMappingIsUnchanged() {
    assertEquals(ApiError.Type.UNAUTHORIZED, ApiError.fromStatus(401));
    assertEquals(ApiError.Type.NOT_FOUND, ApiError.fromStatus(404));
    assertEquals(ApiError.Type.CLIENT, ApiError.fromStatus(422));
    assertEquals(ApiError.Type.CLIENT, ApiError.fromStatus(409));
    assertEquals(ApiError.Type.SERVER, ApiError.fromStatus(500));
  }
}
