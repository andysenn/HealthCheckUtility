package com.jamfsoftware.jss.healthcheck;

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

import java.io.ByteArrayInputStream;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.controller.HTTPController;
import com.jamfsoftware.jss.healthcheck.util.StringConstants;

/*
* JSSConnectionTest.java - Written by Jacob Schultz 1/2016
* This class uses the HTTPController to check the connection to the JSS.
* To verify the connection is uses a simple GET to the /JSSResource/jssuser object.
*/

public class JSSConnectionTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JSSConnectionTest.class);
	
	private HTTPController api;
	private String url;
	
	//Setup the HTTP Connection
	public JSSConnectionTest(String url, String username, String password) {
		this.api = new HTTPController(username, password);
		if (url.endsWith("/")) {
			this.url = url.substring(0, url.length() - 1);
		} else {
			this.url = url;
		}
	}
	
	//Simple method to check if a connection to the JSS can be made.
	//Will return true if the result response code is 200.
	public boolean canConnect() {
		try {
			return api.returnGETResponseCode(url + "/JSSResource/jssuser") == 200;
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		return false;
	}
	
	//This method is a 99.9% way to check for a hosted JSS.
	//Hits the /jssuser object and then checks for "c" in the JSS Version
	//This would indicate a cloud build.
	public boolean isHosted() {
		try {
			String xml_as_string = api.doGet(url + "/JSSResource/jssuser");
			SAXBuilder sb = new SAXBuilder();
			Document doc = sb.build(new ByteArrayInputStream(xml_as_string.getBytes(StringConstants.DEFAULT_ENCODING)));
			String result = doc.getRootElement().getChild("version").getValue();
			
			return result.contains("c");
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		return false;
	}

	public double getJSSVersion() {
		try {
			String xml_as_string = api.doGet(url + "/JSSResource/jssuser");
			SAXBuilder sb = new SAXBuilder();
			Document doc = sb.build(new ByteArrayInputStream(xml_as_string.getBytes("UTF-8")));
			String result = doc.getRootElement().getChild("version").getValue();
			return Double.parseDouble(result);
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		return 0.0;
	}
	
}
