package com.jamfsoftware.jss.healthcheck.json;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public final class JSONArray extends JSONObject {
	
	private final String name;
	private final Collection<JSONObject> objects;
	
	JSONArray(String name) {
		super(name);
		
		this.name = name;
		this.objects = new HashSet<>();
	}
	
	public JSONObject addObject() {
		JSONObject obj = super.addObject(null);
		return objects.add(obj) ? obj : null;
	}
	
	@Override
	public String toString() {
		return "\"" + name + "\":[" +
				String.join(",", objects.stream().map(o -> o.toString(false)).collect(Collectors.toSet())) +
				"]";
	}
	
}
