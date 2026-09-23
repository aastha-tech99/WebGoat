/*
 * SPDX-FileCopyrightText: Copyright © 2018 WebGoat authors
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
@AssignmentHints(
    value = {"SqlStringInjectionHint4-1", "SqlStringInjectionHint4-2", "SqlStringInjectionHint4-3"})
public class SqlInjectionLesson4 implements AssignmentEndpoint {

  private static final Set<String> ALLOWED_TYPES =
      Set.of(
          "varchar", "int", "integer", "char", "boolean", "date", "timestamp", "decimal", "float",
          "double", "bigint", "smallint", "text", "clob", "blob");

  private static final Pattern ALTER_ADD_PATTERN =
      Pattern.compile(
          "^\\s*ALTER\\s+TABLE\\s+(\\w+)\\s+ADD\\s+(\\w+)\\s+(\\w+(?:\\([^)]+\\))?)\\s*;?\\s*$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern TYPE_PATTERN =
      Pattern.compile("^(\\w+)(?:\\(([^)]+)\\))?$");

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
    Matcher matcher = ALTER_ADD_PATTERN.matcher(query.trim());
    if (!matcher.matches()) {
      return failed(this).output("Invalid query format.").build();
    }

    if (!"employees".equalsIgnoreCase(matcher.group(1).trim())) {
      return failed(this).output("Invalid table.").build();
    }

    String colName = matcher.group(2).trim();
    if (!colName.matches("^[a-zA-Z]\\w*$")) {
      return failed(this).output("Invalid column name.").build();
    }

    String rawType = matcher.group(3).trim();
    Matcher typeMatcher = TYPE_PATTERN.matcher(rawType);
    if (!typeMatcher.matches()) {
      return failed(this).output("Invalid data type.").build();
    }

    String safeBaseType = resolveType(typeMatcher.group(1));
    if (safeBaseType == null) {
      return failed(this).output("Invalid data type.").build();
    }

    String sizeSpec = typeMatcher.group(2);
    String safeType = safeBaseType;
    if (sizeSpec != null) {
      if (!sizeSpec.matches("\\d+(?:\\s*,\\s*\\d+)?")) {
        return failed(this).output("Invalid type size.").build();
      }
      safeType = safeBaseType + "(" + sizeSpec + ")";
    }

    try (Connection connection = dataSource.getConnection()) {
      try {
        String safeQuery = "ALTER TABLE employees ADD " + colName + " " + safeType;
        PreparedStatement alterStatement = connection.prepareStatement(safeQuery);
        alterStatement.executeUpdate();
        connection.commit();

        PreparedStatement checkStatement =
            connection.prepareStatement(
                "SELECT phone FROM employees", TYPE_SCROLL_INSENSITIVE, CONCUR_READ_ONLY);
        ResultSet results = checkStatement.executeQuery();
        StringBuilder output = new StringBuilder();
        // user completes lesson if column phone exists
        if (results.first()) {
          output.append("<span class='feedback-positive'>" + query + "</span>");
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

  private static String resolveType(String input) {
    for (String type : ALLOWED_TYPES) {
      if (type.equalsIgnoreCase(input.trim())) {
        return type;
      }
    }
    return null;
  }
}
