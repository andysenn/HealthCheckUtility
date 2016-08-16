package com.jamfsoftware.jss.healthcheck.ui.generator;

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

import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;

/**
 * This class handles producing the icons for the HealthReportGUI
 */
public class PanelIconGenerator {
	
	public boolean showCheckinFreq = false;
	public boolean showExtensionAttributes = false;
	public boolean showSystemRequirements = false;
	public boolean showScalability = false;
	
	public PanelIconGenerator() {
	}
	
	public String getGroupIconType(String object, int count) {
		if (object.equals("groups")) {
			if (count < 5) {
				return "green";
			} else if (count < 10) {
				return "yellow";
			} else {
				return "red";
			}
		}
		return null;
	}
	
	public String getDatabaseInfoIconType(int total_devices, double database_size, int large_table_count) {
		ConfigurationController config = new ConfigurationController(true);
		//Check to make sure they gave a value.
		String data_base_checker = "";
		int data_base_size_recommened = 0;
		if (total_devices < 500) {
			data_base_checker = config.getValue("configurations,jss_size,up_to_500", "db_size")[0];
		} else if (total_devices < 1000 && total_devices > 500) {
			data_base_checker = config.getValue("configurations,jss_size,up_to_1000", "db_size")[0];
		} else if (total_devices < 2000 && total_devices > 10000) {
			data_base_checker = config.getValue("configurations,jss_size,up_to_2000", "db_size")[0];
		} else {
			data_base_checker = config.getValue("configurations,jss_size,up_to_5000", "db_size")[0];
		}
		//They didn't provide a value, so set it high enough to (hopefully) not be triggered.
		if (data_base_checker.equals("")) {
			data_base_size_recommened = 900000000; //MB, This shouldn't be hit for a long, long, long time.
			//They provided a value, so parse the int from XML
		} else {
			data_base_size_recommened = Integer.parseInt(data_base_checker);
		}
		
		if (total_devices < 500) {
			if (database_size > data_base_size_recommened) {
				return "yellow";
			}
		} else if (total_devices < 1000 && total_devices > 500) {
			if (database_size > data_base_size_recommened) {
				return "yellow";
			}
		} else if (total_devices < 2000 && total_devices > 1000) {
			if (database_size > data_base_size_recommened) {
				return "yellow";
			}
		} else {
			if (database_size > data_base_size_recommened) {
				return "yellow";
			}
		}
		return "green";
	}
	
	public String getSystemInfoIconType(int total_devices, String java_version, double max_memory) {
		double memory_in_mb = max_memory / 1000000;
		ConfigurationController config = new ConfigurationController(true);
		//Check to make sure they gave a value.
		String server_mem_checker = "";
		int server_mem_recommended = 0;
		if (total_devices < 500) {
			server_mem_checker = config.getValue("configurations,jss_size,up_to_500", "tomcat_mem")[0];
		} else if (total_devices < 1000 && total_devices > 500) {
			server_mem_checker = config.getValue("configurations,jss_size,up_to_1000", "tomcat_mem")[0];
		} else if (total_devices < 2000 && total_devices > 10000) {
			server_mem_checker = config.getValue("configurations,jss_size,up_to_2000", "tomcat_mem")[0];
		} else {
			server_mem_checker = config.getValue("configurations,jss_size,up_to_5000", "tomcat_mem")[0];
		}
		//They didn't provide a value, so set it high enough to (hopefully) not be triggered.
		if (server_mem_checker.equals("")) {
			server_mem_recommended = 900000000; //MB, This shouldn't be hit for a long, long, long time.
			//They provided a value, so parse the int from XML
		} else {
			server_mem_recommended = Integer.parseInt(server_mem_checker);
		}
		
		if (java_version.contains("1.6")) {
			this.showSystemRequirements = true;
			return "red";
		}
		if (total_devices < 500) {
			if (memory_in_mb < server_mem_recommended) {
				this.showScalability = true;
				return "yellow";
			} else {
				return "green";
			}
		} else if (total_devices < 1000 && total_devices > 500) {
			if (memory_in_mb < server_mem_recommended) {
				this.showScalability = true;
				return "yellow";
			} else {
				return "green";
			}
		} else if (total_devices < 2000 && total_devices > 100) {
			if (memory_in_mb < server_mem_recommended) {
				this.showScalability = true;
				return "yellow";
			} else {
				return "green";
			}
		} else {
			if (memory_in_mb < server_mem_recommended) {
				this.showScalability = true;
				return "yellow";
			} else {
				return "green";
			}
		}
		
	}
	
	public String getPoliciesAndScriptsIconType(int policies_count, int scripts_count) {
		if (policies_count + scripts_count < 3) {
			return "green";
		} else if (policies_count + scripts_count < 10) {
			return "yellow";
		} else {
			return "red";
		}
		
	}
	
	public String getJSSEnvIconType(int total_devices, int checkin_freq, int computer_ea, int mobile_ea) {
		int bad_count = 0;
		ConfigurationController config = new ConfigurationController(true);
		//Check to make sure they gave a value.
		String check_in_time_checker = "";
		int checkin_time = 0;
		if (total_devices < 500) {
			check_in_time_checker = config.getValue("configurations,jss_size,up_to_500", "checkin")[0];
		} else if (total_devices < 1000 && total_devices > 500) {
			check_in_time_checker = config.getValue("configurations,jss_size,up_to_1000", "checkin")[0];
		} else if (total_devices < 2000 && total_devices > 10000) {
			check_in_time_checker = config.getValue("configurations,jss_size,up_to_2000", "checkin")[0];
		} else {
			check_in_time_checker = config.getValue("configurations,jss_size,up_to_5000", "checkin")[0];
		}
		//They didn't provide a value, so set it high enough to be any checkin time
		if (check_in_time_checker.equals("")) {
			checkin_time = 100;
			//They provided a value, so parse the int from XML
		} else {
			checkin_time = Integer.parseInt(check_in_time_checker);
		}
		
		if (total_devices < 500) {
			if (checkin_freq > checkin_time) {
				bad_count++;
				this.showCheckinFreq = true;
			}
		} else if (total_devices < 1000 && total_devices > 500) {
			if (checkin_freq > checkin_time) {
				bad_count++;
				this.showCheckinFreq = true;
			}
		} else if (total_devices < 2000 && total_devices > 10000) {
			if (checkin_freq > checkin_time) {
				bad_count++;
				this.showCheckinFreq = true;
			}
		} else {
			if (checkin_freq > checkin_time) {
				bad_count++;
				this.showCheckinFreq = true;
			}
		}
		if (computer_ea > Integer.parseInt(config.getValue("configurations,extension_attributes", "computer")[0])) {
			bad_count++;
			this.showExtensionAttributes = true;
		}
		if (mobile_ea > Integer.parseInt(config.getValue("configurations,extension_attributes", "mobile")[0])) {
			bad_count++;
			this.showExtensionAttributes = true;
		}
		if (bad_count > 1) {
			return "yellow";
		} else if (bad_count > 3) {
			return "red";
		} else {
			return "green";
		}
		
	}
	
}
