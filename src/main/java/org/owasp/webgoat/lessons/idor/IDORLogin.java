/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.idor;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.container.session.LessonSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints({"idor.hints.idor_login"})
public class IDORLogin implements AssignmentEndpoint {
  private final LessonSession lessonSession;

  public IDORLogin(LessonSession lessonSession) {
    this.lessonSession = lessonSession;
  }

  private final Map<String, Map<String, String>> idorUserInfo = new ConcurrentHashMap<>();

  public void initIDORInfo() {

    idorUserInfo.put("tom", new ConcurrentHashMap<>(Map.of(
        "password", "cat", "id", "2342384", "color", "yellow", "size", "small")));

    idorUserInfo.put("bill", new ConcurrentHashMap<>(Map.of(
        "password", "buffalo", "id", "2342388", "color", "brown", "size", "large")));
  }

  @PostMapping("/IDOR/login")
  @ResponseBody
  public AttackResult completed(@RequestParam String username, @RequestParam String password) {
    initIDORInfo();

    if (idorUserInfo.containsKey(username)) {
      if ("tom".equals(username) && idorUserInfo.get("tom").get("password").equals(password)) {
        lessonSession.setValue("idor-authenticated-as", username);
        lessonSession.setValue("idor-authenticated-user-id", idorUserInfo.get(username).get("id"));
        return success(this).feedback("idor.login.success").feedbackArgs(username).build();
      } else {
        return failed(this).feedback("idor.login.failure").build();
      }
    } else {
      return failed(this).feedback("idor.login.failure").build();
    }
  }
}
