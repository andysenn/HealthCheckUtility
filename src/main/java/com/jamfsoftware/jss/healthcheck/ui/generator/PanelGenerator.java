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

import java.awt.*;

import javax.swing.*;

import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;
import com.jamfsoftware.jss.healthcheck.ui.component.URLLabel;
import com.jamfsoftware.jss.healthcheck.util.StringConstants;

public class PanelGenerator {
	public String jssUrl = "";
	public boolean showGroupsHelp = false;
	public boolean showLargeDatabase = false;
	public boolean showScalability = false;
	public boolean showPolicies = false;
	public boolean showExtensionAttributes = false;
	public boolean showSystemRequirements = false;
	public boolean showCheckinFreq = false;
	public boolean showPrinters = false;
	public boolean showScripts = false;
	public boolean strongerPassword = false;
	public boolean loginInOutHooks = false;
	public boolean showChange = false;
	public boolean isCloudJSS = false;
	public boolean mobileDeviceTableCountMismatch = false;
	public boolean computerDeviceTableCountMismatch = false;
	public boolean mysqlOSXVersionBug = false;
	
	/**
	 * Generate an ImageIcon read from the images folder.
	 *
	 * @return an ImageIcon from the /images/png folder
	 */
	private ImageIcon generateIcon(String type) {
		if (type.equals("")) {
			return null;
		} else if (type.equals("red")) {
			return new ImageIcon(this.getClass().getResource("/images/red.png"));
		} else if (type.equals("yellow")) {
			return new ImageIcon(this.getClass().getResource("/images/yellow.png"));
		} else if (type.equals("green")) {
			return new ImageIcon(this.getClass().getResource("/images/green.png"));
		} else if (type.equals("blank")) {
			return new ImageIcon(this.getClass().getResource("/images/blank.png"));
		} else {
			return null;
		}
	}
	
	/**
	 * Generates a table from titles and a 2d array of content.
	 * //Titles are split by a ",".
	 *
	 * @return JTable filled with content.
	 */
	public JTable generateTable(String titles, Object[][] content) {
		String[] titles_arr = titles.split(",");
		return new JTable(content, titles_arr);
	}
	
	/**
	 * Generates a Panel for the System portion. Lists all of the content in the content [][] param in a table.
	 *
	 * @return a JPanel with content, and icon
	 */
	public JPanel generateContentPanelSystem(String title, String[][] content, String help_title, String help_link, String icon) {
		JPanel panelContent = new JPanel(new BorderLayout());
		//Add the header panel and text
		JPanel header = new JPanel();
		ImageIcon icon_image = generateIcon(icon);
		header.add(new JLabel(title, icon_image, JLabel.CENTER)).setForeground(Color.WHITE);
		header.setBackground(Color.decode("#5C6B84"));
		panelContent.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		//Add the content panel, then loop through array content and add it.
		JPanel container = new JPanel();
		//Make it a BoxLayout so the content is on new lines.
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		if (!(help_title.equals("") && help_link.equals(""))) {
			container.add(new URLLabel(help_title, help_link));
			container.add(new JLabel("   "));
		}
		String[] titles = { "Object", "Data" };
		JTable table = new JTable(content, titles);
		container.add(table);
		
		container.setBackground(Color.decode("#FFFFFF"));
		
		//Make sure the content can be scrolled.
		JScrollPane data = new JScrollPane(container);
		data.setViewportView(table);
		//Set the location of the elements
		panelContent.add(header, BorderLayout.NORTH);
		panelContent.add(data, BorderLayout.CENTER);
		data.getViewport().setBackground(Color.WHITE);
		
		return panelContent;
	}
	
	/**
	 * Generate the content panel for groups. Lists all of the problem groups and their criteria and nested groups
	 * count.
	 *
	 * @return a JPanel with content, and icon
	 */
	public JPanel generateContentPanelGroups(String title, String[][] groups, String help_title, String help_link, String icon) {
		JPanel panelContent = new JPanel(new BorderLayout());
		//Add the header panel and text
		JPanel header = new JPanel();
		ImageIcon icon_image = generateIcon(icon);
		header.add(new JLabel(title, icon_image, JLabel.CENTER)).setForeground(Color.WHITE);
		header.setBackground(Color.decode("#5C6B84"));
		panelContent.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		//Add the content panel, then loop through array content and add it.
		JPanel container = new JPanel();
		//Make it a BoxLayout so the content is on new lines.
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		if (!(help_title.equals("") && help_link.equals(""))) {
			container.add(new URLLabel(help_title, help_link));
			container.add(new JLabel("   "));
		}
		
		JScrollPane data = new JScrollPane(container);
		if (groups.length > 0) {
			JTable group = generateTable("Group Name,Nested Groups,Criteria Count", groups);
			container.add(group);
			//Make sure the content can be scrolled.
			data.setViewportView(group);
		} else {
			container.add(new JLabel("    "));
			JLabel text = new JLabel("<html>All of the smart groups in the JSS<br> are configured and optimized properly.</html>");
			text.setHorizontalAlignment(SwingConstants.CENTER);
			container.add(text);
			data.setViewportView(container);
		}
		
		container.setBackground(Color.decode("#FFFFFF"));
		
		//Set the location of the elements
		panelContent.add(header, BorderLayout.NORTH);
		panelContent.add(data, BorderLayout.CENTER);
		data.getViewport().setBackground(Color.WHITE);
		
		return panelContent;
	}
	
	/**
	 * Generate the content panel for the Policies and Scripts. Also checks if a link for large printer drivers should
	 * be shown.
	 *
	 * @return a JPanel with content, and icon
	 */
	public JPanel generateContentPanelPoliciesScripts(String title, String[][] policies, String[][] scripts, String[][] printers, String[][] certs, String help_title, String help_link, String icon) {
		JPanel panelContent = new JPanel(new BorderLayout());
		//Add the header panel and text
		JPanel header = new JPanel();
		ImageIcon icon_image = generateIcon(icon);
		header.add(new JLabel(title, icon_image, JLabel.CENTER)).setForeground(Color.WHITE);
		header.setBackground(Color.decode("#5C6B84"));
		panelContent.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		//Add the content panel, then loop through array content and add it.
		JPanel container = new JPanel();
		//Make it a BoxLayout so the content is on new lines.
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		if (!(help_title.equals("") && help_link.equals(""))) {
			container.add(new URLLabel(help_title, help_link));
			container.add(new JLabel("   "));
		}
		
		JPanel data1 = new JPanel();
		JPanel data2 = new JPanel();
		JPanel data3 = new JPanel();
		
		if (policies.length > 0) {
			JTable policies_table = generateTable("Policy Name,Ongoing?,Checkin Trigger?", policies);
			data1.add(policies_table);
			JScrollPane scroll1 = new JScrollPane(data1);
			scroll1.setViewportView(policies_table);
			container.add(scroll1);
		}
		if (scripts.length > 0) {
			JTable scripts_table = generateTable("Script Name", scripts);
			data2.add(scripts_table);
			JScrollPane scroll2 = new JScrollPane(data1);
			scroll2.setViewportView(scripts_table);
			container.add(scroll2);
		}
		if (printers.length > 0) {
			this.showPrinters = true;
		}
		JTable change_certs = generateTable("Object,Value", certs);
		data3.add(change_certs);
		JScrollPane scroll3 = new JScrollPane(data3);
		scroll3.setViewportView(change_certs);
		container.add(scroll3);
		
		//        if (scripts.length == 0 && policies.length == 0){
		//            container.add(new JLabel("   "));
		//            JLabel text = new JLabel("<html>All of your policies and scripts are <br>configured and optimized properly.</html>");
		//            text.setHorizontalAlignment(SwingConstants.CENTER);
		//            container.add(text);
		//        }
		
		container.setBackground(Color.decode("#FFFFFF"));
		
		//Make sure the content can be scrolled.
		JScrollPane data = new JScrollPane(container);
		
		//Set the location of the elements
		panelContent.add(header, BorderLayout.NORTH);
		panelContent.add(container, BorderLayout.CENTER);
		
		return panelContent;
	}
	
	/**
	 * Generate the content panel for the JSS Env. Generates separate tables for the ldap, vpp accounts, and other
	 * objects if they contain data.
	 *
	 * @return a JPanel with content, and icon
	 */
	public JPanel generateContentPanelEnv(String title, String[][] env_info, String[][] vpp_accounts, Object[][] ldap_accounts, String help_title, String help_link, String icon) {
		JPanel panelContent = new JPanel(new BorderLayout());
		//Add the header panel and text
		JPanel header = new JPanel();
		ImageIcon icon_image = generateIcon(icon);
		header.add(new JLabel(title, icon_image, JLabel.CENTER)).setForeground(Color.WHITE);
		header.setBackground(Color.decode("#5C6B84"));
		panelContent.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		//Add the content panel, then loop through array content and add it.
		JPanel container = new JPanel();
		//Make it a BoxLayout so the content is on new lines.
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		if (!(help_title.equals("") && help_link.equals(""))) {
			container.add(new URLLabel(help_title, help_link));
			container.add(new JLabel("   "));
		}
		
		JPanel data1 = new JPanel();
		JPanel data2 = new JPanel();
		JPanel data3 = new JPanel();
		//Make sure the content can be scrolled.
		JScrollPane scroll1 = new JScrollPane(data1);
		JScrollPane scroll2 = new JScrollPane(data1);
		JScrollPane scroll3 = new JScrollPane(data1);
		
		JTable env_info_table = generateTable("Object,Data", env_info);
		data1.add(env_info_table);
		scroll1.setViewportView(env_info_table);
		container.add(scroll1);
		
		//Only add VPP Accounts and LDAP servers if there is data available.
		if (vpp_accounts.length > 0) {
			JTable vpp_account_table = generateTable("VPP Account Account Name,Expires", vpp_accounts);
			data2.add(vpp_account_table);
			scroll2.setViewportView(vpp_account_table);
			container.add(scroll2);
		}
		if (ldap_accounts.length > 0) {
			JTable ldap_account_table = generateTable("LDAP ServerName,Type,Address", ldap_accounts);
			data3.add(ldap_account_table);
			scroll3.setViewportView(ldap_account_table);
			container.add(scroll3);
		}
		
		container.setBackground(Color.decode("#FFFFFF"));
		
		//Set the location of the elements
		panelContent.add(header, BorderLayout.NORTH);
		panelContent.add(container, BorderLayout.CENTER);
		
		return panelContent;
	}
	
	/**
	 * Generates the panel for the Help Section. Checks the class level variables for issues found
	 * If issues found, displays link or help message, otherwise display THUMBS UP ART.
	 *
	 * @return a JPanel with content, and icon
	 */
	public JPanel generateContentPanelHelp(String title, String help_title, String help_link, String icon) {
		ConfigurationController config = new ConfigurationController(true);
		JPanel panelContent = new JPanel(new BorderLayout());
		//Add the header panel and text
		JPanel header = new JPanel();
		ImageIcon icon_image = generateIcon(icon);
		header.add(new JLabel(title, icon_image, JLabel.CENTER)).setForeground(Color.WHITE);
		header.setBackground(Color.decode("#F75D59"));
		panelContent.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		//Add the content panel, then loop through array content and add it.
		JPanel container = new JPanel();
		//Make it a BoxLayout so the content is on new lines.
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		if (!(help_title.equals("") && help_link.equals(""))) {
			container.add(new URLLabel(help_title, help_link));
			container.add(new JLabel("   "));
		}
		
		container.add(new JLabel("   "));
		//No issues found woo!
		if ((!this.showGroupsHelp) && (!this.showLargeDatabase) && (!this.showScalability) && (!this.showPolicies) && (!this.showExtensionAttributes) && (!this.showSystemRequirements) && (!this.showCheckinFreq) && (!this.showPrinters) && (!this.showScripts) && (!this.loginInOutHooks) && (!this.strongerPassword)) {
			JLabel text = new JLabel("<html>The JSS is in excellent health.</html>");
			text.setHorizontalAlignment(SwingConstants.CENTER);
			container.add(text);
			JLabel thumbsup = new JLabel(StringConstants.THUMBSUP);
			thumbsup.setHorizontalAlignment(SwingConstants.CENTER);
			container.add(thumbsup);
		} else {
			container.add(new JLabel(""));
			JLabel text = new JLabel("<html>The below links will be helpful in solving issues<br>detected by this tool in the JSS.<br></html>");
			container.add(text);
			container.add(new JLabel("       "));
		}
		
		//Below conditions check for what links to show.
		if (this.mysqlOSXVersionBug) {
			container.add(new URLLabel("<html><u>The tool has detected a version of OSX/MySQL that is known</u><br><u>to cause issues. Click for a link to the bug report.</u><br></html>", "http://bugs.mysql.com/bug.php?id=71960"));
			container.add(new JLabel("   "));
		}
		if (this.showGroupsHelp) {
			container.add(new URLLabel("<html><u>One or more of the smart groups has potential issues.</u><br><u>Click for recommended configuration.</u><br></html>", "Smart Groups that contain more than " + config.getValue("configurations,smart_groups", "criteria_count")[0] + " can increase smart group calculation times.\nAttempt to limit the number of criteria, especially when using the group for scoping.\nSmart Groups with other Smart Groups as criteria are also discouraged.\nPlease consider revising these groups."));
			container.add(new JLabel("   "));
		}
		if (this.showLargeDatabase) {
			container.add(new URLLabel("<html><u>The JSS database is larger than expected.</u><br><u>Click for a few common reasons and solutions.</u></html>", "LINK TO LARGE DB/SCALABILITY  KB ARTICLE"));
			container.add(new JLabel("   "));
		}
		if (this.showScalability) {
			container.add(new URLLabel("<html><u>The JSS could encounter scalability problems in the future.</u><br><u>Click for a scalability guide.</u></html>", "LINK TO GENERAL SCALABILITY ARTICLE"));
			container.add(new JLabel("   "));
		}
		if (this.showPolicies) {
			container.add(new URLLabel("<html><u>One or more policies could potentially have issues.</u> <br><u>Click to find out why.</u></html>", "Policies that are ongoing, triggered by a check in and include an update inventory\ncan potentially cause issues. The database can grow in size relatively fast. Make sure these type of policies\nare not running to often."));
			container.add(new JLabel("   "));
		}
		if (this.showExtensionAttributes) {
			container.add(new URLLabel("<html><u>The tool has detected a large amount of extension attributes.</u> <br><u>Click to learn about inventory updates.</u></html>", "Every time an update inventory occurs, the extension attributes must \ncalculate. This isn't a big deal for a number \nof EAs; but once the JSS contains a lot it starts to add up.\nThis is especially true if the extension attribute is a script."));
			container.add(new JLabel("   "));
		}
		if (this.showSystemRequirements) {
			container.add(new URLLabel("<html><u>One or more recommended system requirement has not been met.</u> <br><u>Click to view system requirements.</u></html>", "http://resources.jamfsoftware.com/documents/products/Casper-Suite-System-Requirements.pdf"));
			container.add(new JLabel("   "));
		}
		if (this.showCheckinFreq) {
			container.add(new URLLabel("<html><u>Given the JSS environment size, the check in frequency is a bit too frequent.</u><br><u>Click to learn about recommended times.</u></html>", "500 Devices: Any check in frequency is recommended.\n\n500-5,000 Devices: 15-30 Min check in time recommended\n\n5,000+: 30 Min check in time recommended."));
			container.add(new JLabel("   "));
		}
		if (this.showPrinters) {
			container.add(new URLLabel("<html><u>Printers with large driver packages detected.</u><br><u>Click to learn about increasing max packet size.</u></html>", "Often times Xerox printers have driver packages over\n1GB in size. This requires us to update the SQL max packed size."));
			container.add(new JLabel("   "));
		}
		if (this.showScripts) {
			container.add(new URLLabel("<html><u>The tool has identified one or more issues with your scripts.</u><br><u>Click to learn about common script issues.</u></html>", "This tool checks for multiple things that could be \nwrong with scripts. For example, using 'rm-rf' (discouraged) or referencing the old JSS binary location. \nThey could also contain the 'jamf recon' command in the script. This can cause database bloat.\nPlease double check the scripts listed."));
			container.add(new JLabel("   "));
		}
		if (this.strongerPassword) {
			container.add(new URLLabel("<html><u>The JSS login password requirement is weak.</u><br><u>Consider updating it.</u></html>", this.jssUrl + "/passwordPolicy.html"));
			container.add(new JLabel("   "));
		}
		if (this.loginInOutHooks) {
			container.add(new URLLabel("<html><u>Log In/Out hooks have not been configured.</u><br><u>Click to configure them.</u></html>", this.jssUrl + "/computerCheckIn.html"));
			container.add(new JLabel("   "));
		}
		if (this.showChange && (!this.isCloudJSS)) {
			container.add(new URLLabel("<html><u>Change Management is not enabled.</u><br><u>Click to enable it.</u></html>", this.jssUrl + "/changeManagement.html"));
			container.add(new JLabel("   "));
		}
		if (this.mobileDeviceTableCountMismatch) {
			container.add(new URLLabel("<html><u>This JSS database contains a mismatch for computer device counts.</u><br><u>Click to learn more.</u></html>", "Ghost records in the database can cause strain on the JSS Server.\nIf the JSS is experiencing performance issues, please contact support.\nThe device counts can be viewed near the end of the report JSON."));
			container.add(new JLabel("   "));
		}
		if (this.computerDeviceTableCountMismatch) {
			container.add(new URLLabel("<html><u>This JSS database contains a mismatch for mobile device counts.</u><br><u>Click to learn more.</u></html>", "Ghost records in the database can cause strain on the JSS Server.\nIf the JSS is experiencing performance issues, please contact support.\nThe device counts can be viewed near the end of the report JSON."));
			container.add(new JLabel("   "));
		}
		
		container.setBackground(Color.decode("#FFFFFF"));
		
		//Make sure the content can be scrolled.
		JScrollPane data = new JScrollPane(container);
		
		//Set the location of the elements
		panelContent.add(header, BorderLayout.NORTH);
		panelContent.add(data, BorderLayout.CENTER);
		data.getViewport().setBackground(Color.WHITE);
		
		return panelContent;
	}
	
}
