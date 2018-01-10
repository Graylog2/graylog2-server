NetFlow Plugin for Graylog
==========================

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-netflow.svg?branch=master)](https://travis-ci.org/Graylog2/graylog-plugin-netflow)

This plugin provides a NetFlow UDP input to act as a Flow collector that receives data from Flow exporters. Each received Flow will be converted to a Graylog message.

**Required Graylog version:** 2.3.0 and later

## Supported NetFlow Versions

The version of the plugin now supports NetFlow V9.  It can support IPv6 addresses without 
conversion and handles all of the fields from the fixed V5 format.  In addition this plugin supports
events from a CISCO ASA 5500, including firewall and routing events.  Beware, there is significant
duplication of typical syslog reporting in the v9 reporting. 

## Installation
> Since Graylog Version 2.4.0 this plugin is already included in the Graylog server installation package as default plugin.

[Download the plugin](https://github.com/Graylog2/graylog-plugin-netflow/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Setup

In the Graylog web interface, go to System/Inputs and create a new NetFlow input like this:

![NetFlow input creation dialog](https://github.com/Graylog2/graylog-plugin-netflow/blob/master/images/netflow-udp-input-1.png)

## Example Message

This is an example NetFlow message in Graylog:

![NetFlow example fields screenshot](https://github.com/Graylog2/graylog-plugin-netflow/blob/master/images/netflow-example.png)

## Example Dashboard

This is an example of a dashboard with NetFlow data:

![NetFlow example dashboard screenshot](https://github.com/Graylog2/graylog-plugin-netflow/blob/master/images/netflow-dashboard.png)

## Credits

The NetFlow parsing code is based on the https://github.com/wasted/netflow project and has been ported from Scala to Java.

## Plugin Development

### Testing

To generate some NetFlow data for debugging and testing you can use softflowd.

Example command and output:

```
# softflowd -D -i eth0 -v 5 -t maxlife=1 -n 10.0.2.2:2055

Using eth0 (idx: 0)
softflowd v0.9.9 starting data collection
Exporting flows to [10.0.2.2]:2055
ADD FLOW seq:1 [10.0.2.2]:48164 <> [10.0.2.15]:22 proto:6
ADD FLOW seq:2 [10.0.2.2]:51428 <> [10.0.2.15]:22 proto:6
Starting expiry scan: mode 0
Queuing flow seq:1 (0x7fef0318bc70) for expiry reason 6
Finished scan 1 flow(s) to be evicted
Sending v5 flow packet len = 120
sent 1 netflow packets
EXPIRED: seq:1 [10.0.2.2]:48164 <> [10.0.2.15]:22 proto:6 octets>:322 packets>:7 octets<:596 packets<:7 start:2015-07-21T13:18:01.236 finish:2015-07-21T13:18:27.718 tcp>:10 tcp<:18 flowlabel>:00000000 flo
wlabel<:00000000  (0x7fef0318bc70)
ADD FLOW seq:3 [10.0.2.2]:2055 <> [10.0.2.15]:48363 proto:17
ADD FLOW seq:4 [10.0.2.2]:48164 <> [10.0.2.15]:22 proto:6
```

## Plugin Release

We are using the Maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. Travis CI will build the release artifacts and upload to GitHub automatically.
