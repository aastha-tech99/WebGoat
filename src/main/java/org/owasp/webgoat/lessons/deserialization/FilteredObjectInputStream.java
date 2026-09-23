/*
 * SPDX-FileCopyrightText: Copyright © 2019 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Set;

/**
 * An ObjectInputStream that restricts deserialization to an explicit set of allowed classes.
 * Classes not in the allowlist are rejected in {@link #resolveClass} before instantiation.
 */
class FilteredObjectInputStream extends ObjectInputStream {

  private final Set<String> allowedClasses;
  private final String allowedPackagePrefix;

  FilteredObjectInputStream(
      InputStream in, Set<String> allowedClasses, String allowedPackagePrefix) throws IOException {
    super(in);
    this.allowedClasses = allowedClasses;
    this.allowedPackagePrefix = allowedPackagePrefix;
  }

  @Override
  protected Class<?> resolveClass(ObjectStreamClass desc)
      throws IOException, ClassNotFoundException {
    String name = desc.getName();
    if (!allowedClasses.contains(name)
        && (allowedPackagePrefix == null || !name.startsWith(allowedPackagePrefix))) {
      throw new InvalidClassException("Deserialization of class not allowed", name);
    }
    return super.resolveClass(desc);
  }
}
