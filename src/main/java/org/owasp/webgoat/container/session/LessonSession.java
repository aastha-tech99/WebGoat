/*
 * SPDX-FileCopyrightText: Copyright © 2024 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.container.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is responsible for managing user session data within a lesson. It uses a
 * ConcurrentHashMap to store key-value pairs representing session data, ensuring thread-safe access
 * from concurrent request threads in singleton Spring beans.
 */
public class LessonSession {

  private final Map<String, Object> userSessionData = new ConcurrentHashMap<>();

  /** Default constructor initializing an empty session. */
  public LessonSession() {}

  /**
   * Retrieves the value associated with the given key.
   *
   * @param key the key for the session data
   * @return the value associated with the key, or null if the key does not exist
   */
  public Object getValue(String key) {
    return userSessionData.get(key);
  }

  /**
   * Sets the value for the given key. If the key already exists, its value is updated.
   *
   * @param key the key for the session data
   * @param value the value to be associated with the key
   */
  public void setValue(String key, Object value) {
    userSessionData.put(key, value);
  }
}
