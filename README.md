# JSS Health Check Utility (Beta)

The JSS Health Check Utility is a lightweight tool for JSS administrators to run inside of their environments to perform automated health checks. It performs checks for things that commonly appear during scheduled health checks, like having too little RAM, expiring items, bad scripts, etc. Simply provide the tool the JSS URL, Username, and Password. The tool will perform API GETs to the JSS, get the JSS Summary, as well as run some commands on the system, and then display a report to the end user. Nothing is ever edited, only read. The goal of this tool is to help administrators identify issues with their environment before the issues arise. This tool was written in Java, to be cross platform and available to as many as possible. 
**This tool is provided "AS IS", therefore no support from JAMF Software is guaranteed.**

## Getting Started

The latest release will always be available in the "Releases" button above. (See screenshot)

![Download Help](http://i.imgur.com/M1bFLiq.png "Download Help")

The tool runs cross platform on Mac, Windows and headless Linux, with the Linux version using a text interface. It requires Java 7+. There is a single binary that runs across all platforms. A configuration XML file is read by the tool to find values to use in health calculation. The configuration file available for download in the releases tab contains general data, but it can be easily tweaked for your specific environment. Simply point the tool to the path to this file, on first run, and then everything is complete. (If the xml file is in the same directory as the jar, it will auto-discover the xml file). 

### Downloading and Running

* Grab (Download) the latest release of the healthcheck.jar as well as the config.xml. (Chrome may warn you that this .jar file may harm your computer, but this can be ignored. The tool never writes anything to either the system, or server.) 
* If you are Windows or Mac OS X simply double click the tool to open it. (If you have gatekeeper enabled, you may have to right click the .jar file, then select "Open")
* If you are running Linux, or would like more debugging available, run the tool with this command:

```
java -jar healthcheck.jar
```

Everything generated by the tool can be outputted by starting it with this command: 

```
java -jar healthcheck.jar > output.txt
```

If you would like to run the headless, text only version on OS X or Windows run the program with the -h flag:

```
java -jar healthcheck.jar -h
```

* Provide a JSS URL and user account that has, at least, full read permissions.
_jss url must include the full fqdn. EG https://jss.jamfsw.com:8443+
* Click "Begin Health Check"!

<img src="http://i.imgur.com/guI5zrS.png" width="50%" height="50%">

## What does the tool check for?
- **Below data is pulled from the system, and will only be accurate if ran on the server**
- The amount of free/max memory and the amount of free/max space on disk
- **Below data is pulled from the JSS Summary**
- Web App Directory, Java Vendor, Database size and large SQL Tables
- Password Requirement Strength, Clustering and Activation Code Expiration
- Change Management Info, Tomcat Info, Log Flushing Settings and Login/Logout Hook Info
- Checks for mismatching database tables
- OS, Java, and Tomcat Version
- **Below data is pulled from the JSS API**
- GETs Activation code to be displayed in the interface
- GETs the computer Check-In frequency and ensures it is not too high for the environment size
- GETs LDAP Server Information to Display 
- GETs GSX and Managed Preference profile information and checks if it is being used
- GETs Computer, Mobile and User group information and warns of high criteria count or nested smart groups
- GETs VPP Accounts and checks for expiring tokens
- GETs all of the scripts, and checks for the old binary version or several other unsafe commands
- GETs printer information, and warns if the printer has a large driver package
- GETs Mobile and Computer Extension Attributes and warns if there are a large number of them
- GETs Network Segments and the SMTP server to display to the user
- GETs all policies, and checks for policies that contain an update inventory with recurring checkin enabled

After all of this data is pulled, the tool will parse the data, and display important items to the end user. The items that relate to the system are not displayed with cloud hosted JSSs. 

### Changelog
v1.0-beta.3
* Several bug fixes
* Changes some wording in health check messages
* Adds additional checks to the headless version
* Adds more error checking

v1.0-beta.2
* Added checks for mismatching dabatabase tables
* Added more script checks
* Prompts user again when they select a directory that doesn't exist for test output
* Updates wording and error messages in several spots
* Error/Exception handling: the tool will continue to run, just without the data that caused the exception
* Gathers as much data as possible from the summary, instead of running system commands
* Other small bug fixes

v1.0-beta.1
* First Release

### Screenshots
![Screen One](http://i.imgur.com/Meu8rmm.png "Screen One")
![Screen Two](http://i.imgur.com/wDGidaO.png "Screen Two")
![Screen Three](http://i.imgur.com/S40Ni8T.png "Screen Three")

## Libraries Used and Acknowledgments

* Apache Commons Codec and Commons Lang
* Google JSON (gson)
* Java X JSON
* JDOM
* Java MySQL Connector

## Authors

* **Jake Schultz (JAMF Software)** -Development
* **Carlton Brumbelow (JAMF Software)** -Test Design and Interface 
* **Matt Hegge (JAMF Software)** -Test Design and Interface 

## Support

This tool is provided AS IS, and thus no support from JAMF Software, or any other entities is guaranteed. While in it's beta stage, please contact Jake Schultz with any questions or bugs. (jacob.schultz@jamfsoftware.com) Please include any output from the tool in any emails. 

## Source Code 

The source code for this tool is now being distributed freely. The code should be considered in a beta stage as well, and is subject to large changes before release. Please create pull requests for any bug fixes or additions/changes you may make. 

To compile you must have all of the required libraries downloaded including Apache Commons Codec, Commons Lang, GSON, JavaX JSON, JDOM and the Java MySQL connector. All of the source code lives in the /src directory. 

## Source Code TODO
* Redo the Interface with a Java -> Javascript bridge allowing the interface to be done with HTML5/Javascript/CSS
* Standarize Variable Names
* Refactor Panel Generation Code
* Finish Java Docs
* Clean up HealthCheck class
* Implement a library to generate the JSON 

## License

The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

