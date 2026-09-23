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
import java.util.Map;
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

  // Parse: UPDATE <table> SET <column> = '<value>' WHERE <column> = '<value>'
  private static final Pattern SAFE_UPDATE =
      Pattern.compile(
          "(?i)^\\s*UPDATE\\s+(\\w+)\\s+SET\\s+(\\w+)\\s*=\\s*'([^']*)'"
              + "\\s+WHERE\\s+(\\w+)\\s*=\\s*'([^']*)'\\s*;?\\s*$");

  private static final Map<String, String> EMPLOYEE_COLUMNS =
      Map.of(
          "userid", "userid",
          "first_name", "first_name",
          "last_name", "last_name",
          "department", "department",
          "salary", "salary",
          "auth_tan", "auth_tan");

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
    try (Connection connection = dataSource.getConnection()) {
      Matcher matcher = SAFE_UPDATE.matcher(query.trim());
      if (!matcher.matches()) {
        return failed(this).build();
      }
      String table = matcher.group(1);
      if (!"employees".equalsIgnoreCase(table)) {
        return failed(this).build();
      }
      String setColumn = matcher.group(2);
      String safeSetColumn = EMPLOYEE_COLUMNS.get(setColumn.toLowerCase());
      if (safeSetColumn == null) {
        return failed(this).build();
      }
      String setValue = matcher.group(3);
      String whereColumn = matcher.group(4);
      String safeWhereColumn = EMPLOYEE_COLUMNS.get(whereColumn.toLowerCase());
      if (safeWhereColumn == null) {
        return failed(this).build();
      }
      String whereValue = matcher.group(5);

      try {
        PreparedStatement statement =
            connection.prepareStatement(
                "UPDATE employees SET "
                    + safeSetColumn
                    + " = ? WHERE "
                    + safeWhereColumn
                    + " = ?");
        statement.setString(1, setValue);
        statement.setString(2, whereValue);
        statement.executeUpdate();

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
      return failed(this)
          .output(this.getClass().getName() + " : " + e.getMessage())
          .build();
    }
  }
}
