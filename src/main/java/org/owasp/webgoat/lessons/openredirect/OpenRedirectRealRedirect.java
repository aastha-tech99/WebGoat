/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import java.net.URI;
import java.net.URISyntaxException;
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
      Set.of("localhost", "127.0.0.1", "webgoat.local");
  private static final String SAFE_DEFAULT = "/welcome.mvc";

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    String safeUrl = validateRedirectUrl(url);
    return new ModelAndView("redirect:" + safeUrl);
  }

  private String validateRedirectUrl(String url) {
    if (url == null || url.isBlank()) {
      return SAFE_DEFAULT;
    }
    // Reject protocol-relative URLs
    if (url.startsWith("//")) {
      return SAFE_DEFAULT;
    }
    // Allow relative paths that start with / and do not contain path traversal
    if (!url.contains("://") && url.startsWith("/") && !url.contains("..")) {
      return url;
    }
    // For absolute URLs, only allow known internal hosts
    try {
      URI uri = new URI(url);
      String host = uri.getHost();
      if (host != null && ALLOWED_HOSTS.contains(host.toLowerCase())) {
        String path = uri.getPath();
        return (path != null && !path.isEmpty()) ? path : SAFE_DEFAULT;
      }
    } catch (URISyntaxException ex) {
      // Invalid URL falls through to safe default
    }
    return SAFE_DEFAULT;
  }
}
