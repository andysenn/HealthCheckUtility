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
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class checks the operating system version, then
 * runs a system command to check with relative certainty
 * if the host is a VM.
 *
 * @author Andy Senn
 * @author Jacob Schultz
 * @since 1.0
 */
public final class EnvironmentUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUtil.class);
	
	private EnvironmentUtil() {
	}
	
	public static boolean isLinux() {
		return System.getProperty("os.name").contains("Linux");
	}
	
	public static boolean isMac() {
		return System.getProperty("os.name").contains("OS X") ||
				System.getProperty("os.name").contains("macOS");
	}
	
	public static boolean isWindows() {
		return System.getProperty("os.name").contains("Windows");
	}
	
	/**
	 * Constructor that detects the OS Type,
	 * then attempts to determine if it is a VM.
	 *
	 * @return boolean of if it is a VM or Not
	 */
	public static boolean isVM() {
		if (EnvironmentUtil.isLinux()) {
			return getVMStatusLinux();
		} else if (EnvironmentUtil.isWindows()) {
			return getVMStatusWindows();
		} else if (EnvironmentUtil.isMac()) {
			return getVMStatusOSX();
		} else {
			LOGGER.warn("Unable to detect OS type.");
			return false;
		}
	}
	
	public static boolean isVM(String rootPassword) {
		if (EnvironmentUtil.isLinux()) {
			return getVMStatusLinux(rootPassword);
		}
		return isVM();
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
	
	// FIXME: High - Use the console instead of echoing the root password!
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
	 * Executes a shell command on the host system
	 *
	 * @return The standard output of the command
	 */
	private static String executeCommand(String[] command) {
		String output = "";
		try {
			Process proc = Runtime.getRuntime().exec(command);
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			stdIn.lines().forEach(output::concat);
		} catch (IOException e) {
			LOGGER.error("Error executing command: " + Arrays.toString(command), e);
		}
		return output;
	}
	
}
