#!/bin/sh -e
BEATS_VERSION="1.2.3"

wget -q "https://download.elastic.co/beats/winlogbeat/winlogbeat-${BEATS_VERSION}-windows.zip"
unzip "winlogbeat-${BEATS_VERSION}-windows.zip"
