package com.jamfsoftware.jss.healthcheck.ui;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.HealthCheck;
import com.jamfsoftware.jss.healthcheck.JSSConnectionTest;
import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;
import com.jamfsoftware.jss.healthcheck.util.EnvironmentUtil;

//import com.apple.eawt.Application;

/*
* UserPrompt.java - Written 12/2015 by Jacob Schultz
* This class opens a prompt frame for the user to provide the JSS URL, Username, Password and MySQL settings
* The popup can trigger a new healthcheck (HealthCheck.java) or to open the options pane.
*/

public class UserPrompt extends JFrame {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserPrompt.class);
	
	private String jssURL;
	private String jssUsername;
	private String jssPassword;
	private Preferences prefs = Preferences.userNodeForPackage(UserPrompt.class);
	
	//Default Constructor. Checks for the OS Version. If mac - add a menu bar
	//Opens the User Prompt JPanel
	public UserPrompt() throws Exception {
		if (EnvironmentUtil.isMac()) {
			//Add a menu bar, and set the icon on OSX.
			//			Application.getApplication().setDockIconImage(new ImageIcon(this.getClass().getResource("/images/icon.png")).getImage());
			JMenuBar menu = new JMenuBar();
			JMenu ops = new JMenu("Health Check Options");
			JMenu load_json = new JMenu("Load Previous Test");
			JMenuItem load_links = new JMenuItem("Load All Available Help Links");
			JMenuItem retest = new JMenuItem("Start new health check");
			JMenuItem load_json_item = new JMenuItem("Insert JSON");
			JMenuItem setup_xml = new JMenuItem("Setup configuration XML");
			JMenuItem edit = new JMenuItem("Edit configuration XML");
			JMenuItem quit = new JMenuItem("Quit health check tool");
			ops.add(load_links);
			ops.add(retest);
			ops.add(setup_xml);
			ops.add(edit);
			ops.add(quit);
			load_json.add(load_json_item);
			menu.add(ops);
			menu.add(load_json);
			//Below are button listeners for the menu on OSX.
			retest.addActionListener(e -> {
				try {
					new UserPrompt();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
			quit.addActionListener(e -> System.exit(0));
			setup_xml.addActionListener(e -> openOptions());
			load_json_item.addActionListener(e -> loadJSON());
			edit.addActionListener(e -> LoadXMLEditor());
			load_links.addActionListener(e -> loadAllHelpLinks());
			
			//Set the Apple Menu bar to our JMenu
			//			Application.getApplication().setDefaultMenuBar(menu);
			//Setting the icon on Windows
		} else if (EnvironmentUtil.isWindows()) {
			setIconImage(new ImageIcon(this.getClass().getResource("/images/icon.png")).getImage());
		}
		//Create the Base Panel
		final JFrame frame = new JFrame("JSS Health Check");
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(5, 0));
		//Setup all of the text fields.
		final JTextField url = new JTextField("JSS URL");
		final JTextField username = new JTextField("JSS User");
		final JTextField password = new JPasswordField("password");
		final JLabel mysql = new JLabel("MySQL Username/Password");
		final JTextField mysql_username = new JTextField("MySQL User");
		final JTextField mysql_password = new JPasswordField("password");
		//Add the buttons to begin check and open options.
		final JButton begin_check = new JButton("Begin Health Check");
		JButton options = new JButton("Options");
		mysql.setHorizontalAlignment(JLabel.CENTER);
		//Create a new configuration controller to load values from the XML.
		ConfigurationController con = new ConfigurationController(true);
		//Load values from the XML. If blank, textfields will appear blank.
		url.setText(con.getValue("healthcheck", "jss_url")[0]);
		username.setText(con.getValue("healthcheck", "jss_username")[0]);
		password.setText(con.getValue("healthcheck", "jss_password")[0]);
		mysql_username.setText(con.getValue("healthcheck", "mysql_user")[0]);
		mysql_password.setText(con.getValue("healthcheck", "mysql_password")[0]);
		
		//Add all of the elements to the frame
		panel.add(url);
		// panel.add(mysql);
		panel.add(username);
		//panel.add(mysql_username);
		panel.add(password);
		//panel.add(mysql_password);
		panel.add(begin_check);
		panel.add(options);
		//Setup the frame options
		frame.add(panel);
		frame.setSize(500, 280);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		//They clicked the start check button
		begin_check.addActionListener(e -> {
			//Make a connection test before we start the check.
			final JSSConnectionTest test = new JSSConnectionTest(url.getText(), username.getText(), password.getText());
			if (test.canConnect()) {
				//Check if it is a cloud JSS, if it is, don't preform system checks.
				if (test.isCloudJSS()) {
					mysql.setText("<html>Unable to perform system<br> checks on a hosted JSS.</html>");
					mysql_username.setEnabled(false);
					mysql_password.setEnabled(false);
				}
				//Start a new thread to handle updating the health check button.
				Thread m = new Thread(new Runnable() {
					@Override
					public void run() {
						begin_check.setText("Running API checks... Please wait");
						//Tell them the tool is still working after 15 seconds
						new java.util.Timer().schedule(
								new java.util.TimerTask() {
									@Override
									public void run() {
										begin_check.setText("Still working..");
									}
								},
								15000
						);
						//Tell them it's still working again after 25 seconds
						new java.util.Timer().schedule(
								new java.util.TimerTask() {
									@Override
									public void run() {
										begin_check.setText("Loading results..");
									}
								},
								25000
						);
					}
				});
				
				//Start the thread and timers
				m.start();
				
				//Start another new thread to start the Health Check.
				Thread t = new Thread(() -> StartCheck(url, username, password, frame, begin_check));
				t.start();
			} else {
				//Throw an error if a connection can not be made.
				JOptionPane.showMessageDialog(frame, "Unable to connect or login to the JSS.", "Connection Error", JOptionPane.ERROR_MESSAGE);
			}
			
		});
		
		//Listens for a click on the options button.
		options.addActionListener(e -> openOptions());
		//Listens for typing on the URL field.
		//If it detects "jamfclould" disable the MySQL field.
		url.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				check_for_hosted();
			}
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				check_for_hosted();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				check_for_hosted();
			}
			
			//Method to handle disabling/enabling the mysql text fields.
			public void check_for_hosted() {
				if (url.getText().contains(".jamfcloud")) {
					mysql.setText("<html>Unable to perform system<br> checks on a hosted JSS.</html>");
					mysql_username.setEnabled(false);
					mysql_password.setEnabled(false);
				} else {
					mysql.setText("MySQL Username/Password");
					mysql_username.setEnabled(true);
					mysql_password.setEnabled(true);
				}
			}
		});
	}
	
	//This method starts the health check. Gathers information from the text fields and creates a new HealthCheck Object
	//If the Health Check object is created without errors, it then creates a new HealthReport object.
	public void StartCheck(JTextField url, JTextField username, JTextField password, JFrame frame, JButton button) {
		jssURL = url.getText();
		jssUsername = username.getText();
		jssPassword = password.getText();
		
		try {
			HealthCheck newHealthCheck = new HealthCheck(jssURL, jssUsername, jssPassword);
			System.out.println("Health Check Complete, Loading Summary..");
			new HealthReport(newHealthCheck.getJSONAsString());
			System.out.println("Report loaded.");
			frame.setVisible(false);
			
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(new JFrame(), "A fatal error has occurred. \n" + ex, "Fatal Error", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
			System.exit(0);
		}
	}
	
	//This method opens the options menu. It allows the config.xml path to be set after opening the program.
	public void openOptions() {
		final JFrame frame = new JFrame("Health Check Options");
		JPanel panel = new JPanel();
		panel.add(new JLabel("Configuration XML path:"));
		final JTextField config_xml_path = new JTextField();
		
		//Load xml path from saved prefs.
		String xml_path = this.prefs.get("config_xml_path", "Path to file '/Users/user/desktop/config.xml'");
		config_xml_path.setText(xml_path);
		
		panel.add(config_xml_path);
		
		JButton save_button = new JButton("Save Path");
		panel.add(save_button);
		panel.add(new JLabel(""));
		JButton load_test_json = new JButton("Load Previous Test");
		panel.add(load_test_json);
		
		frame.add(panel);
		frame.setSize(530, 100);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		//Listener for the Save Path button
		//Checks that the path is valid before saving.
		save_button.addActionListener(e -> {
			ConfigurationController con = new ConfigurationController();
			if (con.canGetFile(config_xml_path.getText())) {
				prefs.put("config_xml_path", config_xml_path.getText());
			} else {
				config_xml_path.setText(prefs.get("config_xml_path", "Path to file '/Users/user/desktop/config.xml'"));
				JOptionPane.showMessageDialog(frame, "This is not a valid configuration XML file. \nNo changes have been made.", "XML Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
		load_test_json.addActionListener(e -> loadJSON());
	}
	
	//This method opens a JPanel to load previous test JSON into.
	//If the JSON is valid, it will open a new health report window.
	public void loadJSON() {
		final JPanel middlePanel = new JPanel();
		middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "Load JSON from a previous test"));
		// create the middle panel components
		final JTextArea display = new JTextArea(16, 58);
		display.setEditable(true);
		JScrollPane scroll = new JScrollPane(display);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		//Add Textarea in to middle panel
		middlePanel.add(scroll);
		JButton load_report = new JButton("Open Health Report");
		middlePanel.add(load_report);
		
		JFrame frame = new JFrame();
		frame.add(middlePanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		load_report.addActionListener(e -> {
			try {
				new HealthReport(display.getText());
			} catch (Exception e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(middlePanel, "The tool was unable to load the pasted JSON.\nIt may be incomplete or not formatted correctly.\nThe error the tool encountered:\n" + e1, "Error Loading JSON", JOptionPane.ERROR_MESSAGE);
			}
		});
	}
	
	public void loadAllHelpLinks() {
		final JPanel middlePanel = new JPanel();
		ConfigurationController config = new ConfigurationController(true);
		middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "All available JSS Health Checks and Help Information"));
		final JTextArea display = new JTextArea(16, 58);
		display.setEditable(false);
		JScrollPane scroll = new JScrollPane(display);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		middlePanel.add(scroll);
		display.append("Issue: One or more of the smart groups has potential issues");
		display.append("\nSmart Groups that contain more than " + config.getValue("configurations,smart_groups", "criteria_count")[0] + " can increase smart group calculation times.\nAttempt to limit the number of criteria, especially when using the group for scoping.\nSmart Groups with other Smart Groups as criteria are also discouraged.\nPlease consider revising these groups.");
		display.append("\n================================================\n");
		
		display.append("Issue: The JSS database is larger than expected");
		display.append("\nLink to Scalability Article here");
		display.append("\n================================================\n");
		
		display.append("Issue: One or more recommended system requirement has not been met");
		display.append("\nhttp://resources.jamfsoftware.com/documents/products/Casper-Suite-System-Requirements.pdf");
		display.append("\n================================================\n");
		
		display.append("Issue: The JSS could encounter scalability problems in the future");
		display.append("\nLink to Scalability Article here");
		display.append("\n================================================\n");
		
		display.append("Issue: One or more policies could potentially have issues");
		display.append("\nPolicies that are ongoing, triggered by a check in and include an update inventory\n" +
				"can potentially cause issues. The database can grow in size relatively fast. Make sure these type of policies\n" +
				"are not running to often.");
		display.append("\n================================================\n");
		
		display.append("Issue: The tool has detected a large amount of extension attributes");
		display.append("\nEvery time an update inventory occurs, the extension attributes must \ncalculate. This isn't a big deal for a number \nof EAs; but once the JSS contains a lot it starts to add up.\nThis is especially true if the extension attribute is a script.");
		display.append("\n================================================\n");
		
		display.append("Issue: Given the JSS environment size, the check in frequency is a bit too frequent");
		display.append("\n500 Devices: Any check in frequency is recommended.\n\n500-5,000 Devices: 15-30 Min check in time recommended\n\n5,000+: 30 Min check in time recommended.");
		display.append("\n================================================\n");
		
		display.append("Issue: Printers with large driver packages detected");
		display.append("\nOften times Xerox printers have driver packages over\n1GB in size. This requires us to update the SQL max packed size.");
		display.append("\n================================================\n");
		
		display.append("Issue: The tool has identified one or more issues with your scripts");
		display.append("This tool checks for multiple things that could be \nwrong with scripts. For example, using 'rm-rf' (discouraged) or referencing the old JSS binary location. \nPlease double check the scripts listed.");
		display.append("\n================================================\n");
		
		display.append("Issue: The JSS login password requirement is weak");
		display.append("\n" + this.jssURL + "/passwordPolicy.html");
		display.append("\n================================================\n");
		
		display.append("Issue: Log In/Out hooks have not been configured");
		display.append("\n" + this.jssURL + "/computerCheckIn.html");
		display.append("\n================================================\n");
		
		display.append("Issue: Change Management is not enabled");
		display.append("\n" + this.jssURL + "/changeManagement.html");
		display.append("\n================================================\n");
		
		JFrame frame = new JFrame();
		frame.add(middlePanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	public void LoadXMLEditor() {
		final JPanel middlePanel = new JPanel();
		middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "Edit Configuration XML File"));
		final ConfigurationController con = new ConfigurationController(true);
		String[] values = { "JSS URL", "JSS Username", "JSS Password", "Smart Group Criteria Count", "Extension Attribute Count" };
		final JComboBox<String> options = new JComboBox<>(values);
		
		final JTextField value = new JTextField(con.getValue("healthcheck", "jss_url")[0]);
		JButton update = new JButton("Update XML Value");
		
		middlePanel.add(options);
		middlePanel.add(value);
		middlePanel.add(update);
		
		JFrame frame = new JFrame();
		frame.add(middlePanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		options.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				JComboBox combo = (JComboBox) e.getSource();
				String selected = (String) combo.getSelectedItem();
				//con.updateXMLValue("jss_url", "test");
				if (selected.equals("JSS URL")) {
					value.setText(con.getValue("healthcheck", "jss_url")[0]);
				} else if (selected.equals("JSS Username")) {
					value.setText(con.getValue("healthcheck", "jss_username")[0]);
				} else if (selected.equals("JSS Password")) {
					value.setText(con.getValue("healthcheck", "jss_password")[0]);
				} else if (selected.equals("Smart Group Criteria Count")) {
					value.setText(con.getValue("configurations,smart_groups", "criteria_count")[0]);
				} else if (selected.equals("Extension Attribute Count")) {
					value.setText(con.getValue("configurations,extension_attributes", "computer")[0]);
				}
			}
		});
		
		update.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selected = (String) options.getSelectedItem();
				if (selected.equals("JSS URL")) {
					con.updateXMLValue("jss_url", value.getText());
				} else if (selected.equals("JSS Username")) {
					con.updateXMLValue("jss_username", value.getText());
				} else if (selected.equals("JSS Password")) {
					con.updateXMLValue("jss_password", value.getText());
				} else if (selected.equals("Smart Group Criteria Count")) {
					con.updateXMLValue("smart_groups", value.getText());
				} else if (selected.equals("Extension Attribute Count")) {
					con.updateXMLValue("extension_attributes", value.getText());
				}
			}
		});
	}
	
	//Returns the JSS URL
	public String getURL() {
		return this.jssURL;
	}
	
	//Returns the JSS Username
	public String getUsername() {
		return this.jssUsername;
	}
	
	//Returns the JSS Password
	public String getPassword() {
		return this.jssPassword;
	}
	
}
