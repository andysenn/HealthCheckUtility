package com.jamfsoftware.jss.healthcheck.controller;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.TrustModifier;

/**
 * This class handles all of the HTTP Connections and API calls.
 *
 * @author Jacob Schultz
 * @since 1.0
 */
public class HTTPController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPController.class);
	private static final String USER_AGENT = "Mozilla/5.0";
	
	private String username;
	private String password;
	
	/**
	 * Constructs a new {@link ConfigurationController} that optionally loads the XML configuration file
	 *
	 * @param username The username to use when authenticating to the JSS
	 * @param password The password to use when authenticating to the JSS
	 */
	public HTTPController(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public String doGet(String url)
			throws IOException, KeyManagementException, NoSuchAlgorithmException {
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		
		HttpURLConnection con = getConnection(url);
		
		int responseCode = con.getResponseCode();
		LOGGER.debug("Sending 'GET' request to URL : " + url);
		LOGGER.debug("Response Code : " + responseCode);
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			StringBuilder response = new StringBuilder();
			
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			
			return response.toString();
		}
	}
	
	/**
	 * Performs an authenticated GET request to the specified URL and returns the status code.
	 *
	 * @param url The URL to request
	 *
	 * @return The status code of the response
	 *
	 * @throws IOException If an IOException occurs in the underlying connection
	 * @throws KeyManagementException If a KeyManagementException is thrown while relaxing trust
	 * @throws NoSuchAlgorithmException 
	 */
	public int returnGETResponseCode(String url)
			throws IOException, KeyManagementException, NoSuchAlgorithmException {
		return getConnection(url).getResponseCode();
	}
	
	private HttpURLConnection getConnection(String urlString)
			throws IOException, KeyManagementException, NoSuchAlgorithmException {
		URL url = new URL(urlString);
		
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		TrustModifier.relaxHostChecking(connection);
		
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User_Agent", USER_AGENT);
		Base64 b = new Base64();
		String encoding = b.encodeAsString((username + ":" + password).getBytes());
		connection.setRequestProperty("Authorization", "Basic " + encoding);
		
		return connection;
	}
	
}
