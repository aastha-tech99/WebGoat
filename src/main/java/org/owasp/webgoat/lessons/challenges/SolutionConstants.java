/*
 * SPDX-FileCopyrightText: Copyright © 2017 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.challenges;

public interface SolutionConstants {

  String PASSWORD = System.getenv().getOrDefault("CHALLENGE_PASSWORD", "change-me");
}
