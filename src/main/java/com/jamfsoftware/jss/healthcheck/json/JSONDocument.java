package com.jamfsoftware.jss.healthcheck.json;

public class JSONDocument extends JSONObject {
	
	public JSONDocument(String root) {
		super(root);
	}
	
	@Override
	public String toString() {
		return "{" + super.toString() + "}";
	}
}
