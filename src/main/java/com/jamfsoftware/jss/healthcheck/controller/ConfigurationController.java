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

import java.io.File;
import java.io.FileWriter;
import java.util.prefs.Preferences;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.ui.UserPrompt;

/*
* ConfigurationController.java - Written by Jacob Schultz 1/2016
* This class handles loading the config XML and the values from it.
* Reads the path from the location stored in the prefs.
*/
public class ConfigurationController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);
	
	private String configurationPath;
	private Preferences prefs = Preferences.userNodeForPackage(UserPrompt.class);
	private Element root;
	
	/**
	 * Default constructor that just sets the path
	 */
	public ConfigurationController() {
		this.configurationPath = this.prefs.get("configurationPath", "Path to file '/Users/user/desktop/config.xml'");
	}
	
	/**
	 * Constructor that optionally loads the XML file.
	 */
	public ConfigurationController(boolean shouldLoadXML) {
		if (shouldLoadXML) {
			this.configurationPath = this.prefs.get("configurationPath", "Path to file '/Users/user/desktop/config.xml'");
			if (isCustomConfigurationPath() && canGetFile()) {
				try {
					SAXBuilder builder = new SAXBuilder();
					File xmlFile = new File(this.configurationPath);
					Document document = builder.build(xmlFile);
					this.root = document.getRootElement();
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			}
		}
	}
	
	private String getConfigurationPath() {
		return this.prefs.get("configurationPath", "Path to file '/Users/user/desktop/config.xml'");
	}
	
	/**
	 * This method attempts to find the config XML in the same directory that the tool is
	 * currently executing from. If it can find a config.xml file, it then verifies it is
	 * in the format the tool is expecting. Returns false if can't be found or not properly formatted.
	 */
	public boolean attemptAutoDiscover() {
		try {
			String current_path_full = new File(ConfigurationController.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).toString();
			if (current_path_full.lastIndexOf("healthcheck.jar") == -1) {
				System.out.println("Unable to auto discover healthcheck config.xml file. Prompting user.");
				return false;
			}
			
			String base_path = current_path_full.substring(0, current_path_full.lastIndexOf("healthcheck.jar"));
			String final_path = base_path + "config.xml";
			if (canGetFile(final_path)) {
				prefs.put("configurationPath", final_path);
				return true;
			} else {
				System.out.println("Unable to auto discover healthcheck config.xml file. Prompting user.");
				return false;
			}
		} catch (Exception e) {
			LOGGER.error("Exception during auto discovery", e);
			return false;
		}
	}
	
	/**
	 * Checks to see if the config.xml is in the default location.
	 */
	private boolean isCustomConfigurationPath() {
		return !this.configurationPath.equals("Path to file '/Users/user/desktop/config.xml'");
	}
	
	/**
	 * This method checks if the file can be read.
	 * Just supplying any old XML file will cause this method to return false.
	 * It checks it can read elements like the JSS_URL, it is important that the XML if formatted correctly.
	 */
	public boolean canGetFile() {
		return canGetFile(new File(this.prefs.get("configurationPath", "Path to file '/Users/user/desktop/config.xml'")));
	}
	
	/**
	 * This method checks if the file can be read.
	 * Just supplying any old XML file will cause this method to return false.
	 * It checks it can read elements like the JSS_URL, it is important that the XML if formatted correctly.
	 * This method takes in a string path, instead of what is stored in the user pref.
	 */
	public boolean canGetFile(String path) {
		return canGetFile(new File(path));
	}
	
	private boolean canGetFile(File file) {
		if (file.exists()) {
			SAXBuilder builder = new SAXBuilder();
			try {
				Document document = builder.build(file);
				Element root = document.getRootElement();
				root.getChild("jss_url").getValue();
				return true;
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
		
		return false;
	}
	
	/**
	 * This method reads in a CSV path to keys and a CSV string of keys.
	 * It loops down the path given, and then searches for all of the keys.
	 *
	 * @return string array of all of the found keys.
	 */
	public String[] getValue(String path_string, String keys_string) {
		Element object = null;
		//Make two arrays out of the path given and a final content array that will be filled.
		String[] path = path_string.split(",");
		String[] keys = keys_string.split(",");
		String[] content = new String[keys.length];
		//If the path is not the root XML
		if (path.length > 1) {
			//Loop through the path
			for (int i = 0; i < path.length; i++) {
				//Get the first child from the root initialized by the constructor
				if (i == 0) {
					//Set object equal to the root.
					object = this.root.getChild(path[i]);
					//Its not the root, so use the object.
				} else {
					object = object.getChild(path[i]);
				}
			}
			//If the path is only one in length then just get elements from the root
		} else {
			object = this.root;
		}
		
		//Loop through the keys and fill the content array.
		for (int i = 0; i < keys.length; i++) {
			content[i] = object.getChild(keys[i]).getValue();
		}
		
		return content;
	}
	
	/**
	 * This method updates XML values from the Health Check GUI.
	 * Not all items are supported. If it can't find the XML file,
	 * it will print the error message. Could cause errors if the structure
	 * of the XML file has been modified.
	 */
	public void updateXMLValue(String item, String value) {
		if (item.equals("jss_url")) {
			this.root.getChildren().get(0).setText(value);
		} else if (item.equals("jss_username")) {
			this.root.getChildren().get(1).setText(value);
		} else if (item.equals("jss_password")) {
			this.root.getChildren().get(2).setText(value);
		} else if (item.equals("smart_groups")) {
			this.root.getChildren().get(5).getChildren().get(1).setText(value);
		} else if (item.equals("extension_attributes")) {
			this.root.getChildren().get(5).getChildren().get(2).getChildren().get(0).setText(value);
			this.root.getChildren().get(5).getChildren().get(2).getChildren().get(1).setText(value);
		}
		
		try {
			XMLOutputter o = new XMLOutputter();
			o.setFormat(Format.getPrettyFormat());
			o.output(this.root, new FileWriter(getConfigurationPath()));
		} catch (Exception e) {
			LOGGER.error("Unable to update XML file.", e);
		}
		
	}
	
}
