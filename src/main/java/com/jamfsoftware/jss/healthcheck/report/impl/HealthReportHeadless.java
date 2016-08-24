package com.jamfsoftware.jss.healthcheck.report.impl;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jamfsoftware.jss.healthcheck.report.HealthReport;

/**
 * HealthReportHeadless.java, Written January 2016, Jacob Schultz
 * This takes in a JSON String, and generates a Text Health Report
 * The health report can then be written to a text file.
 * Issue items are denoted by a "!!!".
 */

public class HealthReportHeadless extends HealthReport {
	
	//Class level JSON Objects for the three main sections.
	private JsonObject healthcheck;
	private JsonObject system;
	private JsonObject data;
	
	//Default constructor that loads the JSON objects from the string.
	public HealthReportHeadless(String json) {
		super(json);
		
		JsonElement report = null;
		try {
			report = new JsonParser().parse(json);
		} catch (Exception e) {
			System.out.println("Unable to parse JSON. Outputting generated JSON string for debugging:\n");
			System.out.println(json.replace("\n", ""));
			System.exit(0);
		}
		
		this.healthcheck = ((JsonObject) report).get("healthcheck").getAsJsonObject();
		this.system = ((JsonObject) report).get("system").getAsJsonObject();
		this.data = ((JsonObject) report).get("checkdata").getAsJsonObject();
	}
	
	//This calls the method to parse the JSON report, and then prints it. It then prompts to write to a file.
	public void printReport() {
		String report = getString();
		System.out.println(report);
		shouldWriteToFile(report);
	}
	
	public String getReportString() {
		return getString();
	}
	
	/**
	 * This method generates strings for group information.
	 */
	private void printGroupInformation(String output, String[][] array) {
		if (array.length > 0) {
			for (String[] anArray : array) {
				output += "\n  Group: " + anArray[0] + " Nested Groups: " + anArray[1] + " Criteria Count: " + anArray[2];
			}
		}
	}
	
	/**
	 * Parses data from the JSON, checks for warnings, and builds an output report
	 *
	 * @return The Health Report text as a string.
	 */
	private String getString() {
		int output_count = 0;
		String output = "";
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date dateobj = new Date();
		//Add some header information.
		output += ("JSS Health Check Report Performed on " + df.format(dateobj));
		output += ("\nJSS URL: " + extractData(healthcheck, "jss_url"));
		output += ("\nTotal Computers: " + extractData(healthcheck, "totalcomputers"));
		output += ("\nTotal Mobile Devices: " + extractData(healthcheck, "totalmobile"));
		output += ("\nTotal Users: " + extractData(healthcheck, "totalusers"));
		
		//Print System Info
		output += ("\nSystem Information: ");
		output += ("\n  Server OS: " + extractData(system, "os"));
		output += ("\n  Java Vendor: " + extractData(system, "javavendor"));
		output += ("\n  Java Version: " + extractData(system, "javaversion"));
		output += ("\n  Is Clustered?: " + extractData(system, "clustering"));
		output += ("\n  Web App Directory: " + extractData(system, "webapp_dir"));
		output += ("\n  SSL Cert Issuer: " + extractData(data, "tomcat", "ssl_cert_issuer"));
		output += ("\n  SSL Cert Expires: " + extractData(data, "tomcat", "cert_expires"));
		output += ("\n  MDM Push Cert Expires: " + extractData(data, "push_cert_expirations", "mdm_push_cert"));
		output += ("\n  Push Proxy Expires: " + extractData(data, "push_cert_expirations", "push_proxy"));
		output += ("\n  Change Management Enabled?: " + extractData(data, "changemanagment", "isusinglogfile"));
		output += ("\n  Log File Path: " + extractData(data, "changemanagment", "logpath"));
		if (extractData(system, "javaversion").contains("1.6")) {
			output += ("\n!!!Java 6 is no longer supported.");
			output_count++;
		}
		//output += ("\n  Server Memory: " + extractData(system,"memory"));
		//output += ("\n  Tomcat Version: " + extractData(system,"tomcatversion"));
		
		String[][] sql_tables = extractArrayData(system, "largeSQLtables", "table_name", "table_size");
		if (sql_tables.length > 0) {
			output += ("\n  Top Ten Largest SQL tables: (Over 1GB should be investigated)");
			for (String[] sql_table : sql_tables) {
				output += ("\n    Table Name:  " + sql_table[0] + " Size: " + sql_table[1]);
			}
			
		}
		
		try {
			if (Integer.parseInt(extractData(data, "device_row_counts", "computers").trim()) != Integer.parseInt(extractData(data, "device_row_counts", "computers_denormalized").trim())) {
				output += ("\n!!!The tool has detected a mismatch in the count of the computers and computers_denormalized tables in your JSS datbase. This can cause performance issues if left unchecked.");
			}
			if (Integer.parseInt(extractData(data, "device_row_counts", "mobile_devices").trim()) != Integer.parseInt(extractData(data, "device_row_counts", "mobile_devices_denormalized").trim())) {
				output += ("\n!!!The tool has detected a mismatch in the count of the mobile_devices and mobile_devices_denormalized tables in your JSS datbase. This can cause performance issues if left unchecked.");
			}
		} catch (Exception e) {
			output += ("\nUnable to parse device row counts.");
		}
		
		//Print the rest of the check data
		output += ("\nGeneral Information: ");
		
		int password_strength = 0;
		if (extractData(data, "password_strength", "uppercase?").contains("true")) {
			password_strength++;
		}
		if (extractData(data, "password_strength", "lowercase?").contains("true")) {
			password_strength++;
		}
		if (extractData(data, "password_strength", "number?").contains("true")) {
			password_strength++;
		}
		if (extractData(data, "password_strength", "spec_chars?").contains("true")) {
			password_strength++;
		}
		String password_strength_desc;
		if (password_strength == 4) {
			password_strength_desc = "Excellent";
		} else if (password_strength == 3 || password_strength == 2) {
			password_strength_desc = "Good";
		} else if (password_strength == 1) {
			password_strength_desc = "Poor";
		} else {
			password_strength_desc = "Needs Updating";
		}
		
		output += ("\n  Login Password Strength: " + password_strength_desc);
		if (password_strength < 3) {
			output += ("\n!!The JSS Login Password has weak requirements, consider updating.");
		}
		
		output += ("\n  Activation Code: " + extractData(data, "activationcode", "code"));
		output += ("\n  Computer Check In: " + extractData(data, "computercheckin", "frequency"));
		output += ("\n  GSX is: " + extractData(data, "gsxconnection", "status"));
		output += ("\n  Managed Preference Profiles " + extractData(data, "managedpreferenceprofiles", "status"));
		if (extractData(data, "managedpreferenceprofiles", "status").contains("enabled")) {
			output += ("\n!!!Preference Profiles are deprecated by Apple. Consider using Configuration Profiles.");
			output_count++;
		}
		output += ("\n  Computer Extension Attributes: " + extractData(data, "computerextensionattributes", "count"));
		output += ("\n  Mobile Extension Attributes: " + extractData(data, "mobiledeviceextensionattributes", "count"));
		output += ("\n  !Attempt to limit the amount of Extension Attributes used, they must calculate at every inventory update.");
		
		output += ("\n  Log In/Out Hooks Enabled: " + extractData(data, "loginlogouthooks", "is_configured"));
		if (extractData(data, "loginlogouthooks", "is_configured").contains("false")) {
			output += ("\n!!Login/Logout hooks are not enabled.");
		}
		
		output += ("\n  Log Flushing Time: " + extractData(data, "logflushing", "log_flush_time"));
		
		output += ("\n  Computer Configurations: " + extractData(data, "computerconfigurations", "count"));
		
		output += ("\nVPP Accounts: ");
		String[][] vpp_accounts = extractArrayData(data, "vppaccounts", "name", "days_until_expire");
		if (vpp_accounts.length > 0) {
			for (String[] vpp_account : vpp_accounts) {
				output += ("\n  Account Name: " + vpp_account[0] + " Expires in: " + vpp_account[1] + " Days.");
			}
		}
		
		//Print general info
		output += ("\nNetwork Services Information: ");
		output += ("\n  SMTP Server: " + extractData(data, "smtpserver", "server") + " Email: " + extractData(data, "smtpserver", "sender_email"));
		String[][] ldap_servers = extractArrayData(data, "ldapservers", "name", "type", "address");
		for (String[] ldap_server : ldap_servers) {
			output += ("\n  LDAP Server: " + ldap_server[0] + " (" + ldap_server[1] + ") Address: " + ldap_server[2]);
		}
		String[][] printers = extractArrayData(data, "printer_warnings", "model");
		if (printers.length > 0) {
			output_count++;
			output += ("\n !!The JSS contains one or more Xerox printers. They often have large driver packages. Ensure the MySQL max packet size is configured to allow this.");
		}
		
		if ((extractData(system, "mysql_version").contains("5.6.16") || extractData(system, "mysql_version").contains("5.6.20")) && (extractData(system, "os").contains("OS X") || extractData(system, "os").contains("Mac") || extractData(system, "os").contains("OSX"))) {
			output += ("\n!!! The tool has detected that the server is running on a version of MySQL and OSX that have known performance issues. See this defect: http://bugs.mysql.com/bug.php?id=71960.");
		}
		
		String[][] computer_groups = extractArrayData(data, "computergroups", "name", "nested_groups_count", "criteria_count");
		String[][] mobile_groups = extractArrayData(data, "mobiledevicegroups", "name", "nested_groups_count", "criteria_count");
		String[][] user_groups = extractArrayData(data, "usergroups", "name", "nested_groups_count", "criteria_count");
		if (computer_groups.length > 0 || mobile_groups.length > 0 || user_groups.length > 0) {
			output += ("\nSmart Group Information: ");
			output_count++;
			output += ("\n!!!Smart groups with more than four criteria or nested smart groups are discouraged. These Smart Groups are listed below.");
		}
		printGroupInformation(output, computer_groups);
		printGroupInformation(output, mobile_groups);
		printGroupInformation(output, user_groups);
		
		String[][] scripts = extractArrayData(data, "scripts_needing_update", "name");
		if (scripts.length > 0) {
			output += "\nScripts Needing Updates: ";
			for (String[] script : scripts) {
				output += "\n  Name: " + script[0];
				output_count++;
			}
			output += "\n!!!These scripts either reference the old binary location or use the 'rm-rf' (discouraged) command. They could also contain the 'jamf recon' command in the script. This can cause database bloat.";
		}
		
		String[][] policies = extractArrayData(data, "policies_with_issues", "name", "ongoing", "checkin_trigger");
		if (policies.length > 0) {
			output += "\nPotential issues with Policies:";
			for (String[] policy : policies) {
				output += "\n  Policy: " + policy[0] + " Ongoing with Inventory Update: " + policy[1] + " Checkin Trigger: " + policy[2];
			}
			output_count++;
			output += "\n!!!The above policies are triggered by a check in, contain an update inventory and are ongoing. This can cause network and database congestion.";
		}
		
		if (output_count > 6) {
			output += "\n\n!!!The health tool has detected a relatively large number of issues (6+) with your environment. Please consider implementing the suggested tweaks.";
		} else if (output_count < 6 && output_count > 3) {
			output += "\n\n!!The health tool has detected just a few issues with your JSS (3-6), but it is in otherwise good health. Please consider implementing the suggested tweaks.";
		} else {
			output += "\n\n!The JSS is in excellent condition.";
		}
		return output;
	}
	
	/**
	 * This method writes a string to a text file. Used to write the health report as:
	 * 'user_specified_path/health_check_results.text'
	 *
	 * @param report string
	 */
	private void shouldWriteToFile(String report) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("\nIf you would like the results written to a file (recommended), type the full desired path (/Users/admin/Desktop), otherwise type 'n'");
		String output_path = scanner.next();
		if (output_path.equals("n")) {
			System.out.println("\nThanks for using the Health Check Tool.");
		} else {
			SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy");
			Date dateobj = new Date();
			try {
				PrintWriter out = new PrintWriter(output_path + "/health_check_results_" + df.format(dateobj) + ".txt");
				out.println(report);
				out.close();
			} catch (Exception e) {
				System.out.println("\nUnable to write to file, trying again.");
				shouldWriteToFile(report);
			}
			System.out.println("\nResults written to file.");
		}
	}
	
	/**
	 * This method parses JSON and extracts data into a 2D String Array
	 * Pass as many strings as should be included in the 2D array.
	 *
	 * @return an String[][] filled with content from the JSON.
	 */
	public String[][] extractArrayData(JsonObject obj, String array_item, String... keys) {
		JsonArray items = obj.get(array_item).getAsJsonArray();
		String[][] listOfObjects = new String[items.size()][keys.length];
		
		for (int i = 0; i < items.size(); i++) {
			JsonObject data = items.get(i).getAsJsonObject();
			for (int j = 0; j < keys.length; j++) {
				listOfObjects[i][j] = String.valueOf(data.get(keys[j]));
			}
		}
		return listOfObjects;
	}
	
}
