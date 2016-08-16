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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;
import com.jamfsoftware.jss.healthcheck.controller.HTTPController;
import com.jamfsoftware.jss.healthcheck.controller.SystemCommandController;
import com.jamfsoftware.jss.healthcheck.util.JSONBuilder;

/**
 * HealthCheck.java, Written December 2015, Jacob Schultz This class is responsible for making all of the API calls and
 * then building a JSON String to be output or used by the interface. It will also get information from the JSS Health
 * Check page and the JSS Summary Must provide a JSS URL, Username and Password
 */

public class HealthCheck {
	
	//This String array represents all of the API objects that will be hit during the Health Check.
	private static final String[] APIObjects = { "computers", "mobiledevices", "users", "activationcode", "computercheckin", "ldapservers", "gsxconnection", "vppaccounts", "computergroups", "mobiledevicegroups", "usergroups", "managedpreferenceprofiles", "printers", "computerextensionattributes", "mobiledeviceextensionattributes", "computerconfigurations", "scripts", "policies", "summarydata", "smtpserver" };
	private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	//Class level variables used for calculations and storing the JSON generated.
	private int numberOfComputers;
	private int numberOfMobileDevices;
	private int numberOfUsers;
	private JSONBuilder outputJSON;
	private JSSSummary summary;
	private String isCloudJSS = "";
	
	//New Health Check object. Set the number of devices/users for later calculations.
	//Create the JSON, and perform all of the API calls.
	public HealthCheck(String url, String username, String password) {
		//Check if there is a slash on the end of the URL
		if ((url.lastIndexOf("/") + 1) == url.length()) {
			url = url.substring(0, url.length() - 1);
		}
		//Start the API Checks and build the JSON.
		System.out.println("Getting JSS Summary..");
		this.summary = new JSSSummary(getJSSSummary(url, username, password));
		JSSConnectionTest test = new JSSConnectionTest(url, username, password);
		getJSSHealthCheckPage(url, username, password);
		this.isCloudJSS = Boolean.toString(test.isCloudJSS());
		this.numberOfComputers = checkAPILength(url, username, password, "computers");
		this.numberOfMobileDevices = checkAPILength(url, username, password, "mobiledevices");
		this.numberOfUsers = checkAPILength(url, username, password, "users");
		this.outputJSON = new JSONBuilder();
		this.outputJSON.updateTotals(Integer.toString(this.numberOfComputers), Integer.toString(this.numberOfMobileDevices), Integer.toString(this.numberOfUsers), url);
		this.outputJSON.addComma();
		//Check to make sure a MySQL user was provided, if not, don't perform System Checks.
		SystemChecks(this.outputJSON);
		//Add the body of the JSON.
		this.outputJSON.addObject("checkdata");
		//Call the methods that loops through the API Objects
		APIChecks(url, username, password);
		
	}
	
	//Blank Constructor to access object methods.
	public HealthCheck() {
	}
	
	//Use this constructor for OS's without a GUI.
	public HealthCheck(boolean headless, String url, String username, String password) {
		System.out.println("Performing Health Check, Please Wait...");
		System.out.println("Getting the JSS Summary");
		JSSConnectionTest test = new JSSConnectionTest(url, username, password);
		this.isCloudJSS = Boolean.toString(test.isCloudJSS());
		this.summary = new JSSSummary(getJSSSummary(url, username, password));
		this.numberOfComputers = checkAPILength(url, username, password, "computers");
		this.numberOfMobileDevices = checkAPILength(url, username, password, "mobiledevices");
		this.numberOfUsers = checkAPILength(url, username, password, "users");
		this.outputJSON = new JSONBuilder();
		this.outputJSON.updateTotals(Integer.toString(this.numberOfComputers), Integer.toString(this.numberOfMobileDevices), Integer.toString(this.numberOfUsers), url);
		this.outputJSON.addComma();
		System.out.println("Running System and Database Checks");
		SystemChecks(this.outputJSON);
		this.outputJSON.addObject("checkdata");
		APIChecks(url, username, password);
		
	}
	
	//Below two methods used for date calculations
	public static long calculateDays(String startDate, String endDate) {
		Date sDate = new Date(startDate);
		Date eDate = new Date(endDate);
		Calendar cal3 = Calendar.getInstance();
		cal3.setTime(sDate);
		Calendar cal4 = Calendar.getInstance();
		cal4.setTime(eDate);
		return daysBetween(cal3, cal4);
	}
	
	public static long daysBetween(Calendar startDate, Calendar endDate) {
		Calendar date = (Calendar) startDate.clone();
		long daysBetween = 0;
		while (date.before(endDate)) {
			date.add(Calendar.DAY_OF_MONTH, 1);
			daysBetween++;
		}
		return daysBetween;
	}
	
	/**
	 * Loop through all of the API Objects and perform checks
	 *
	 * @param url JSS URL
	 * @param username JSS Username
	 * @param password JSS Password
	 */
	public void APIChecks(String url, String username, String password) {
		//Loops through the API Objects and informs the user what is currently being checked.
		for (String Object : APIObjects) {
			System.out.println("Hitting API Object: " + Object);
			APIObject(Object, url, username, password, this.outputJSON);
		}
		
		//Close the JSON.
		this.outputJSON.closeFinalObject();
		this.outputJSON.closeJSON();
	}
	
	/**
	 * Peform all of the system checks, and add them to the JSON string.
	 *
	 * @param json the JSON string.
	 */
	public void SystemChecks(JSONBuilder json) {
		json.addObject("system");
		//make a new system command controller object to handle the system commands.
		SystemCommandController commands = new SystemCommandController();
		json.addElement("os", this.summary.getOperatingSystem());
		json.addElement("iscloudjss", this.isCloudJSS);
		json.addElement("javaversion", this.summary.getJavaVersion());
		json.addElement("javavendor", this.summary.getJavaVendor());
		json.addElement("webapp_dir", this.summary.getWebAppDir());
		json.addElement("clustering", this.summary.getIsClustered());
		json.addElement("mysql_version", this.summary.getMySQLVersion().trim());
		
		ArrayList<String[]> large_tables = this.summary.getLargeMySQLTables();
		json.addArrayObject("largeSQLtables");
		for (int i = 0; i < large_tables.size(); i++) {
			String[] table = large_tables.get(i);
			json.openArrayObject();
			json.addElement("table_name", table[0]);
			json.addFinalElement("table_size", table[1] + " MB");
			json.closeObject();
		}
		if (large_tables.size() > 0) {
			json.removeComma();
		}
		json.closeArrayObject();
		json.addElement("database_size", Double.toString(this.summary.getDatabaseSize())
		);
		json.addElement("proc_cores", Integer.toString(commands.getProcCores()));
		json.addElement("free_memory", Long.toString(commands.getFreeMem()));
		json.addElement("max_memory", Long.toString(commands.getMaxMemory()));
		json.addElement("memory_currently_in_use", Long.toString(commands.getMemoryInUse()));
		json.addElement("total_space", Long.toString(commands.getSpaceDetails()[0]));
		json.addElement("free_space", Long.toString(commands.getSpaceDetails()[1]));
		json.addElement("usable_space", Long.toString(commands.getSpaceDetails()[2]));
		//Close Object
		json.removeComma();
		json.closeObject();
	}
	
	/**
	 * This method POSTs to the JSS Summary page, with all options enabled.
	 *
	 * @return The entire JSS summary result from the post.
	 */
	public String getJSSSummary(String url, String username, String password) {
		HTTPController api = new HTTPController(username, password);
		try {
			return api.doGet(url + "/summary.html?2=on&3=off&4=on&6=on&5=on&9=on&7=on&313=on&24=on&350=on&22=on&26=on&23=on&24=on&25=on&28=on&27=on&312=on&53=on&54=on&54=on&255=on&24=on&51=on&65=on&80=on&136=on&135=on&133=on&134=on&137=on&221=on&166=on&72=on&141=on&124=on&125=on&158=on&252=on&163=on&310=on&381=on&90=on&91=on&92=on&96=on&95=on&94=on&93=on&74=on&75=on&76=on&82=on&81=on&122=on&118=on&119=on&73=on&117=on&123=on&83=on&11=on&77=on&171=on&128=on&86=on&131=on&314=on&169=on&87=on&41=on&42=on&43=on&360=on&44=on&45=on&tableRowCounts=on&tableSize=on&Action=Create&username=" + username + "&password=" + password);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * This method POSTs to the JSS Health Check page and checks for any errors.
	 * If there are errors, print them to the console.
	 */
	public void getJSSHealthCheckPage(String url, String username, String password) {
		HTTPController api = new HTTPController(username, password);
		try {
			System.out.println("Getting JSS healthCheck.html data...");
			String result = api.doGet(url + "/healthCheck.html?username=" + username + "&password=" + password);
			if (result.equals("[]")) {
				System.out.println("No JSS healthCheck.html page errors detected.");
			} else if (result.contains("DBConnectionError")) {
				System.out.println("An error occurred while testing the database connection. (JSS Error)");
			} else if (result.contains("SetupAssistant")) {
				System.out.println("The JSS Setup Assistant was detected. (JSS Warning)");
			} else if (result.contains("DBConnectionConfigError")) {
				System.out.println("A configuration error occurred while attempting to connect to the database. (JSS Error)");
			} else if (result.contains("Initializing")) {
				System.out.println("The JSS web application is initializing. (JSS Warning)");
			} else if (result.contains("ChildNodeStartUpError")) {
				System.out.println("An instance of the JSS web application in a clustered environment failed to start. (JSS Error)");
			} else if (result.contains("InitializationError")) {
				System.out.println("A fatal error occurred and prevented the JSS web application from starting (JSS Error)");
			}
			
		} catch (Exception e) {
			System.out.println("Unable to get JSS healthCheck.html data.");
		}
		
	}
	
	/**
	 * Hit a given API Object, parse the XML returned, then add the results
	 * to the JSON String. Also handles parsing of the JSS Summary.
	 *
	 * @param Object An API Object as a String
	 * @param url JSS URL
	 * @param username JSS Username
	 * @param password JSS Password
	 * @param jsonString The running JSON String
	 */
	public void APIObject(String Object, String url, String username, String password, JSONBuilder jsonString) {
		HTTPController api = new HTTPController(username, password);
		//Create XML Object Parser
		SAXBuilder sb = new SAXBuilder();
		//Build the JSON
		try {
			//Send an API GET and store the returned String.
			String result = "";
			//Create a new XML element.
			Document doc = null;
			//If it's not the Summary Object, build the XML doc from the result of the API call.
			if (!Object.equals("summarydata")) {
				try {
					result = replaceSpecChars(api.doGet(url + "/JSSResource/" + Object));
					doc = sb.build(new ByteArrayInputStream(result.getBytes("UTF-8")));
				} catch (Exception e) {
					System.out.println("Unable to parse XML document for object: " + Object);
				}
				//Add the JSS Summary items to the JSON
			} else {
				System.out.println("Parsing JSS Summary..");
				//Do all of the summary checks
				jsonString.addObject("password_strength");
				String[] password_info = this.summary.getPasswordInformation();
				jsonString.addElement("uppercase?", password_info[0]);
				jsonString.addElement("lowercase?", password_info[1]);
				jsonString.addElement("number?", password_info[2]);
				jsonString.addFinalElement("spec_chars?", password_info[3]);
				jsonString.closeObject();
				
				String[] change_info = this.summary.getChangeManagementInfo();
				jsonString.addObject("changemanagment");
				jsonString.addElement("isusinglogfile", change_info[0]);
				jsonString.addFinalElement("logpath", change_info[1]);
				jsonString.closeObject();
				
				String[] tomcat_info = this.summary.getTomcatCert();
				jsonString.addObject("tomcat");
				jsonString.addElement("ssl_cert_issuer", tomcat_info[0]);
				jsonString.addFinalElement("cert_expires", tomcat_info[1]);
				jsonString.closeObject();
				
				jsonString.addObject("logflushing");
				jsonString.addFinalElement("log_flush_time", this.summary.getLogFlushingInfo());
				jsonString.closeObject();
				
				String[] push_cert_info = this.summary.getPushCertInfo();
				jsonString.addObject("push_cert_expirations");
				jsonString.addElement("mdm_push_cert", push_cert_info[0]);
				jsonString.addFinalElement("push_proxy", push_cert_info[1]);
				jsonString.closeObject();
				
				jsonString.addObject("loginlogouthooks");
				jsonString.addFinalElement("is_configured", this.summary.loginLogoutHooksEnabled().toString());
				jsonString.closeObject();
				
				try {
					String[] device_table_counts = this.summary.getTableRowCounts().split(",");
					jsonString.addObject("device_row_counts");
					jsonString.addElement("computers", device_table_counts[0]);
					jsonString.addElement("computers_denormalized", device_table_counts[1]);
					jsonString.addElement("mobile_devices", device_table_counts[2]);
					jsonString.addFinalElement("mobile_devices_denormalized", device_table_counts[3]);
					jsonString.closeObject();
				} catch (Exception e) {
					System.out.println("Unable to parse table row counts from the JSS Summary.");
				}
			}
			if (Object.equals("activationcode")) {
				List<Element> activationcode = doc.getRootElement().getChildren();
				jsonString.addObject("activationcode");
				jsonString.addElement("expires", this.summary.getActivationCodeExpiration());
				jsonString.addFinalElement("code", activationcode.get(1).getValue());
				jsonString.closeObject();
			} else if (Object.equals("computercheckin")) {
				List<Element> computercheckin = doc.getRootElement().getChildren();
				jsonString.addObject("computercheckin");
				jsonString.addFinalElement("frequency", computercheckin.get(0).getValue());
				jsonString.closeObject();
				//Get all of the LDAP servers and parse each one. Can take awhile if a lot of LDAP servers.
			} else if (Object.equals("ldapservers")) {
				
				List<Element> ldapservers = doc.getRootElement().getChildren();
				//Get all of the computer group IDS
				ArrayList<String> ldap_servers = parseMultipleObjects(ldapservers);
				jsonString.addArrayObject("ldapservers");
				for (int l = 0; l < ldap_servers.size(); l++) {
					String ldap_info = api.doGet(url + "/JSSResource/ldapservers/id/" + ldap_servers.get(l));
					Document account_as_xml = sb.build(new ByteArrayInputStream(ldap_info.getBytes("UTF-8")));
					List<Element> serv = account_as_xml.getRootElement().getChildren();
					jsonString.openArrayObject();
					jsonString.addElement("id", serv.get(0).getContent().get(0).getValue());
					jsonString.addElement("name", serv.get(0).getContent().get(1).getValue());
					jsonString.addElement("type", serv.get(0).getContent().get(3).getValue());
					jsonString.addFinalElement("address", serv.get(0).getContent().get(2).getValue());
					jsonString.closeObject();
				}
				if (ldap_servers.size() > 0) {
					//Remove a comma from the last element
					jsonString.removeComma();
				}
				jsonString.closeArrayObject();
			} else if (Object.equals("gsxconnection")) {
				List<Element> gsxconnection = doc.getRootElement().getChildren();
				jsonString.addObject("gsxconnection");
				if (gsxconnection.get(0).getValue().equals("true")) {
					jsonString.addElement("status", "enabled");
					jsonString.addFinalElement("uri", gsxconnection.get(5).getValue());
				} else {
					jsonString.addFinalElement("status", "disabled");
				}
				jsonString.closeObject();
			} else if (Object.equals("managedpreferenceprofiles")) {
				List<Element> managedpreferenceprofiles = doc.getRootElement().getChildren();
				jsonString.addObject("managedpreferenceprofiles");
				if (!(managedpreferenceprofiles.get(0).getValue().equals("0"))) {
					jsonString.addFinalElement("status", "enabled");
				} else {
					jsonString.addFinalElement("status", "disabled");
				}
				jsonString.closeObject();
				//Method to parse the group info, since they all follow the same format.
				//Gets all of the groups by ID, then parses each one. Can take awhile if the JSS contains a lot of groups.
			} else if (Object.equals("computergroups") || Object.equals("mobiledevicegroups") || Object.equals("usergroups")) {
				parseGroupObjects(Object, url, username, password, jsonString);
			} else
				//Looping through the VPP Accounts and checking the token expire date. Can take awhile if a lot of VPP accounts.
				if (Object.equals("vppaccounts")) {
					List<Element> vpp_accounts = doc.getRootElement().getChildren();
					//Get all of the vpp_account IDS
					ArrayList<String> vpp_account_ids = parseMultipleObjects(vpp_accounts);
					//Get the current date
					Date date = new Date();
					jsonString.addArrayObject("vppaccounts");
					//Loop through all of the IDS and get individual account information
					for (int a = 0; a < vpp_account_ids.size(); a++) {
						String account_info = api.doGet(url + "/JSSResource/vppaccounts/id/" + vpp_account_ids.get(a));
						Document account_as_xml = sb.build(new ByteArrayInputStream(account_info.getBytes("UTF-8")));
						List<Element> acc = account_as_xml.getRootElement().getChildren();
						//Get the exp date
						String exp_date = acc.get(5).getContent().get(0).getValue();
						jsonString.openArrayObject();
						jsonString.addElement("id", acc.get(0).getContent().get(0).getValue());
						jsonString.addElement("name", acc.get(1).getContent().get(0).getValue());
						jsonString.addFinalElement("days_until_expire", Long.toString(calculateDays(dateFormat.format(date), exp_date)));
						jsonString.closeObject();
					}
					if (vpp_accounts.size() > 1) {
						jsonString.removeComma();
					}
					jsonString.closeArrayObject();
					//Get each script by ID and then parse each one
					//Can take a while if the JSS contains a lot of scripts.
				} else if (Object.equals("scripts")) {
					List<Element> scripts = doc.getRootElement().getChildren();
					ArrayList<String> script_ids = parseMultipleObjects(scripts);
					ArrayList<String> scripts_needing_update = new ArrayList<>();
					//Get all of the scripts
					jsonString.addArrayObject("scripts_needing_update");
					for (int s = 0; s < script_ids.size(); s++) {
						String script_info = api.doGet(url + "/JSSResource/scripts/id/" + script_ids.get(s));
						Document script_as_xml = sb.build(new ByteArrayInputStream(script_info.getBytes("UTF-8")));
						List<Element> script = script_as_xml.getRootElement().getChildren();
						//Get the script name and the actual content of the script
						String script_name = "";
						if (script.size() > 0) {
							script_name = script.get(1).getContent().get(0).getValue();
						}
						String script_code = "";
						//Check to make the script actually has contents
						if (script.size() >= 10) {
							if (script.get(9).getContent().size() > 0) {
								script_code = script.get(9).getContent().get(0).getValue();
							}
						}
						//Check for the old binary location, if it is present, add it to an arraylist.
						if (script_code.toLowerCase().contains("/usr/sbin/jamf") || script_code.toLowerCase().contains("rm -rf") || script_code.toLowerCase().contains("jamf recon")) {
							scripts_needing_update.add(script_name);
						}
					}
					
					//Check if there are any scripts that use the old location
					if (scripts_needing_update.size() > 0) {
						for (int s = 0; s < scripts_needing_update.size(); s++) {
							jsonString.openArrayObject();
							jsonString.addFinalElement("name", scripts_needing_update.get(s));
							jsonString.closeObject();
						}
						jsonString.removeComma();
					}
					
					jsonString.closeArrayObject();
					//Get each printer by ID and then parse each one individually.
					//Can take a while if a lot of printers are present.
				} else if (Object.equals("printers")) {
					List<Element> printers = doc.getRootElement().getChildren();
					ArrayList<String> printer_ids = parseMultipleObjects(printers);
					jsonString.addArrayObject("printer_warnings");
					int xerox_count = 0;
					for (int p = 0; p < printer_ids.size(); p++) {
						String printer_info = api.doGet(url + "/JSSResource/printers/id/" + printer_ids.get(p));
						Document printer_as_xml = sb.build(new ByteArrayInputStream(printer_info.getBytes("UTF-8")));
						List<Element> printer = printer_as_xml.getRootElement().getChildren();
						if (printer.get(6).getContent().size() != 0) {
							String printer_model = printer.get(6).getContent().get(0).getValue();
							//Warn of large Xerox drivers.
							if (printer_model.toLowerCase().contains("xerox")) {
								xerox_count++;
								jsonString.openArrayObject();
								jsonString.addFinalElement("model", printer_model);
								jsonString.closeObject();
							}
						}
					}
					if (xerox_count > 0) {
						jsonString.removeComma();
					}
					jsonString.closeArrayObject();
					//Check the count of several JSS items.
				} else if (Object.equals("computerextensionattributes")) {
					parseObjectCount(Object, url, username, password, jsonString);
				} else if (Object.equals("mobiledeviceextensionattributes")) {
					parseObjectCount(Object, url, username, password, jsonString);
				} else if (Object.equals("computerconfigurations")) {
					parseObjectCount(Object, url, username, password, jsonString);
				} else if (Object.equals("networksegments")) {
					parseObjectCount(Object, url, username, password, jsonString);
					//Get every policy by ID and then parse each one.
					//Can take a while if a lot of policies are present.
				} else if (Object.equals("policies")) {
					List<Element> policies = doc.getRootElement().getChildren();
					ArrayList<String> policy_ids = parseMultipleObjects(policies);
					jsonString.addArrayObject("policies_with_issues");
					int issue_policy_count = 0;
					for (int p = 0; p < policy_ids.size(); p++) {
						String policy_info = api.doGet(url + "/JSSResource/policies/id/" + policy_ids.get(p));
						Document policy_info_as_xml = sb.build(new ByteArrayInputStream(policy_info.getBytes("UTF-8")));
						List<Element> policy = policy_info_as_xml.getRootElement().getChildren();
						//A policy that ongoing and updates inventory AND  is triggered on a checkin
						if ((policy.get(9).getContent().get(0).getValue().equals("true")) && (policy.get(0).getContent().get(11).getValue().equals("Ongoing") && policy.get(0).getContent().get(4).getValue().equals("true"))) {
							jsonString.openArrayObject();
							jsonString.addElement("name", policy.get(0).getContent().get(1).getValue());
							jsonString.addElement("ongoing", Boolean.toString(policy.get(9).getContent().get(0).getValue().equals("true") && policy.get(0).getContent().get(11).getValue().equals("Ongoing")));
							jsonString.addFinalElement("checkin_trigger", Boolean.toString(policy.get(0).getContent().get(4).getValue().equals("true")));
							jsonString.closeObject();
							issue_policy_count++;
						}
					}
					if (issue_policy_count > 0) {
						jsonString.removeComma();
					}
					jsonString.closeArrayObject();
					//List the SMTP Server.
				} else if (Object.equals("smtpserver")) {
					List<Element> smtp_server = doc.getRootElement().getChildren();
					jsonString.addObject("smtpserver");
					if (smtp_server.get(10).getContent().size() > 0) {
						jsonString.addElement("server", smtp_server.get(1).getContent().get(0).getValue());
						jsonString.addFinalElement("sender_email", smtp_server.get(10).getContent().get(0).getValue());
					}
					jsonString.closeFinalObject();
				}
			//Catch all API Call errors and print the error.
		} catch (Exception e) {
			//Should still close JSON objects
			if (Object.equals("activationcode") || Object.equals("computercheckin") || Object.equals("gsxconnection") || Object.equals("managedpreferenceprofiles")) {
				jsonString.closeObject();
			} else if (Object.equals("ldapservers") || Object.equals("vppaccounts") || Object.equals("printers") || Object.equals("scripts") || Object.equals("policies")) {
				jsonString.closeArrayObject();
			} else if (Object.equals("smtpserver")) {
				jsonString.closeFinalObject();
			}
			System.out.println("Error making API call: " + e);
			e.printStackTrace();
		}
		//System.out.println(jsonString.returnJSON());
	}
	
	/**
	 * This method parses the ID out of an XML Object when multiple items
	 * are returned from the API. (Like a list of Computer IDS).
	 *
	 * @param object The XML Doc returned from the JSS
	 *
	 * @return An array list of IDs
	 */
	public ArrayList<String> parseMultipleObjects(List<Element> object) {
		ArrayList<String> list_of_ids = new ArrayList<String>();
		//Starts at one because the first returned item is the size of the xml object
		for (int i = 1; i < object.size(); i++) {
			Element account = object.get(i);
			//Get the ID of the object, which is the first returned in the XML
			String id = account.getContent().get(0).getValue();
			//Add to list of IDS
			list_of_ids.add(id);
		}
		return list_of_ids;
	}
	
	//This method gets all Computer, Mobile or Smart Groups by ID, then tallies the Criteria and Nested counts. Adds problem groups to JSON.
	
	/**
	 * Checks the length of an object in the JSS via the API.
	 *
	 * @param url JSS URL.
	 * @param username JSS Username.
	 * @param password JSS Password.
	 * @param object JSS API Object.
	 *
	 * @return size of the API Object.
	 */
	public int checkAPILength(String url, String username, String password, String object) {
		try {
			HTTPController api = new HTTPController(username, password);
			SAXBuilder sb = new SAXBuilder();
			String result = api.doGet(url + "/JSSResource/" + object);
			Document doc = sb.build(new ByteArrayInputStream(result.getBytes("UTF-8")));
			List<Element> returned = doc.getRootElement().getChildren();
			return returned.size() - 1;
		} catch (Exception e) {
			System.out.println("Error making API Call: " + e);
			e.printStackTrace();
			return 0;
		}
	}
	
	/**
	 * Method that counts the elements in a JSS and writes to the JSON string.
	 */
	public void parseObjectCount(String Object, String url, String username, String password, JSONBuilder jsonString) {
		try {
			HTTPController api = new HTTPController(username, password);
			SAXBuilder sb = new SAXBuilder();
			String result = api.doGet(url + "/JSSResource/" + Object);
			Document doc = sb.build(new ByteArrayInputStream(result.getBytes("UTF-8")));
			List<Element> objects = doc.getRootElement().getChildren();
			int count = parseMultipleObjects(objects).size();
			
			jsonString.addObject(Object);
			jsonString.addFinalElement("count", Integer.toString(count));
			jsonString.closeObject();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.print(e);
		}
		
	}
	
	/**
	 * This method gets all of the Computer, Mobile or User Smart Groups
	 * by ID, then tallies the Criteria and Nested counts.
	 * Adds problem groups to the JSON String.
	 */
	public void parseGroupObjects(String Object, String url, String username, String password, JSONBuilder jsonString) {
		ConfigurationController con = new ConfigurationController(true);
		try {
			HTTPController api = new HTTPController(username, password);
			SAXBuilder sb = new SAXBuilder();
			String result = api.doGet(url + "/JSSResource/" + Object);
			Document doc = sb.build(new ByteArrayInputStream(result.getBytes("UTF-8")));
			
			List<Element> groups = doc.getRootElement().getChildren();
			//Get all of the computer group IDS
			ArrayList<String> group_ids = parseMultipleObjects(groups);
			jsonString.addArrayObject(Object);
			int problems_added = 0;
			for (int c = 0; c < group_ids.size(); c++) {
				String group_info = api.doGet(url + "/JSSResource/" + Object + "/id/" + group_ids.get(c));
				Document account_as_xml = sb.build(new ByteArrayInputStream(group_info.getBytes("UTF-8")));
				List<Element> group = account_as_xml.getRootElement().getChildren();
				
				String name = group.get(1).getContent().get(0).getValue();
				int nested_groups_count = 0;
				int crit_count = 0;
				
				//Criteria has a different XML index value in each object for some reason.
				if (Object.equals("computergroups")) {
					crit_count = Integer.parseInt(group.get(4).getContent().get(0).getValue());
				} else if (Object.equals("mobiledevicegroups")) {
					crit_count = Integer.parseInt(group.get(3).getContent().get(0).getValue());
				} else {
					crit_count = Integer.parseInt(group.get(5).getContent().get(0).getValue());
				}
				//Loop through all of the Crit and check for nested groups.
				for (int cri = 1; cri < group.get(4).getContent().size(); cri++) {
					String value = group.get(4).getContent().get(1).getValue();
					if (value.contains("Computer Group") || value.contains("Mobile Device Group") || value.contains("User Group")) {
						nested_groups_count++;
					}
				}
				//Should only add problem groups
				if (nested_groups_count != 0 || crit_count > Integer.parseInt(con.getValue("configurations,smart_groups", "criteria_count")[0])) {
					jsonString.openArrayObject();
					jsonString.addElement("id", group.get(0).getContent().get(0).getValue());
					jsonString.addElement("name", name);
					jsonString.addElement("nested_groups_count", Integer.toString(nested_groups_count));
					jsonString.addFinalElement("criteria_count", Integer.toString(crit_count));
					jsonString.closeObject();
					problems_added++;
				}
			}
			if (problems_added > 0) {
				//Need to remove the comma off of the final object in the group object
				jsonString.removeComma();
			}
			jsonString.closeArrayObject();
			//Print an error making the the group api call.
		} catch (Exception e) {
			jsonString.closeArrayObject();
			e.printStackTrace();
			System.out.println("Error with group: " + e);
		}
	}
	
	//Return output JSON as a string to be parsed.
	public String getJSONAsString() {
		return this.outputJSON.returnJSON();
	}
	
	String replaceSpecChars(String input) {
		String cleaned = input;
		if (input.contains("&")) {
			cleaned = input.replaceAll("&", "&amp;");
		}
		if (input.contains("#")) {
			cleaned = input.replaceAll("#", "&#035;");
		}
		if (input.contains(":")) {
			cleaned = input.replaceAll("&", "&#058;");
		}
		if (input.contains(";")) {
			cleaned = input.replaceAll("&", "&#059;");
		}
		return cleaned;
	}
	
}