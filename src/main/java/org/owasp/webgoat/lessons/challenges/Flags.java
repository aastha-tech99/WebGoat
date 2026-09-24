/*
 * SPDX-FileCopyrightText: Copyright © 2023 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.challenges;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Flags {
  private final Map<Integer, Flag> FLAGS = new ConcurrentHashMap<>();

  public Flags() {
    IntStream.range(1, 10).forEach(i -> FLAGS.put(i, new Flag(i, UUID.randomUUID().toString())));
  }

  public Flag getFlag(int flagNumber) {
    return FLAGS.get(flagNumber);
  }
}
