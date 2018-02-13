# Map Widget Plugin for Graylog

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-map-widget.svg)](https://travis-ci.org/Graylog2/graylog-plugin-map-widget)

Provides a message filter plugin to lookup GeoIP information for IP addresses, and a map widget to display that location
information.

**Required Graylog version:** 2.0 and later

Installation
------------

This plugin is included by default in Graylog 2.0 tarballs and packages, so you
do not need to install it by hand.

For now we do not provide individual releases of the plugin, but you can still
find it inside the Graylog tarball available in the
[downloads page](https://www.graylog.org/download/). The plugin is located in
the plugin directory of the tarball.

Once you get the .jar file from the tarball, place it in your Graylog plugin
directory. The plugin directory is the plugins/ folder relative from your
graylog-server directory by default and can be configured in your graylog.conf file.

Restart graylog-server and you are done.

Development
-----------

You can improve your development experience for the web interface part of your plugin
dramatically by making use of hot reloading. To do this, do the following:

* `git clone https://github.com/Graylog2/graylog2-server.git`
* `cd graylog2-server/graylog2-web-interface`
* `ln -s $YOURPLUGIN plugin/`
* `npm install && npm start`

Getting started
---------------

This project is using Maven 3 and requires Java 7 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart the Graylog.

Plugin Release
--------------

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. Travis CI will build the release artifacts and upload to GitHub automatically.
