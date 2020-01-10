## Vert.x JSON Schema

[![Build Status](https://travis-ci.org/vert-x3/vertx-json-schema.svg?branch=master)](https://travis-ci.org/vert-x3/vertx-json-schema)

## Architecture

* `SchemaParser`: Parses the schemas. It can be used to parse various schemas. Every JSON Schema dialect/version has one
* `SchemaRouter`: Contains a cache of parsed schemas and resolve cached/local/external `$ref`. You can share it across various `SchemaParser`
* `Validator`: Contains validation logic for single/multiple keyword(s)
* `Schema`: Represents a schema and contains `Validator` instances
* `ValidatorFactory`: Represents a factory for a `Validator`

## How to use
````java
SchemaRouter router = SchemaRouter.create(vertx)
SchemaParserOptions options = new SchemaParserOptions();
SchemaParser parser = Draft7SchemaParser.create(options, router);
Schema schema = parser.parse(schema, scope);
Future validationResult = schema.validateAsync(objectToValidate);
````

Or shorthand

```java
Schema schema = Draft7SchemaParser.parse(vertx, schema, scope);
Future validationResult = schema.validateAsync(objectToValidate);
```

## Extend the validator
To support custom keywords, you can create a new `ValidatorFactory` and register to a `SchemaParser` with `SchemaParserOptions.putAdditionalValidatorFactory()`
