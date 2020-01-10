package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class PatternValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      String pattern = (String) schema.getValue("pattern");
      return new PatternValidator(Pattern.compile(pattern));
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for pattern keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null pattern keyword", e);
    } catch (PatternSyntaxException e) {
      throw new SchemaException(schema, "Invalid pattern in pattern keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("pattern");
  }

  public class PatternValidator extends BaseSyncValidator {
    private final Pattern pattern;

    public PatternValidator(Pattern pattern) {
      this.pattern = pattern;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof String) {
        Matcher m = pattern.matcher((String) in);
        if (!(m.matches() || m.lookingAt() || m.find())) {
          throw createException("provided string should respect pattern " + pattern, "pattern", in);
        }
      }
    }
  }

}
