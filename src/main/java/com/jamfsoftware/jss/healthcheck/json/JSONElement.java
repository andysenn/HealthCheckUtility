package com.jamfsoftware.jss.healthcheck.json;

public final class JSONElement {
	
	private final String name;
	private final Object value;
	
	JSONElement(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "\"" + name + "\":\"" + value + "\"";
	}
	
}