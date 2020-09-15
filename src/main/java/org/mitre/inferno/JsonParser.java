package org.mitre.inferno;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.r4.formats.JsonCreatorCanonical;
import org.hl7.fhir.r4.formats.JsonCreatorDirect;
import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;

public class JsonParser extends org.hl7.fhir.r4.formats.JsonParser {

  /**
   * Parses the given string into an R4 FHIR model class. Supports all complex datatypes
   * (Resources, Types, BackboneElements) but not primitives. The type parameter is required
   * for parsing Types and BackboneElements, but is ignored for parsing Resources.
   *
   * @param input the input to parse
   * @param type the FHIR type to parse the input into (e.g. "HumanName" or "Patient.contact")
   * @return the FHIR model instance representing the parsed input
   * @throws IOException if there was an error parsing the input
   */
  public Base parse(String input, String type) throws IOException {
    JsonElement json = com.google.gson.JsonParser.parseString(input);
    if (!json.isJsonObject()) {
      throw new IllegalArgumentException("Expected JSON object.");
    }
    JsonObject obj = (JsonObject) json;
    if (obj.has("resourceType")) {
      return parse(obj);
    } else if (type.contains(".")) {
      return parseBackboneElement(obj, type);
    } else {
      return parseType(obj, type);
    }
  }

  /**
   * Converts the given Base element into its string representation.
   *
   * @param item the element to serialize
   * @return the string representation of the input element
   * @throws IOException if there was an error serializing the input
   */
  public String composeBase(Base item) throws IOException {
    String type = item.fhirType();
    if (item.isPrimitive()) {
      return composePrimitive(item);
    } else if (item.isResource()) {
      return composeString((Resource) item);
    } else if (item instanceof Type) {
      return composeString((Type) item, type);
    } else if (item instanceof BackboneElement) {
      return composeBackboneElement((BackboneElement) item);
    } else {
      throw new IllegalArgumentException("Unsupported type '" + type + "'.");
    }
  }

  private String composePrimitive(Base primitive) {
    String type = primitive.fhirType();
    String value = primitive.primitiveValue();
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

  private Base parseBackboneElement(JsonObject json, String type) {
    try {
      return (Base) getParseMethod(type).invoke(this, json, null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse BackboneElement", e);
    }
  }

  private String composeBackboneElement(BackboneElement element) throws IOException {
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
      getComposeMethod(element).invoke(this, null, element);
    } catch (Exception e) {
      throw new RuntimeException("Failed to compose BackboneElement", e);
    } finally {
      json.finish();
      osw.flush();
    }
    return bytes.toString();
  }

  private Method getParseMethod(String path) throws ClassNotFoundException, NoSuchMethodException {
    Class<?> elementClass = getModelClassForPath(path);
    Class<?> modelClass = getModelClass(path.split("\\.")[0]);
    String methodName = "parse" + getBackboneElementName(elementClass);
    return getClass().getSuperclass().getDeclaredMethod(methodName, JsonObject.class, modelClass);
  }

  private Method getComposeMethod(BackboneElement element) throws NoSuchMethodException {
    Class<?> elementClass = element.getClass();
    String methodName = "compose" + getBackboneElementName(elementClass);
    return getClass().getSuperclass().getDeclaredMethod(methodName, String.class, elementClass);
  }

  private Class<?> getModelClassForPath(String path) throws ClassNotFoundException {
    try {
      String[] pathParts = path.split("\\.");
      Class<?> clazz = getModelClass(pathParts[0]);
      for (int i = 1; i < pathParts.length; i++) {
        String part = pathParts[i];
        Field field = clazz.getDeclaredField(part);
        clazz = field.getType();
        if (List.class.isAssignableFrom(clazz)) {
          ParameterizedType type = (ParameterizedType) field.getGenericType();
          clazz = (Class<?>) type.getActualTypeArguments()[0];
        }
      }
      return clazz;
    } catch (Exception e) {
      throw new ClassNotFoundException("Could not find class for path '" + path + "'", e);
    }
  }

  private Class<?> getModelClass(String name) throws ClassNotFoundException {
    return Class.forName("org.hl7.fhir.r4.model." + name);
  }

  private String getBackboneElementName(Class<?> clazz) {
    Class<?> enclosing = clazz.getEnclosingClass();
    return enclosing.getSimpleName() + clazz.getSimpleName();
  }
}
