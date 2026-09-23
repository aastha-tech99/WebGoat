/*
 * SPDX-FileCopyrightText: Copyright © 2018 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.jwt;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.WithWebGoatUser;
import org.owasp.webgoat.container.plugins.LessonTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WithWebGoatUser
public class JWTRefreshEndpointTest extends LessonTest {

  @Value("${webgoat.lesson.jwt.refresh.password}")
  private String jwtRefreshPassword;

  @Value("${webgoat.lesson.jwt.refresh.secret}")
  private String jwtRefreshSecret;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  void solveAssignment() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    // First login to obtain tokens for Jerry
    var loginJson = Map.of("user", "Jerry", "password", jwtRefreshPassword);
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/JWT/refresh/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginJson)))
            .andExpect(status().isOk())
            .andReturn();
    Map<String, String> tokens =
        objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    String refreshToken = tokens.get("refresh_token");

    // Generate an expired token for Tom signed with the configured secret
    Instant past = Instant.parse("2018-05-12T15:03:31Z");
    String accessTokenTom =
        Jwts.builder()
            .setIssuedAt(Date.from(past))
            .setExpiration(Date.from(past.plus(Duration.ofDays(1))))
            .addClaims(Map.of("admin", "false", "user", "Tom"))
            .signWith(io.jsonwebtoken.SignatureAlgorithm.HS512, jwtRefreshSecret)
            .compact();
    Map<String, Object> refreshJson = new HashMap<>();
    refreshJson.put("refresh_token", refreshToken);
    result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/JWT/refresh/newToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessTokenTom)
                    .content(objectMapper.writeValueAsString(refreshJson)))
            .andExpect(status().isOk())
            .andReturn();
    tokens = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    accessTokenTom = tokens.get("access_token");

    // Now checkout with the new token from Tom
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/checkout")
                .header("Authorization", "Bearer " + accessTokenTom))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonCompleted", is(true)));
  }

  @Test
  void solutionWithAlgNone() throws Exception {
    String tokenWithNoneAlgorithm =
        Jwts.builder()
            .setHeaderParam("alg", "none")
            .addClaims(Map.of("admin", "true", "user", "Tom"))
            .compact();

    // Now checkout with the new token from Tom
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/checkout")
                .header("Authorization", "Bearer " + tokenWithNoneAlgorithm))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonCompleted", is(true)))
        .andExpect(
            jsonPath("$.feedback", CoreMatchers.is(messages.getMessage("jwt-refresh-alg-none"))));
  }

  @Test
  void checkoutWithTomsTokenFromAccessLogShouldFail() throws Exception {
    // Generate an expired token for Tom signed with the configured secret
    Instant past = Instant.parse("2018-05-12T15:03:31Z");
    String accessTokenTom =
        Jwts.builder()
            .setIssuedAt(Date.from(past))
            .setExpiration(Date.from(past.plus(Duration.ofDays(1))))
            .addClaims(Map.of("admin", "false", "user", "Tom"))
            .signWith(io.jsonwebtoken.SignatureAlgorithm.HS512, jwtRefreshSecret)
            .compact();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/checkout")
                .header("Authorization", "Bearer " + accessTokenTom))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.output", CoreMatchers.containsString("JWT expired at")));
  }

  @Test
  void checkoutWitRandomTokenShouldFail() throws Exception {
    // Token signed with wrong key to simulate a random/invalid token
    String accessTokenTom =
        Jwts.builder()
            .addClaims(Map.of("admin", "false", "user", "Tom"))
            .signWith(io.jsonwebtoken.SignatureAlgorithm.HS512, "wrong-signing-key")
            .compact();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/checkout")
                .header("Authorization", "Bearer " + accessTokenTom))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.feedback", CoreMatchers.is(messages.getMessage("jwt-invalid-token"))));
  }

  @Test
  void flowForJerryAlwaysWorks() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    var loginJson = Map.of("user", "Jerry", "password", jwtRefreshPassword);
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/JWT/refresh/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginJson)))
            .andExpect(status().isOk())
            .andReturn();
    Map<String, String> tokens =
        objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    String accessToken = tokens.get("access_token");

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/checkout")
                .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.feedback", is("User is not Tom but Jerry, please try again")));
  }

  @Test
  void loginShouldNotWorkForJerryWithWrongPassword() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    var loginJson = Map.of("user", "Jerry", "password", jwtRefreshPassword + "wrong");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginJson)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginShouldNotWorkForTom() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    var loginJson = Map.of("user", "Tom", "password", jwtRefreshPassword);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginJson)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void newTokenShouldWorkForJerry() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> loginJson = new HashMap<>();
    loginJson.put("user", "Jerry");
    loginJson.put("password", jwtRefreshPassword);
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/JWT/refresh/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginJson)))
            .andExpect(status().isOk())
            .andReturn();
    Map<String, String> tokens =
        objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    String accessToken = tokens.get("access_token");
    String refreshToken = tokens.get("refresh_token");

    var refreshJson = Map.of("refresh_token", refreshToken);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/newToken")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(refreshJson)))
        .andExpect(status().isOk());
  }

  @Test
  void unknownRefreshTokenShouldGiveUnauthorized() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> loginJson = new HashMap<>();
    loginJson.put("user", "Jerry");
    loginJson.put("password", jwtRefreshPassword);
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/JWT/refresh/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginJson)))
            .andExpect(status().isOk())
            .andReturn();
    Map<String, String> tokens =
        objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    String accessToken = tokens.get("access_token");

    var refreshJson = Map.of("refresh_token", "wrong_refresh_token");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/JWT/refresh/newToken")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(refreshJson)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void noTokenWhileCheckoutShouldReturn401() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.post("/JWT/refresh/checkout"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void noTokenWhileRequestingNewTokenShouldReturn401() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.post("/JWT/refresh/newToken"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void noTokenWhileLoginShouldReturn401() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.post("/JWT/refresh/login"))
        .andExpect(status().isUnauthorized());
  }
}
