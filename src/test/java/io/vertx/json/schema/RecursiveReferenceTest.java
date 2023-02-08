package io.vertx.json.schema;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RecursiveReferenceTest {

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testRecursiveReferenceFromSpec202012() {

    final SchemaRepository repository = SchemaRepository
      .create(
        new JsonSchemaOptions()
          .setDraft(Draft.DRAFT202012)
          .setOutputFormat(OutputFormat.Basic)
          .setBaseUri("https://example.com"));

    // Consider the following two schemas describing a simple recursive tree
    // structure, where each node in the tree can have a "data" field of any
    // type.  The first schema allows and ignores other instance properties.
    // The second is more strict and only allows the "data" and "children"
    // properties.  An example instance with "data" misspelled as "daat" is
    // also shown.

    // tree schema, extensible
    JsonSchema tree = JsonSchema.of(new JsonObject(
      "{\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
        "  \"$id\": \"https://example.com/tree\",\n" +
        "  \"$dynamicAnchor\": \"node\",\n" +
        "\n" +
        "  \"type\": \"object\",\n" +
        "  \"properties\": {\n" +
        "    \"data\": true,\n" +
        "    \"children\": {\n" +
        "      \"type\": \"array\",\n" +
        "      \"items\": {\n" +
        "        \"$dynamicRef\": \"#node\"\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}\n"
    ));

    // strict-tree schema, guards against misspelled properties
    JsonSchema strictTree = JsonSchema.of(new JsonObject(
      "{\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
        "  \"$id\": \"https://example.com/strict-tree\",\n" +
        "  \"$dynamicAnchor\": \"node\",\n" +
        "\n" +
        "  \"$ref\": \"tree\",\n" +
        "  \"unevaluatedProperties\": false\n" +
        "}\n"
    ));

    // instance with misspelled field
    JsonObject instance = new JsonObject(
      "{\n" +
        "  \"children\": [ { \"daat\": 1 } ]\n" +
        "}"
    );

    // When we load these two schemas, we will notice the "$dynamicAnchor"
    // named "node" (note the lack of "#" as this is just the name) present
    // in each, resulting in the following full schema URIs:

    // * "https://example.com/tree#node"
    // * "https://example.com/strict-tree#node"

    repository
      .dereference(tree)
      .dereference(strictTree);

    assertThat(repository.find("https://example.com/tree#node")).isNotNull();
    assertThat(repository.find("https://example.com/strict-tree#node")).isNotNull();


    // In addition, JSON Schema implementations keep track of the fact that
    // these fragments were created with "$dynamicAnchor".
    //
    // If we apply the "strict-tree" schema to the instance, we will follow
    // the "$ref" to the "tree" schema, examine its "children" subschema,
    // and find the "$dynamicRef": to "#node" (note the "#" for URI fragment
    // syntax) in its "items" subschema.  That reference resolves to
    // "https://example.com/tree#node", which is a URI with a fragment
    // created by "$dynamicAnchor".  Therefore we must examine the dynamic
    // scope before following the reference.
    //
    // At this point, the dynamic path is "#/$ref/properties/children/
    // items/$dynamicRef", with a dynamic scope containing (from the
    // outermost scope to the innermost):
    //
    // 1.  "https://example.com/strict-tree#"
    // 2.  "https://example.com/tree#"
    // 3.  "https://example.com/tree#/properties/children"
    // 4.  "https://example.com/tree#/properties/children/items"
    //
    // Since we are looking for a plain name fragment, which can be defined
    // anywhere within a schema resource, the JSON Pointer fragments are
    // irrelevant to this check.  That means that we can remove those
    // fragments and eliminate consecutive duplicates, producing:
    //
    // 1.  "https://example.com/strict-tree"
    // 2.  "https://example.com/tree"
    //
    // In this case, the outermost resource also has a "node" fragment
    // defined by "$dynamicAnchor".  Therefore instead of resolving the
    // "$dynamicRef" to "https://example.com/tree#node", we resolve it to
    // "https://example.com/strict-tree#node".
    //
    // This way, the recursion in the "tree" schema recurses to the root of
    // "strict-tree", instead of only applying "strict-tree" to the instance
    // root, but applying "tree" to instance children.
    //
    // This example shows both "$dynamicAnchor"s in the same place in each
    // schema, specifically the resource root schema.  Since plain-name
    // fragments are independent of the JSON structure, this would work just
    // as well if one or both of the node schema objects were moved under
    // "$defs".  It is the matching "$dynamicAnchor" values which tell us
    // how to resolve the dynamic reference, not any sort of correlation in
    // JSON structure.

    OutputUnit ou = repository
      .validator("https://example.com/strict-tree")
      .validate(instance);

    assertThat(ou.getValid()).isFalse();
  }
}
