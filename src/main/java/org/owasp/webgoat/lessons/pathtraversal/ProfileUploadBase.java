/*
 * SPDX-FileCopyrightText: Copyright © 2020 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.pathtraversal;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.informationMessage;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
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
      var requestedFile = new File(uploadDirectory, fullName);

      // Detect path traversal: check if resolved path escapes the upload directory
      if (attemptWasMade(uploadDirectory, requestedFile)) {
        // Traversal detected - write safely within directory using only the base filename
        var safeName = requestedFile.getCanonicalFile().getName();
        var safeFile = new File(uploadDirectory, safeName);
        safeFile.createNewFile();
        FileCopyUtils.copy(file.getBytes(), safeFile);
        return solvedIt(requestedFile);
      }

      // No traversal - validate canonical path stays within upload directory
      var canonicalDir = uploadDirectory.getCanonicalPath();
      var resolvedFile = requestedFile.getCanonicalFile();
      if (!resolvedFile.getPath().startsWith(canonicalDir + File.separator)) {
        return failed(this).feedback("path-traversal-profile-attempt").feedbackArgs(fullName).build();
      }

      resolvedFile.createNewFile();
      FileCopyUtils.copy(file.getBytes(), resolvedFile);
      return informationMessage(this)
          .feedback("path-traversal-profile-updated")
          .feedbackArgs(resolvedFile.getAbsoluteFile())
          .build();

    } catch (IOException e) {
      return failed(this).output(e.getMessage()).build();
    }
  }

  @SneakyThrows
  protected File cleanupAndCreateDirectoryForUser(String username) {
    var parentDir = new File(this.webGoatHomeDirectory, "/PathTraversal");
    var uploadDirectory = new File(parentDir, username);
    // Validate resolved path stays within the expected parent directory
    if (!uploadDirectory.toPath().normalize().startsWith(parentDir.toPath().normalize())) {
      throw new IllegalArgumentException("Invalid username for directory creation");
    }
    if (uploadDirectory.exists()) {
      FileSystemUtils.deleteRecursively(uploadDirectory);
    }
    Files.createDirectories(uploadDirectory.toPath());
    return uploadDirectory;
  }

  private boolean attemptWasMade(File expectedUploadDirectory, File uploadedFile)
      throws IOException {
    return !expectedUploadDirectory
        .getCanonicalPath()
        .equals(uploadedFile.getParentFile().getCanonicalPath());
  }

  private AttackResult solvedIt(File uploadedFile) throws IOException {
    if (uploadedFile.getCanonicalFile().getParentFile().getName().endsWith("PathTraversal")) {
      return success(this).build();
    }
    return failed(this)
        .attemptWasMade()
        .feedback("path-traversal-profile-attempt")
        .feedbackArgs(uploadedFile.getCanonicalPath())
        .build();
  }

  public ResponseEntity<?> getProfilePicture(@CurrentUsername String username) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(MediaType.IMAGE_JPEG_VALUE))
        .body(getProfilePictureAsBase64(username));
  }

  protected byte[] getProfilePictureAsBase64(String username) {
    var parentDir = new File(this.webGoatHomeDirectory, "/PathTraversal");
    var profilePictureDirectory = new File(parentDir, username);
    // Validate resolved path stays within the expected parent directory
    if (!profilePictureDirectory.toPath().normalize().startsWith(parentDir.toPath().normalize())) {
      return defaultImage();
    }
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
                  if (!targetFile
                      .getCanonicalPath()
                      .startsWith(
                          profilePictureDirectory.getCanonicalPath() + File.separator)) {
                    return defaultImage();
                  }
                  try (var inputStream = new FileInputStream(targetFile)) {
                    return Base64.getEncoder().encode(FileCopyUtils.copyToByteArray(inputStream));
                  }
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
