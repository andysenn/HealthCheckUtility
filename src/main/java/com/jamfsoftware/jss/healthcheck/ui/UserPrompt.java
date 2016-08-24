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
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.apple.eawt.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jamfsoftware.jss.healthcheck.HealthCheck;
import com.jamfsoftware.jss.healthcheck.JSSConnectionTest;
import com.jamfsoftware.jss.healthcheck.controller.ConfigurationController;
import com.jamfsoftware.jss.healthcheck.report.impl.HealthReportAWT;
import com.jamfsoftware.jss.healthcheck.ui.component.model.CSVElement;
import com.jamfsoftware.jss.healthcheck.util.EnvironmentUtil;
import com.jamfsoftware.jss.healthcheck.util.StringConstants;

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
	public UserPrompt() {
		if (EnvironmentUtil.isMac()) {
			// Add a menu bar, and set the icon on OSX.
			// Application.getApplication().setDockIconImage(new ImageIcon(this.getClass().getResource("/images/icon.png")).getImage());
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
			edit.addActionListener(e -> loadXMLEditor());
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
				if (test.isHosted()) {
					mysql.setText("<html>Unable to perform system<br> checks on a hosted JSS.</html>");
					mysql_username.setEnabled(false);
					mysql_password.setEnabled(false);
				}
				//Start a new thread to handle updating the health check button.
				Thread m = new Thread(() -> {
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
				});
				
				//Start the thread and timers
				m.start();
				
				//Start another new thread to start the Health Check.
				Thread t = new Thread(() -> performHealthCheck(url, username, password, frame, begin_check));
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
	//If the Health Check object is created without errors, it then creates a new HealthReportAWT object.
	private void performHealthCheck(JTextField url, JTextField username, JTextField password, JFrame frame, JButton button) {
		jssURL = url.getText();
		jssUsername = username.getText();
		jssPassword = password.getText();
		
		try {
			HealthCheck healthCheck = new HealthCheck(jssURL, jssUsername, jssPassword, false);
			LOGGER.info("Health Check Complete, Loading Summary..");
			new HealthReportAWT(healthCheck.getJSONAsString());
			LOGGER.info("Report loaded.");
			frame.setVisible(false);
		} catch (Exception e) {
			LOGGER.error("", e);
			JOptionPane.showMessageDialog(new JFrame(), "A fatal error has occurred. \n" + e, "Fatal Error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}
	
	//This method opens the options menu. It allows the config.xml path to be set after opening the program.
	private void openOptions() {
		JPanel pnlRoot = new JPanel();
		pnlRoot.add(new JLabel("Configuration XML path:"));
		
		JFrame frmOptions = new JFrame("Health Check Options");
		JTextField txtConfigurationPath = new JTextField();
		
		//Load xml path from saved prefs.
		String configurationPath = this.prefs.get("config_xml_path", StringConstants.DEFAULT_CONFIGURATION_PATH);
		txtConfigurationPath.setText(configurationPath);
		
		pnlRoot.add(txtConfigurationPath);
		
		JButton btnSavePath = new JButton("Save Path");
		pnlRoot.add(btnSavePath);
		pnlRoot.add(new JLabel(""));
		
		JButton btnLoadPreviousTest = new JButton("Load Previous Test");
		pnlRoot.add(btnLoadPreviousTest);
		
		frmOptions.add(pnlRoot);
		frmOptions.setSize(530, 100);
		frmOptions.setLocationRelativeTo(null);
		frmOptions.setVisible(true);
		
		btnSavePath.addActionListener(e -> {
			ConfigurationController con = new ConfigurationController(false);
			if (con.canGetFile(txtConfigurationPath.getText())) {
				prefs.put("config_xml_path", txtConfigurationPath.getText());
			} else {
				txtConfigurationPath.setText(prefs.get("config_xml_path", StringConstants.DEFAULT_CONFIGURATION_PATH));
				JOptionPane.showMessageDialog(frmOptions, "This is not a valid configuration XML file. \nNo changes have been made.", "XML Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
		btnLoadPreviousTest.addActionListener(e -> loadJSON());
	}
	
	//This method opens a JPanel to load previous test JSON into.
	//If the JSON is valid, it will open a new health report window.
	private void loadJSON() {
		final JPanel middlePanel = new JPanel();
		middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "Load JSON from a previous test"));
		// create the middle panel components
		final JTextArea display = new JTextArea(16, 58);
		display.setEditable(true);
		JScrollPane scroll = new JScrollPane(display);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		//Add Textarea in to middle panel
		middlePanel.add(scroll);
		JButton loadReport
				= new JButton("Open Health Report");
		middlePanel.add(loadReport);
		
		JFrame frame = new JFrame();
		frame.add(middlePanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		loadReport.addActionListener(e -> {
			try {
				new HealthReportAWT(display.getText());
			} catch (Exception e1) {
				LOGGER.error("", e1);
				JOptionPane.showMessageDialog(middlePanel, "The tool was unable to load the pasted JSON.\nIt may be incomplete or not formatted correctly.\nThe error the tool encountered:\n" + e1, "Error Loading JSON", JOptionPane.ERROR_MESSAGE);
			}
		});
	}
	
	private void loadAllHelpLinks() {
		ConfigurationController config = new ConfigurationController(true);
		
		JTextArea display = new JTextArea(16, 58);
		display.setEditable(false);
		display.setText(String.format(
				StringConstants.OPTIONS_TEXT,
				config.getValue("configurations,smart_groups", "criteria_count")[0],
				this.jssURL
		));
		
		JScrollPane scroll = new JScrollPane(display);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		JPanel middlePanel = new JPanel();
		middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "All available JSS Health Checks and Help Information"));
		middlePanel.add(scroll);
		
		JFrame frame = new JFrame();
		frame.add(middlePanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	public void loadXMLEditor() {
		ConfigurationController con = new ConfigurationController(true);
		
		JTextField txtValue = new JTextField();
		
		JComboBox<CSVElement> cmbOptions = new JComboBox<>(new CSVElement[] {
				new CSVElement("JSS URL", "healthcheck", "jss_url"),
				new CSVElement("JSS Username", "healthcheck", "jss_username"),
				new CSVElement("JSS Password", "healthcheck", "jss_password"),
				new CSVElement("Smart Group Criteria Count", "configurations,smart_groups", "criteria_count"),
				new CSVElement("Extension Attribute Count", "configurations,extension_attributes", "computer")
		});
		
		cmbOptions.addActionListener(e -> {
			int index = cmbOptions.getSelectedIndex();
			CSVElement element = cmbOptions.getItemAt(index);
			
			txtValue.setText(con.getValue(element.getCSVPaths(), element.getCSVKeys())[0]);
		});
		
		cmbOptions.setSelectedIndex(0);
		
		// TODO: Not Implemented
		JButton btnUpdate = new JButton("Update XML Value");
		
		JPanel pnlRoot = new JPanel();
		pnlRoot.setBorder(new TitledBorder(new EtchedBorder(), "Edit Configuration XML File"));
		pnlRoot.add(cmbOptions);
		pnlRoot.add(txtValue);
		pnlRoot.add(btnUpdate);
		
		JFrame frame = new JFrame();
		frame.add(pnlRoot);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
}
