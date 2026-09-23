/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import java.net.URI;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  private static final Set<String> ALLOWED_HOSTS =
      Set.of("webgoat.local", "localhost", "127.0.0.1");
  private static final String DEFAULT_REDIRECT = "/welcome.mvc";

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    String validated = validateRedirectUrl(url);
    return new ModelAndView("redirect:" + validated);
  }

  private String validateRedirectUrl(String url) {
    if (url == null || url.isBlank()) {
      return DEFAULT_REDIRECT;
    }
    // Allow relative paths starting with a single slash (not protocol-relative //)
    if (url.startsWith("/") && !url.startsWith("//") && !url.contains("..")) {
      return url;
    }
    // For absolute URLs, only allow hosts on the internal allowlist
    try {
      URI uri = new URI(url);
      String host = uri.getHost();
      if (host != null && ALLOWED_HOSTS.contains(host.toLowerCase())) {
        return url;
      }
    } catch (Exception ex) {
      // Malformed URI — fall through to default
    }
    return DEFAULT_REDIRECT;
  }
}
