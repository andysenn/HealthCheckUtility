package com.jamfsoftware.jss.healthcheck.util;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DetectVM.java Created by Jacob Schultz on 9.2.16.
 * This class checks the operating system version, then
 * runs a system command to check with relative certainty
 * if the host is a VM.
 */
public class DetectVM {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DetectVM.class);
	
	private final boolean isVM;
	
	/**
	 * Constructor that detects the OS Type,
	 * then attempts to determine if it is a VM.
	 */
	public DetectVM() {
		if (EnvironmentUtil.isLinux()) {
			this.isVM = getVMStatusLinux();
		} else if (EnvironmentUtil.isWindows()) {
			this.isVM = getVMStatusWindows();
		} else if (EnvironmentUtil.isMac()) {
			this.isVM = getVMStatusOSX();
		} else {
			LOGGER.warn("Unable to detect OS type.");
			this.isVM = false;
		}
	}
	
	public DetectVM(String rootPassword) {
		if (EnvironmentUtil.isLinux()) {
			this.isVM = getVMStatusLinux(rootPassword);
		} else if (EnvironmentUtil.isWindows()) {
			this.isVM = getVMStatusWindows();
		} else if (EnvironmentUtil.isMac()) {
			this.isVM = getVMStatusOSX();
		} else {
			LOGGER.warn("Unable to detect OS type.");
			this.isVM = false;
		}
	}
	
	private static boolean getVMStatusLinux() {
		String[] command = { "/bin/sh", "-c", "ls -l /dev/disk/by-id/" };
		String value = executeCommand(command);
		
		return value.contains("QEMU")
				|| value.contains("VMware")
				|| value.contains("VirtualBox")
				|| value.contains("KVM")
				|| value.contains("Bochs")
				|| value.contains("Parallels");
	}
	
	private static boolean getVMStatusLinux(String rootPassword) {
		String[] command = { "echo " + rootPassword + " | sudo -S dmidecode -s system-product-name" };
		String value = executeCommand(command);
		if (value.contains("VMware Virtual Platform")
				|| value.contains("VirtualBox")
				|| value.contains("KVM")
				|| value.contains("Bochs")
				|| value.contains("Parallels")) {
			return true;
		}
		
		String[] command2 = { "echo " + rootPassword + " | sudo -S dmidecode egrep -i 'manufacturer|product'" };
		value = executeCommand(command2);
		
		return value.contains("Microsoft Corporation")
				&& value.contains("Virtual Machine");
	}
	
	private static boolean getVMStatusWindows() {
		String[] command = { "SYSTEMINFO" };
		String value = executeCommand(command);
		
		return value.contains("VMWare")
				|| value.contains("VirtualBox")
				|| value.contains("KVM")
				|| value.contains("Bochs")
				|| value.contains("Parallels");
	}
	
	private static boolean getVMStatusOSX() {
		String[] command = { "/bin/sh", "-c", "ioreg -l | grep -e Manufacturer -e 'Vendor Name'" };
		String value = executeCommand(command);
		
		return value.contains("VirtualBox")
				|| value.contains("VMware")
				|| value.contains("Oracle")
				|| value.contains("Bochs")
				|| value.contains("Parallels");
	}
	
	/**
	 * This method executes a command on the host system.
	 *
	 * @return Command output
	 */
	private static String executeCommand(String[] command) {
		String s;
		String output = "";
		try {
			Process p = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = stdInput.readLine()) != null) {
				output += s;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}
	
	/**
	 * Method that returns private VM boolean.
	 *
	 * @return boolean of if it is a VM or Not
	 */
	public boolean getIsVM() {
		return this.isVM;
	}
}

