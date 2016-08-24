package com.jamfsoftware.jss.healthcheck.ui.component;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.*;

/**
 * This class handles creating a clickable JLabel. If the param text contains a URL, open in the browser, otherwise
 * display a help message on click.
 *
 * @author ludovicianul
 * @author Jacob Schultz
 * @since 1.0
 */
public class URLLabel extends JLabel {
	
	private final String url;
	
	public URLLabel(String label, String url) {
		super(label);
		this.url = url;
		setForeground(Color.BLUE.darker());
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		addMouseListener(new URLOpenAdapter());
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		g.setColor(Color.blue);
	}
	
	private class URLOpenAdapter extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (Desktop.isDesktopSupported()) {
				try {
					if (url.contains("http")) {
						Desktop.getDesktop().browse(new URI(url));
					} else {
						JOptionPane.showMessageDialog(null, url);
					}
				} catch (Exception i) {
					i.printStackTrace();
				}
			} else {
				JOptionPane.showMessageDialog(new JFrame(), "The utility will not be able to open links on your system.\nThis most often happens inside of VMs.\nPlease consider running on a different system, or copying the report for viewing.", "Java Desktop Unsupported", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
}