/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.introduction;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints(
    value = {
      "SqlStringInjectionHint5-1",
      "SqlStringInjectionHint5-2",
      "SqlStringInjectionHint5-3",
      "SqlStringInjectionHint5-4"
    })
public class SqlInjectionLesson5 implements AssignmentEndpoint {

  private final LessonDataSource dataSource;

  public SqlInjectionLesson5(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostConstruct
  public void createUser() {
    // HSQLDB does not support CREATE USER with IF NOT EXISTS so we need to do it in code (using
    // DROP first will throw error if user does not exists)
    try (Connection connection = dataSource.getConnection()) {
      try (var statement =
          connection.prepareStatement("CREATE USER unauthorized_user PASSWORD test")) {
        statement.execute();
      }
    } catch (Exception e) {
      // user already exists continue
    }
  }

  @PostMapping("/SqlInjection/attack5")
  @ResponseBody
  public AttackResult completed(String query) {
    createUser();
    return injectableQuery(query);
  }

  private static final Set<String> ALLOWED_PRIVILEGES =
      Set.of("select", "insert", "update", "delete", "all");

  private static final Set<String> ALLOWED_TABLES =
      Set.of(
          "grant_rights", "employees", "user_data", "user_system_data", "access_log",
          "sql_challenge_users");

  private static final Pattern GRANT_PATTERN =
      Pattern.compile(
          "^\\s*GRANT\\s+(\\w+)\\s+ON\\s+(\\w+)\\s+TO\\s+(\\w+)\\s*$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern SELECT_PATTERN =
      Pattern.compile(
          "^\\s*SELECT\\s+([\\w\\s,*]+?)\\s+FROM\\s+(\\w+)\\s*;?\\s*$",
          Pattern.CASE_INSENSITIVE);

  protected AttackResult injectableQuery(String query) {
    try (Connection connection = dataSource.getConnection()) {
      Matcher grantMatcher = GRANT_PATTERN.matcher(query.trim());
      if (grantMatcher.matches()) {
        return executeGrant(connection, query, grantMatcher);
      }

      Matcher selectMatcher = SELECT_PATTERN.matcher(query.trim());
      if (selectMatcher.matches()) {
        return executeSelect(connection, query, selectMatcher);
      }

      return failed(this).output("Invalid query format. Your query was: " + query).build();
    } catch (Exception e) {
      return failed(this)
          .output(
              this.getClass().getName() + " : " + e.getMessage() + "<br> Your query was: " + query)
          .build();
    }
  }

  private AttackResult executeGrant(Connection connection, String query, Matcher matcher) {
    String safePriv = resolveValue(matcher.group(1), ALLOWED_PRIVILEGES);
    String safeTable = resolveValue(matcher.group(2), ALLOWED_TABLES);
    String user = matcher.group(3).trim();
    if (safePriv == null || safeTable == null || !user.matches("^\\w+$")) {
      return failed(this).output("Invalid query. Your query was: " + query).build();
    }
    try {
      String safeQuery = "GRANT " + safePriv + " ON " + safeTable + " TO " + user;
      PreparedStatement statement = connection.prepareStatement(safeQuery);
      statement.execute();
      if (checkSolution(connection)) {
        return success(this).build();
      }
      return failed(this).output("Your query was: " + query).build();
    } catch (SQLException e) {
      return failed(this)
          .output(e.getMessage() + "<br> Your query was: " + query)
          .build();
    }
  }

  private AttackResult executeSelect(Connection connection, String query, Matcher matcher) {
    String safeTable = resolveValue(matcher.group(2), ALLOWED_TABLES);
    if (safeTable == null) {
      return failed(this).output("Invalid table. Your query was: " + query).build();
    }
    try {
      String safeQuery = "SELECT * FROM " + safeTable;
      PreparedStatement statement =
          connection.prepareStatement(
              safeQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
      statement.executeQuery();
      if (checkSolution(connection)) {
        return success(this).build();
      }
      return failed(this).output("Your query was: " + query).build();
    } catch (SQLException e) {
      return failed(this)
          .output(e.getMessage() + "<br> Your query was: " + query)
          .build();
    }
  }

  private static String resolveValue(String input, Set<String> allowed) {
    for (String val : allowed) {
      if (val.equalsIgnoreCase(input.trim())) {
        return val;
      }
    }
    return null;
  }

  private boolean checkSolution(Connection connection) {
    try {
      var stmt =
          connection.prepareStatement(
              "SELECT * FROM INFORMATION_SCHEMA.TABLE_PRIVILEGES WHERE TABLE_NAME = ? AND GRANTEE ="
                  + " ?");
      stmt.setString(1, "GRANT_RIGHTS");
      stmt.setString(2, "UNAUTHORIZED_USER");
      var resultSet = stmt.executeQuery();
      return resultSet.next();
    } catch (SQLException throwables) {
      return false;
    }
  }
}
