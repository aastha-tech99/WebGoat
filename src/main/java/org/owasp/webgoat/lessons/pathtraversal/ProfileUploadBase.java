/*
 * SPDX-FileCopyrightText: Copyright © 2020 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.pathtraversal;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.informationMessage;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.owasp.webgoat.container.CurrentUsername;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class ProfileUploadBase implements AssignmentEndpoint {

  private final String webGoatHomeDirectory;

  public ProfileUploadBase(String webGoatHomeDirectory) {
    this.webGoatHomeDirectory = webGoatHomeDirectory;
  }

  protected AttackResult execute(MultipartFile file, String fullName, String username) {
    if (file.isEmpty()) {
      return failed(this).feedback("path-traversal-profile-empty-file").build();
    }
    if (StringUtils.isEmpty(fullName)) {
      return failed(this).feedback("path-traversal-profile-empty-name").build();
    }

    File uploadDirectory = cleanupAndCreateDirectoryForUser(username);

    try {
      // Validate input: reject null bytes and excessively long filenames
      if (fullName.indexOf('\0') >= 0 || fullName.length() > 255) {
        return failed(this).feedback("path-traversal-profile-empty-name").build();
      }
      // Use Path API to detect traversal without constructing an unsafe File object
      var uploadPath = uploadDirectory.toPath().normalize();
      var resolvedPath = uploadPath.resolve(fullName).normalize();

      // Detect traversal: input contains ".." sequences or resolved path escapes the directory
      if (fullName.contains("..") || !resolvedPath.startsWith(uploadPath)) {
        // Traversal detected - write safely within directory using only the base filename
        var safeName = resolvedPath.getFileName().toString();
        var safeTarget = uploadPath.resolve(safeName).normalize();
        if (!safeTarget.startsWith(uploadPath)) {
          return failed(this)
              .feedback("path-traversal-profile-attempt")
              .feedbackArgs(fullName)
              .build();
        }
        safeTarget.toFile().createNewFile();
        FileCopyUtils.copy(file.getBytes(), safeTarget.toFile());
        return solvedIt(resolvedPath);
      }

      // No traversal - path is verified within upload directory
      resolvedPath.toFile().createNewFile();
      FileCopyUtils.copy(file.getBytes(), resolvedPath.toFile());
      return informationMessage(this)
          .feedback("path-traversal-profile-updated")
          .feedbackArgs(resolvedPath.toAbsolutePath())
          .build();

    } catch (IOException e) {
      return failed(this).output(e.getMessage()).build();
    }
  }

  @SneakyThrows
  protected File cleanupAndCreateDirectoryForUser(String username) {
    var parentPath = Path.of(this.webGoatHomeDirectory, "PathTraversal").normalize();
    var uploadPath = parentPath.resolve(username).normalize();
    // Validate resolved path stays within the expected parent directory
    if (!uploadPath.startsWith(parentPath)) {
      throw new IllegalArgumentException("Invalid username for directory creation");
    }
    var uploadDirectory = uploadPath.toFile();
    if (uploadDirectory.exists()) {
      FileSystemUtils.deleteRecursively(uploadDirectory);
    }
    Files.createDirectories(uploadPath);
    return uploadDirectory;
  }

  private AttackResult solvedIt(Path resolvedPath) {
    if (resolvedPath.getParent() != null
        && resolvedPath.getParent().getFileName() != null
        && resolvedPath.getParent().getFileName().toString().endsWith("PathTraversal")) {
      return success(this).build();
    }
    return failed(this)
        .attemptWasMade()
        .feedback("path-traversal-profile-attempt")
        .feedbackArgs(resolvedPath.toString())
        .build();
  }

  public ResponseEntity<?> getProfilePicture(@CurrentUsername String username) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(MediaType.IMAGE_JPEG_VALUE))
        .body(getProfilePictureAsBase64(username));
  }

  protected byte[] getProfilePictureAsBase64(String username) {
    var parentPath = Path.of(this.webGoatHomeDirectory, "PathTraversal").normalize();
    var profilePicturePath = parentPath.resolve(username).normalize();
    // Validate resolved path stays within the expected parent directory
    if (!profilePicturePath.startsWith(parentPath)) {
      return defaultImage();
    }
    var profilePictureDirectory = profilePicturePath.toFile();
    var profileDirectoryFiles = profilePictureDirectory.listFiles();

    if (profileDirectoryFiles != null && profileDirectoryFiles.length > 0) {
      return Arrays.stream(profileDirectoryFiles)
          .filter(file -> FilenameUtils.isExtension(file.getName(), List.of("jpg", "png")))
          .findFirst()
          .map(
              file -> {
                try {
                  var targetFile = profileDirectoryFiles[0];
                  // Validate file stays within the expected directory
                  if (!targetFile.toPath().normalize().startsWith(profilePicturePath)) {
                    return defaultImage();
                  }
                  return Base64.getEncoder()
                      .encode(Files.readAllBytes(targetFile.toPath()));
                } catch (IOException e) {
                  return defaultImage();
                }
              })
          .orElse(defaultImage());
    } else {
      return defaultImage();
    }
  }

  @SneakyThrows
  protected byte[] defaultImage() {
    var inputStream = getClass().getResourceAsStream("/images/account.png");
    return Base64.getEncoder().encode(FileCopyUtils.copyToByteArray(inputStream));
  }
}
