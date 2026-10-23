package com.donemosmobile.data.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Adapter between Retrofit and {@link ApiCallback}. It owns the error routing
 * shared by every repository, so each status is handled in one place:
 *
 * <ul>
 *   <li>2xx with a body: the mapper runs (cache side effects live there) and
 *       its result goes to {@code onSuccess}.
 *   <li>401: the unauthorized hook runs (session wipe and redirect), then
 *       {@code onError(UNAUTHORIZED)}.
 *   <li>409 on a write with a warning parser: {@code onWarning} when the body
 *       parses as an actionable envelope, otherwise {@code onError}.
 *   <li>Other 4xx with field errors on a write: {@code onValidationError}.
 *   <li>Anything else: {@code onError} with the backend message; no response
 *       at all is {@link ApiError.Type#NETWORK}.
 * </ul>
 *
 * The error body is read exactly once, through {@link ApiError#read}.
 */
public final class ApiCalls {

  /** Turns the response body into the callback value; may cache it. */
  public interface Mapper<R, T> {
    @Nullable
    T map(@NonNull R body);
  }

  /** Parses a 409 body; {@code null} when it is not an actionable envelope. */
  public interface WarningParser<W> {
    @Nullable
    W parse(@NonNull ApiError.Body error);
  }

  private final Runnable onUnauthorized;

  /** @param onUnauthorized runs once per 401, before {@code onError}. */
  public ApiCalls(@NonNull Runnable onUnauthorized) {
    this.onUnauthorized = onUnauthorized;
  }

  /** Read call: field errors are not routed, every failure is {@code onError}. */
  public <R, T> void enqueue(@NonNull Call<R> call, @NonNull Mapper<R, T> mapper,
      @NonNull ApiCallback<T> callback) {
    dispatch(call, mapper, false, false, null, callback, null);
  }

  /** Write call: field errors go to {@code onValidationError}. */
  public <R, T> void enqueueWrite(@NonNull Call<R> call, @NonNull Mapper<R, T> mapper,
      @NonNull ApiCallback<T> callback) {
    dispatch(call, mapper, true, false, null, callback, null);
  }

  /**
   * Write call that also accepts a 2xx without a body, delivered as
   * {@code onSuccess(null)}.
   */
  public <T> void enqueueWriteAllowingEmpty(@NonNull Call<T> call,
      @NonNull ApiCallback<T> callback) {
    dispatch(call, body -> body, true, true, null, callback, null);
  }

  /** Write call whose 409 is parsed into an actionable warning. */
  public <R, T, W> void enqueueWrite(@NonNull Call<R> call, @NonNull Mapper<R, T> mapper,
      @NonNull WarningParser<W> warnings, @NonNull WarningCallback<T, W> callback) {
    dispatch(call, mapper, true, false, warnings, callback, callback);
  }

  private <R, T, W> void dispatch(Call<R> call, Mapper<R, T> mapper, boolean validation,
      boolean emptyBodyOk, @Nullable WarningParser<W> warnings, ApiCallback<T> callback,
      @Nullable WarningCallback<T, W> warningCallback) {
    call.enqueue(new Callback<R>() {
      @Override
      public void onResponse(@NonNull Call<R> c, @NonNull Response<R> response) {
        R body = response.body();
        if (response.isSuccessful() && body != null) {
          callback.onSuccess(mapper.map(body));
          return;
        }
        if (response.isSuccessful() && emptyBodyOk) {
          callback.onSuccess(null);
          return;
        }
        int code = response.code();
        ApiError.Type type = ApiError.fromStatus(code);
        ApiError.Body error = ApiError.read(response);
        if (type == ApiError.Type.UNAUTHORIZED) {
          onUnauthorized.run();
          callback.onError(type, error.message);
          return;
        }
        if (code == 409 && warnings != null && warningCallback != null) {
          W warning = warnings.parse(error);
          if (warning != null) {
            warningCallback.onWarning(warning);
          } else {
            callback.onError(type, error.message);
          }
          return;
        }
        if (validation && type == ApiError.Type.CLIENT && code != 409
            && !error.fieldErrors.isEmpty()) {
          callback.onValidationError(error.fieldErrors);
          return;
        }
        callback.onError(type, error.message);
      }

      @Override
      public void onFailure(@NonNull Call<R> c, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }
}
