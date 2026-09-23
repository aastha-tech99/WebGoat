/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.lessons.Category;
import org.springframework.web.servlet.ModelAndView;

class OpenRedirectLessonMetadataTest {

  private final OpenRedirect lesson = new OpenRedirect();
  private final OpenRedirectSecureController secureController = new OpenRedirectSecureController();
  private final OpenRedirectRealRedirect realRedirect = new OpenRedirectRealRedirect();

  @Test
  void lessonMetadataMatchesRegistration() {
    assertThat(lesson.getDefaultCategory()).isEqualTo(Category.GENERAL);
    assertThat(lesson.getTitle()).isEqualTo("openredirect.title");
  }

  @Test
  void safeRedirectUsesMappedDestinationWhenKnown() {
    ModelAndView response = secureController.safe(3);

    assertThat(response.getViewName()).isEqualTo("redirect:/logout");
  }

  @Test
  void safeRedirectFallsBackToWelcomeWhenUnknownId() {
    ModelAndView response = secureController.safe(99);

    assertThat(response.getViewName()).isEqualTo("redirect:/welcome.mvc");
  }

  @Test
  void realRedirectBlocksExternalUrlAndFallsBackToSafeDefault() {
    ModelAndView response = realRedirect.real("https://attacker.example");

    assertThat(response.getViewName()).isEqualTo("redirect:/welcome.mvc");
  }

  @Test
  void realRedirectAllowsRelativeInternalPath() {
    ModelAndView response = realRedirect.real("/OpenRedirect/safe?destId=1");

    assertThat(response.getViewName()).isEqualTo("redirect:/OpenRedirect/safe?destId=1");
  }

  @Test
  void realRedirectBlocksProtocolRelativeUrl() {
    ModelAndView response = realRedirect.real("//evil.example/steal");

    assertThat(response.getViewName()).isEqualTo("redirect:/welcome.mvc");
  }
}
