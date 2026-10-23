package com.donemosmobile.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JVM tests for binding async results to the patient they were sent for. */
public class StaleResultsTest {

  @Test
  public void resultForTheCurrentPatientIsKept() {
    assertFalse(StaleResults.isStale(false, "30111222", "30111222"));
  }

  @Test
  public void resultForAnotherPatientIsDropped() {
    assertTrue(StaleResults.isStale(false, "30111222", "40999888"));
  }

  @Test
  public void resultAfterTheSelectionWasClearedIsDropped() {
    assertTrue(StaleResults.isStale(false, "30111222", null));
  }

  @Test
  public void resultWithoutARequestedPatientIsDropped() {
    assertTrue(StaleResults.isStale(false, null, null));
  }

  @Test
  public void resultAfterTheScreenIsGoneIsDropped() {
    assertTrue(StaleResults.isStale(true, "30111222", "30111222"));
  }
}
