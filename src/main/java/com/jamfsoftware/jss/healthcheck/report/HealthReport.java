package com.jamfsoftware.jss.healthcheck.report;

/*-
 * #%L
 * HealthCheckUtility
 * %%
 * Copyright (C) 2002 - 2016 JAMF Software, LLC
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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jamfsoftware.jss.healthcheck.report.impl.HealthReportHeadless;
import com.jamfsoftware.jss.healthcheck.ui.UserPrompt;
import com.jamfsoftware.jss.healthcheck.ui.generator.PanelGenerator;
import com.jamfsoftware.jss.healthcheck.ui.generator.PanelIconGenerator;
import com.jamfsoftware.jss.healthcheck.util.EnvironmentUtil;
import com.jamfsoftware.jss.healthcheck.util.StringConstants;

public abstract class HealthReport {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HealthReport.class);
	
	protected String jssUrl = "";
	protected boolean showGroupsHelp = false;
	protected boolean showLargeDatabase = false;
	protected boolean showScalability = false;
	protected boolean showPolicies = false;
	protected boolean showExtensionAttributes = false;
	protected boolean showSystemRequirements = false;
	protected boolean showCheckinFreq = false;
	protected boolean showPrinters = false;
	protected boolean showScripts = false;
	protected boolean strongerPassword = false;
	protected boolean loginInOutHooks = false;
	protected boolean showChange = false;
	protected boolean isCloudJSS = false;
	protected boolean mobileDeviceTableCountMismatch = false;
	protected boolean computerDeviceTableCountMismatch = false;
	protected boolean mysqlOSXVersionBug = false;
	
	public HealthReport(String json) {
		uiStuff(json);
	}
	
	public void updatePanelGenVariables(PanelGenerator pGen) {
		pGen.jssUrl = this.jssUrl;
		pGen.showGroupsHelp = this.showGroupsHelp;
		pGen.showLargeDatabase = this.showLargeDatabase;
		pGen.showScalability = this.showScalability;
		pGen.showPolicies = this.showPolicies;
		pGen.showExtensionAttributes = this.showExtensionAttributes;
		pGen.showSystemRequirements = this.showSystemRequirements;
		pGen.showCheckinFreq = this.showCheckinFreq;
		pGen.showPrinters = this.showPrinters;
		pGen.showScripts = this.showScripts;
		pGen.strongerPassword = this.strongerPassword;
		pGen.loginInOutHooks = this.loginInOutHooks;
		pGen.showChange = this.showChange;
		pGen.isCloudJSS = this.isCloudJSS;
		pGen.mobileDeviceTableCountMismatch = this.mobileDeviceTableCountMismatch;
		pGen.computerDeviceTableCountMismatch = this.computerDeviceTableCountMismatch;
		pGen.mysqlOSXVersionBug = this.mysqlOSXVersionBug;
	}
	
	private void uiStuff(String json) {
		LOGGER.debug("[DEBUG] - JSON String (Copy entire below line)");
		LOGGER.debug(json.replace("\n", ""));
		LOGGER.debug("Attempting to parse Health Report JSON");
		
		JsonElement report = new JsonParser().parse(json);
		JsonObject healthcheck = ((JsonObject) report).get("healthcheck").getAsJsonObject();
		Boolean show_system_info = true;
		JsonObject system;
		//Check if the check JSON contains system information and show/hide panels accordingly later.
		system = ((JsonObject) report).get("system").getAsJsonObject();
		
		final JsonObject data = ((JsonObject) report).get("checkdata").getAsJsonObject();
		
		this.jssUrl = extractData(healthcheck, "jss_url");
		
		if (extractData(system, "iscloudjss").contains("true")) {
			show_system_info = false;
			isCloudJSS = true;
		}
		
		PanelIconGenerator iconGen = new PanelIconGenerator();
		PanelGenerator panelGen = new PanelGenerator();
		
		//Top Level Frame
		final JFrame frame = new JFrame("JSS Health Check Report");
		
		//Top Level Content
		JPanel outer = new JPanel(new BorderLayout());
		
		//Two Blank Panels for the Sides
		JPanel blankLeft = new JPanel();
		blankLeft.add(new JLabel("        "));
		JPanel blankRight = new JPanel();
		blankRight.add(new JLabel("        "));
		blankLeft.setMinimumSize(new Dimension(100, 100));
		blankRight.setMinimumSize(new Dimension(100, 100));
		//Header
		JPanel header = new JPanel();
		header.add(new JLabel("Total Computers: " + extractData(healthcheck, "totalcomputers")));
		header.add(new JLabel("Total Mobile Devices: " + extractData(healthcheck, "totalmobile")));
		header.add(new JLabel("Total Users: " + extractData(healthcheck, "totalusers")));
		int total_devices = Integer.parseInt(extractData(healthcheck, "totalcomputers")) + Integer.parseInt(extractData(healthcheck, "totalmobile"));
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date dateobj = new Date();
		header.add(new JLabel("JSS Health Check Report Performed On " + df.format(dateobj)));
		//Foooter
		JPanel footer = new JPanel();
		JButton view_report_json = new JButton("View Report JSON");
		footer.add(view_report_json);
		JButton view_activation_code = new JButton("View Activation Code");
		footer.add(view_activation_code);
		JButton test_again = new JButton("Run Test Again");
		footer.add(test_again);
		JButton view_text_report = new JButton("View Text Report");
		footer.add(view_text_report);
		JButton about_and_terms = new JButton("About and Terms");
		footer.add(about_and_terms);
		//Middle Content, set the background white and give it a border
		JPanel content = new JPanel(new GridLayout(2, 3));
		content.setBackground(Color.WHITE);
		content.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		//Setup Outer Placement
		outer.add(header, BorderLayout.NORTH);
		outer.add(footer, BorderLayout.SOUTH);
		outer.add(blankLeft, BorderLayout.WEST);
		outer.add(blankRight, BorderLayout.EAST);
		outer.add(content, BorderLayout.CENTER);
		
		//Don't show system info if it is hosted.
		JPanel system_info = null;
		JPanel database_health = null;
		if (show_system_info) {
			//Read all of the System information variables from the JSON and perform number conversions.
			String[][] sys_info = { { "Operating System", extractData(system, "os") },
					{ "Java Version", extractData(system, "javaversion") },
					{ "Java Vendor", extractData(system, "javavendor") },
					{ "Processor Cores", extractData(system, "proc_cores") },
					{ "Is Clustered?", extractData(system, "clustering") },
					{ "Web App Directory", extractData(system, "webapp_dir") },
					{ "Free Memory", Double.toString(round((Double.parseDouble(extractData(system, "free_memory")) / 1000000000), 2)) + " GB" },
					{ "Max Memory", Double.toString(round((Double.parseDouble(extractData(system, "max_memory")) / 1000000000), 2)) + " GB" },
					{ "Memory currently in use", Double.toString(round((Double.parseDouble(extractData(system, "memory_currently_in_use")) / 1000000000), 2)) + " GB" },
					{ "Total space", Double.toString(round((Double.parseDouble(extractData(system, "total_space")) / 1000000000), 2)) + " GB" },
					{ "Free Space", Double.toString(round((Double.parseDouble(extractData(system, "usable_space")) / 1000000000), 2)) + " GB" } };
			//Generate the system info panel.
			String systemInfoIcon = iconGen.getSystemInfoIconType(Integer.parseInt(extractData(healthcheck, "totalcomputers")) + Integer.parseInt(extractData(healthcheck, "totalmobile")), extractData(system, "javaversion"), Double.parseDouble(extractData(system, "max_memory")));
			system_info = panelGen.generateContentPanelSystem("System Info", sys_info, "JSS Minimum Requirements", "http://www.jamfsoftware.com/resources/casper-suite-system-requirements/", systemInfoIcon);
			
			//Get all of the DB information.
			String[][] db_health = { { "Database Size", extractData(system, "database_size") + " MB" } };
			if (extractData(system, "database_size").equals("0")) {
				db_health[0][0] = "Unable to connect to database.";
			}
			
			String[][] large_sql_tables = extractArrayData(system, "largeSQLtables", "table_name", "table_size");
			String[][] db_health_for_display = ArrayUtils.addAll(db_health, large_sql_tables);
			//Generate the DB Health panel.
			String databaseIconType = iconGen.getDatabaseInfoIconType(total_devices, Double.parseDouble(extractData(system, "database_size")), extractArrayData(system, "largeSQLtables", "table_name", "table_size").length);
			database_health = panelGen.generateContentPanelSystem("Database Health", db_health_for_display, "Too Large SQL Tables", "https://google.com", databaseIconType);
			if (!databaseIconType.equals("green")) {
				this.showLargeDatabase = true;
			}
		}
		
		int password_strenth = 0;
		if (extractData(data, "password_strength", "uppercase?").contains("true")) {
			password_strenth++;
		}
		if (extractData(data, "password_strength", "lowercase?").contains("true")) {
			password_strenth++;
		}
		if (extractData(data, "password_strength", "number?").contains("true")) {
			password_strenth++;
		}
		if (extractData(data, "password_strength", "spec_chars?").contains("true")) {
			password_strenth++;
		}
		String password_strength_desc;
		if (password_strenth == 4) {
			password_strength_desc = "Excellent";
		} else if (password_strenth == 3 || password_strenth == 2) {
			password_strength_desc = "Good";
		} else if (password_strenth == 1) {
			this.strongerPassword = true;
			password_strength_desc = "Poor";
		} else {
			this.strongerPassword = true;
			password_strength_desc = "Needs Updating";
		}
		
		if (extractData(data, "loginlogouthooks", "is_configured").contains("false")) {
			this.loginInOutHooks = true;
		}
		
		try {
			if (Integer.parseInt(extractData(data, "device_row_counts", "computers").trim()) != Integer.parseInt(extractData(data, "device_row_counts", "computers_denormalized").trim())) {
				this.computerDeviceTableCountMismatch = true;
			}
			
			if (Integer.parseInt(extractData(data, "device_row_counts", "mobile_devices").trim()) != Integer.parseInt(extractData(data, "device_row_counts", "mobile_devices_denormalized").trim())) {
				this.mobileDeviceTableCountMismatch = true;
			}
		} catch (Exception e) {
			System.out.println("Unable to parse device row counts.");
		}
		
		if ((extractData(system, "mysql_version").contains("5.6.16") || extractData(system, "mysql_version").contains("5.6.20")) && (extractData(system, "os").contains("OS X") || extractData(system, "os").contains("Mac") || extractData(system, "os").contains("OSX"))) {
			this.mysqlOSXVersionBug = true;
		}
		
		//Get all of the information for the JSS ENV and generate the panel.
		String[][] env_info = { { "Checkin Frequency", extractData(data, "computercheckin", "frequency") + " Minutes" },
				{ "Log Flushing", extractData(data, "logflushing", "log_flush_time") },
				{ "Log In/Out Hooks", extractData(data, "loginlogouthooks", "is_configured") },
				{ "Computer EA", extractData(data, "computerextensionattributes", "count") },
				{ "Mobile Deivce EA", extractData(data, "mobiledeviceextensionattributes", "count") },
				{ "Password Strength", password_strength_desc },
				{ "SMTP Server", extractData(data, "smtpserver", "server") },
				{ "Sender Email", extractData(data, "smtpserver", "sender_email") },
				{ "GSX Connection", extractData(data, "gsxconnection", "status") } };
		String[][] vpp_accounts = extractArrayData(data, "vppaccounts", "name", "days_until_expire");
		String[][] ldap_servers = extractArrayData(data, "ldapservers", "name", "type", "address", "id");
		String envIconType = iconGen.getJSSEnvIconType(Integer.parseInt(extractData(healthcheck, "totalcomputers")), Integer.parseInt(extractData(data, "computercheckin", "frequency")), Integer.parseInt(extractData(data, "computerextensionattributes", "count")), Integer.parseInt(extractData(data, "mobiledeviceextensionattributes", "count")));
		JPanel env = panelGen.generateContentPanelEnv("JSS Environment", env_info, vpp_accounts, ldap_servers, "", "", envIconType);
		
		//Get all of the group information from the JSON, merge the arrays, and then generate the groups JPanel.
		String[][] groups_1 = ArrayUtils.addAll(extractArrayData(data, "computergroups", "name", "nested_groups_count", "criteria_count", "id"), extractArrayData(data, "mobiledevicegroups", "name", "nested_groups_count", "criteria_count", "id"));
		String[][] groups_2 = ArrayUtils.addAll(groups_1, extractArrayData(data, "usergroups", "name", "nested_groups_count", "criteria_count", "id"));
		String groupIconType = iconGen.getGroupIconType("groups", countJSONObjectSize(data, "computergroups") + countJSONObjectSize(data, "mobiledevicegroups") + countJSONObjectSize(data, "usergroups"));
		JPanel groups = panelGen.generateContentPanelGroups("Groups", groups_2, "", "", groupIconType);
		if (groupIconType.equals("yellow") || groupIconType.equals("red")) {
			this.showGroupsHelp = true;
		}
		
		//Get all of the information for the printers, policies and scripts, then generate the panel.
		String[][] printers = extractArrayData(data, "printer_warnings", "model");
		String[][] policies = extractArrayData(data, "policies_with_issues", "name", "ongoing", "checkin_trigger");
		String[][] scripts = extractArrayData(data, "scripts_needing_update", "name");
		String[][] certs = {
				{ "SSL Cert Issuer", extractData(data, "tomcat", "ssl_cert_issuer") },
				{ "SLL Cert Expires", extractData(data, "tomcat", "cert_expires") },
				{ "MDM Push Cert Expires", extractData(data, "push_cert_expirations", "mdm_push_cert") },
				{ "Push Proxy Expires", extractData(data, "push_cert_expirations", "push_proxy") },
				{ "Change Management Enabled?", extractData(data, "changemanagment", "isusinglogfile") },
				{ "Log File Path:", extractData(data, "changemanagment", "logpath") } };
		String policiesScriptsIconType = iconGen.getPoliciesAndScriptsIconType(extractArrayData(data, "policies_with_issues", "name", "ongoing", "checkin_trigger").length, extractArrayData(data, "scripts_needing_update", "name").length);
		JPanel policies_scripts = panelGen.generateContentPanelPoliciesScripts("Policies, Scripts, Certs and Change", policies, scripts, printers, certs, "", "", policiesScriptsIconType);
		if (extractArrayData(data, "policies_with_issues", "name", "ongoing", "checkin_trigger").length > 0) {
			this.showPolicies = true;
		}
		if (extractArrayData(data, "scripts_needing_update", "name").length > 0) {
			this.showScripts = true;
		}
		if (extractData(data, "changemanagment", "isusinglogfile").contains("false")) {
			this.showChange = true;
		}
		this.showCheckinFreq = iconGen.showCheckinFreq;
		this.showExtensionAttributes = iconGen.showCheckinFreq;
		this.showSystemRequirements = iconGen.showSystemRequirements;
		this.showScalability = iconGen.showScalability;
		//Update Panel Gen Variables
		updatePanelGenVariables(panelGen);
		
		//Generate the Help Section.
		content.add(panelGen.generateContentPanelHelp("Modifications to Consider", "", "", "blank"));
		//If contains system information, add those panels, otherwise just continue adding the rest of the panels.
		if (show_system_info) {
			content.add(system_info);
			content.add(database_health);
		}
		content.add(env);
		content.add(groups);
		content.add(policies_scripts);
		
		//View report action listner.
		//Opens a window with the health report JSON listed
		view_report_json.addActionListener(e -> {
			JPanel middlePanel = new JPanel();
			middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "Health Report JSON"));
			// create the middle panel components
			JTextArea display = new JTextArea(16, 58);
			display.setEditable(false);
			//Make a new GSON object so the text can be Pretty Printed.
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String pp_json = gson.toJson(json.trim());
			display.append(json);
			JScrollPane scroll = new JScrollPane(display);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			//Add Textarea in to middle panel
			middlePanel.add(scroll);
			
			JFrame frame13 = new JFrame();
			frame13.add(middlePanel);
			frame13.pack();
			frame13.setLocationRelativeTo(null);
			frame13.setVisible(true);
		});
		//Action listener for the Terms, About and Licence button.
		//Opens a new window with the AS IS License, 3rd Party Libs used and a small about section
		about_and_terms.addActionListener(e -> {
			JPanel middlePanel = new JPanel();
			middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "About, Licence and Terms"));
			// create the middle panel components
			JTextArea display = new JTextArea(16, 58);
			display.setEditable(false);
			display.append(StringConstants.ABOUT);
			display.append("\n\nThird Party Libraries Used:");
			display.append(" Apache Commons Codec, Google JSON (gson), Java X JSON, JDOM, JSON-Simple, MySQL-connector");
			display.append(StringConstants.LICENSE);
			JScrollPane scroll = new JScrollPane(display);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			//Add Textarea in to middle panel
			middlePanel.add(scroll);
			
			JFrame frame12 = new JFrame();
			frame12.add(middlePanel);
			frame12.pack();
			frame12.setLocationRelativeTo(null);
			frame12.setVisible(true);
		});
		
		//Listener for a button click to open a window containing the activation code.
		view_activation_code.addActionListener(e -> JOptionPane.showMessageDialog(frame, extractData(data, "activationcode", "code") + "\nExpires: " + extractData(data, "activationcode", "expires")));
		
		view_text_report.addActionListener(e -> {
			JPanel middlePanel = new JPanel();
			middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "Text Health Report"));
			// create the middle panel components
			JTextArea display = new JTextArea(16, 58);
			display.setEditable(false);
			//Make a new GSON object so the text can be Pretty Printed.
			display.append(new HealthReportHeadless(json).getReportString());
			JScrollPane scroll = new JScrollPane(display);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			//Add Textarea in to middle panel
			middlePanel.add(scroll);
			
			JFrame frame1 = new JFrame();
			frame1.add(middlePanel);
			frame1.pack();
			frame1.setLocationRelativeTo(null);
			frame1.setVisible(true);
		});
		
		//Listener for the Test Again button. Opens a new UserPrompt object and keeps the Health Report open in the background.
		test_again.addActionListener(e -> {
			try {
				new UserPrompt();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
		
		frame.add(outer);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		if (EnvironmentUtil.isMac() && EnvironmentUtil.isVM()) {
			JOptionPane.showMessageDialog(new JFrame(), "The tool has detected that it is running in a OSX Virtual Machine.\nThe opening of links is not supported on OSX VMs.\nPlease open the tool on a non-VM computer and run it again OR\nyou can also copy the JSON from the report to a non-VM OR view the text report.\nIf you are not running a VM, ignore this message.", "VM Detected", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Method to round doubles at given places.
	 *
	 * @return rounded double
	 */
	protected double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();
		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
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
				listOfObjects[i][j] = data.get(keys[j]).toString();
			}
		}
		return listOfObjects;
	}
	
	/**
	 * Checks how large a JSON Object is
	 *
	 * @return an int size.
	 */
	public int countJSONObjectSize(JsonObject obj, String key) {
		JsonArray items = obj.get(key).getAsJsonArray();
		return items.size();
	}
	
	/**
	 * Two helper methods to extract data from JSON more simply.
	 * If the object is null, print a message instead of crashing and burning.
	 *
	 * @return string derived from the JSON.
	 */
	public String extractData(JsonObject obj, String key) {
		if (obj.get(key) != null) {
			return obj.get(key).getAsString();
		} else {
			return "No Data Available";
		}
	}
	
	public String extractData(JsonObject obj, String key, String key2) {
		if (obj.get(key).getAsJsonObject().get(key2) != null) {
			return obj.get(key).getAsJsonObject().get(key2).getAsString();
		} else {
			return "No Data Available";
		}
	}
	
}
