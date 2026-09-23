/*
 * SPDX-FileCopyrightText: Copyright © 2019 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.integration;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;


public class WebGoatIntegrationTest extends IntegrationTest {

  @Test
  public void runTests() {
    startLesson("WebGoatIntroduction");

    Map<String, Object> params = new HashMap<>();
    params.put("email", this.getUser() + "@webgoat.org");
      checkAssignment(webGoatUrlConfig.url("WebGoat/mail/send"), params, false);

    String responseBody = readMailbox();

    String decoded = responseBody.replace("%20", " ");
    int codeStart = decoded.lastIndexOf("your unique code is: ");
    String uniqueCode =
        decoded.substring(
            21 + codeStart,
            codeStart + (21 + this.getUser().length()));
    params.clear();
    params.put("uniqueCode", uniqueCode);
      checkAssignment(webGoatUrlConfig.url("WebGoat/mail"), params, true);

    checkResults("WebGoatIntroduction");
  }
}
