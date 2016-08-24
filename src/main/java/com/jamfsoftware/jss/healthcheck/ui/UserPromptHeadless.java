package com.jamfsoftware.jss.healthcheck.ui;

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

import java.io.Console;
import java.util.Scanner;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.HealthCheck;
import com.jamfsoftware.jss.healthcheck.JSSConnectionTest;
import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;
import com.jamfsoftware.jss.healthcheck.report.impl.HealthReportHeadless;

/*
* UserPromptHeadless.java - Written 1/2016 by Jacob Schultz
* This class handles taking input from the user in the text based or headless interface.
* It prompts for the JSS Username, Password and URL as well as the MySQL information.
* It is designed to run on linux servers, but also is supported on Mac and Windows.
* The tool will run against a cloud JSS, but it is not recommenced, as the system checks will not be accurate.
*/

public class UserPromptHeadless {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserPromptHeadless.class);
	
	//Prefs that store the config.xml.
	private static Preferences prefs = Preferences.userNodeForPackage(UserPrompt.class);
	
	//Default constrictor. Called from HealthCheckUtility.java.
	public UserPromptHeadless() {
		Scanner scanner = new Scanner(System.in);
		Console console = System.console();
		//Create a new config controller to handle loading config.xml values.
		ConfigurationController con = new ConfigurationController(false);
		//Loop to prompt the user for a valid config.xml file. If it is not found, loop back through.
		while (!con.canGetFile() && !con.attemptAutoDiscover()) {
			System.out.println("Path to Config.xml not found. Please type the full path below or type 'exit' to close the program. ");
			String path = scanner.next();
			//If they type exit to grab the XML, exit.
			if (path.equals("exit")) {
				System.exit(0);
			} else {
				prefs.put("config_xml_path", path);
			}
		}
		//Print a welcome message.
		System.out.println("Welcome to the JSS Health Check Tool. \nThis tool will generate a report that can be printed to a text file.\nThe headless version of this tool is not designed to work with a cloud hosted JSS.");
		//Read in all of the user information securely.
		String jssURL = console.readLine("\nEnter JSS URL: ");
		String jssUsername = console.readLine("Enter JSS Username: ");
		String jssPassword = new String(console.readPassword("Enter JSS Password: "));
		
		//Setup a new connection test to make sure we can connect before starting the test.
		final JSSConnectionTest test = new JSSConnectionTest(jssURL, jssUsername, jssPassword);
		while (!test.canConnect()) {
			System.out.println("Unable to connect to the JSS. Prompting again.\nType 'exit' to leave the program.");
			jssURL = console.readLine("\nEnter JSS URL: ");
			if (jssURL.equals("exit")) {
				System.exit(0);
			}
			jssUsername = console.readLine("Enter JSS Username: ");
			jssPassword = new String(console.readPassword("Enter JSS Password: "));
			if (jssUsername.equals("exit") || jssPassword.equals("exit")) {
				System.exit(0);
			}
		}
		
		if (test.isHosted()) {
			System.out.println("The headless version of this tool is not intended to run against a cloud JSS.");
			System.out.println("Please run the tool on Mac OSX or Windows to view a health report.");
		}
		
		//Perform a new Health Check
		HealthCheck newHealthCheck = new HealthCheck(true, jssURL, jssUsername, jssPassword);
		
		//Generate a new headless report
		HealthReportHeadless report = new HealthReportHeadless(newHealthCheck.getJSONAsString());
		try {
			//Print the report
			report.printReport();
		} catch (Exception e) {
			System.out.println("Encountered a fatal error.");
			e.printStackTrace();
		}
	}
}
