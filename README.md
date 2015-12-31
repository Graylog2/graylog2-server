# Graylog Message Processor Plugin

Getting started
---------------

This project is using Maven 3 and requires Java 8 or higher. The plugin will require Graylog 2.0.0 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart the Graylog.
