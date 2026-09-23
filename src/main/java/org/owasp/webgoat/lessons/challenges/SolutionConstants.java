/*
 * SPDX-FileCopyrightText: Copyright © 2017 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.challenges;

public interface SolutionConstants {

  // Credential loaded from environment variable; generated randomly per server start when not set
  String PASSWORD =
      System.getenv().getOrDefault(
          "WEBGOAT_CHALLENGE_PASSWORD", java.util.UUID.randomUUID().toString());
}
