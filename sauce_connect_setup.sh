#!/bin/bash

ZIP_URL="http://saucelabs.com/downloads/Sauce-Connect-latest.zip"
DIR="/tmp/sauce-connect-$RANDOM"
DL="Sauce_Connect.zip"
READYFILE="connect-ready-$RANDOM"

mkdir -p $DIR
cd $DIR && curl $ZIP_URL > $DL
unzip $DL && rm $DL

java -jar Sauce-Connect.jar $SAUCE_USERNAME $SAUCE_ACCESS_KEY --readyfile $READYFILE --tunnel-identifier $TRAVIS_JOB_NUMBER &

# wait a bit for connect to start if starting succeedd
if [[ $rc == 0 ]] ; then
    while [ ! -f $READYFILE ]; do
      sleep 1
    done
fi