/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.introduction;

import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints(
    value = {
      "SqlStringInjectionHint2-1",
      "SqlStringInjectionHint2-2",
      "SqlStringInjectionHint2-3",
      "SqlStringInjectionHint2-4"
    })
public class SqlInjectionLesson2 implements AssignmentEndpoint {

  private static final Set<String> ALLOWED_COLUMNS =
      Set.of("userid", "first_name", "last_name", "department", "salary", "auth_tan");

  private static final Pattern SELECT_PATTERN =
      Pattern.compile(
          "^\\s*SELECT\\s+([\\w\\s,*]+?)\\s+FROM\\s+(\\w+)"
              + "(?:\\s+WHERE\\s+(\\w+)\\s*=\\s*(?:'([^']*)'|([\\w.]+)))?\\s*;?\\s*$",
          Pattern.CASE_INSENSITIVE);

  private final LessonDataSource dataSource;

  public SqlInjectionLesson2(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjection/attack2")
  @ResponseBody
  public AttackResult completed(@RequestParam String query) {
    return injectableQuery(query);
  }

  protected AttackResult injectableQuery(String query) {
    Matcher matcher = SELECT_PATTERN.matcher(query.trim());
    if (!matcher.matches()) {
      return failed(this).feedback("sql-injection.2.failed").build();
    }

    if (!"employees".equalsIgnoreCase(matcher.group(2).trim())) {
      return failed(this).feedback("sql-injection.2.failed").build();
    }

    String safeCols = resolveColumns(matcher.group(1));
    if (safeCols == null) {
      return failed(this).feedback("sql-injection.2.failed").build();
    }

    try (var connection = dataSource.getConnection()) {
      ResultSet results;
      if (matcher.group(3) != null) {
        String safeWhereCol = resolveColumn(matcher.group(3));
        if (safeWhereCol == null) {
          return failed(this).feedback("sql-injection.2.failed").build();
        }
        String whereValue = matcher.group(4) != null ? matcher.group(4) : matcher.group(5);
        String safeQuery =
            "SELECT " + safeCols + " FROM employees WHERE " + safeWhereCol + " = ?";
        PreparedStatement statement =
            connection.prepareStatement(safeQuery, TYPE_SCROLL_INSENSITIVE, CONCUR_READ_ONLY);
        statement.setString(1, whereValue);
        results = statement.executeQuery();
      } else {
        String safeQuery = "SELECT " + safeCols + " FROM employees";
        PreparedStatement statement =
            connection.prepareStatement(safeQuery, TYPE_SCROLL_INSENSITIVE, CONCUR_READ_ONLY);
        results = statement.executeQuery();
      }

      StringBuilder output = new StringBuilder();
      if (!results.first()) {
        return failed(this).feedback("sql-injection.2.failed").build();
      }

      if ("Marketing".equals(results.getString("department"))) {
        output
            .append("<span class='feedback-positive'>")
            .append(query)
            .append("</span>");
        output.append(SqlInjectionLesson8.generateTable(results));
        return success(this)
            .feedback("sql-injection.2.success")
            .output(output.toString())
            .build();
      } else {
        return failed(this)
            .feedback("sql-injection.2.failed")
            .output(output.toString())
            .build();
      }
    } catch (SQLException sqle) {
      return failed(this).feedback("sql-injection.2.failed").output(sqle.getMessage()).build();
    }
  }

  private static String resolveColumn(String input) {
    if ("*".equals(input.trim())) return "*";
    for (String col : ALLOWED_COLUMNS) {
      if (col.equalsIgnoreCase(input.trim())) {
        return col;
      }
    }
    return null;
  }

  private static String resolveColumns(String rawColumns) {
    String[] parts = rawColumns.split("\\s*,\\s*");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      String safe = resolveColumn(parts[i]);
      if (safe == null) return null;
      if (i > 0) sb.append(", ");
      sb.append(safe);
    }
    return sb.toString();
  }
}
