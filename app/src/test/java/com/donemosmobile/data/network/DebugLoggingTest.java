package com.donemosmobile.data.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JVM tests for which requests may have their bodies logged in debug. */
public class DebugLoggingTest {

  @Test
  public void loginIsACredentialRequest() {
    assertTrue(DebugLogging.isCredentialRequest("/api/auth/login"));
  }

  @Test
  public void otherRequestsAreNot() {
    assertFalse(DebugLogging.isCredentialRequest("/api/auth/me"));
    assertFalse(DebugLogging.isCredentialRequest("/api/pacientes/30111222"));
  }
}
