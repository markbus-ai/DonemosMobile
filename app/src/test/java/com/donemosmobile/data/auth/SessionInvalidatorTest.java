package com.donemosmobile.data.auth;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

/** JVM tests for the data-to-UI session seam. */
public class SessionInvalidatorTest {

  @After
  public void unregister() {
    SessionInvalidator.setListener(null);
  }

  @Test
  public void forwardsEventsToTheRegisteredListener() {
    List<String> events = new ArrayList<>();
    SessionInvalidator.setListener(new SessionInvalidator.Listener() {
      @Override
      public void onSessionInvalidated() {
        events.add("invalidated");
      }

      @Override
      public void onSessionStarted() {
        events.add("started");
      }
    });
    SessionInvalidator.invalidate();
    SessionInvalidator.sessionStarted();
    assertEquals(List.of("invalidated", "started"), events);
  }

  @Test
  public void withoutAListenerEventsAreIgnored() {
    SessionInvalidator.invalidate();
    SessionInvalidator.sessionStarted();
  }
}
