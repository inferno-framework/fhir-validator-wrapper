package org.mitre.inferno;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.formats.JsonCreatorCanonical;
import org.hl7.fhir.r4.formats.JsonCreatorDirect;
import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;

public class JsonParser extends org.hl7.fhir.r4.formats.JsonParser {

  public Base parse(String input, String type) throws IOException {
    JsonElement json = com.google.gson.JsonParser.parseString(input);
    if (!json.isJsonObject()) {
      throw new IllegalArgumentException("Expected JSON object.");
    }
    JsonObject obj = (JsonObject) json;
    if (obj.has("resourceType")) {
      return parse(obj);
    } else {
      return parseType(obj, type);
    }
  }

  public String composeString(Base item) {
    String type = item.fhirType();
    if (item.isPrimitive()) {
      String value = item.primitiveValue();
      switch (type) {
        case "boolean":
        case "integer":
        case "decimal":
        case "unsignedInt":
        case "positiveInt":
          // convert to JSON boolean/number
          return value;
        case "string":
        case "uri":
        case "url":
        case "canonical":
        case "base64Binary":
        case "instant":
        case "date":
        case "dateTime":
        case "time":
        case "code":
        case "oid":
        case "id":
        case "markdown":
        case "uuid":
          // convert to JSON string
          return new Gson().toJson(value);
        default:
          throw new IllegalArgumentException("Unexpected primitive type '" + type + "'.");
      }
    }
    try {
      if (item.isResource()) {
        return composeString((Resource) item);
      } else if (item instanceof Type) {
        return composeString((Type) item, type);
      } else if (item instanceof BackboneElement) {
        return composeBackboneElement((BackboneElement) item);
      } else {
        throw new IllegalArgumentException("Type '" + type + "' was not a Resource or Type.");
      }
    } catch (IOException e) {
      throw new FHIRException("Failed to compose item of type '" + type + "'.", e);
    }
  }

  private String composeBackboneElement(BackboneElement element) throws IOException {
    Class<?> clazz = element.getClass();
    Class<?> enclosing = clazz.getEnclosingClass();
    String methodName = "compose"
        + (enclosing != null ? enclosing.getSimpleName() + clazz.getSimpleName() : "Backbone");
    Method method;
    try {
      method = getClass().getSuperclass().getDeclaredMethod(methodName, String.class, clazz);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Could not find method '" + methodName + "'.", e);
    }

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    OutputStreamWriter osw = new OutputStreamWriter(bytes, StandardCharsets.UTF_8);
    if (style == OutputStyle.CANONICAL) {
      json = new JsonCreatorCanonical(osw);
    } else {
      // use this instead of Gson because this preserves decimal formatting
      json = new JsonCreatorDirect(osw);
    }
    json.setIndent(style == OutputStyle.PRETTY ? "  " : "");
    try {
      method.invoke(this, null, element);
    } catch (Exception e) {
      throw new RuntimeException("Error invoking method", e);
    } finally {
      json.finish();
      osw.flush();
    }
    return bytes.toString();
  }
}
