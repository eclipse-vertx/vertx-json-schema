package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static io.vertx.ext.json.schema.ValidationException.createException;

public abstract class BaseFormatValidatorFactory implements ValidatorFactory {

  protected final static Predicate<String> URI_VALIDATOR = in -> {
    try {
      return URI.create(in).isAbsolute();
    } catch (IllegalArgumentException e) {
      return false;
    }
  };

  protected final static Predicate<String> URI_REFERENCE_VALIDATOR = in -> {
    try {
      URI.create(in);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  };

  protected final static Predicate<String> REGEX_VALIDATOR = in -> {
    try {
      Pattern.compile(in);
      return true;
    } catch (PatternSyntaxException e) {
      return false;
    }
  };

  protected final static Predicate<String> IDN_EMAIL_VALIDATOR = in -> {
    return true;
  };

  class FormatValidator extends BaseSyncValidator {

    Predicate<String> validator;

    public FormatValidator(Predicate<String> validator) {
      this.validator = validator;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof String) {
        if (!validator.test((String) in)) {
          throw createException("Provided value don't match pattern", "pattern", in);
        }
      }
    }
  }

  protected Map<String, Predicate<String>> formats;
  protected List<String> ignoringFormats;

  public BaseFormatValidatorFactory() {
    this.formats = initFormatsMap();
    this.ignoringFormats = initIgnoringFormats();
  }

  protected List<String> initIgnoringFormats() {
    return Arrays.asList(
        "int32",
        "int64",
        "float",
        "double"
    );
  }

  public abstract Map<String, Predicate<String>> initFormatsMap();

  public void addStringFormatValidator(String formatName, Predicate<String> validator) {
    this.formats.put(formatName, validator);
  }

  protected Predicate<String> createPredicateFromPattern(final Pattern pattern) {
    return (in) -> pattern.matcher(in).matches();
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    String format = schema.getString("format");
    if (ignoringFormats.contains(format)) return null;
    else {
      Predicate<String> v = formats.get(format);
      if (v == null) throw new SchemaException(schema, "Format not supported");
      else return new FormatValidator(v);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("format");
  }
}
