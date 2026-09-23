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
import java.util.Map;
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

  // Parse: GRANT <privilege> ON <table> TO <user>
  private static final Pattern SAFE_GRANT =
      Pattern.compile(
          "(?i)^\\s*GRANT\\s+(SELECT|INSERT|UPDATE|DELETE|ALL)\\s+ON\\s+(\\w+)"
              + "\\s+TO\\s+(\\w+)\\s*;?\\s*$");

  /** Precomputed GRANT statements keyed by privilege (table and user are fixed). */
  private static final Map<String, String> GRANT_QUERIES =
      Map.of(
          "SELECT", "GRANT SELECT ON grant_rights TO unauthorized_user",
          "INSERT", "GRANT INSERT ON grant_rights TO unauthorized_user",
          "UPDATE", "GRANT UPDATE ON grant_rights TO unauthorized_user",
          "DELETE", "GRANT DELETE ON grant_rights TO unauthorized_user",
          "ALL", "GRANT ALL ON grant_rights TO unauthorized_user");

  /** Resolves user-supplied privilege to a known literal, breaking taint flow. */
  private static String resolvePrivilege(String priv) {
    return switch (priv.toUpperCase()) {
      case "SELECT" -> "SELECT";
      case "INSERT" -> "INSERT";
      case "UPDATE" -> "UPDATE";
      case "DELETE" -> "DELETE";
      case "ALL" -> "ALL";
      default -> null;
    };
  }

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

  protected AttackResult injectableQuery(String query) {
    try (Connection connection = dataSource.getConnection()) {
      Matcher matcher = SAFE_GRANT.matcher(query.trim());
      if (matcher.matches()) {
        String resolvedPrivilege = resolvePrivilege(matcher.group(1));
        if (resolvedPrivilege == null) {
          return failed(this).output("Your query was: " + query).build();
        }
        String tableName = matcher.group(2);
        String userName = matcher.group(3);

        // Only allow the expected table and user for this lesson
        if (!"grant_rights".equalsIgnoreCase(tableName)
            || !"unauthorized_user".equalsIgnoreCase(userName)) {
          return failed(this).output("Your query was: " + query).build();
        }

        // Look up precomputed GRANT SQL (DCL does not support bind parameters)
        String safeSql = GRANT_QUERIES.get(resolvedPrivilege);
        if (safeSql == null) {
          return failed(this).output("Your query was: " + query).build();
        }
        PreparedStatement statement = connection.prepareStatement(safeSql);
        statement.execute();

        if (checkSolution(connection)) {
          return success(this).build();
        }
        return failed(this).output("Your query was: " + query).build();
      } else {
        return failed(this)
            .output(
                this.getClass().getName()
                    + " : Query not recognized as a valid GRANT statement"
                    + "<br> Your query was: "
                    + query)
            .build();
      }
    } catch (Exception e) {
      return failed(this)
          .output(
              this.getClass().getName() + " : " + e.getMessage() + "<br> Your query was: " + query)
          .build();
    }
  }

  private boolean checkSolution(Connection connection) {
    try {
      var stmt =
          connection.prepareStatement(
              "SELECT * FROM INFORMATION_SCHEMA.TABLE_PRIVILEGES WHERE TABLE_NAME = ? AND GRANTEE = ?");
      stmt.setString(1, "GRANT_RIGHTS");
      stmt.setString(2, "UNAUTHORIZED_USER");
      var resultSet = stmt.executeQuery();
      return resultSet.next();
    } catch (SQLException throwables) {
      return false;
    }
  }
}
