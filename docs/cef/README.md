# Graylog CEF message input

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-cef.svg?branch=master)](https://travis-ci.org/Graylog2/graylog-plugin-cef)

Graylog input plugin to receive CEF logs via UDP or TCP. Install the plugin and launch a new CEF input from `System -> Inputs` in your Graylog Web Interface.

This plugin is strictly following the CEF standard and will probably not work with non-compliant messages. Please open an issue in this repository in case of any problems.

![](https://github.com/Graylog2/graylog-plugin-cef/blob/master/screenshot.png)

**Required Graylog version:** 2.4.x and later

## Installation
> Since Graylog Version 2.4.0 this plugin is already included in the Graylog server installation package as default plugin.

[Download the plugin](https://github.com/Graylog2/graylog-plugin-cef/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Usage

### Reading CEF embedded in syslog envelopes

Some systems will send CEF as part of a RFC compliant syslog message. In this case, you can just start a CEF Message Input from `System -> Inputs` and you are done.

### Parsing raw CEF or CEF embedded in any other envelopes

If the envelope is not syslog or the CEF message is not in an envelope at all, you can use the [Graylog Processing Pipelines](http://docs.graylog.org/en/latest/pages/pipelines.html) and the `parse_cef` function this plugin provides:

1. Use a pipeline rule to parse out the CEF part of the message (for example, using regex) and then apply the `parse_cef()` function on that extracted string.
1. If desired, use a second pipeline step to rename the `cef_` prefixed message fields to something easier to use and easier to remember.

## Development

This project is using Maven 3 and requires Java 8 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart Graylog.

## Plugin Release

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. Travis CI will build the release artifacts and upload to GitHub automatically.
