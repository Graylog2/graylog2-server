#!/bin/bash

BUILD_NUMBER=$1
BUILD_NAME=graylog2-server-$BUILD_NUMBER
BUILD_DIR=builds/$BUILD_NAME
BUILD_DATE=`date`
LOGFILE=`pwd`/logs/$BUILD_NAME

# Check if required version parameter is given
if [ -z $BUILD_NUMBER ]; then
  echo "ERROR: Missing parameter. (build number)"
  exit 1
fi

# Create directories
mkdir -p logs
mkdir -p builds
mkdir -p $BUILD_DIR

# Create logfile
touch $LOGFILE
date >> $LOGFILE

echo "BUILDING $BUILD_NAME"

# Add build date to release.
echo $BUILD_DATE > $BUILD_DIR/build_date

echo "Copying files ..."

# Copy files.
cp ../target/*-with-dependencies.jar ../README ../COPYING $BUILD_DIR -r

# Rename jar
mv $BUILD_DIR/*-with-dependencies.jar $BUILD_DIR/graylog2-server.jar

# Copy example config file
cp ../misc/graylog2.conf $BUILD_DIR/graylog2.conf.example

# Copy control script
cp copy/bin $BUILD_DIR -r

cd builds/

# tar it
echo "Building Tarball ..."
tar cfz $BUILD_NAME.tar.gz $BUILD_NAME
rm -rf ./$BUILD_NAME

echo "DONE! Created Graylog2 release $BUILD_NAME on $BUILD_DATE"
