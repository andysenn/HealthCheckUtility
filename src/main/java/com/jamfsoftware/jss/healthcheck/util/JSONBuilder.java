package com.jamfsoftware.jss.healthcheck.util;

/*-
 * #%L
 * HealthCheckUtility
 * %%
 * Copyright (C) 2015 - 2016 JAMF Software, LLC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

/**
 * JSONBuilder.java, Written December 2015, Jacob Schultz
 * This is a simple class that generates a JSON String.
 * This class was used instead of a 3rd Party Library for simplicity.
 */
public class JSONBuilder {
	
	//Start with a blank JSON String
	private String json = "";
	
	//Add the health check header
	public JSONBuilder() {
		this.json += "{ \"healthcheck\" : {\n";
	}
	
	//Update the URL, total mobile, computer and users.
	public void updateTotals(String totalcopmuters, String totalmobiledevices, String totalusers, String url) {
		this.json += "\"jss_url\" : \"" + url + "\",\n";
		this.json += "\"totalcomputers\" : \"" + totalcopmuters + "\",\n";
		this.json += "\"totalmobile\" : \"" + totalmobiledevices + "\",\n";
		this.json += "\"totalusers\" : \"" + totalusers + "\" \n}";
	}

    /*
	   *The below methods handle various elements of building a JSON string
       *Any methods called will append to the private JSON string class variable.
     */
	
	public void addObject(String key) {
		this.json += "\"" + key + "\" : {\n";
	}
	
	public void addArrayObject(String key) {
		this.json += "\"" + key + "\" : [\n";
	}
	
	public void openArrayObject() {
		this.json += "{";
	}
	
	public void closeArrayObject() {
		this.json += "\n],";
	}
	
	public void closeFinalArrayObject() {
		this.json += "\n]";
	}
	
	public void closeObject() {
		this.json += "\n},";
	}
	
	public void closeFinalObject() {
		this.json += "\n}";
	}
	
	public void addComma() {
		this.json += ",";
	}
	
	public void addElement(String key, String value) {
		this.json += "\n\"" + key + "\" : \"" + value + "\",";
	}
	
	public void addFinalElement(String key, String value) {
		this.json += "\n\"" + key + "\" : \"" + value + "\"";
	}
	
	public void removeComma() {
		int location = this.json.lastIndexOf(",");
		this.json = this.json.substring(0, location);
	}
	
	public void closeJSON() {
		this.json += "\n}";
	}
	
	/**
	 * Returns the built JSON as a string
	 *
	 * @return Generated JSON as a string.
	 */
	public String returnJSON() {
		return this.json.trim();
	}
	
}
