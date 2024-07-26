package io.vertx.tests;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TCKUpdater {

  // current TCK
  private static final String TCK = "https://github.com/json-schema-org/JSON-Schema-Test-Suite/archive/76b529ff69fa6f284d3e8916cb82cb2e93f82557.zip";
  private static final String BASEDIR = "src/test/resources/json-schema-org/tck/";

  // we only verify a subset of the drafts, 4, 7, 2019-09 and 2020-12 These are required for OpenAPI
  private static final List<String> ALLOWED_DRAFTS = Arrays.asList("4", "7", "2019-09", "2020-12");

  public static void main(String[] args) throws IOException {
    URL tckUrl = new URL(TCK);
    HttpURLConnection connection = (HttpURLConnection) tckUrl.openConnection();
    connection.setRequestMethod("GET");

    try (InputStream in = connection.getInputStream()) {
      try (ZipInputStream zipIn = new ZipInputStream(in)) {
        ZipEntry entry = zipIn.getNextEntry();

        final Path target = Paths.get(BASEDIR);

        while (entry != null) {

          String zipEntryName = entry.getName();

          // skip 1 directory
          zipEntryName = zipEntryName.substring(zipEntryName.indexOf('/') + 1);

          // only extract remotes and tests
          if (!zipEntryName.startsWith("remotes/") && !zipEntryName.startsWith("tests/")) {
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
            continue;
          }

          Path newPath = zipSlipProtect(zipEntryName, target);

          if (entry.isDirectory()) {
            Files.createDirectories(newPath);
          } else {
            // some zip stored file path only, need create parent directories
            // e.g data/folder/file.txt
            if (newPath.getParent() != null) {
              if (Files.notExists(newPath.getParent())) {
                Files.createDirectories(newPath.getParent());
              }
            }
            // copy files, nio
            Files.copy(zipIn, newPath, StandardCopyOption.REPLACE_EXISTING);

          }
          zipIn.closeEntry();
          entry = zipIn.getNextEntry();
        }
      }
    }

    // compute the TCK JSON
    JsonObject tck = new JsonObject();

    JsonArray suites = new JsonArray();
    tck.put("suites", suites);

    listFiles(Paths.get("src", "test", "resources", "json-schema-org", "tck", "tests"))
      .forEach(p -> {
        String draft = p.getName(6).toString().substring(5);
        String name = p.subpath(6, p.getNameCount()).toString();
        name = name
          .replace(".json", "");

        if (ALLOWED_DRAFTS.contains(draft)) {
          try {
            suites.add(new JsonObject()
              .put("draft", draft)
              .put("name", name)
              .put("value", new JsonArray(Buffer.buffer(Files.readAllBytes(p))))
            );
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });

    JsonArray remotes = new JsonArray();
    tck.put("remotes", remotes);

    listFiles(Paths.get("src", "test", "resources", "json-schema-org", "tck", "remotes"))
      .forEach(p -> {
        String name = p.subpath(6, p.getNameCount()).toString();
        name = "http://localhost:1234/" + name;
        try {
          remotes.add(new JsonObject()
            .put("name", name)
            .put("value", new JsonObject(Buffer.buffer(Files.readAllBytes(p))))
          );
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

    Files.write(Paths.get("src", "test", "resources", "test-suite-tck.json"), tck.encodePrettily().getBytes(StandardCharsets.UTF_8));
  }

  // protect zip slip attack
  private static Path zipSlipProtect(String zipEntryName, Path targetDir)
    throws IOException {

    // test zip slip vulnerability
    Path targetDirResolved = targetDir.resolve(zipEntryName);

    // make sure normalized file still has targetDir as its prefix
    // else throws exception
    Path normalizePath = targetDirResolved.normalize();
    if (!normalizePath.startsWith(targetDir)) {
      throw new IOException("Bad zip entry: " + zipEntryName);
    }

    return normalizePath;
  }

  // list all files from this path
  private static List<Path> listFiles(Path path) throws IOException {
    try (Stream<Path> walk = Files.walk(path)) {
      return walk
        .filter(Files::isRegularFile)
        .sorted(Comparator.naturalOrder())
        .collect(Collectors.toList());
    }
  }
}
