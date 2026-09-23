/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.introduction;

import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints(value = {"SqlStringInjectionHint3-1", "SqlStringInjectionHint3-2"})
public class SqlInjectionLesson3 implements AssignmentEndpoint {

  private static final Set<String> ALLOWED_COLUMNS =
      Set.of("userid", "first_name", "last_name", "department", "salary", "auth_tan");

  private static final Pattern UPDATE_PATTERN =
      Pattern.compile(
          "^\\s*UPDATE\\s+(\\w+)\\s+SET\\s+(\\w+)\\s*=\\s*(?:'([^']*)'|([\\w.]+))"
              + "(?:\\s+WHERE\\s+(\\w+)\\s*=\\s*(?:'([^']*)'|([\\w.]+)))?\\s*;?\\s*$",
          Pattern.CASE_INSENSITIVE);

  private final LessonDataSource dataSource;

  public SqlInjectionLesson3(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjection/attack3")
  @ResponseBody
  public AttackResult completed(@RequestParam String query) {
    return injectableQuery(query);
  }

  protected AttackResult injectableQuery(String query) {
    Matcher matcher = UPDATE_PATTERN.matcher(query.trim());
    if (!matcher.matches()) {
      return failed(this).output("Invalid query format.").build();
    }

    if (!"employees".equalsIgnoreCase(matcher.group(1).trim())) {
      return failed(this).output("Invalid table.").build();
    }

    String safeSetCol = resolveColumn(matcher.group(2));
    if (safeSetCol == null) {
      return failed(this).output("Invalid column.").build();
    }
    String setValue = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);

    try (Connection connection = dataSource.getConnection()) {
      try {
        if (matcher.group(5) != null) {
          String safeWhereCol = resolveColumn(matcher.group(5));
          if (safeWhereCol == null) {
            return failed(this).output("Invalid column.").build();
          }
          String whereValue = matcher.group(6) != null ? matcher.group(6) : matcher.group(7);
          String safeQuery =
              "UPDATE employees SET " + safeSetCol + " = ? WHERE " + safeWhereCol + " = ?";
          PreparedStatement statement = connection.prepareStatement(safeQuery);
          statement.setString(1, setValue);
          statement.setString(2, whereValue);
          statement.executeUpdate();
        } else {
          String safeQuery = "UPDATE employees SET " + safeSetCol + " = ?";
          PreparedStatement statement = connection.prepareStatement(safeQuery);
          statement.setString(1, setValue);
          statement.executeUpdate();
        }

        PreparedStatement checkStatement =
            connection.prepareStatement(
                "SELECT * FROM employees WHERE last_name = ?",
                TYPE_SCROLL_INSENSITIVE,
                CONCUR_READ_ONLY);
        checkStatement.setString(1, "Barnett");
        ResultSet results = checkStatement.executeQuery();
        StringBuilder output = new StringBuilder();
        // user completes lesson if the department of Tobi Barnett now is 'Sales'
        results.first();
        if (results.getString("department").equals("Sales")) {
          output.append("<span class='feedback-positive'>" + query + "</span>");
          output.append(SqlInjectionLesson8.generateTable(results));
          return success(this).output(output.toString()).build();
        } else {
          return failed(this).output(output.toString()).build();
        }

      } catch (SQLException sqle) {
        return failed(this).output(sqle.getMessage()).build();
      }
    } catch (Exception e) {
      return failed(this).output(this.getClass().getName() + " : " + e.getMessage()).build();
    }
  }

  private static String resolveColumn(String input) {
    for (String col : ALLOWED_COLUMNS) {
      if (col.equalsIgnoreCase(input.trim())) {
        return col;
      }
    }
    return null;
  }
}
