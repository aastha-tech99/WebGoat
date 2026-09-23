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

  // Parse simple SELECT ... FROM <table> WHERE <column> = <value>
  private static final Pattern SAFE_SELECT =
      Pattern.compile(
          "(?i)^\\s*SELECT\\s+(?:\\*|[\\w\\s,]+)\\s+FROM\\s+(\\w+)"
              + "\\s+WHERE\\s+(\\w+)\\s*=\\s*(?:'([^']*)'|(\\d+))\\s*;?\\s*$");

  private static final String QUERY_BY_USERID = "SELECT * FROM employees WHERE userid = ?";
  private static final String QUERY_BY_FIRST_NAME =
      "SELECT * FROM employees WHERE first_name = ?";
  private static final String QUERY_BY_LAST_NAME =
      "SELECT * FROM employees WHERE last_name = ?";
  private static final String QUERY_BY_DEPARTMENT =
      "SELECT * FROM employees WHERE department = ?";
  private static final String QUERY_BY_SALARY = "SELECT * FROM employees WHERE salary = ?";
  private static final String QUERY_BY_AUTH_TAN =
      "SELECT * FROM employees WHERE auth_tan = ?";

  /** Resolves user-supplied column to a known constant SQL query, breaking taint flow. */
  private static String resolveQuery(String column) {
    return switch (column.toLowerCase()) {
      case "userid" -> QUERY_BY_USERID;
      case "first_name" -> QUERY_BY_FIRST_NAME;
      case "last_name" -> QUERY_BY_LAST_NAME;
      case "department" -> QUERY_BY_DEPARTMENT;
      case "salary" -> QUERY_BY_SALARY;
      case "auth_tan" -> QUERY_BY_AUTH_TAN;
      default -> null;
    };
  }

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
    try (var connection = dataSource.getConnection()) {
      Matcher matcher = SAFE_SELECT.matcher(query.trim());
      if (!matcher.matches()) {
        return failed(this).feedback("sql-injection.2.failed").build();
      }
      String table = matcher.group(1);
      if (!"employees".equalsIgnoreCase(table)) {
        return failed(this).feedback("sql-injection.2.failed").build();
      }
      String column = matcher.group(2);
      String querySql = resolveQuery(column);
      if (querySql == null) {
        return failed(this).feedback("sql-injection.2.failed").build();
      }
      String stringValue = matcher.group(3);
      String numericValue = matcher.group(4);
      String value = (numericValue != null) ? numericValue : stringValue;

      PreparedStatement statement =
          connection.prepareStatement(
              querySql,
              TYPE_SCROLL_INSENSITIVE,
              CONCUR_READ_ONLY);
      statement.setString(1, value);
      ResultSet results = statement.executeQuery();
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
}
