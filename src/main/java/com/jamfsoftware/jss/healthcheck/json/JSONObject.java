package com.jamfsoftware.jss.healthcheck.json;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class JSONObject {
	
	private final String name;
	private final Collection<JSONArray> arrays;
	private final Collection<JSONElement> elements;
	private final Collection<JSONObject> objects;
	
	JSONObject(String name) {
		this.name = name;
		this.arrays = new HashSet<>();
		this.elements = new HashSet<>();
		this.objects = new HashSet<>();
	}
	
	public Object addElement(String name, Object value) {
		return elements.add(new JSONElement(name, value));
	}
	
	public JSONObject addObject(String name) {
		JSONObject obj = new JSONObject(name);
		return objects.add(obj) ? obj : null;
	}
	
	public JSONArray addArray(String name) {
		JSONArray arr = new JSONArray(name);
		return arrays.add(arr) ? arr : null;
	}
	
	@Override
	public String toString() {
		return toString(true);
	}
	
	public String toString(boolean includeName) {
		return (includeName ? "\"" + name + "\":{" : "{") +
				Stream.of(arrays, elements, objects)
						.flatMap(Collection::stream)
						.map(Object::toString)
						.filter(StringUtils::isNotBlank)
						.collect(Collectors.joining(",")) +
				"}";
	}
	
}
