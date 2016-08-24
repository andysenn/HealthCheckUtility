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

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;
import com.jamfsoftware.jss.healthcheck.ui.UserPrompt;
import com.jamfsoftware.jss.healthcheck.ui.UserPromptHeadless;
import com.jamfsoftware.jss.healthcheck.ui.component.MonitorGraph;
import com.jamfsoftware.jss.healthcheck.util.EnvironmentUtil;
import com.jamfsoftware.jss.healthcheck.util.StringConstants;

/**
 * HealthCheckUtility.java - Written by Jacob Schultz 12/2015
 * This class handles the initial load of the program.
 * It detects the OS, and then opens the text interface for linux and a GUI for Windows and Mac.
 * Can be run with a -h flag to open the text interface on Mac and Windows
 * [STUB] If ran with the -m flag it will start a POC for the health monitor.
 */
public class HealthCheckUtility {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckUtility.class);
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-mm-dd HH:mm");
	private static final String LOG_FORMAT = "%s %d %d %d %d\n";
	private final String[] args;
	private final Preferences prefs = Preferences.userNodeForPackage(UserPrompt.class);
	
	public HealthCheckUtility(String... args) {
		this.args = args;
	}
	
	public static void main(String[] args) throws Exception {
		new HealthCheckUtility(args).start();
	}
	
	private void start() {
		if (isFlag("-m")) { // Monitor
			startMonitor();
		}
		
		if (isFlag("-g")) { // Graph
			new MonitorGraph("");
		}
		
		startUI(isFlag("-h")
				|| GraphicsEnvironment.isHeadless()
				|| EnvironmentUtil.isLinux());
	}
	
	private void startMonitor() {
		Scanner scanner = new Scanner(System.in);
		ConfigurationController con = new ConfigurationController(false);
		
		while (!con.canGetFile()) {
			if (!con.attemptAutoDiscover()) {
				System.out.println("Path to Config.xml not found. Please type the full path below or type 'exit' to close the program. ");
				String path = scanner.next();
				if (path.equals("exit")) {
					System.exit(0);
				} else {
					prefs.put("config_xml_path", path);
				}
			}
		}
		
		String xmlPath = prefs.get("config_xml_path", StringConstants.DEFAULT_CONFIGURATION_PATH);
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(xmlPath);
		try {
			Document document = builder.build(xmlFile);
			Element root = document.getRootElement();
			
			
			HealthCheck hc = new HealthCheck(
					root.getChild("jss_url").getValue(),
					root.getChild("jss_username").getValue(),
					root.getChild("jss_password").getValue(),
					true
			);
			
			int mobile_device_length = hc.getAPIObjectCount("mobiledevicecommands");
			int computer_length = hc.getAPIObjectCount("computercommands");
			writeLogEntry(mobile_device_length, computer_length);
		} catch (Exception e) {
			LOGGER.error("Config XML file damaged. Unable to run monitor.", e);
		}
	}
	
	private void startUI(boolean headless) {
		if (headless) {
			new UserPromptHeadless();
		} else {
			ConfigurationController con = new ConfigurationController(false);
			while (!con.canGetFile() && !con.attemptAutoDiscover()) {
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Select Configuration XML File");
				
				FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Files", "xml");
				chooser.setFileFilter(filter);
				
				int returnVal = chooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					prefs.put("config_xml_path", chooser.getSelectedFile().getAbsolutePath());
					prefs.put("configurationPath", chooser.getSelectedFile().getAbsolutePath());
				} else {
					System.exit(0);
				}
			}
			
			try {
				new UserPrompt();
			} catch (Exception e) {
				LOGGER.error("A fatal error has occurred", e);
				JOptionPane.showMessageDialog(new JFrame(), "A fatal error has occurred.\n" + e, "Fatal Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void writeLogEntry(int mobileDeviceLength, int computerLength) {
		try {
			FileWriter writer = new FileWriter("", true);
			writer.write(String.format(LOG_FORMAT,
					DATE_FORMAT.format(new Date()),
					mobileDeviceLength,
					computerLength,
					Runtime.getRuntime().freeMemory(),
					Runtime.getRuntime().totalMemory()));
			writer.close();
		} catch (IOException e) {
			LOGGER.error("Error writing to log", e);
		}
	}
	
	boolean isFlag(String arg) {
		return Stream.of(args).anyMatch(arg::equals);
	}
	
}
