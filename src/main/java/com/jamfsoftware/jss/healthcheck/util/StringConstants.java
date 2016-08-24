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

public class StringConstants {
	
	public static final String ABOUT = String.join(System.lineSeparator(),
			"The JSS Health Check tool is provided for JSS administrators to run in their environment to attempt",
			"to help self diagnose issues, without having to contact JAMF Software.",
			"While it won't catch everything, this tool attempts to solve the",
			"most common issues found in health checks."
	);
	
	public static final String DEFAULT_CONFIGURATION_PATH = "Path to file '/Users/user/desktop/config.xml'";
	
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	public static final String LICENSE = String.join(System.lineSeparator(),
			"Copyright (C) 2016, JAMF Software, LLC All rights reserved.",
			"",
			"SUPPORT FOR THIS PROGRAM",
			"",
			"This program is distributed \"as is\" by JAMF Software, LLC.  For more information or",
			"support for the tool, please utilize the following resources:",
			"",
			"https://jamfnation.jamfsoftware.com/",
			"",
			"Redistribution and use in source and binary forms, with or without modification, are",
			"permitted provided that the following conditions are met:",
			"",
			"* Redistributions of source code must retain the above copyright notice, this list of",
			"conditions and the following disclaimer.",
			"",
			"* Redistributions in binary form must reproduce the above copyright notice, this list of",
			"conditions and the following disclaimer in the documentation and/or other materials",
			"provided with the distribution.",
			"",
			"* Neither the name of the JAMF Software, LLC nor the names of its contributors may be used",
			"to endorse or promote products derived from this software without specific prior written",
			"permission.",
			"",
			"THIS SOFTWARE IS PROVIDED BY JAMF SOFTWARE, LLC \"AS IS\" AND ANY EXPRESS OR IMPLIED",
			"WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND",
			"FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JAMF SOFTWARE, LLC BE",
			"LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES",
			"(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,",
			"DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,",
			"WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING",
			"IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH",
			"DAMAGE."
	);
	
	public static final String OPTIONS_TEXT = String.join(System.lineSeparator(),
			"Issue: One or more of the smart groups has potential issues",
			"Smart Groups that contain more than %1$s can increase smart group calculation times.",
			"Attempt to limit the number of criteria, especially when using the group for scoping.",
			"Smart Groups with other Smart Groups as criteria are also discouraged.",
			"Please consider revising these groups.",
			"================================================",
			"Issue: The JSS database is larger than expected",
			"Link to Scalability Article here",
			"================================================",
			"Issue: One or more recommended system requirement has not been met",
			"http://resources.jamfsoftware.com/documents/products/Casper-Suite-System-Requirements.pdf",
			"================================================",
			"Issue: The JSS could encounter scalability problems in the future",
			"Link to Scalability Article here",
			"================================================",
			"Issue: One or more policies could potentially have issues",
			"Policies that are ongoing, triggered by a check in and include an update inventory",
			"can potentially cause issues. The database can grow in size relatively fast. Make sure these type of policies",
			"are not running to often.",
			"================================================",
			"Issue: The tool has detected a large amount of extension attributes",
			"Every time an update inventory occurs, the extension attributes must ",
			"calculate. This isn't a big deal for a number",
			"of EAs; but once the JSS contains a lot it starts to add up.",
			"This is especially true if the extension attribute is a script.",
			"================================================",
			"Issue: Given the JSS environment size, the check in frequency is a bit too frequent",
			"500 Devices: Any check in frequency is recommended.",
			"",
			"500-5,000 Devices: 15-30 Min check in time recommended",
			"",
			"5,000+: 30 Min check in time recommended.",
			"================================================",
			"Issue: Printers with large driver packages detected",
			"Often times Xerox printers have driver packages over",
			"1GB in size. This requires us to update the SQL max packed size.",
			"================================================",
			"Issue: The tool has identified one or more issues with your scripts",
			"This tool checks for multiple things that could be",
			"wrong with scripts. For example, using 'rm -rf' (discouraged) or referencing the old JSS binary location.",
			"Please double check the scripts listed.",
			"================================================",
			"Issue: The JSS login password requirement is weak",
			"%2$s/passwordPolicy.html",
			"================================================",
			"Issue: Log In/Out hooks have not been configured",
			"%2$s/computerCheckIn.html",
			"================================================",
			"Issue: Change Management is not enabled",
			"%2$s/changeManagement.html",
			"================================================"
	);
	
	public static final String THUMBSUP = "<html>░░░░░░░░░░░░▄▄░░░░░░░░░ <br>" +
			"░░░░░░░░░░░█░░█░░░░░░░░ <br>" +
			"░░░░░░░░░░░█░░█░░░░░░░░ <br>" +
			"░░░░░░░░░░█░░░█░░░░░░░░ <br>" +
			"░░░░░░░░░█░░░░█░░░░░░░░ <br>" +
			"███████▄▄█░░░░░██████▄░░ <br>" +
			"▓▓▓▓▓▓█░░░░░░░░░░░░░░█░ <br>" +
			"▓▓▓▓▓▓█░░░░░░░░░░░░░░█░ <br>" +
			"▓▓▓▓▓▓█░░░░░░░░░░░░░░█░ <br>" +
			"▓▓▓▓▓▓█░░░░░░░░░░░░░░█░ <br>" +
			"▓▓▓▓▓▓█░░░░░░░░░░░░░░█░ <br>" +
			"▓▓▓▓▓▓█████░░░░░░░░░█░░ <br>" +
			"██████▀░░░░▀▀██████▀░░░</html>";
	
}
