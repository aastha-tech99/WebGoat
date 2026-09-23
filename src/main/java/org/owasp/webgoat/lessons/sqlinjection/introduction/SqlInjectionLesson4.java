/*
 * SPDX-FileCopyrightText: Copyright © 2018 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.introduction;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints(
    value = {"SqlStringInjectionHint4-1", "SqlStringInjectionHint4-2", "SqlStringInjectionHint4-3"})
public class SqlInjectionLesson4 implements AssignmentEndpoint {

  // Parse: ALTER TABLE <table> ADD <column> <type>
  private static final Pattern SAFE_ALTER_ADD =
      Pattern.compile(
          "(?i)^\\s*ALTER\\s+TABLE\\s+(\\w+)\\s+ADD\\s+(\\w+)"
              + "\\s+(VARCHAR\\(\\d+\\)|INT|CHAR\\(\\d+\\)|BOOLEAN|DATE|TIMESTAMP|BIGINT)"
              + "\\s*;?\\s*$");

  // Allowed column name pattern: simple word characters only
  private static final Pattern SAFE_COLUMN_NAME = Pattern.compile("^[a-zA-Z]\\w{0,29}$");

  /** Resolves user-supplied column name to a known literal, breaking taint flow. */
  private static String resolveColumnName(String name) {
    return switch (name.toLowerCase()) {
      case "phone" -> "phone";
      case "email" -> "email";
      case "mobile" -> "mobile";
      case "address" -> "address";
      case "fax" -> "fax";
      case "title" -> "title";
      case "city" -> "city";
      case "state" -> "state";
      case "zip" -> "zip";
      case "country" -> "country";
      default -> null;
    };
  }

  /** Resolves user-supplied SQL type to a known constant, breaking taint flow. */
  private static String resolveType(String type) {
    String upper = type.toUpperCase();
    return switch (upper) {
      case "INT" -> "INT";
      case "BOOLEAN" -> "BOOLEAN";
      case "DATE" -> "DATE";
      case "TIMESTAMP" -> "TIMESTAMP";
      case "BIGINT" -> "BIGINT";
      default -> {
        if (upper.startsWith("VARCHAR(") && upper.endsWith(")")) {
          int size = Integer.parseInt(upper.substring(8, upper.length() - 1));
          if (size > 0 && size <= 255) {
            yield "VARCHAR(" + size + ")";
          }
        } else if (upper.startsWith("CHAR(") && upper.endsWith(")")) {
          int size = Integer.parseInt(upper.substring(5, upper.length() - 1));
          if (size > 0 && size <= 255) {
            yield "CHAR(" + size + ")";
          }
        }
        yield null;
      }
    };
  }

  private final LessonDataSource dataSource;

  public SqlInjectionLesson4(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjection/attack4")
  @ResponseBody
  public AttackResult completed(@RequestParam String query) {
    return injectableQuery(query);
  }

  protected AttackResult injectableQuery(String query) {
    try (Connection connection = dataSource.getConnection()) {
      Matcher matcher = SAFE_ALTER_ADD.matcher(query.trim());
      if (!matcher.matches()) {
        return failed(this).output(query).build();
      }
      String table = matcher.group(1);
      if (!"employees".equalsIgnoreCase(table)) {
        return failed(this).output(query).build();
      }
      String columnName = matcher.group(2);
      if (!SAFE_COLUMN_NAME.matcher(columnName).matches()) {
        return failed(this).output(query).build();
      }
      String safeColumn = resolveColumnName(columnName);
      if (safeColumn == null) {
        return failed(this).output(query).build();
      }
      String safeType = resolveType(matcher.group(3));
      if (safeType == null) {
        return failed(this).output(query).build();
      }

      try {
        // Reconstruct DDL from resolved identifiers (DDL does not support bind parameters)
        PreparedStatement statement =
            connection.prepareStatement(
                "ALTER TABLE employees ADD " + safeColumn + " " + safeType);
        statement.executeUpdate();
        connection.commit();

        PreparedStatement checkStatement =
            connection.prepareStatement("SELECT phone FROM employees");
        ResultSet results = checkStatement.executeQuery();
        StringBuilder output = new StringBuilder();
        // user completes lesson if column phone exists
        if (results.next()) {
          output.append("<span class='feedback-positive'>" + query + "</span>");
          return success(this).output(output.toString()).build();
        } else {
          return failed(this).output(output.toString()).build();
        }
      } catch (SQLException sqle) {
        return failed(this).output(sqle.getMessage()).build();
      }
    } catch (Exception e) {
      return failed(this)
          .output(this.getClass().getName() + " : " + e.getMessage())
          .build();
    }
  }
}
