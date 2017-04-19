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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.util.EnvironmentUtil;

public class JSSSummary {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JSSSummary.class);
	
	private String[][] summary;
	private int password_info_index = 0;
	private int clustered_index = 0;
	private int activation_code_index = 0;
	private int change_management_index = 0;
	private int tomcat_index = 0;
	private int log_flushing_index = 0;
	private int push_cert_index = 0;
	private int login_logout_hooks_index = 0;
	private int table_sizes = 0;
	private int table_row_counts = 0;
	
	public JSSSummary(String summary) {
		if (summary == null || summary.contains("java.io.IOException: Server returned HTTP response code: 401 for URL:")) {
			if (!EnvironmentUtil.isLinux()) {
				showPasteError();
			}
			LOGGER.error("The tool was unable to get the JSS Summary with this account. Please try again with another account that has read access and access to the JSS Summary. (HTTP 401)");
			System.exit(0);
		}
		
		generateArrays(summary);
		generateIndexes();
	}
	
	private void generateArrays(String summary) {
		String[] sections = summary.split("==========================================================================================");
		String[][] sum = new String[sections.length][1];
		for (int i = 0; i < sections.length; i++) {
			sum[i] = sections[i].split("----------------------------------------------------------------------------------");
		}
		this.summary = sum;
	}
	
	private void showPasteError() {
		JOptionPane.showMessageDialog(null, "Unable to get the JSS Summary with the supplied account. \nYou have encountered a JSS oddity that causes some accounts to not be able to access the summary.\nPlease create a new account with at least read privileges and try again.", "JSS Summary Error", JOptionPane.ERROR_MESSAGE);
	}
	
	private void generateIndexes() {
		String category;
		for (int i = 0; i < this.summary.length; i++) {
			if (this.summary[i] != null && this.summary[i].length > 0) {
				category = this.summary[i][0];
				
				if ("Password Policy".equals(category)) {
					password_info_index = i;
				} else if ("Clustering".equals(category)) {
					clustered_index = i;
				} else if ("Activation Code".equals(category)) {
					activation_code_index = i;
				} else if ("Change Management".equals(category)) {
					change_management_index = i;
				} else if ("Apache Tomcat Settings".equals(category)) {
					tomcat_index = i;
				} else if ("Log Flushing".equals(category)) {
					log_flushing_index = i;
				} else if ("Push Certificates".equals(category)) {
					push_cert_index = i;
				} else if ("Check-In".equals(category)) {
					login_logout_hooks_index = i;
				} else if ("Table sizes".equals(category)) {
					table_sizes = i;
				} else if ("Table row counts".equals(category)) {
					table_row_counts = i;
				}
			}
		}
	}
	
	public String getWebAppDir() {
		String[] el = this.summary[1][3].split("\t");
		String check = "Web App Installed To";
		String with_chars = el[1].substring(el[1].indexOf(check) + check.length(), el[1].length()).trim();
		return with_chars.replaceAll(":", "").replaceAll("\\\\", "/");
	}
	
	public String getJavaVendor() {
		String[] el = this.summary[1][4].split("\t");
		String check = "Java Vendor";
		return el[3].substring(el[3].indexOf(check) + check.length(), el[3].length()).trim();
	}
	
	public String getTableRowCounts() {
		String[] el = this.summary[table_row_counts + 1][0].split("\t");
		String computers_denormalized = "";
		String computers = "";
		String mobile_devices = "";
		String mobile_devices_denormalized = "";
		for (String anEl : el) {
			if (anEl.contains("computers")) {
				if (anEl.contains("computers_denormalized")) {
					String check = "computers_denormalized";
					computers_denormalized = anEl.substring(anEl.indexOf(check) + check.length(), anEl.length()).trim().replace(".", "");
				} else {
					String check = "computers";
					computers = anEl.substring(anEl.indexOf(check) + check.length(), anEl.length()).trim().replace(".", "");
				}
			}
			
			if (anEl.contains("mobile_devices")) {
				if (anEl.contains("mobile_devices_denormalized")) {
					String check = "mobile_devices_denormalized";
					mobile_devices_denormalized = anEl.substring(anEl.indexOf(check) + check.length(), anEl.length()).trim().replace(".", "");
				} else {
					String check = "mobile_devices";
					mobile_devices = anEl.substring(anEl.indexOf(check) + check.length(), anEl.length()).trim().replace(".", "");
				}
			}
			
		}
		return (computers + "," + computers_denormalized + "," + mobile_devices + "," + mobile_devices_denormalized).trim();
	}
	
	public String getJavaVersion() {
		String[] el = this.summary[1][4].split("\t");
		String check = "Java Version";
		return el[1].substring(el[1].indexOf(check) + check.length(), el[1].length()).trim();
	}
	
	public String getOperatingSystem() {
		String[] el = this.summary[1][2].split("\t");
		String check = "Operating System";
		return el[1].substring(el[1].indexOf(check) + check.length(), el[1].length()).trim();
	}
	
	public String getMySQLVersion() {
		String[] el = this.summary[1][9].split("\t");
		String check = "version";
		String v = el[20].substring(el[20].indexOf(check) + check.length(), el[20].length()).trim();
		return v.replace("....................", "");
	}
	
	public double getDatabaseSize() {
		String[] el = this.summary[1][5].split("\t");
		String check = "Database Size";
		String size_split = el[6].substring(el[6].indexOf(check) + check.length(), el[6].length()).trim();
		String size = size_split.substring(size_split.indexOf(" "), size_split.length());
		
		double size_in_mb = 0;
		if (size.contains("KB")) {
			double size_in_kb = Double.parseDouble(size.substring(1, size.length() - 3));
			size_in_mb = size_in_kb * 0.001;
		} else if (size.contains("MB")) {
			size_in_mb = Double.parseDouble(size.substring(1, size.length() - 3));
		} else if (size.contains("GB")) {
			double size_in_gb = Double.parseDouble(size.substring(1, size.length() - 3));
			size_in_mb = size_in_gb * 1000;
		}
		
		return size_in_mb;
	}
	
	public String[] getPasswordInformation() {
		String[] values = new String[4];
		String[] el = this.summary[password_info_index + 1][1].split("\t");
		String check = "Require Uppercase";
		values[0] = el[1].substring(el[1].indexOf(check) + check.length(), el[1].length()).trim();
		
		check = "Require Lowercase";
		values[1] = el[2].substring(el[2].indexOf(check) + check.length(), el[2].length()).replace(".", "").trim();
		
		check = "Require Number";
		values[2] = el[3].substring(el[3].indexOf(check) + check.length(), el[3].length()).trim();
		
		check = "Require Special Characters";
		values[3] = el[4].substring(el[4].indexOf(check) + check.length(), el[4].length()).replace(".", "").trim();
		
		return values;
	}
	
	public String getIsClustered() {
		String[] el = this.summary[this.clustered_index + 1][0].split("\t");
		String check = "Clustering Enabled";
		return el[1].substring(el[1].indexOf(check) + check.length(), el[1].length()).trim();
	}
	
	public String getActivationCodeExpiration() {
		String[] el = this.summary[this.activation_code_index + 1][1].split("\t");
		String check = "Expires";
		return el[4].substring(el[4].indexOf(check) + check.length(), el[4].length()).replace(".", "").trim();
	}
	
	public String[] getChangeManagementInfo() {
		String[] values = new String[2];
		String[] el = this.summary[this.change_management_index + 1][0].split("\t");
		String check = "Use Log File";
		values[0] = el[1].substring(el[1].indexOf(check) + check.length(), el[1].length()).trim();
		
		check = "Location of Log File";
		values[1] = el[2].substring(el[2].indexOf(check) + check.length(), el[2].length()).replace(".", "").trim();
		
		return values;
	}
	
	public String[] getTomcatCert() {
		String[] values = new String[2];
		String[] el = this.summary[this.tomcat_index + 1][0].split("\t");
		
		String check = "SSL Cert Issuer";
		values[0] = el[2].substring(el[2].indexOf(check) + check.length(), el[2].length()).replace(".", "").trim();
		
		check = "SSL Cert Expires";
		values[1] = el[3].substring(el[3].indexOf(check) + check.length(), el[3].length()).replace(".", "").trim();
		
		return values;
	}
	
	public String getLogFlushingInfo() {
		return parseSummaryValue(this.log_flushing_index, "Time to Flush Logs Each Day");
	}
	
	public String[] getPushCertInfo() {
		String[] el;
		String check;
		String[] values = new String[2];
		
		if (this.summary[this.push_cert_index + 1][0].contains("MDM Push Notification Certificate")) {
			el = this.summary[this.push_cert_index + 1][0].split("\t");
			check = "Expires";
			values[0] = el[3].substring(el[3].indexOf(check) + check.length(), el[3].length()).trim();
		} else {
			values[0] = "No Data Available.";
		}
		
		if (this.summary[this.push_cert_index + 2][0].contains("Push Proxy Authorization Token")) {
			el = this.summary[this.push_cert_index + 1][0].split("\t");
			check = "Expires";
			if (el.length > 3) {
				values[1] = el[3].substring(el[3].indexOf(check) + check.length(), el[3].length()).trim();
			} else {
				values[1] = "No Data Available.";
			}
		} else {
			values[1] = "No Data Available.";
		}
		
		return values;
	}
	
	public Boolean loginLogoutHooksEnabled() {
		return Boolean.parseBoolean(parseSummaryValue(this.login_logout_hooks_index, "Login/Logout Hooks"));
	}
	
	public Map<String, Double> getLargeMySQLTables() {
		String[] el = this.summary[this.table_sizes + 1][0].split("\t");
		System.out.println(Arrays.toString(el));
		
		return Stream.of(el)
				.filter(s -> !s.isEmpty())
				.map(s -> s.split(" "))
				.sorted((o1, o2) -> Objects.compare( // Descending Order
						Double.valueOf(o2[o2.length - 2]),
						Double.valueOf(o1[o1.length - 2]),
						Double::compareTo
				))
				.limit(el.length - 11) // Not sure why -11 was chosen...
				.collect(Collectors.toMap(
						a -> a[0],
						a -> {
							double size = Double.parseDouble(a[a.length - 2]);
							String unit = a[a.length - 1];
							return unit.contains("KB") ? size * 0.001 : unit.contains("GB") ? size * 1000 : size;
						}
				));
	}
	
	private String parseValue(String input, String key) {
		return input.substring(
				input.indexOf(key) + key.length(),
				input.length()
		).trim();
	}
	
	private String parseSummaryValue(int index, String key) {
		return parseValue(this.summary[index + 1][1].split("\t")[1].replaceAll("\\.", ""), key);
	}
	
}
