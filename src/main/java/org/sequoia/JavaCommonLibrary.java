package org.sequoia;

import com.fasterxml.jackson.core.type.TypeReference;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface JavaCommonLibrary
{
	Map<String, Object> convertBlobToJson(byte[] blobValue);
	<T> T customObjectMapper(Object sourceObject, TypeReference<T> typeReference, Map<String, String> fieldMapper, List<String> ignoredFields);
	boolean isNotEmpty(Object value);
	Map<String, Object> jsonObjectToMap(JSONObject jsonObject);
	byte[] jsonToBlobParser(Map<String, Object> jsonInput);
	String convertToSnakeCase(String value);
	Map<String, Object> convertMapKeysToSnakeCase(Map<String, Object> inputObject);
	String convertToPascalCase(String value);
	void removeFieldsFromObject(Map<?, ?> object, List<String> listOfFieldsToRemove);
}
