package org.sequoia.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sequoia.JavaCommonLibrary;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavaCommonLibraryImpl implements JavaCommonLibrary
{
	/**
	 * @param blobValue byte[]
	 * @return Map<String, Object>
	 * @apiNote <h3>Converts blob to json</h3>
	 */
	@Override
	public Map<String, Object> convertBlobToJson(byte[] blobValue)
	{
		Map<String, Object> mapValue = new HashMap<>();
		try
		{
			JSONObject jsonObject = new JSONObject(new String(blobValue));
			if (!jsonObject.isEmpty())
			{
				mapValue = jsonObjectToMap(jsonObject);
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			return null;
		}
		return mapValue;
	}

	/**
	 * @param sourceObject source object
	 * @param typeReference target class type
	 * @param fieldMapper map the difference between source and target
	 * @param ignoredFields fields to be ignored from object conversion
	 * @param <T> target type
	 * @return return converted object
	 * @apiNote <h3>Custom Object Mapper<h3/> <br/> <p>This method's main purpose to convert any customer object (Employee, Car or Animal) to another custom type.
	 * Following mappings are supported</p>
	 * <ul>
	 *     <li>Custom Object --> Custom Object</li>
	 *     <li>Custom Object --> Map</li>
	 *     <li>Map --> Custom Object</li>
	 *     <li>Object [] --> Custom Object</li>
	 *     <li>DTO --> Entity</li>
	 *     <li>Entity --> DTO</li>
	 * </ul>
	 */
	@Override
	public <T> T customObjectMapper(Object sourceObject, TypeReference<T> typeReference, Map<String, String> fieldMapper, List<String> ignoredFields)
	{
		try
		{
			if(isNotEmpty(sourceObject) && isNotEmpty(typeReference))
			{
				ObjectMapper objectMapper = new ObjectMapper();
				boolean isMappingActive = isNotEmpty(fieldMapper);

				if(typeReference.getType().toString().contains("Map") ||
						typeReference.getType().toString().contains("List"))
				{
					T convertedValue = objectMapper.convertValue(sourceObject, typeReference);
					if(isNotEmpty(ignoredFields))
					{
						for (String field : ignoredFields)
						{
							((Map<?, ?>) convertedValue).remove(field);
						}
					}
					return convertedValue;
				}

				JavaType javaType = objectMapper.getTypeFactory().constructType(typeReference);
				T targetObject = objectMapper.convertValue(Collections.emptyMap(), javaType);
				TypeReference<Map<String, Object>> mapObject = new TypeReference<>(){};
				Map<String, Object> convertedTargetObject = objectMapper.convertValue(targetObject, mapObject);
				if(sourceObject instanceof Object[])
				{
					Object[] sourceObjectArr = (Object[]) sourceObject;
					AtomicInteger sourceObjectArrIndex = new AtomicInteger();
					for (Object oKey : convertedTargetObject.keySet())
					{
						if (!ignoredFields.contains(oKey.toString()))
						{
							convertedTargetObject.put(oKey.toString(), sourceObjectArr[sourceObjectArrIndex.get()]);
							sourceObjectArrIndex.getAndIncrement();
						}
					}
					return getMappedObject(typeReference, objectMapper, convertedTargetObject);
				}
				else
				{
					return convertIfSourceAndTargetAreCustomTypes(sourceObject, typeReference, fieldMapper,
							ignoredFields, isMappingActive, objectMapper);
				}
			}
			else
			{
				throw new ClassNotFoundException("Invalid" + (!isNotEmpty(sourceObject) ? "source" : "target") + " object.");
			}
		}
		catch(Exception e)
		{
			System.out.println("Failed to cast " + sourceObject.getClass().getName() + " to " + typeReference.getType().getTypeName());
		}
		return null;
	}

	private <T> T convertIfSourceAndTargetAreCustomTypes(
			Object sourceObject, TypeReference<T> typeReference, Map<String, String> fieldMapper, List<String> ignoredFields,
			boolean isMappingActive, ObjectMapper objectMapper
	)
	{
		TypeReference<Map<String, Object>> mapObject = new TypeReference<>(){};
		Map<String, Object> convertedSourceObject = objectMapper.convertValue(sourceObject, mapObject);
		JavaType javaType = objectMapper.getTypeFactory().constructType(typeReference);
		T targetObject = objectMapper.convertValue(Collections.emptyMap(), javaType);
		Map<String, Object> convertedTargetObject = objectMapper.convertValue(targetObject, mapObject);

		convertedSourceObject.forEach((key, value) -> {
			String targetKey = isMappingActive
					? (isNotEmpty(fieldMapper.get(key))
					? fieldMapper.get(key)
					: key)
					: key;
			if(convertedTargetObject.containsKey(targetKey))
			{
				if (!ignoredFields.contains(targetKey))
				{
					convertedTargetObject.put(targetKey, convertedSourceObject.get(key));
				}
			}
		});
		return getMappedObject(typeReference, objectMapper, convertedTargetObject);
	}

	private <T> T getMappedObject(TypeReference<T> typeReference, ObjectMapper objectMapper, Map<String, Object> convertedTargetObject) {
		T mappedObject = objectMapper.convertValue(convertedTargetObject, typeReference);
		if (mappedObject != null) {
			return mappedObject;
		} else {
			throw new RuntimeException("Failed to map object.");
		}
	}

	/**
	 * <h2>Datatype value checker</h2>
	 * <h3>Method checks whether the input value is null or not</h3>
	 *
	 * @param value value should be any type below-mentioned
	 * @return boolean
	 * @apiNote <h3>Supported Types are:</h3>
	 * <ul>
	 * <li>Object</li>
	 * <li>Integer | int</li>
	 * <li>String</li>
	 * <li>Boolean | boolean</li>
	 * <li>Array</li>
	 * <li>List</li>
	 * <li>Map</li>
	 * <li>JSONObject</li>
	 * <li>JSONArray</li>
	 * </ul>
	 * @return-note return true if value is present else return false
	 */
	@Override
	public boolean isNotEmpty(Object value)
	{
		try
		{
			if (value == null)
			{
				return false;
			}
			else if(value instanceof Integer)
			{
				return Integer.parseInt(String.valueOf(value)) != 0;
			}
			else if(value instanceof String)
			{
				return !"".equalsIgnoreCase(((String) value).trim()) || !((String) value).isBlank() ||
						!((String) value).isEmpty() || !"null".equalsIgnoreCase(String.valueOf(value).trim());
			}
			else if(value instanceof Boolean)
			{
				return (boolean) value;
			}
			else if(value instanceof Object[])
			{
				return ((Object[]) value).length > 0;
			}
			else if(value instanceof List)
			{
				return !((List<?>) value).isEmpty();
			}
			else if(value instanceof Set)
			{
				return !((Set<?>) value).isEmpty();
			}
			else if(value instanceof Map)
			{
				return !((Map<?, ?>) value).isEmpty();
			}
			else if(value instanceof JSONObject)
			{
				return !((JSONObject) value).isEmpty();
			}
			else if(value instanceof JSONArray)
			{
				return !((JSONArray) value).isEmpty();
			}
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}

	@Override
	public Map<String, Object> jsonObjectToMap(JSONObject jsonObject)
	{
		if (isNotEmpty(jsonObject))
		{
			Map<String, Object> map = new HashMap<>();
			Iterator<String> keys = jsonObject.keys();
			while (keys.hasNext())
			{
				String key = keys.next();
				Object object = jsonObject.get(key);
				if (object instanceof JSONArray)
				{
					object = jsonArrayToList((JSONArray) object);
				}
				else if(object instanceof JSONObject)
				{
					object = jsonObjectToMap((JSONObject) object);
				}
				map.put(key, object);
			}
			return map;
		}
		else
		{
			throw new NullPointerException("Invalid input");
		}
	}

	private List<Object> jsonArrayToList(JSONArray jsonArray)
	{
		if (isNotEmpty(jsonArray))
		{
			List<Object> list = new ArrayList<>();
			for (int idx = 0; idx < jsonArray.length(); idx++)
			{
				Object object = jsonArray.get(idx);
				if (object instanceof JSONArray)
				{
					object = jsonArrayToList((JSONArray) object);
				}
				else if(object instanceof JSONObject)
				{
					object = jsonObjectToMap((JSONObject) object);
				}
				list.add(object);
			}
			return list;
		}
		else
		{
			throw new NullPointerException("Invalid input");
		}
	}

	@Override
	public byte[] jsonToBlobParser(Map<String, Object> jsonInput)
	{
		if (isNotEmpty(jsonInput))
		{
			return new JSONObject(jsonInput).toString().getBytes();
		}
		else
		{
			throw new NullPointerException("Invalid input");
		}
	}

	@Override
	public String convertToSnakeCase(String value)
	{
		if (isNotEmpty(value))
		{
			final Pattern STRING_TO_SNAKE_CASE_PATTERN = Pattern.compile("([a-z])([A-Z])");
			return STRING_TO_SNAKE_CASE_PATTERN.matcher(value)
					.replaceAll("$1_$2")
					.toLowerCase()
					.replaceAll("-", "_")
					.replaceAll(" ", "_");
		}
		else
		{
			throw new NullPointerException("Invalid input");
		}
	}

	@Override
	public Map<String, Object> convertMapKeysToSnakeCase(Map<String, Object> inputObject)
	{
		if (isNotEmpty(inputObject))
		{
			Map<String, Object> formattedObject = new HashMap<>();
			try
			{
				if (isNotEmpty(inputObject))
				{
					for (Map.Entry<String, Object> entry : inputObject.entrySet())
					{
						String key = entry.getKey();
						Object value = entry.getValue();
						key = convertToSnakeCase(key);
						if (isNotEmpty(value))
						{
							if (value instanceof Map)
							{
								value = convertMapKeysToSnakeCase((Map<String, Object>) value);
							} else if (value instanceof List)
							{
								List list = (List) value;
								for (int i = 0; i < list.size(); i++)
								{
									Object innerObject = list.get(i);
									if (innerObject instanceof Map)
									{
										value = convertMapKeysToSnakeCase(inputObject);
									}
								}

							}
						}
						formattedObject.put(key, value);
					}
				}
				return formattedObject;
			}
			catch(Exception exception)
			{
				System.out.println(exception.getMessage());
				return formattedObject;
			}
		}
		else
		{
			throw new NullPointerException("Invalid input");
		}
	}

	@Override
	public String convertToPascalCase(String value)
	{
		if (isNotEmpty(value))
		{
			final String WORD_SEPARATOR = " ";
			return Arrays.stream(value.split(WORD_SEPARATOR))
					.map(word -> isNotEmpty(word)
							? Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase()
							: value)
					.collect(Collectors.joining(WORD_SEPARATOR));
		}
		else
		{
			throw new NullPointerException("Invalid input");
		}
	}

	@Override
	public void removeFieldsFromObject(Map<?, ?> object, List<String> listOfFieldsToRemove)
	{
		if (isNotEmpty(object) && isNotEmpty(listOfFieldsToRemove))
		{
			for (int i = 0, listOfFieldsToRemoveSize = listOfFieldsToRemove.size(); i < listOfFieldsToRemoveSize; i++)
			{
				String s = listOfFieldsToRemove.get(i);
				object.remove(s);
			}
		}
		else
		{
			throw new NullPointerException("Invalid input");
		}
	}
}
