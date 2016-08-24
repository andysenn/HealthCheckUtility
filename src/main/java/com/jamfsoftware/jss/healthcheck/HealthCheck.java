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
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;
import com.jamfsoftware.jss.healthcheck.controller.HTTPController;
import com.jamfsoftware.jss.healthcheck.controller.SystemCommandController;
import com.jamfsoftware.jss.healthcheck.json.JSONArray;
import com.jamfsoftware.jss.healthcheck.json.JSONDocument;
import com.jamfsoftware.jss.healthcheck.json.JSONObject;
import com.jamfsoftware.jss.healthcheck.util.DateUtil;
import com.jamfsoftware.jss.healthcheck.util.StringConstants;

/**
 * HealthCheck.java, Written December 2015, Jacob Schultz This class is responsible for making all of the API calls and
 * then building a JSON String to be output or used by the interface. It will also get information from the JSS Health
 * Check page and the JSS Summary Must provide a JSS URL, Username and Password
 */

public class HealthCheck {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheck.class);
	private static final String[] APIObjects = { "computers", "mobiledevices", "users", "activationcode", "computercheckin", "ldapservers", "gsxconnection", "vppaccounts", "computergroups", "mobiledevicegroups", "usergroups", "managedpreferenceprofiles", "printers", "computerextensionattributes", "mobiledeviceextensionattributes", "computerconfigurations", "scripts", "policies", "summarydata", "smtpserver" };
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
	
	private final String url;
	private final String username;
	private final String password;
	private final boolean headless;
	private final JSONDocument document;
	
	private int numberOfComputers;
	private int numberOfMobileDevices;
	private int numberOfUsers;
	private JSSSummary summary;
	private boolean hosted;
	
	/**
	 * New Health Check object. Set the number of devices/users for later calculations.
	 * Create the JSON, and perform all of the API calls.
	 *
	 * @param url The URL of the JSS
	 * @param username The username to use when authenticating to the JSS
	 * @param password The password to use when authenticating to the JSS
	 * @param headless Indicates whether the health check is running in a headless environment
	 */
	public HealthCheck(String url, String username, String password, boolean headless) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.headless = headless;
		
		// Remove the "/" at the end of the URL if it exists
		if ((url.lastIndexOf("/") + 1) == url.length()) {
			url = url.substring(0, url.length() - 1);
		}
		
		if (headless) {
			System.out.println("Performing Health Check, Please Wait...");
			System.out.println("Getting the JSS Summary");
		} else {
			LOGGER.info("Getting JSS Summary...");
		}
		
		JSSConnectionTest test = new JSSConnectionTest(url, username, password);
		getJSSHealthCheckPage();
		
		this.hosted = test.isHosted();
		this.summary = new JSSSummary(getJSSSummary(test.getJSSVersion()));
		this.numberOfComputers = getAPIObjectCount("computers");
		this.numberOfMobileDevices = getAPIObjectCount("mobiledevices");
		this.numberOfUsers = getAPIObjectCount("users");
		
		document = new JSONDocument("healthcheck");
		document.addElement("jss_url", url);
		document.addElement("totalcomputers", numberOfComputers);
		document.addElement("totalmobile", numberOfMobileDevices);
		document.addElement("totalusers", numberOfUsers);
		
		if (headless)
			System.out.println("Running System and Database Checks");
		
		//Check to make sure a MySQL user was provided. If not, don't perform System Checks.
		performSystemChecks(document);
		
		//Call the methods that loops through the API Objects
		performAPIChecks(document);
	}
	
	/**
	 * Loop through all of the API Objects and perform checks
	 */
	private void performAPIChecks(JSONObject parent) {
		JSONArray array = parent.addArray("checkdata");
		for (String object : APIObjects) {
			LOGGER.info("Checking API Object: " + object);
			getAPIObject(object, array);
		}
	}
	
	/**
	 * Perform all of the system checks, and add them to the JSON string.
	 *
	 * @param json the JSON string.
	 */
	private void performSystemChecks(JSONDocument json) {
		JSONObject system = json.addObject("system");
		
		SystemCommandController commands = new SystemCommandController();
		system.addElement("os", this.summary.getOperatingSystem());
		system.addElement("iscloudjss", this.hosted);
		system.addElement("javaversion", this.summary.getJavaVersion());
		system.addElement("javavendor", this.summary.getJavaVendor());
		system.addElement("webapp_dir", this.summary.getWebAppDir());
		system.addElement("clustering", this.summary.getIsClustered());
		system.addElement("mysql_version", this.summary.getMySQLVersion().trim());
		
		Map<String, Double> large_tables = this.summary.getLargeMySQLTables();
		JSONArray array = system.addArray("largeSQLtables");
		large_tables.forEach((k, v) -> {
			array.addElement("table_name", k);
			array.addElement("table_size", v + " MB");
		});
		
		system.addElement("database_size", this.summary.getDatabaseSize());
		
		// FIXME - These are not accurate. These will report the values of THIS environment, not the JSS'
		system.addElement("proc_cores", commands.getProcCores());
		system.addElement("free_memory", commands.getFreeMem());
		system.addElement("max_memory", commands.getMaxMemory());
		system.addElement("memory_currently_in_use", commands.getMemoryInUse());
		
		long[] spaceDetails = commands.getSpaceDetails();
		system.addElement("total_space", spaceDetails[0]);
		system.addElement("free_space", spaceDetails[1]);
		system.addElement("usable_space", spaceDetails[2]);
	}
	
	/**
	 * This method POSTs to the JSS Summary page, with all options enabled.
	 *
	 * @return The entire JSS summary result from the post.
	 */
	private String getJSSSummary(double version) {
		HTTPController api = new HTTPController(username, password);
		try {
			if (version >= 9.93) {
				return api.doGet(url + "/summary.html?2=on&3=on&4=on&6=on&5=on&9=on&7=on&313=on&24=on&350=on&600=on&22=on&26=on&23=on&24=on&25=on&28=on&27=on&312=on&53=on&54=on&54=on&255=on&24=on&51=on&65=on&80=on&136=on&135=on&133=on&134=on&137=on&221=on&166=on&390=on&72=on&141=on&124=on&125=on&158=on&252=on&163=on&310=on&381=on&500=on&90=on&91=on&92=on&96=on&95=on&94=on&93=on&74=on&75=on&76=on&82=on&81=on&122=on&118=on&119=on&73=on&117=on&123=on&83=on&11=on&77=on&171=on&128=on&86=on&131=on&314=on&169=on&87=on&41=on&42=on&43=on&360=on&44=on&45=on&tableRowCounts=on&tableSize=on&action=Create&safari_autofill_target=&FIELD_JAMF_NATION_USERNAME=&fakeUsername=&fakePassword=&FIELD_JAMF_NATION_PASSWORD=&username=" + username + "&password=" + password);
			} else {
				return api.doGet(url + "/summary.html?2=on&3=off&4=on&6=on&5=on&9=on&7=on&313=on&24=on&350=on&22=on&26=on&23=on&24=on&25=on&28=on&27=on&312=on&53=on&54=on&54=on&255=on&24=on&51=on&65=on&80=on&136=on&135=on&133=on&134=on&137=on&221=on&166=on&72=on&141=on&124=on&125=on&158=on&252=on&163=on&310=on&381=on&90=on&91=on&92=on&96=on&95=on&94=on&93=on&74=on&75=on&76=on&82=on&81=on&122=on&118=on&119=on&73=on&117=on&123=on&83=on&11=on&77=on&171=on&128=on&86=on&131=on&314=on&169=on&87=on&41=on&42=on&43=on&360=on&44=on&45=on&tableRowCounts=on&tableSize=on&Action=Create&username=" + username + "&password=" + password);
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * This method POSTs to the JSS Health Check page and checks for any errors.
	 * If there are errors, print them to the console.
	 */
	private void getJSSHealthCheckPage() {
		HTTPController api = new HTTPController(username, password);
		try {
			LOGGER.info("Getting JSS healthCheck.html data...");
			String result = api.doGet(url + "/healthCheck.html?username=" + username + "&password=" + password);
			
			if (headless) {
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
			}
		} catch (Exception e) {
			LOGGER.error("Unable to get JSS healthCheck.html data.", e);
		}
	}
	
	/**
	 * Hit a given API Object, parse the XML returned, then add the results
	 * to the JSON String. Also handles parsing of the JSS Summary.
	 *
	 * @param objectName An API Object as a String
	 * @param parent The running JSON String
	 */
	private void getAPIObject(String objectName, JSONObject parent) {
		HTTPController api = new HTTPController(username, password);
		
		JSONObject object = parent.addObject(objectName);
		JSONObject details;
		SAXBuilder sb = new SAXBuilder();
		try {
			String result;
			Document doc = null;
			if (!objectName.equals("summarydata")) {
				try {
					result = api.doGet(url + "/JSSResource/" + objectName);
					result = encodeSpecialCharacters(result);
					
					doc = sb.build(new ByteArrayInputStream(result.getBytes(StringConstants.DEFAULT_ENCODING)));
					
				} catch (Exception e) {
					System.out.println("Unable to parse XML document for object: " + objectName);
				}
			} else {
				System.out.println("Parsing JSS Summary..");
				
				String[] password_info = this.summary.getPasswordInformation();
				details = object.addObject("password_strength");
				details.addElement("uppercase?", password_info[0]);
				details.addElement("lowercase?", password_info[1]);
				details.addElement("number?", password_info[2]);
				details.addElement("spec_chars?", password_info[3]);
				
				String[] change_info = this.summary.getChangeManagementInfo();
				details = object.addObject("changemanagment");
				details.addElement("isusinglogfile", change_info[0]);
				details.addElement("logpath", change_info[1]);
				
				String[] tomcat_info = this.summary.getTomcatCert();
				details = object.addObject("tomcat");
				details.addElement("ssl_cert_issuer", tomcat_info[0]);
				details.addElement("cert_expires", tomcat_info[1]);
				
				details = object.addObject("logflushing");
				details.addElement("log_flush_time", this.summary.getLogFlushingInfo());
				
				String[] push_cert_info = this.summary.getPushCertInfo();
				details = object.addObject("push_cert_expirations");
				details.addElement("mdm_push_cert", push_cert_info[0]);
				details.addElement("push_proxy", push_cert_info[1]);
				
				details = object.addObject("loginlogouthooks");
				details.addElement("is_configured", this.summary.loginLogoutHooksEnabled().toString());
				
				try {
					String[] device_table_counts = this.summary.getTableRowCounts().split(",");
					details = object.addObject("device_row_counts");
					details.addElement("computers", device_table_counts[0]);
					details.addElement("computers_denormalized", device_table_counts[1]);
					details.addElement("mobile_devices", device_table_counts[2]);
					details.addElement("mobile_devices_denormalized", device_table_counts[3]);
				} catch (Exception e) {
					System.out.println("Unable to parse table row counts from the JSS Summary.");
				}
			}
			
			if (objectName.equals("activationcode")) {
				List<Element> activationcode = doc.getRootElement().getChildren();
				details = object.addObject("activationcode");
				details.addElement("expires", this.summary.getActivationCodeExpiration());
				details.addElement("code", activationcode.get(1).getValue());
			} else if (objectName.equals("computercheckin")) {
				List<Element> computercheckin = doc.getRootElement().getChildren();
				details = object.addObject("computercheckin");
				details.addElement("frequency", computercheckin.get(0).getValue());
			} else if (objectName.equals("ldapservers")) {
				List<Element> ldapservers = doc.getRootElement().getChildren();
				Collection<String> ldap_servers = parseMultipleObjects(ldapservers);
				JSONArray array = object.addArray("ldapservers");
				for (String ldap_server : ldap_servers) {
					String ldap_info = api.doGet(url + "/JSSResource/ldapservers/id/" + ldap_server);
					Document account_as_xml = sb.build(new ByteArrayInputStream(ldap_info.getBytes(StringConstants.DEFAULT_ENCODING)));
					List<Element> serv = account_as_xml.getRootElement().getChildren();
					
					details = array.addObject();
					details.addElement("id", serv.get(0).getContent().get(0).getValue());
					details.addElement("name", serv.get(0).getContent().get(1).getValue());
					details.addElement("type", serv.get(0).getContent().get(3).getValue());
					details.addElement("address", serv.get(0).getContent().get(2).getValue());
				}
			} else if (objectName.equals("gsxconnection")) {
				List<Element> gsxconnection = doc.getRootElement().getChildren();
				details = object.addObject("gsxconnection");
				if (gsxconnection.get(0).getValue().equals("true")) {
					details.addElement("status", "enabled");
					details.addElement("uri", gsxconnection.get(5).getValue());
				} else {
					details.addElement("status", "disabled");
				}
			} else if (objectName.equals("managedpreferenceprofiles")) {
				List<Element> managedpreferenceprofiles = doc.getRootElement().getChildren();
				details = object.addObject("managedpreferenceprofiles");
				if (!(managedpreferenceprofiles.get(0).getValue().equals("0"))) {
					details.addElement("status", "enabled");
				} else {
					details.addElement("status", "disabled");
				}
			} else if (objectName.equals("computergroups")
					|| objectName.equals("mobiledevicegroups")
					|| objectName.equals("usergroups")) {
				parseGroupObjects(objectName, object);
			} else if (objectName.equals("vppaccounts")) {
				List<Element> vpp_accounts = doc.getRootElement().getChildren();
				Collection<String> vpp_account_ids = parseMultipleObjects(vpp_accounts);
				Date date = new Date();
				
				JSONArray array = object.addArray("vppaccounts");
				for (String vpp_account_id : vpp_account_ids) {
					String account_info = api.doGet(url + "/JSSResource/vppaccounts/id/" + vpp_account_id);
					Document account_as_xml = sb.build(new ByteArrayInputStream(account_info.getBytes(StringConstants.DEFAULT_ENCODING)));
					List<Element> acc = account_as_xml.getRootElement().getChildren();
					
					String expirationDate = acc.get(5).getContent().get(0).getValue();
					details = array.addObject();
					details.addElement("id", acc.get(0).getContent().get(0).getValue());
					details.addElement("name", acc.get(1).getContent().get(0).getValue());
					details.addElement("days_until_expire", DateUtil.calculateDays(DATE_FORMAT.format(date), expirationDate));
				}
			} else if (objectName.equals("scripts")) {
				List<Element> scripts = doc.getRootElement().getChildren();
				Collection<String> scriptIDs = parseMultipleObjects(scripts);
				Collection<String> scriptsToUpdate = new ArrayList<>();
				for (String script_id : scriptIDs) {
					String script_info = api.doGet(url + "/JSSResource/scripts/id/" + script_id);
					Document script_as_xml = sb.build(new ByteArrayInputStream(script_info.getBytes(StringConstants.DEFAULT_ENCODING)));
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
						scriptsToUpdate.add(script_name);
					}
				}
				
				//Check if there are any scripts that use the old location
				if (scriptsToUpdate.size() > 0) {
					JSONArray array = object.addArray("scripts_needing_update");
					for (String script : scriptsToUpdate) {
						array.addElement("name", script);
					}
				}
			} else if (objectName.equals("printers")) {
				List<Element> printers = doc.getRootElement().getChildren();
				Collection<String> printer_ids = parseMultipleObjects(printers);
				JSONArray array = object.addArray("printer_warnings");
				int xerox_count = 0;
				for (String printer_id : printer_ids) {
					String printer_info = api.doGet(url + "/JSSResource/printers/id/" + printer_id);
					Document printer_as_xml = sb.build(new ByteArrayInputStream(printer_info.getBytes(StringConstants.DEFAULT_ENCODING)));
					List<Element> printer = printer_as_xml.getRootElement().getChildren();
					if (printer.get(6).getContent().size() != 0) {
						String printer_model = printer.get(6).getContent().get(0).getValue();
						//Warn of large Xerox drivers.
						if (printer_model.toLowerCase().contains("xerox")) {
							xerox_count++;
							details = array.addObject();
							details.addElement("model", printer_model);
						}
					}
				}
			} else if (objectName.equals("computerextensionattributes")
					|| objectName.equals("mobiledeviceextensionattributes")
					|| objectName.equals("computerconfigurations")
					|| objectName.equals("networksegments")) {
				details = object.addObject(objectName);
				details.addElement("count", Integer.toString(parseObjectCount(objectName)));
			} else if (objectName.equals("policies")) {
				List<Element> policies = doc.getRootElement().getChildren();
				Collection<String> policy_ids = parseMultipleObjects(policies);
				JSONArray array = object.addArray("policies_with_issues");
				int issue_policy_count = 0;
				for (String policy_id : policy_ids) {
					String policy_info = api.doGet(url + "/JSSResource/policies/id/" + policy_id);
					Document policy_info_as_xml = sb.build(new ByteArrayInputStream(policy_info.getBytes(StringConstants.DEFAULT_ENCODING)));
					List<Element> policy = policy_info_as_xml.getRootElement().getChildren();
					
					//A policy that ongoing and updates inventory AND  is triggered on a checkin
					if (policy.get(9).getContent().get(0).getValue().equals("true")
							&& (policy.get(0).getContent().get(11).getValue().equals("Ongoing")
							&& policy.get(0).getContent().get(4).getValue().equals("true"))) {
						details = array.addObject();
						details.addElement("name", policy.get(0).getContent().get(1).getValue());
						details.addElement("ongoing", policy.get(9).getContent().get(0).getValue().equals("true") && policy.get(0).getContent().get(11).getValue().equals("Ongoing"));
						details.addElement("checkin_trigger", policy.get(0).getContent().get(4).getValue().equals("true"));
						issue_policy_count++;
					}
				}
			} else if (objectName.equals("smtpserver")) {
				List<Element> smtp_server = doc.getRootElement().getChildren();
				details = object.addObject("smtpserver");
				if (smtp_server.get(10).getContent().size() > 0) {
					details.addElement("server", smtp_server.get(1).getContent().get(0).getValue());
					details.addElement("sender_email", smtp_server.get(10).getContent().get(0).getValue());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error making API call", e);
		}
	}
	
	/**
	 * This method parses the ID out of an XML Object when multiple items
	 * are returned from the API.
	 *
	 * @param elements The XML elements returned from the JSS
	 *
	 * @return A {@link Collection} of IDs
	 */
	private Collection<String> parseMultipleObjects(Collection<Element> elements) {
		return elements
				.stream()
				.map(e -> e.getContent().get(0).getValue())
				.collect(Collectors.toSet());
	}
	
	//This method gets all Computer, Mobile or Smart Groups by ID, then tallies the Criteria and Nested counts. Adds problem groups to JSON.
	
	/**
	 * Checks the length of an object in the JSS via the API.
	 *
	 * @param objectName The JSS API object name
	 *
	 * @return The size of the API object
	 */
	public int getAPIObjectCount(String objectName) {
		return parseObjectCount(objectName) - 1; // TODO: Why -1???
	}
	
	/**
	 * Method that counts the elements in a JSS and writes to the JSON string.
	 */
	private int parseObjectCount(String objectName) {
		try {
			return requestAPIObjects(objectName).size();
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		return -1;
	}
	
	private Collection<Element> requestAPIObjects(String objectName)
			throws IOException, JDOMException, KeyManagementException, NoSuchAlgorithmException {
		HTTPController api = new HTTPController(username, password);
		SAXBuilder sb = new SAXBuilder();
		String result = api.doGet(url + "/JSSResource/" + objectName);
		Document doc = sb.build(new ByteArrayInputStream(result.getBytes(StringConstants.DEFAULT_ENCODING)));
		
		return doc.getRootElement().getChildren();
	}
	
	private Element requestAPIObject(String objectName, int id)
			throws IOException, JDOMException, KeyManagementException, NoSuchAlgorithmException {
		HTTPController api = new HTTPController(username, password);
		SAXBuilder sb = new SAXBuilder();
		String result = api.doGet(url + "/JSSResource/" + objectName + "/id/" + id);
		Document doc = sb.build(new ByteArrayInputStream(result.getBytes(StringConstants.DEFAULT_ENCODING)));
		
		return doc.getRootElement();
	}
	
	/**
	 * This method gets all of the Computer, Mobile or User Smart Groups
	 * by ID, then tallies the Criteria and Nested counts.
	 * Adds problem groups to the JSON String.
	 */
	private void parseGroupObjects(String objectName, JSONObject parent) {
		ConfigurationController con = new ConfigurationController(true);
		
		try {
			Collection<Element> groups = requestAPIObjects(objectName);
			
			//Get all of the computer group IDS
			Collection<String> groupIds = parseMultipleObjects(groups);
			JSONArray array = parent.addArray(objectName);
			JSONObject details;
			int problemCount = 0;
			for (String groupId : groupIds) {
				List<Element> group = requestAPIObject(objectName, Integer.parseInt(groupId)).getChildren();
				
				String name = group.get(1).getContent().get(0).getValue();
				int nestedGroupCount = 0;
				int criticalCount;
				
				//Criteria has a different XML index value in each object for some reason.
				if (objectName.equals("computergroups")) {
					criticalCount = Integer.parseInt(group.get(4).getContent().get(0).getValue());
				} else if (objectName.equals("mobiledevicegroups")) {
					criticalCount = Integer.parseInt(group.get(3).getContent().get(0).getValue());
				} else {
					criticalCount = Integer.parseInt(group.get(5).getContent().get(0).getValue());
				}
				
				//Loop through all of the Critical and check for nested groups.
				for (int cri = 1; cri < group.get(4).getContent().size(); cri++) {
					String value = group.get(4).getContent().get(1).getValue();
					if (value.contains("Computer Group") || value.contains("Mobile Device Group") || value.contains("User Group")) {
						nestedGroupCount++;
					}
				}
				
				//Should only add problem groups
				if (nestedGroupCount != 0 || criticalCount > Integer.parseInt(con.getValue("configurations,smart_groups", "criteria_count")[0])) {
					details = array.addObject();
					details.addElement("id", group.get(0).getContent().get(0).getValue());
					details.addElement("name", name);
					details.addElement("nested_groups_count", nestedGroupCount);
					details.addElement("criteria_count", criticalCount);
					problemCount++;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error parsing groups" + e);
		}
	}
	
	public String getJSONAsString() {
		return this.document.toString();
	}
	
	// FIXME - This will not work. Subsequent #replaceAll calls will replace newly-inserted control characters
	// FIXME - Also verify validity of the left-hand arguments in the context of regex
	// FIXME - This is being bypassed until the other issues are resolved
	private String encodeSpecialCharacters(String input) {
		//		String cleaned = input;
		//		if (input.contains("&")) {
		//			cleaned = input.replaceAll("&", "&amp;");
		//		}
		//		if (input.contains("#")) {
		//			cleaned = input.replaceAll("#", "&#035;");
		//		}
		//		if (input.contains(":")) {
		//			cleaned = input.replaceAll(":", "&#058;");
		//		}
		//		if (input.contains(";")) {
		//			cleaned = input.replaceAll(";", "&#059;");
		//		}
		//		return cleaned;
		
		return input;
	}
	
}