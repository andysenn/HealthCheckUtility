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
import java.net.URI;
import java.util.Objects;
import java.util.prefs.Preferences;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.ui.UserPrompt;
import com.jamfsoftware.jss.healthcheck.util.StringConstants;

/**
 * This class handles loading the config XML and the values from it.
 * Reads the path from the location stored in the preferences.
 *
 * @author Jacob Schultz
 * @since 1.0
 */
public class ConfigurationController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);
	
	private String configurationPath;
	private Preferences preferences;
	private Element root;
	
	/**
	 * Constructs a new {@link ConfigurationController} that optionally loads the XML configuration file
	 *
	 * @param shouldLoadXML Indicates whether the configuration file should be loaded into memory
	 */
	public ConfigurationController(boolean shouldLoadXML) {
		this.preferences = Preferences.userNodeForPackage(UserPrompt.class);
		this.configurationPath = this.preferences.get("configurationPath", StringConstants.DEFAULT_CONFIGURATION_PATH);
		
		if (shouldLoadXML && isCustomConfigurationPath() && canGetFile()) {
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
	
	/**
	 * This method attempts to find the config XML in the same directory that the tool is
	 * currently executing from. If it can find a config.xml file, it then verifies it is
	 * in the format the tool is expecting. Returns false if can't be found or not properly formatted.
	 *
	 * @return {@code true} if the file was found; otherwise, {@code false}
	 */
	public boolean attemptAutoDiscover() {
		try {
			URI execURI = ConfigurationController.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			File execFile = new File(execURI.getPath());
			
			String path = execFile.getPath() + "/config.xml";
			if (canGetFile(path)) {
				preferences.put("configurationPath", path);
				return true;
			}
			
			path = execFile.getParentFile().getPath() + "/config.xml";
			if (canGetFile(path)) {
				preferences.put("configurationPath", path);
				return true;
			}
			
			LOGGER.info("Unable to auto discover healthcheck config.xml file. Prompting user.");
			return false;
		} catch (Exception e) {
			LOGGER.error("Exception during auto discovery", e);
			return false;
		}
	}
	
	/**
	 * Checks to see if the config.xml is in the default location.
	 *
	 * @return {@code true} if the configuration path is set to the default value; otherwise, {@code false}
	 */
	private boolean isCustomConfigurationPath() {
		return !Objects.equals(configurationPath, StringConstants.DEFAULT_CONFIGURATION_PATH);
	}
	
	/**
	 * This method checks if the file can be read.
	 * Just supplying any old XML file will cause this method to return false.
	 * It checks it can read elements like the JSS_URL, it is important that the XML if formatted correctly.
	 *
	 * @return {@code true} if the configuration file was found and contained a 'jss_url' element; otherwise, {@code
	 * false}
	 *
	 * @see #canGetFile(File)
	 */
	public boolean canGetFile() {
		return canGetFile(new File(configurationPath));
	}
	
	/**
	 * This method checks if the file can be read.
	 * Just supplying any old XML file will cause this method to return false.
	 * It checks it can read elements like the JSS_URL, it is important that the XML if formatted correctly.
	 *
	 * @param path The path to the XML configuration file
	 *
	 * @return {@code true} if the configuration file was found and contained a 'jss_url' element; otherwise, {@code
	 * false}
	 *
	 * @see #canGetFile(File)
	 */
	public boolean canGetFile(String path) {
		return canGetFile(new File(path));
	}
	
	/**
	 * This method checks if the file can be read.
	 * Just supplying any old XML file will cause this method to return false.
	 * It checks it can read elements like the JSS_URL, it is important that the XML if formatted correctly.
	 *
	 * @param file The XML configuration file
	 *
	 * @return {@code true} if the configuration file was found and contained a 'jss_url' element; otherwise, {@code
	 * false}
	 */
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
	 * @param pathString A comma-delimited path of the XML element
	 * @param keysString A comma-delimited list of attribute keys
	 *
	 * @return A String[] of values for all of the found keys
	 */
	public String[] getValue(String pathString, String keysString) {
		String[] path = pathString == null ? new String[0] : pathString.split(",");
		String[] keys = keysString.split(",");
		String[] content = new String[keys.length];
		
		Element object = this.root;
		for (String child : path) {
			object = object.getChild(child);
		}
		
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
	 *
	 * @param item The name of the XML element to set
	 * @param value The value to which the XML element will be set
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
			o.output(this.root, new FileWriter(configurationPath));
		} catch (Exception e) {
			LOGGER.error("Unable to update XML file.", e);
		}
	}
	
}
