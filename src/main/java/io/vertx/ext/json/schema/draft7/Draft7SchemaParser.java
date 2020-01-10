package io.vertx.ext.json.schema.draft7;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.common.*;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

public class Draft7SchemaParser extends BaseSchemaParser {

  protected Draft7SchemaParser(SchemaRouter router) {
    super(router);
  }

  @Override
  protected List<ValidatorFactory> initValidatorFactories() {
    List<ValidatorFactory> factories = new LinkedList<>();
    factories.add(new DefinitionsValidatorFactory());
    factories.add(new FormatValidatorFactory());
    factories.add(new MaximumValidatorFactory());
    factories.add(new MinimumValidatorFactory());
    factories.add(new ContainsValidatorFactory());
    factories.add(new ConstValidatorFactory());
    factories.add(new TypeValidatorFactory());
    factories.add(new AllOfValidatorFactory());
    factories.add(new AnyOfValidatorFactory());
    factories.add(new EnumValidatorFactory());
    factories.add(new ItemsValidatorFactory());
    factories.add(new MaxItemsValidatorFactory());
    factories.add(new MaxLengthValidatorFactory());
    factories.add(new MaxPropertiesValidatorFactory());
    factories.add(new MinItemsValidatorFactory());
    factories.add(new MinLengthValidatorFactory());
    factories.add(new MinPropertiesValidatorFactory());
    factories.add(new MultipleOfValidatorFactory());
    factories.add(new NotValidatorFactory());
    factories.add(new OneOfValidatorFactory());
    factories.add(new PatternValidatorFactory());
    factories.add(new PropertiesValidatorFactory());
    factories.add(new RequiredValidatorFactory());
    factories.add(new UniqueItemsValidatorFactory());
    factories.add(new DependenciesValidatorFactory());
    factories.add(new ExclusiveMaximumValidatorFactory());
    factories.add(new ExclusiveMinimumValidatorFactory());
    factories.add(new IfThenElseValidatorFactory());
    factories.add(new PropertyNamesValidatorFactory());
    return factories;
  }

  /**
   * Instantiate a Draft7SchemaParser
   *
   * @param router router to associate to read $ref
   * @return a new instance of Draft7SchemaParser
   */
  public static Draft7SchemaParser create(SchemaRouter router) {
    return new Draft7SchemaParser(router);
  }

  /**
   * Parse a draft-7 schema
   *
   * @param vertx this vertx instance
   * @param schema parsed json schema
   * @param scope scope of json schema
   * @return a new instance of Draft7SchemaParser
   * @throws io.vertx.ext.json.schema.SchemaException if schema is invalid
   */
  public static Schema parse(Vertx vertx, JsonObject schema, URI scope) {
    return new Draft7SchemaParser(SchemaRouter.create(vertx, new SchemaRouterOptions())).parse(schema, scope);
  }
}
