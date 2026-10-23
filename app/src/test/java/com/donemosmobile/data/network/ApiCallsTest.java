package com.donemosmobile.data.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** JVM tests for the shared status routing of {@link ApiCalls}. */
public class ApiCallsTest {

  private static final MediaType JSON = MediaType.parse("application/json");

  private final AtomicInteger unauthorized = new AtomicInteger();
  private final ApiCalls calls = new ApiCalls(unauthorized::incrementAndGet);

  /** Records which callback fired and with what. */
  private static final class Recorder implements WarningCallback<String, String> {
    final List<String> events = new ArrayList<>();
    ApiError.Type type;
    String message;
    String data;
    Map<String, String> fields;
    String warning;

    @Override
    public void onSuccess(@Nullable String data) {
      events.add("success");
      this.data = data;
    }

    @Override
    public void onError(@NonNull ApiError.Type type, @Nullable String message) {
      events.add("error");
      this.type = type;
      this.message = message;
    }

    @Override
    public void onValidationError(@NonNull Map<String, String> fieldErrors) {
      events.add("validation");
      this.fields = fieldErrors;
    }

    @Override
    public void onWarning(@NonNull String warning) {
      events.add("warning");
      this.warning = warning;
    }
  }

  /** Synchronous Call that answers with a fixed response or failure. */
  private static final class FakeCall<T> implements Call<T> {
    private final Response<T> response;
    private final Throwable failure;

    FakeCall(Response<T> response) {
      this.response = response;
      this.failure = null;
    }

    FakeCall(Throwable failure) {
      this.response = null;
      this.failure = failure;
    }

    @Override
    public void enqueue(@NonNull Callback<T> callback) {
      if (failure != null) {
        callback.onFailure(this, failure);
      } else {
        callback.onResponse(this, response);
      }
    }

    @NonNull
    @Override
    public Response<T> execute() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExecuted() {
      return true;
    }

    @Override
    public void cancel() {}

    @Override
    public boolean isCanceled() {
      return false;
    }

    @NonNull
    @Override
    public Call<T> clone() {
      return this;
    }

    @NonNull
    @Override
    public Request request() {
      return new Request.Builder().url("http://localhost/").build();
    }

    @NonNull
    @Override
    public Timeout timeout() {
      return Timeout.NONE;
    }
  }

  private static FakeCall<String> error(int code, String body) {
    return new FakeCall<>(Response.error(code, ResponseBody.create(body, JSON)));
  }

  private static final String VALIDATION =
      "{\"message\":\"Datos inválidos\",\"errors\":{\"dni\":[\"Ya existe un paciente con ese DNI.\"]}}";

  @Test
  public void successRunsTheMapperAndDeliversItsValue() {
    Recorder recorder = new Recorder();
    calls.enqueue(new FakeCall<>(Response.success("body")), body -> body + ":mapped", recorder);
    assertEquals(List.of("success"), recorder.events);
    assertEquals("body:mapped", recorder.data);
  }

  @Test
  public void successWithoutBodyIsAnUnknownError() {
    Recorder recorder = new Recorder();
    calls.enqueue(new FakeCall<String>(Response.success((String) null)), body -> body, recorder);
    assertEquals(List.of("error"), recorder.events);
    assertEquals(ApiError.Type.UNKNOWN, recorder.type);
  }

  @Test
  public void emptyBodyIsASuccessWhenAllowed() {
    Recorder recorder = new Recorder();
    calls.enqueueWriteAllowingEmpty(new FakeCall<String>(Response.success((String) null)),
        recorder);
    assertEquals(List.of("success"), recorder.events);
    assertNull(recorder.data);
  }

  @Test
  public void unauthorizedRunsTheHookOnceAndReportsTheError() {
    Recorder recorder = new Recorder();
    calls.enqueue(error(401, "{\"message\":\"Unauthenticated.\"}"), body -> body, recorder);
    assertEquals(1, unauthorized.get());
    assertEquals(List.of("error"), recorder.events);
    assertEquals(ApiError.Type.UNAUTHORIZED, recorder.type);
    assertEquals("Unauthenticated.", recorder.message);
  }

  @Test
  public void otherErrorsDoNotRunTheUnauthorizedHook() {
    Recorder recorder = new Recorder();
    calls.enqueue(error(500, "{\"message\":\"Server Error\"}"), body -> body, recorder);
    assertEquals(0, unauthorized.get());
    assertEquals(ApiError.Type.SERVER, recorder.type);
    assertEquals("Server Error", recorder.message);
  }

  @Test
  public void notFoundIsReportedAsNotFound() {
    Recorder recorder = new Recorder();
    calls.enqueue(error(404, "{\"message\":\"No se encontró el recurso solicitado.\"}"),
        body -> body, recorder);
    assertEquals(ApiError.Type.NOT_FOUND, recorder.type);
  }

  @Test
  public void fieldErrorsOnAWriteGoToValidation() {
    Recorder recorder = new Recorder();
    calls.enqueueWrite(error(422, VALIDATION), body -> body, recorder);
    assertEquals(List.of("validation"), recorder.events);
    assertEquals("Ya existe un paciente con ese DNI.", recorder.fields.get("dni"));
  }

  @Test
  public void fieldErrorsOnAReadStayAPlainErrorWithTheMessage() {
    Recorder recorder = new Recorder();
    calls.enqueue(error(422, VALIDATION), body -> body, recorder);
    assertEquals(List.of("error"), recorder.events);
    assertEquals(ApiError.Type.CLIENT, recorder.type);
    assertEquals("Datos inválidos", recorder.message);
  }

  @Test
  public void parsedConflictGoesToWarning() {
    Recorder recorder = new Recorder();
    calls.enqueueWrite(error(409, "{\"warning\":true}"), body -> body,
        e -> e.raw, recorder);
    assertEquals(List.of("warning"), recorder.events);
    assertEquals("{\"warning\":true}", recorder.warning);
  }

  @Test
  public void unparseableConflictBecomesAnErrorNotAnEmptyWarning() {
    Recorder recorder = new Recorder();
    calls.enqueueWrite(error(409, "<html>Conflict</html>"), body -> body, e -> null, recorder);
    assertEquals(List.of("error"), recorder.events);
    assertEquals(ApiError.Type.CLIENT, recorder.type);
  }

  @Test
  public void conflictWithFieldErrorsIsNeverValidation() {
    Recorder recorder = new Recorder();
    calls.enqueueWrite(error(409, VALIDATION), body -> body, e -> null, recorder);
    assertEquals(List.of("error"), recorder.events);
    assertEquals("Datos inválidos", recorder.message);
  }

  @Test
  public void noResponseIsANetworkError() {
    Recorder recorder = new Recorder();
    calls.enqueue(new FakeCall<String>(new IOException("timeout")), body -> body, recorder);
    assertEquals(List.of("error"), recorder.events);
    assertEquals(ApiError.Type.NETWORK, recorder.type);
    assertEquals("timeout", recorder.message);
    assertEquals(0, unauthorized.get());
  }
}
