package io.vertx.json.schema;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class MetaUpdater {

  private static final List<String> METASCHEMAS = Arrays.asList(
    "http://json-schema.org/draft-04/schema",
    "http://json-schema.org/draft-07/schema",
    "https://json-schema.org/draft/2019-09/schema",
    "https://json-schema.org/draft/2019-09/meta/core",
    "https://json-schema.org/draft/2019-09/meta/applicator",
    "https://json-schema.org/draft/2019-09/meta/validation",
    "https://json-schema.org/draft/2019-09/meta/meta-data",
    "https://json-schema.org/draft/2019-09/meta/format",
    "https://json-schema.org/draft/2019-09/meta/content",
    "https://json-schema.org/draft/2020-12/schema",
    "https://json-schema.org/draft/2020-12/meta/core",
    "https://json-schema.org/draft/2020-12/meta/applicator",
    "https://json-schema.org/draft/2020-12/meta/validation",
    "https://json-schema.org/draft/2020-12/meta/meta-data",
    "https://json-schema.org/draft/2020-12/meta/format-annotation",
    "https://json-schema.org/draft/2020-12/meta/content",
    "https://json-schema.org/draft/2020-12/meta/unevaluated"
  );

  public static void main(String[] args) throws IOException {

    for (String meta : METASCHEMAS) {
      URL metaUrl = new URL(meta);
      HttpURLConnection connection = (HttpURLConnection) metaUrl.openConnection();
      connection.setRequestMethod("GET");

      try (InputStream in = connection.getInputStream()) {
        Files.createDirectories(Paths.get("src", "main", "resources", "json-schema.org", metaUrl.getPath()));
        Files.copy(in, Paths.get("src", "main", "resources", "json-schema.org", metaUrl.getPath()), StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }
}

